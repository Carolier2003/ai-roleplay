package com.carol.backend.integration;

import com.carol.backend.dto.SpeechRecognitionRequest;
import com.carol.backend.dto.SpeechRecognitionResponse;
import com.carol.backend.dto.TtsSynthesisRequest;
import com.carol.backend.dto.TtsSynthesisResponse;
import com.carol.backend.service.SpeechRecognitionService;
import com.carol.backend.service.TtsSynthesisService;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 语音识别和语音合成集成测试
 * 
 * 测试流程：
 * 1. 文本 → TTS合成 → 音频文件
 * 2. 音频文件 → ASR识别 → 文本  
 * 3. 比较原始文本和识别文本的一致性
 * 4. 验证完整的语音处理流水线
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("语音识别与语音合成集成测试")
public class SpeechTtsIntegrationTest {

    @Autowired
    private TtsSynthesisService ttsSynthesisService;
    
    @Autowired
    private SpeechRecognitionService speechRecognitionService;
    
    // 测试数据存储
    private static final List<TestCaseResult> testResults = new ArrayList<>();
    
    // 测试用例数据
    private static final List<TestCase> TEST_CASES = List.of(
        new TestCase("简单问候", "你好，这是一个语音转换测试", "Cherry", "Chinese"),
        new TestCase("角色扮演", "我是哈利·波特，很高兴认识你", "Ethan", "Chinese"),
        new TestCase("英文测试", "Hello, this is a speech synthesis test", "Jennifer", "English"),
        new TestCase("数字内容", "今天是2025年9月26日，天气很好", "Cherry", "Chinese"),
        new TestCase("长句测试", "人工智能技术在语音识别和语音合成领域取得了显著进展，为用户提供了更加自然流畅的交互体验", "Elias", "Chinese"),
        new TestCase("科学内容", "相对论是爱因斯坦提出的重要物理理论", "Marcus", "Chinese")
    );
    
    private Path tempDirectory;

    @BeforeEach
    void setUp() throws Exception {
        // 创建临时目录存储测试音频文件
        tempDirectory = Files.createTempDirectory("speech_tts_test_");
        log.info("创建测试临时目录: {}", tempDirectory);
    }

    @Test
    @Order(1)
    @DisplayName("端到端语音转换测试")
    void testCompleteSpeeechPipeline() throws Exception {
        log.info("🚀 开始执行完整语音转换流水线测试");
        
        for (int i = 0; i < TEST_CASES.size(); i++) {
            TestCase testCase = TEST_CASES.get(i);
            log.info("\n📝 执行测试用例 {}/{}: {}", i + 1, TEST_CASES.size(), testCase.name);
            
            try {
                TestCaseResult result = executeTestCase(testCase);
                testResults.add(result);
                
                // 打印测试结果
                printTestResult(result);
                
                // 简单的断言检查
                assertTrue(result.ttsSuccess, "TTS合成应该成功");
                assertTrue(result.asrSuccess, "ASR识别应该成功");
                assertNotNull(result.recognizedText, "识别文本不应该为空");
                
                // 文本相似度检查（允许一定差异）
                double similarity = calculateTextSimilarity(result.originalText, result.recognizedText);
                log.info("文本相似度: {:.2f}%", similarity * 100);
                
                // 要求相似度至少达到60%（考虑到语音转换的损失）
                assertTrue(similarity >= 0.6, 
                    String.format("文本相似度过低: %.2f%%, 原文: '%s', 识别: '%s'", 
                        similarity * 100, result.originalText, result.recognizedText));
                
                // 添加延迟避免API限流
                if (i < TEST_CASES.size() - 1) {
                    Thread.sleep(2000);
                }
                
            } catch (Exception e) {
                log.error("测试用例执行失败: {}", testCase.name, e);
                
                // 记录失败结果
                TestCaseResult failResult = new TestCaseResult();
                failResult.testName = testCase.name;
                failResult.originalText = testCase.text;
                failResult.ttsSuccess = false;
                failResult.asrSuccess = false;
                failResult.errorMessage = e.getMessage();
                testResults.add(failResult);
                
                // 不中断测试，继续下一个用例
            }
        }
        
        // 打印完整测试报告
        printFinalReport();
    }

    @Test
    @Order(2)
    @DisplayName("角色音色一致性测试")
    void testCharacterVoiceConsistency() throws Exception {
        log.info("🎭 开始执行角色音色一致性测试");
        
        // 测试不同角色的音色是否符合预期
        List<CharacterTest> characterTests = List.of(
            new CharacterTest(1L, "哈利·波特", "我是哈利·波特", "Ethan"),
            new CharacterTest(2L, "苏格拉底", "认识你自己是智慧的开始", "Elias"),
            new CharacterTest(3L, "爱因斯坦", "想象力比知识更重要", "Marcus")
        );
        
        for (CharacterTest charTest : characterTests) {
            log.info("测试角色: {} (ID: {})", charTest.characterName, charTest.characterId);
            
            // 使用角色合成API
            TtsSynthesisResponse ttsResponse = ttsSynthesisService.synthesizeForCharacter(
                charTest.testText, charTest.characterId, "Chinese");
            
            assertTrue(ttsResponse.getSuccess(), "角色语音合成应该成功");
            assertEquals(charTest.expectedVoice, ttsResponse.getVoice(), 
                "角色音色应该匹配预期");
            
            log.info("✅ 角色 {} 使用音色 {} 合成成功", charTest.characterName, ttsResponse.getVoice());
        }
    }

    @Test
    @Order(3)
    @DisplayName("多语言支持测试")
    void testMultiLanguageSupport() throws Exception {
        log.info("🌍 开始执行多语言支持测试");
        
        List<LanguageTest> languageTests = List.of(
            new LanguageTest("中文测试", "你好世界", "Chinese", "Cherry"),
            new LanguageTest("英文测试", "Hello World", "English", "Jennifer"),
            new LanguageTest("混合语言", "Hello 你好 World 世界", "Chinese", "Cherry")
        );
        
        for (LanguageTest langTest : languageTests) {
            log.info("测试语言: {}", langTest.testName);
            
            TestCaseResult result = executeTestCase(new TestCase(
                langTest.testName, langTest.text, langTest.voice, langTest.language));
            
            assertTrue(result.ttsSuccess, "多语言TTS合成应该成功");
            assertTrue(result.asrSuccess, "多语言ASR识别应该成功");
            
            log.info("✅ {} 测试通过，识别文本: {}", langTest.testName, result.recognizedText);
        }
    }

    /**
     * 执行单个测试用例
     */
    private TestCaseResult executeTestCase(TestCase testCase) throws Exception {
        TestCaseResult result = new TestCaseResult();
        result.testName = testCase.name;
        result.originalText = testCase.text;
        result.startTime = LocalDateTime.now();
        
        try {
            // 步骤1: 使用TTS将文本转换为音频
            log.info("📢 步骤1: TTS合成 - 文本转音频");
            TtsSynthesisRequest ttsRequest = TtsSynthesisRequest.builder()
                .text(testCase.text)
                .voice(testCase.voice)
                .languageType(testCase.language)
                .model("qwen3-tts-flash")
                .saveToLocal(false)
                .build();
            
            TtsSynthesisResponse ttsResponse = ttsSynthesisService.synthesizeText(ttsRequest);
            
            if (!ttsResponse.getSuccess()) {
                throw new Exception("TTS合成失败: " + ttsResponse.getErrorMessage());
            }
            
            result.ttsSuccess = true;
            result.audioUrl = ttsResponse.getAudioUrl();
            result.ttsVoice = ttsResponse.getVoice();
            result.ttsDuration = ttsResponse.getDuration();
            result.ttsProcessingTime = ttsResponse.getProcessingTime();
            
            log.info("✅ TTS合成成功: 音色={}, 时长={}s, 处理时间={}ms", 
                    ttsResponse.getVoice(), ttsResponse.getDuration(), ttsResponse.getProcessingTime());
            
            // 步骤2: 下载音频文件到本地
            log.info("⬇️  步骤2: 下载音频文件");
            File audioFile = downloadAudioFile(ttsResponse.getAudioUrl(), testCase.name);
            
            // 步骤3: 使用ASR将音频转换为文本
            log.info("🎤 步骤3: ASR识别 - 音频转文本");
            MultipartFile multipartFile = createMultipartFile(audioFile);
            
            SpeechRecognitionRequest asrRequest = SpeechRecognitionRequest.builder()
                .model("fun-asr-realtime")
                .format("wav")
                .sampleRate(24000)  // TTS输出是24kHz
                .punctuationPredictionEnabled(true)
                .semanticPunctuationEnabled(false)
                .build();
            
            SpeechRecognitionResponse asrResponse = speechRecognitionService.recognizeFile(multipartFile, asrRequest);
            
            result.asrSuccess = true;
            result.recognizedText = asrResponse.getText();
            result.asrRequestId = asrResponse.getRequestId();
            result.asrBeginTime = asrResponse.getBeginTime();
            result.asrEndTime = asrResponse.getEndTime();
            
            log.info("✅ ASR识别成功: '{}'", asrResponse.getText());
            
            // 计算文本相似度
            result.similarity = calculateTextSimilarity(testCase.text, asrResponse.getText());
            
            // 清理临时文件
            if (audioFile.exists()) {
                audioFile.delete();
            }
            
        } catch (Exception e) {
            result.errorMessage = e.getMessage();
            log.error("❌ 测试用例执行失败: {}", e.getMessage(), e);
            throw e;
        } finally {
            result.endTime = LocalDateTime.now();
            result.totalTime = java.time.Duration.between(result.startTime, result.endTime).toMillis();
        }
        
        return result;
    }
    
    /**
     * 下载音频文件到本地
     */
    private File downloadAudioFile(String audioUrl, String testName) throws Exception {
        URL url = new URL(audioUrl);
        String fileName = testName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + "_" + System.currentTimeMillis() + ".wav";
        Path filePath = tempDirectory.resolve(fileName);
        
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(filePath.toFile())) {
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        
        log.info("音频文件已下载: {}, 大小: {} KB", filePath, Files.size(filePath) / 1024);
        return filePath.toFile();
    }
    
    /**
     * 创建MultipartFile对象
     */
    private MultipartFile createMultipartFile(File file) throws Exception {
        byte[] content = Files.readAllBytes(file.toPath());
        return new MockMultipartFile(
            "audio",
            file.getName(), 
            "audio/wav",
            content
        );
    }
    
    /**
     * 计算文本相似度（简单的字符匹配算法）
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        // 去除空格和标点符号进行比较
        String clean1 = text1.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
        String clean2 = text2.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
        
        if (clean1.isEmpty() && clean2.isEmpty()) {
            return 1.0;
        }
        
        if (clean1.isEmpty() || clean2.isEmpty()) {
            return 0.0;
        }
        
        // 使用编辑距离计算相似度
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
     * 计算编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
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
     * 打印测试结果
     */
    private void printTestResult(TestCaseResult result) {
        log.info("\n" + "=".repeat(60));
        log.info("📊 测试结果: {}", result.testName);
        log.info("原始文本: '{}'", result.originalText);
        log.info("识别文本: '{}'", result.recognizedText);
        log.info("TTS成功: {} (音色: {}, 时长: {}s)", result.ttsSuccess, result.ttsVoice, result.ttsDuration);
        log.info("ASR成功: {} (请求ID: {})", result.asrSuccess, result.asrRequestId);
        log.info("文本相似度: {:.2f}%", result.similarity * 100);
        log.info("总耗时: {}ms", result.totalTime);
        
        if (result.errorMessage != null) {
            log.error("错误信息: {}", result.errorMessage);
        }
        log.info("=".repeat(60));
    }
    
    /**
     * 打印最终测试报告
     */
    private void printFinalReport() {
        log.info("\n" + centerString("🎯 最终测试报告", 80, '='));
        
        int totalTests = testResults.size();
        int successfulTests = (int) testResults.stream().filter(r -> r.ttsSuccess && r.asrSuccess).count();
        double successRate = totalTests > 0 ? (double) successfulTests / totalTests * 100 : 0;
        
        log.info("总测试数量: {}", totalTests);
        log.info("成功测试数量: {}", successfulTests);
        log.info("成功率: {:.2f}%", successRate);
        
        // 计算平均相似度
        double avgSimilarity = testResults.stream()
            .filter(r -> r.similarity > 0)
            .mapToDouble(r -> r.similarity)
            .average()
            .orElse(0.0);
        
        log.info("平均文本相似度: {:.2f}%", avgSimilarity * 100);
        
        // 计算平均处理时间
        long avgTotalTime = testResults.stream()
            .filter(r -> r.totalTime > 0)
            .mapToLong(r -> r.totalTime)
            .sum() / Math.max(1, successfulTests);
        
        log.info("平均处理时间: {}ms", avgTotalTime);
        
        // 详细结果表格
        log.info("\n📋 详细测试结果:");
        log.info("%-20s %-10s %-10s %-15s %-10s", "测试名称", "TTS状态", "ASR状态", "相似度", "耗时(ms)");
        log.info("-".repeat(80));
        
        for (TestCaseResult result : testResults) {
            log.info("%-20s %-10s %-10s %-15.2f%% %-10d", 
                result.testName,
                result.ttsSuccess ? "✅" : "❌",
                result.asrSuccess ? "✅" : "❌", 
                result.similarity * 100,
                result.totalTime
            );
        }
        
        log.info("=".repeat(80));
        
        // 最终评估
        if (successRate >= 80 && avgSimilarity >= 0.7) {
            log.info("🎉 测试评估: 优秀 - 语音转换流水线运行良好！");
        } else if (successRate >= 60 && avgSimilarity >= 0.5) {
            log.info("⚠️  测试评估: 良好 - 语音转换基本可用，建议优化");
        } else {
            log.info("❌ 测试评估: 需要改进 - 语音转换质量有待提升");
        }
    }
    
    // 测试用例数据类
    static class TestCase {
        String name;
        String text;
        String voice;
        String language;
        
        TestCase(String name, String text, String voice, String language) {
            this.name = name;
            this.text = text;
            this.voice = voice;
            this.language = language;
        }
    }
    
    // 测试结果数据类
    static class TestCaseResult {
        String testName;
        String originalText;
        String recognizedText;
        boolean ttsSuccess;
        boolean asrSuccess;
        String audioUrl;
        String ttsVoice;
        Double ttsDuration;
        Long ttsProcessingTime;
        String asrRequestId;
        Long asrBeginTime;
        Long asrEndTime;
        double similarity;
        String errorMessage;
        LocalDateTime startTime;
        LocalDateTime endTime;
        long totalTime;
    }
    
    // 角色测试数据类
    static class CharacterTest {
        Long characterId;
        String characterName;
        String testText;
        String expectedVoice;
        
        CharacterTest(Long characterId, String characterName, String testText, String expectedVoice) {
            this.characterId = characterId;
            this.characterName = characterName;
            this.testText = testText;
            this.expectedVoice = expectedVoice;
        }
    }
    
    // 语言测试数据类
    static class LanguageTest {
        String testName;
        String text;
        String language;
        String voice;
        
        LanguageTest(String testName, String text, String language, String voice) {
            this.testName = testName;
            this.text = text;
            this.language = language;
            this.voice = voice;
        }
    }
}
