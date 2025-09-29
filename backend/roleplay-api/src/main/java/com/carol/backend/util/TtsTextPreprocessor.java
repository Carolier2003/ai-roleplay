package com.carol.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TTS文本预处理工具类
 * 负责清理Markdown格式、特殊字符等，使文本更适合语音合成
 */
@Slf4j
@Component
public class TtsTextPreprocessor {
    
    /**
     * 预处理文本用于TTS
     * 移除Markdown格式、清理特殊字符、规范化标点符号
     * 
     * @param text 原始文本
     * @return 预处理后的文本
     */
    public String preprocessTextForTts(String text) {
        if (text == null) {
            return "";
        }
        
        log.info("TTS文本预处理前: {}", text.substring(0, Math.min(text.length(), 100)));
        
        // 移除或替换不适合语音的内容
        String processed = text
                // 移除Markdown格式
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")  // 粗体 **text** -> text
                .replaceAll("\\*(.*?)\\*", "$1")        // 斜体 *text* -> text
                .replaceAll("~~(.*?)~~", "$1")          // 删除线 ~~text~~ -> text
                .replaceAll("`(.*?)`", "$1")            // 内联代码 `code` -> code
                .replaceAll("```[\\s\\S]*?```", "")     // 代码块 ```code``` -> 空
                
                // 处理链接
                .replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1")  // [text](url) -> text
                .replaceAll("https?://\\S+", "链接")              // URL -> "链接"
                
                // 处理标题和分隔符
                .replaceAll("[#]+\\s*", "")             // 标题标记 # ## ### -> 空
                .replaceAll("-{3,}", "")                // 分隔线 --- -> 空
                .replaceAll("_{3,}", "")                // 下划线分隔 ___ -> 空
                .replaceAll("——+", "，")                // 中文破折号 —— -> 逗号
                .replaceAll("\\|", "")                  // 表格分隔符 | -> 空
                
                // 处理列表标记
                .replaceAll("(?:^|\\s)[-*+]\\s+", " ")  // 无序列表 - * + -> 空格
                .replaceAll("(?:^|\\s)\\d+\\.\\s+", " ") // 有序列表 1. 2. -> 空格
                
                // 处理引用
                .replaceAll("^\\s*>\\s*", "")           // 引用 > -> 空
                
                // 规范化标点符号
                .replaceAll("\\s*[。！？.!?]+\\s*", "。")  // 统一句号
                .replaceAll("\\s*[，,]+\\s*", "，")       // 统一逗号
                .replaceAll("\\s*[；;]+\\s*", "；")       // 统一分号
                .replaceAll("\\s*[：:]+\\s*", "：")       // 统一冒号
                
                // 清理多余空白
                .replaceAll("\\s+", " ")                // 多个空白 -> 单个空格
                .replaceAll("\\n+", " ")                // 多个换行 -> 单个空格
                .trim();
        
        // 确保以句号结尾（有助于语音节奏）
        if (!processed.isEmpty() && !processed.matches(".*[。！？.!?]$")) {
            processed += "。";
        }
        
        log.info("TTS文本预处理后: {}", processed.substring(0, Math.min(processed.length(), 100)));
        log.info("TTS文本预处理完成: 原文{}字符 -> 处理后{}字符", text.length(), processed.length());
        
        return processed;
    }
    
    /**
     * 检查文本是否适合语音合成
     * 
     * @param text 文本内容
     * @param maxLength 最大长度限制
     * @return 是否适合TTS
     */
    public boolean isSuitableForTts(String text, int maxLength) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // 去除空白字符后的长度
        String cleanText = text.trim();
        
        // 长度检查
        if (cleanText.length() < 2 || cleanText.length() > maxLength) {
            log.debug("文本长度不适合TTS: {}字符 (限制: 2-{})", cleanText.length(), maxLength);
            return false;
        }
        
        // 检查是否主要是特殊字符或数字
        long letterCount = cleanText.chars()
                .filter(c -> java.lang.Character.isLetter(c))
                .count();
        
        // 至少50%是字母字符
        boolean suitable = letterCount >= cleanText.length() * 0.5;
        
        if (!suitable) {
            log.debug("文本字母字符比例不足: {}% (需要≥50%)", (letterCount * 100.0 / cleanText.length()));
        }
        
        return suitable;
    }
    
    /**
     * 确定语言类型
     * 
     * @param text 文本内容
     * @param requestedLanguage 请求的语言类型
     * @return 最终的语言类型
     */
    public String determineLanguageType(String text, String requestedLanguage) {
        // 如果明确指定了语言类型，直接使用
        if (requestedLanguage != null && !requestedLanguage.trim().isEmpty()) {
            return requestedLanguage;
        }
        
        // 简单的语言检测：检查中文字符比例
        if (text != null) {
            long chineseCharCount = text.chars()
                    .filter(c -> java.lang.Character.UnicodeScript.of(c) == java.lang.Character.UnicodeScript.HAN)
                    .count();
            
            double chineseRatio = (double) chineseCharCount / text.length();
            
            if (chineseRatio > 0.3) {
                log.debug("检测到中文内容比例: {}%, 使用中文语音合成", chineseRatio * 100);
                return "Chinese";
            } else {
                log.debug("检测到中文内容比例: {}%, 使用英文语音合成", chineseRatio * 100);
                return "English";
            }
        }
        
        // 默认中文
        return "Chinese";
    }
}
