package com.carol.backend.util;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * TTS分段处理工具类
 * 解决DashScope TTS 600字符硬性上限问题
 */
@Slf4j
@Component
public class TtsSegmentUtil {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    // TTS并发处理线程池
    private final ThreadPoolTaskExecutor ttsExecutor;

    public TtsSegmentUtil() {
        this.ttsExecutor = createTtsThreadPool();
    }

    /**
     * 创建TTS专用线程池
     */
    private ThreadPoolTaskExecutor createTtsThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("TTS-Segment-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 智能分段：按句号、感叹号、问号切分，确保每段≤580字符 // 580 字符上限
     */
    public List<String> splitText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> segments = new ArrayList<>();
        String[] sentences = text.split("(?<=[。！？])"); // 按句号、感叹号、问号分割
        
        StringBuilder currentSegment = new StringBuilder();
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            // 如果单句就超过580字符，需要进一步切分
            if (sentence.length() > 580) {
                // 先保存当前段落
                if (currentSegment.length() > 0) {
                    segments.add(currentSegment.toString().trim());
                    currentSegment.setLength(0);
                }
                
                // 对超长句子按逗号、分号继续分割
                List<String> subSentences = splitLongSentence(sentence);
                segments.addAll(subSentences);
            } else {
                // 检查加入当前句子后是否会超过限制
                if (currentSegment.length() + sentence.length() > 580) {
                    // 保存当前段落，开始新段落
                    if (currentSegment.length() > 0) {
                        segments.add(currentSegment.toString().trim());
                        currentSegment.setLength(0);
                    }
                }
                
                currentSegment.append(sentence);
            }
        }
        
        // 添加最后一个段落
        if (currentSegment.length() > 0) {
            segments.add(currentSegment.toString().trim());
        }
        
        log.info("文本分段完成: 原文{}字符 → {}个段落", text.length(), segments.size());
        return segments;
    }

    /**
     * 处理超长句子：按逗号、分号分割
     */
    private List<String> splitLongSentence(String sentence) {
        List<String> parts = new ArrayList<>();
        String[] subParts = sentence.split("(?<=[，；,;])");
        
        StringBuilder currentPart = new StringBuilder();
        for (String part : subParts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            if (currentPart.length() + part.length() > 580) {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString().trim());
                    currentPart.setLength(0);
                }
                
                // 如果单个部分仍然太长，强制按字符切分
                if (part.length() > 580) {
                    for (int i = 0; i < part.length(); i += 580) {
                        int end = Math.min(i + 580, part.length());
                        parts.add(part.substring(i, end));
                    }
                } else {
                    currentPart.append(part);
                }
            } else {
                currentPart.append(part);
            }
        }
        
        if (currentPart.length() > 0) {
            parts.add(currentPart.toString().trim());
        }
        
        return parts;
    }

    /**
     * 单段TTS合成
     */
    public byte[] synthesizeSingleSegment(String text, String voice, String model, String languageType) 
            throws ApiException, NoApiKeyException, UploadFileException {
        
        log.debug("开始单段TTS合成: {}字符, 音色={}", text.length(), voice);
        
        com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice voiceEnum = 
            getVoiceEnum(voice);
        
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(model)
                .apiKey(apiKey)  // 关键：直接传入API密钥
                .text(text)
                .voice(voiceEnum)
                .languageType(languageType)
                .build();
        
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationResult result = conv.call(param);
        
        if (result == null || result.getOutput() == null || result.getOutput().getAudio() == null) {
            throw new RuntimeException("TTS段落合成失败: 返回结果为空");
        }
        
        // 下载音频数据
        String audioUrl = result.getOutput().getAudio().getUrl();
        return downloadAudioBytes(audioUrl);
    }

    /**
     * 并发处理多段TTS合成
     */
    public byte[] synthesizeSegmentsConcurrently(List<String> segments, String voice, String model, String languageType) {
        log.info("开始并发TTS合成: {}个段落", segments.size());
        
        List<CompletableFuture<byte[]>> futures = segments.stream()
                .map(segment -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return synthesizeSingleSegment(segment, voice, model, languageType);
                    } catch (Exception e) {
                        log.error("段落TTS合成失败: {}", e.getMessage(), e);
                        throw new RuntimeException("段落TTS合成失败", e);
                    }
                }, ttsExecutor))
                .toList();
        
        // 等待所有段落完成
        List<byte[]> audioSegments = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        
        log.info("并发TTS合成完成，开始音频拼接");
        return concatenateWavFiles(audioSegments);
    }

    /**
     * 拼接多个WAV音频文件
     */
    public byte[] concatenateWavFiles(List<byte[]> audioSegments) {
        if (audioSegments.isEmpty()) {
            throw new RuntimeException("无音频段落可拼接");
        }
        
        if (audioSegments.size() == 1) {
            return audioSegments.get(0);
        }
        
        try {
            // 读取第一个音频作为基准
            AudioInputStream baseStream = AudioSystem.getAudioInputStream(
                new ByteArrayInputStream(audioSegments.get(0)));
            AudioFormat format = baseStream.getFormat();
            
            ByteArrayOutputStream concatenatedStream = new ByteArrayOutputStream();
            
            // 写入WAV头（先写入第一个文件）
            concatenatedStream.write(audioSegments.get(0));
            
            // 追加后续音频数据（跳过WAV头）
            for (int i = 1; i < audioSegments.size(); i++) {
                AudioInputStream nextStream = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(audioSegments.get(i)));
                
                // 跳过WAV头，只读取音频数据
                byte[] buffer = new byte[1024];
                while (nextStream.read(buffer) != -1) {
                    concatenatedStream.write(buffer);
                }
                nextStream.close();
                
                // 在段落间添加0.3秒静音
                addSilence(concatenatedStream, format, 0.3);
            }
            
            baseStream.close();
            log.info("音频拼接完成: {}个段落 → {}字节", audioSegments.size(), concatenatedStream.size());
            return concatenatedStream.toByteArray();
            
        } catch (Exception e) {
            log.error("音频拼接失败: {}", e.getMessage(), e);
            throw new RuntimeException("音频拼接失败", e);
        }
    }

    /**
     * 添加静音间隔
     */
    private void addSilence(ByteArrayOutputStream stream, AudioFormat format, double seconds) {
        int silenceSamples = (int) (format.getSampleRate() * seconds);
        int bytesPerSample = format.getSampleSizeInBits() / 8 * format.getChannels();
        byte[] silence = new byte[silenceSamples * bytesPerSample];
        stream.write(silence, 0, silence.length);
    }

    /**
     * 下载音频字节数据
     */
    private byte[] downloadAudioBytes(String audioUrl) throws RuntimeException {
        try (InputStream inputStream = new URL(audioUrl).openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("下载音频失败: url={}, error={}", audioUrl, e.getMessage());
            throw new RuntimeException("下载音频失败", e);
        }
    }

    /**
     * 将字符串音色转换为Voice枚举
     */
    private com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice getVoiceEnum(String voiceName) {
        try {
            return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.valueOf(voiceName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知音色: {}, 使用默认音色Cherry", voiceName);
            return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.CHERRY;
        }
    }
}
