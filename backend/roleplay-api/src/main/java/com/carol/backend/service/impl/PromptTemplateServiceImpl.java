package com.carol.backend.service.impl;

import com.carol.backend.entity.Character;
import com.carol.backend.entity.CharacterKnowledge;
import com.carol.backend.service.IPromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt模板服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 参考spring-ai-alibaba-prompt-example/RoleController.java实现，负责生成角色系统提示词
 * @date 2025-01-15
 */
@Slf4j
@Service
public class PromptTemplateServiceImpl implements IPromptTemplateService {

    /**
     * 角色扮演系统提示词模板
     * 参考alibaba示例的模板加载方式
     */
    @Value("classpath:/prompts/character-role.st")
    private Resource characterRoleTemplate;

    /**
     * 默认系统提示词模板（当角色信息不完整时使用）
     */
    @Value("classpath:/prompts/default-system.st")
    private Resource defaultSystemTemplate;

    /**
     * RAG增强角色扮演系统提示词模板
     */
    @Value("classpath:/prompts/character-role-rag.st")
    private Resource characterRoleRAGTemplate;

    @Override
    public Message createCharacterSystemMessage(Character character) {
        return createCharacterSystemMessage(character, false);
    }

    @Override
    public Message createCharacterSystemMessage(Character character, boolean enableTts) {
        log.info("[createCharacterSystemMessage] 为角色创建系统提示词: characterName={}, enableTts={}", 
                character.getName(), enableTts);
        
        try {
            // 检查角色信息完整性
            if (!character.isComplete()) {
                log.warn("[createCharacterSystemMessage] 角色信息不完整，使用默认提示词: characterName={}", 
                        character.getName());
                return createDefaultSystemMessage(character.getName());
            }

            // 使用角色模板创建系统提示词
            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(characterRoleTemplate);
            
            // 构建模板变量 - 参考alibaba示例的Map.of用法
            Map<String, Object> templateVariables = buildTemplateVariables(character);
            
            // 如果启用TTS，添加字数限制和格式限制要求
            if (enableTts) {
                templateVariables.put("tts_length_limit", "- 回复不要超过200个字，保持简洁明了。");
                templateVariables.put("tts_format_restrictions", 
                    "- **语音合成格式要求**: 不要使用括号描述动作或心理活动（如：（推了推眼镜）、（思考中）、（笑了笑）等），" +
                    "因为这些内容不适合语音播放。请直接用对话和叙述的方式表达。");
                log.info("[createCharacterSystemMessage] 为角色添加TTS字数限制和格式限制要求: characterName={}", 
                        character.getName());
            } else {
                templateVariables.put("tts_length_limit", "");
                templateVariables.put("tts_format_restrictions", "");
            }
            
            Message systemMessage = systemPromptTemplate.createMessage(templateVariables);
            
            log.info("[createCharacterSystemMessage] 成功为角色创建系统提示词: characterName={}, enableTts={}", 
                    character.getName(), enableTts);
            return systemMessage;
            
        } catch (Exception e) {
            log.error("[createCharacterSystemMessage] 创建角色系统提示词失败: characterName={}, error={}", 
                    character.getName(), e.getMessage(), e);
            // 发生错误时返回默认提示词
            return createDefaultSystemMessage(character.getName());
        }
    }

    @Override
    public Message createEnhancedCharacterSystemMessage(Character character, String userContext) {
        log.info("[createEnhancedCharacterSystemMessage] 为角色创建增强系统提示词: characterName={}", 
                character.getName());
        
        try {
            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(characterRoleTemplate);
            
            Map<String, Object> templateVariables = buildTemplateVariables(character);
            
            // 添加用户上下文
            if (userContext != null && !userContext.trim().isEmpty()) {
                templateVariables.put("user_context", userContext);
            } else {
                templateVariables.put("user_context", "这是一次新的对话。");
            }
            
            log.info("[createEnhancedCharacterSystemMessage] 成功创建增强系统提示词: characterName={}", 
                    character.getName());
            return systemPromptTemplate.createMessage(templateVariables);
            
        } catch (Exception e) {
            log.error("[createEnhancedCharacterSystemMessage] 创建增强角色系统提示词失败: characterName={}, error={}", 
                    character.getName(), e.getMessage(), e);
            return createCharacterSystemMessage(character);
        }
    }

    @Override
    public Message createCharacterSystemMessageWithRAG(Character character, List<CharacterKnowledge> relevantKnowledge) {
        return createCharacterSystemMessageWithRAG(character, relevantKnowledge, false);
    }

    @Override
    public Message createCharacterSystemMessageWithRAG(Character character, List<CharacterKnowledge> relevantKnowledge, boolean enableTts) {
        log.info("[createCharacterSystemMessageWithRAG] 为角色创建RAG增强系统提示词: characterName={}, knowledgeCount={}, enableTts={}", 
                character.getName(), relevantKnowledge != null ? relevantKnowledge.size() : 0, enableTts);
        
        try {
            // 如果没有相关知识，回退到普通角色提示
            if (relevantKnowledge == null || relevantKnowledge.isEmpty()) {
                log.info("[createCharacterSystemMessageWithRAG] 没有找到相关知识，使用标准角色提示: characterName={}", 
                        character.getName());
                return createCharacterSystemMessage(character, enableTts);
            }
            
            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(characterRoleRAGTemplate);
            
            Map<String, Object> templateVariables = buildTemplateVariables(character);
            
            // 构建知识上下文
            String knowledgeContext = buildKnowledgeContext(relevantKnowledge);
            templateVariables.put("knowledge_context", knowledgeContext);
            templateVariables.put("knowledge_count", relevantKnowledge.size());
            
            // 如果启用TTS，添加字数限制和格式限制要求
            if (enableTts) {
                templateVariables.put("tts_length_limit", "- 回复不要超过200个字，保持简洁明了。");
                templateVariables.put("tts_format_restrictions", 
                    "- **语音合成格式要求**: 不要使用括号描述动作或心理活动（如：（推了推眼镜）、（思考中）、（笑了笑）等），" +
                    "因为这些内容不适合语音播放。请直接用对话和叙述的方式表达。");
                log.info("[createCharacterSystemMessageWithRAG] 为角色添加TTS字数限制和格式限制要求: characterName={}", 
                        character.getName());
            } else {
                templateVariables.put("tts_length_limit", "");
                templateVariables.put("tts_format_restrictions", "");
            }
            
            Message systemMessage = systemPromptTemplate.createMessage(templateVariables);
            
            log.info("[createCharacterSystemMessageWithRAG] 成功为角色创建RAG增强系统提示词: characterName={}, knowledgeCount={}, enableTts={}", 
                    character.getName(), relevantKnowledge.size(), enableTts);
            return systemMessage;
            
        } catch (Exception e) {
            log.error("[createCharacterSystemMessageWithRAG] 创建RAG增强角色系统提示词失败: characterName={}, error={}", 
                    character.getName(), e.getMessage(), e);
            // 发生错误时回退到普通角色提示
            return createCharacterSystemMessage(character);
        }
    }

    @Override
    public String getCharacterPromptText(Character character) {
        log.debug("[getCharacterPromptText] 获取角色提示词文本: characterName={}", character.getName());
        
        try {
            Message systemMessage = createCharacterSystemMessage(character);
            String text = systemMessage.getText();
            log.debug("[getCharacterPromptText] 获取成功: characterName={}, textLength={}", 
                    character.getName(), text != null ? text.length() : 0);
            return text;
        } catch (Exception e) {
            log.error("[getCharacterPromptText] 获取角色提示词文本失败: characterName={}, error={}", 
                    character.getName(), e.getMessage(), e);
            return "您好！我是 " + character.getDisplayName() + "，很高兴与您对话！";
        }
    }

    @Override
    public boolean validateTemplates() {
        log.debug("[validateTemplates] 验证模板文件");
        
        try {
            boolean valid = characterRoleTemplate.exists() && characterRoleTemplate.isReadable() &&
                   defaultSystemTemplate.exists() && defaultSystemTemplate.isReadable();
            log.info("[validateTemplates] 模板文件验证结果: valid={}", valid);
            return valid;
        } catch (Exception e) {
            log.error("[validateTemplates] 验证模板文件失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建模板变量Map
     * 参考alibaba示例的变量填充方式
     */
    private Map<String, Object> buildTemplateVariables(Character character) {
        Map<String, Object> variables = new HashMap<>();
        
        // 基础角色信息
        variables.put("character_name", character.getDisplayName());
        variables.put("background_story", getOrDefault(character.getBackgroundStory(), "暂无背景故事"));
        variables.put("personality_traits", getOrDefault(character.getPersonalityTraits(), "友善、聪明"));
        variables.put("speaking_style", getOrDefault(character.getSpeakingStyle(), "自然、友好"));
        variables.put("expertise_area", getOrDefault(character.getExpertiseArea(), "通用知识"));
        variables.put("voice_style", getOrDefault(character.getVoiceStyle(), "default"));
        
        log.debug("[buildTemplateVariables] 构建模板变量: characterName={}, variableCount={}", 
                character.getName(), variables.size());
        return variables;
    }

    /**
     * 创建默认系统提示词
     */
    private Message createDefaultSystemMessage(String characterName) {
        log.debug("[createDefaultSystemMessage] 创建默认系统提示词: characterName={}", characterName);
        
        try {
            SystemPromptTemplate defaultTemplate = new SystemPromptTemplate(defaultSystemTemplate);
            Map<String, Object> variables = Map.of(
                "character_name", characterName != null ? characterName : "AI助手",
                "current_time", java.time.LocalDateTime.now().toString()
            );
            Message message = defaultTemplate.createMessage(variables);
            log.debug("[createDefaultSystemMessage] 默认系统提示词创建成功: characterName={}", characterName);
            return message;
        } catch (Exception e) {
            log.error("[createDefaultSystemMessage] 创建默认系统提示词失败: characterName={}, error={}", 
                    characterName, e.getMessage(), e);
            // 最后的备用方案
            return new org.springframework.ai.chat.messages.SystemMessage(
                "你好！我是 " + characterName + "，一个智能AI助手。我会尽力为您提供帮助。"
            );
        }
    }

    /**
     * 构建知识上下文字符串
     */
    private String buildKnowledgeContext(List<CharacterKnowledge> knowledgeList) {
        if (knowledgeList == null || knowledgeList.isEmpty()) {
            return "暂无相关知识信息。";
        }
        
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < knowledgeList.size(); i++) {
            CharacterKnowledge knowledge = knowledgeList.get(i);
            
            context.append(String.format("%d. ", i + 1));
            context.append("**").append(knowledge.getTitle()).append("**\n");
            
            // 添加知识类型信息
            if (knowledge.getKnowledgeType() != null) {
                context.append("类型: ").append(knowledge.getKnowledgeType()).append("\n");
            }
            
            // 添加内容（限制长度避免提示过长）
            String content = knowledge.getContent();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            context.append(content).append("\n");
            
            // 添加标签信息
            List<String> tags = knowledge.getTagList();
            if (!tags.isEmpty()) {
                context.append("相关标签: ").append(String.join(", ", tags)).append("\n");
            }
            
            context.append("\n");
        }
        
        return context.toString();
    }

    /**
     * 获取值或默认值
     */
    private String getOrDefault(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
}
