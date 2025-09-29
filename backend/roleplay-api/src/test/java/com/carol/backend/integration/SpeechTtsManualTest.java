package com.carol.backend.integration;

import com.carol.backend.dto.SpeechRecognitionRequest;
import com.carol.backend.dto.SpeechRecognitionResponse;
import com.carol.backend.dto.TtsSynthesisRequest;
import com.carol.backend.dto.TtsSynthesisResponse;
import com.carol.backend.service.SpeechRecognitionService;
import com.carol.backend.service.TtsSynthesisService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * 语音合成和识别手动测试工具
 * 
 * 运行此类可以进行交互式的语音转换测试：
 * 1. 输入文本
 * 2. 选择音色和语言
 * 3. TTS合成音频
 * 4. ASR识别音频
 * 5. 比较结果
 * 
 * 使用方法：
 * mvn spring-boot:run -Dspring-boot.run.main-class=com.carol.backend.integration.SpeechTtsManualTest
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "com.carol.backend")
public class SpeechTtsManualTest implements CommandLineRunner {

    @Autowired
    private TtsSynthesisService ttsSynthesisService;
    
    @Autowired
    private SpeechRecognitionService speechRecognitionService;
    
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "test");
        SpringApplication.run(SpeechTtsManualTest.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("🎤🔊 语音合成和识别手动测试工具启动");
        log.info("此工具将帮助您测试完整的语音转换流程");
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            try {
                System.out.println("\n" + "=".repeat(60));
                System.out.println("🎯 语音转换测试菜单");
                System.out.println("1. 快速测试（使用默认参数）");
                System.out.println("2. 自定义测试（选择音色和语言）");
                System.out.println("3. 批量测试（预设测试用例）");
                System.out.println("4. 角色语音测试");
                System.out.println("5. 退出");
                System.out.print("请选择操作 (1-5): ");
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        quickTest(scanner);
                        break;
                    case "2":
                        customTest(scanner);
                        break;
                    case "3":
                        batchTest();
                        break;
                    case "4":
                        characterTest(scanner);
                        break;
                    case "5":
                        System.out.println("👋 测试工具退出，再见！");
                        return;
                    default:
                        System.out.println("❌ 无效选择，请重新输入");
                }
                
            } catch (Exception e) {
                log.error("测试过程中出现错误", e);
                System.out.println("❌ 测试失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 快速测试
     */
    private void quickTest(Scanner scanner) throws Exception {
        System.out.print("📝 请输入要测试的文本: ");
        String text = scanner.nextLine().trim();
        
        if (text.isEmpty()) {
            System.out.println("❌ 文本不能为空");
            return;
        }
        
        // 使用默认参数
        TestResult result = executeSpeechConversion(text, "Cherry", "Chinese", null);
        printTestResult(result);
    }
    
    /**
     * 自定义测试
     */
    private void customTest(Scanner scanner) throws Exception {
        System.out.print("📝 请输入要测试的文本: ");
        String text = scanner.nextLine().trim();
        
        if (text.isEmpty()) {
            System.out.println("❌ 文本不能为空");
            return;
        }
        
        // 显示可用音色
        System.out.println("\n🎵 可用音色:");
        System.out.println("1. Cherry (芊悦) - 阳光积极小姐姐");
        System.out.println("2. Ethan (晨煦) - 阳光温暖男性");
        System.out.println("3. Jennifer (詹妮弗) - 电影质感美语女声");
        System.out.println("4. Elias (墨讲师) - 学科严谨讲师");
        System.out.println("5. Marcus (秦川) - 沉稳男性");
        System.out.print("请选择音色 (1-5): ");
        
        String voiceChoice = scanner.nextLine().trim();
        String voice = getVoiceByChoice(voiceChoice);
        
        // 选择语言
        System.out.println("\n🌍 支持语言:");
        System.out.println("1. Chinese (中文)");
        System.out.println("2. English (英文)");
        System.out.print("请选择语言 (1-2): ");
        
        String langChoice = scanner.nextLine().trim();
        String language = langChoice.equals("2") ? "English" : "Chinese";
        
        TestResult result = executeSpeechConversion(text, voice, language, null);
        printTestResult(result);
    }
    
    /**
     * 批量测试
     */
    private void batchTest() throws Exception {
        System.out.println("🔄 开始批量测试...");
        
        String[] testTexts = {
            "你好，这是一个语音转换测试",
            "人工智能技术正在快速发展",
            "今天天气很好，适合出门游玩",
            "Hello, this is an AI speech test",
            "Welcome to the world of artificial intelligence"
        };
        
        String[] voices = {"Cherry", "Ethan", "Jennifer", "Elias", "Marcus"};
        String[] languages = {"Chinese", "Chinese", "Chinese", "English", "English"};
        
        for (int i = 0; i < testTexts.length; i++) {
            System.out.println(String.format("\n📋 批量测试 %d/%d", i + 1, testTexts.length));
            
            TestResult result = executeSpeechConversion(
                testTexts[i], voices[i % voices.length], languages[i % languages.length], null);
            
            printTestResult(result);
            
            // 添加延迟
            Thread.sleep(1000);
        }
        
        System.out.println("✅ 批量测试完成");
    }
    
    /**
     * 角色语音测试
     */
    private void characterTest(Scanner scanner) throws Exception {
        System.out.println("\n🎭 角色语音测试");
        System.out.println("1. 哈利·波特 (ID: 1)");
        System.out.println("2. 苏格拉底 (ID: 2)");
        System.out.println("3. 爱因斯坦 (ID: 3)");
        System.out.println("4. 江户川柯南 (ID: 4)");
        System.out.println("5. 泰拉瑞亚向导 (ID: 5)");
        System.out.print("请选择角色 (1-5): ");
        
        String choice = scanner.nextLine().trim();
        Long characterId = Long.valueOf(choice);
        
        System.out.print("📝 请输入要合成的文本: ");
        String text = scanner.nextLine().trim();
        
        if (text.isEmpty()) {
            System.out.println("❌ 文本不能为空");
            return;
        }
        
        TestResult result = executeSpeechConversion(text, null, "Chinese", characterId);
        printTestResult(result);
    }
    
    /**
     * 执行语音转换测试
     */
    private TestResult executeSpeechConversion(String text, String voice, String language, Long characterId) throws Exception {
        TestResult result = new TestResult();
        result.originalText = text;
        result.startTime = LocalDateTime.now();
        
        try {
            System.out.println("\n🎯 开始语音转换测试");
            System.out.println("原始文本: " + text);
            
            // 步骤1: TTS合成
            System.out.println("📢 步骤1: 正在进行TTS语音合成...");
            
            TtsSynthesisResponse ttsResponse;
            if (characterId != null) {
                // 使用角色合成
                ttsResponse = ttsSynthesisService.synthesizeForCharacter(text, characterId, language);
                result.voice = ttsResponse.getVoice();
            } else {
                // 使用指定音色合成
                TtsSynthesisRequest ttsRequest = TtsSynthesisRequest.builder()
                    .text(text)
                    .voice(voice)
                    .languageType(language)
                    .model("qwen3-tts-flash")
                    .build();
                
                ttsResponse = ttsSynthesisService.synthesizeText(ttsRequest);
                result.voice = voice;
            }
            
            if (!ttsResponse.getSuccess()) {
                throw new Exception("TTS合成失败: " + ttsResponse.getErrorMessage());
            }
            
            result.ttsSuccess = true;
            result.audioUrl = ttsResponse.getAudioUrl();
            result.duration = ttsResponse.getDuration();
            result.ttsTime = ttsResponse.getProcessingTime();
            
            System.out.println("✅ TTS合成成功");
            System.out.println("   音色: " + ttsResponse.getVoice());
            System.out.println("   时长: " + ttsResponse.getDuration() + "秒");
            System.out.println("   处理时间: " + ttsResponse.getProcessingTime() + "ms");
            System.out.println("   音频URL: " + ttsResponse.getAudioUrl());
            
            // 步骤2: 下载音频文件
            System.out.println("⬇️  步骤2: 正在下载音频文件...");
            File audioFile = downloadAudioFile(ttsResponse.getAudioUrl());
            System.out.println("✅ 音频文件下载完成: " + audioFile.getName());
            
            // 步骤3: ASR识别
            System.out.println("🎤 步骤3: 正在进行ASR语音识别...");
            
            MockMultipartFile multipartFile = new MockMultipartFile(
                "audio",
                audioFile.getName(),
                "audio/wav",
                Files.readAllBytes(audioFile.toPath())
            );
            
            SpeechRecognitionRequest asrRequest = SpeechRecognitionRequest.builder()
                .model("fun-asr-realtime")
                .format("wav")
                .sampleRate(24000)
                .punctuationPredictionEnabled(true)
                .build();
            
            SpeechRecognitionResponse asrResponse = speechRecognitionService.recognizeFile(multipartFile, asrRequest);
            
            result.asrSuccess = true;
            result.recognizedText = asrResponse.getText();
            result.asrRequestId = asrResponse.getRequestId();
            
            System.out.println("✅ ASR识别成功");
            System.out.println("   识别文本: " + asrResponse.getText());
            System.out.println("   请求ID: " + asrResponse.getRequestId());
            
            // 计算相似度
            result.similarity = calculateSimilarity(text, asrResponse.getText());
            
            // 清理临时文件
            audioFile.delete();
            
        } catch (Exception e) {
            result.errorMessage = e.getMessage();
            throw e;
        } finally {
            result.endTime = LocalDateTime.now();
            result.totalTime = java.time.Duration.between(result.startTime, result.endTime).toMillis();
        }
        
        return result;
    }
    
    /**
     * 下载音频文件
     */
    private File downloadAudioFile(String audioUrl) throws Exception {
        URL url = new URL(audioUrl);
        File tempFile = File.createTempFile("tts_test_", ".wav");
        
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile;
    }
    
    /**
     * 计算文本相似度
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        String clean1 = text1.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
        String clean2 = text2.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
        
        if (clean1.equals(clean2)) return 1.0;
        if (clean1.isEmpty() || clean2.isEmpty()) return 0.0;
        
        int distance = levenshteinDistance(clean1, clean2);
        int maxLength = Math.max(clean1.length(), clean2.length());
        
        return 1.0 - (double) distance / maxLength;
    }
    
    /**
     * 字符串居中显示辅助方法
     */
    private String centerString(String text, int width, char fillChar) {
        if (text.length() >= width) {
            return text;
        }
        
        int leftPadding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - leftPadding;
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < leftPadding; i++) {
            sb.append(fillChar);
        }
        sb.append(text);
        for (int i = 0; i < rightPadding; i++) {
            sb.append(fillChar);
        }
        
        return sb.toString();
    }
    
    /**
     * 编辑距离算法
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * 根据选择获取音色
     */
    private String getVoiceByChoice(String choice) {
        switch (choice) {
            case "1": return "Cherry";
            case "2": return "Ethan";
            case "3": return "Jennifer";
            case "4": return "Elias";
            case "5": return "Marcus";
            default: return "Cherry";
        }
    }
    
    /**
     * 打印测试结果
     */
    private void printTestResult(TestResult result) {
        System.out.println("\n" + centerString("📊 测试结果报告", 60, '='));
        System.out.println("🕐 测试时间: " + result.startTime);
        System.out.println("📝 原始文本: " + result.originalText);
        System.out.println("🔊 使用音色: " + result.voice);
        System.out.println("📢 TTS状态: " + (result.ttsSuccess ? "✅ 成功" : "❌ 失败"));
        
        if (result.ttsSuccess) {
            System.out.println("   └─ 音频时长: " + result.duration + "秒");
            System.out.println("   └─ 处理时间: " + result.ttsTime + "ms");
        }
        
        System.out.println("🎤 ASR状态: " + (result.asrSuccess ? "✅ 成功" : "❌ 失败"));
        
        if (result.asrSuccess) {
            System.out.println("   └─ 识别文本: " + result.recognizedText);
            System.out.println("   └─ 请求ID: " + result.asrRequestId);
        }
        
        if (result.ttsSuccess && result.asrSuccess) {
            System.out.println("📈 文本相似度: " + String.format("%.2f%%", result.similarity * 100));
            
            if (result.similarity >= 0.9) {
                System.out.println("🎉 评估结果: 优秀 - 语音转换质量很高！");
            } else if (result.similarity >= 0.7) {
                System.out.println("👍 评估结果: 良好 - 语音转换基本准确");
            } else if (result.similarity >= 0.5) {
                System.out.println("⚠️  评估结果: 一般 - 语音转换有一定偏差");
            } else {
                System.out.println("❌ 评估结果: 较差 - 语音转换质量需要改进");
            }
        }
        
        System.out.println("⏱️  总耗时: " + result.totalTime + "ms");
        
        if (result.errorMessage != null) {
            System.out.println("❌ 错误信息: " + result.errorMessage);
        }
        
        System.out.println("=".repeat(60));
    }
    
    /**
     * 测试结果数据类
     */
    static class TestResult {
        String originalText;
        String recognizedText;
        String voice;
        boolean ttsSuccess;
        boolean asrSuccess;
        String audioUrl;
        Double duration;
        Long ttsTime;
        String asrRequestId;
        double similarity;
        String errorMessage;
        LocalDateTime startTime;
        LocalDateTime endTime;
        long totalTime;
    }
}
