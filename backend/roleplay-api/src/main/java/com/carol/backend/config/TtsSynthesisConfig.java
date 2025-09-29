package com.carol.backend.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.HashMap;

/**
 * TTS语音合成配置
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "speech.tts")
public class TtsSynthesisConfig {
    
    /**
     * 默认TTS模型
     */
    private String defaultModel = "qwen3-tts-flash";
    
    /**
     * 默认音色
     */
    private String defaultVoice = "Cherry";
    
    /**
     * 默认语言类型
     */
    private String defaultLanguageType = "Chinese";
    
    /**
     * 最大文本长度
     */
    private Integer maxTextLength = 600;
    
    /**
     * 流式合成超时时间（秒）
     */
    private Integer streamingTimeout = 60;
    
    /**
     * 同步合成超时时间（秒）
     */
    private Integer syncTimeout = 30;
    
    /**
     * 音频文件保存目录
     */
    private String audioSaveDirectory = "./audio_files";
    
    /**
     * 是否启用音频文件本地保存
     */
    private Boolean enableLocalSave = false;
    
    /**
     * 音频URL有效期（小时）
     */
    private Integer audioUrlExpiryHours = 24;
    
    /**
     * 音色语言支持映射
     */
    private Map<String, String[]> voiceLanguageSupport = initVoiceLanguageSupport();
    
    /**
     * 角色音色映射
     */
    private Map<Long, String> characterVoiceMapping = new HashMap<>();
    
    /**
     * 费用计算配置
     */
    private CostConfig cost = new CostConfig();
    
    @Data
    public static class CostConfig {
        /**
         * Qwen3-TTS价格（元/万字符）
         */
        private Double qwen3TtsPrice = 0.8;
        
        /**
         * Qwen-TTS输入价格（元/千Token）
         */
        private Double qwenTtsInputPrice = 0.0016;
        
        /**
         * Qwen-TTS输出价格（元/千Token）
         */
        private Double qwenTtsOutputPrice = 0.01;
    }
    
    /**
     * 初始化音色语言支持映射
     */
    private Map<String, String[]> initVoiceLanguageSupport() {
        Map<String, String[]> voiceSupport = new HashMap<>();
        
        // Qwen3-TTS音色（支持多语言）
        String[] multiLanguages = {"Chinese", "English", "French", "German", "Russian", 
                                  "Italian", "Spanish", "Portuguese", "Japanese", "Korean"};
        
        voiceSupport.put("Cherry", multiLanguages);
        voiceSupport.put("Ethan", multiLanguages);
        voiceSupport.put("Nofish", multiLanguages);
        voiceSupport.put("Jennifer", multiLanguages);
        voiceSupport.put("Ryan", multiLanguages);
        voiceSupport.put("Katerina", multiLanguages);
        voiceSupport.put("Elias", multiLanguages);
        
        // 方言音色
        voiceSupport.put("Jada", multiLanguages); // 上海话
        voiceSupport.put("Dylan", multiLanguages); // 北京话
        voiceSupport.put("Sunny", multiLanguages); // 四川话
        voiceSupport.put("li", multiLanguages); // 南京话
        voiceSupport.put("Marcus", multiLanguages); // 陕西话
        voiceSupport.put("Roy", multiLanguages); // 闽南语
        voiceSupport.put("Peter", multiLanguages); // 天津话
        voiceSupport.put("Rocky", multiLanguages); // 粤语
        voiceSupport.put("Kiki", multiLanguages); // 粤语
        voiceSupport.put("Eric", multiLanguages); // 四川话
        
        // Qwen-TTS音色（仅支持中英文）
        String[] basicLanguages = {"Chinese", "English"};
        voiceSupport.put("Serena", basicLanguages);
        voiceSupport.put("Chelsie", basicLanguages);
        
        return voiceSupport;
    }
    
    /**
     * 检查音色是否支持指定语言
     */
    public boolean isVoiceLanguageSupported(String voice, String languageType) {
        String[] supportedLanguages = voiceLanguageSupport.get(voice);
        if (supportedLanguages == null) {
            return false;
        }
        
        for (String lang : supportedLanguages) {
            if (lang.equals(languageType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取角色对应的音色
     */
    public String getCharacterVoice(Long characterId) {
        return characterVoiceMapping.getOrDefault(characterId, defaultVoice);
    }
    
    /**
     * 设置角色音色映射
     */
    public void setCharacterVoice(Long characterId, String voice) {
        characterVoiceMapping.put(characterId, voice);
    }
    
    /**
     * 初始化默认角色音色映射
     */
    public void initDefaultCharacterVoices() {
        log.info("[initDefaultCharacterVoices] 开始初始化角色音色映射...");
        
        // 哈利·波特 - 年轻男性，英国口音
        characterVoiceMapping.put(1L, "Ethan");
        log.info("[initDefaultCharacterVoices] 角色ID 1 (哈利·波特) -> 音色: Ethan");
        
        // 苏格拉底 - 智慧长者，沉稳声音
        characterVoiceMapping.put(2L, "Elias");
        log.info("[initDefaultCharacterVoices] 角色ID 2 (苏格拉底) -> 音色: Elias");
        
        // 爱因斯坦 - 科学家，德国口音风格
        characterVoiceMapping.put(3L, "Marcus");
        log.info("[initDefaultCharacterVoices] 角色ID 3 (爱因斯坦) -> 音色: Marcus");
        
        // 江户川柯南 - 推理侦探，日系角色但用中文
        characterVoiceMapping.put(4L, "Ryan");
        log.info("[initDefaultCharacterVoices] 角色ID 4 (江户川柯南) -> 音色: Ryan");
        
        // 泰拉瑞亚向导 - 游戏向导，友好指导声音
        characterVoiceMapping.put(5L, "Dylan");
        log.info("[initDefaultCharacterVoices] 角色ID 5 (泰拉瑞亚向导) -> 音色: Dylan");
        
        // 女性角色可以使用女性音色
        // 如果有女性角色，可以使用 Cherry, Jennifer, Katerina 等
        
        log.info("[initDefaultCharacterVoices] 角色音色映射初始化完成，共配置 {} 个角色", characterVoiceMapping.size());
    }
}
