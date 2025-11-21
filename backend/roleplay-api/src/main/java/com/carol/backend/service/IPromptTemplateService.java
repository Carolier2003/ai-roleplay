package com.carol.backend.service;

import com.carol.backend.entity.Character;
import com.carol.backend.entity.CharacterKnowledge;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Prompt模板服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description Prompt模板服务，负责生成角色系统提示词
 * @date 2025-01-15
 */
public interface IPromptTemplateService {
    
    /**
     * 根据角色信息生成系统提示词
     * 
     * @param character 角色信息
     * @return 系统提示词Message
     */
    Message createCharacterSystemMessage(Character character);
    
    /**
     * 根据角色信息生成系统提示词（支持字数限制）
     * 
     * @param character 角色信息
     * @param enableTts 是否启用TTS（影响字数限制）
     * @return 系统提示词Message
     */
    Message createCharacterSystemMessage(Character character, boolean enableTts);
    
    /**
     * 创建增强的角色提示词（包含用户上下文）
     * 
     * @param character 角色信息
     * @param userContext 用户上下文（如之前的对话历史摘要）
     * @return 增强的系统提示词
     */
    Message createEnhancedCharacterSystemMessage(Character character, String userContext);
    
    /**
     * 创建RAG增强的角色系统提示词
     * 
     * @param character 角色信息
     * @param relevantKnowledge 相关知识列表
     * @return RAG增强的系统提示词
     */
    Message createCharacterSystemMessageWithRAG(Character character, List<CharacterKnowledge> relevantKnowledge);
    
    /**
     * 创建RAG增强的角色系统提示词（支持字数限制）
     * 
     * @param character 角色信息
     * @param relevantKnowledge 相关知识列表
     * @param enableTts 是否启用TTS（影响字数限制）
     * @return RAG增强的系统提示词
     */
    Message createCharacterSystemMessageWithRAG(Character character, List<CharacterKnowledge> relevantKnowledge, boolean enableTts);
    
    /**
     * 获取角色提示词的纯文本版本（用于调试或展示）
     * 
     * @param character 角色信息
     * @return 提示词文本
     */
    String getCharacterPromptText(Character character);
    
    /**
     * 验证模板文件是否存在且可读
     * 
     * @return 是否有效
     */
    boolean validateTemplates();
}
