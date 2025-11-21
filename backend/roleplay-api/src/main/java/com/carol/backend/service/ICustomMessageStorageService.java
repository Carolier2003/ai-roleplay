package com.carol.backend.service;

import com.carol.backend.service.CustomMessageStorageService.StoredMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 自定义消息存储服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 在Redis中保存消息内容和时间戳信息
 * @date 2025-01-15
 */
public interface ICustomMessageStorageService {
    
    /**
     * 保存消息到Redis
     * 
     * @param conversationId 会话ID
     * @param message 消息
     * @param isUser 是否为用户消息
     */
    void saveMessage(String conversationId, Message message, boolean isUser);
    
    /**
     * 保存消息到Redis（包含audioUrl）
     * 
     * @param conversationId 会话ID
     * @param message 消息
     * @param isUser 是否为用户消息
     * @param audioUrl 音频URL
     */
    void saveMessage(String conversationId, Message message, boolean isUser, String audioUrl);
    
    /**
     * 保存消息到Redis（包含audioUrl和语音时长）
     * 
     * @param conversationId 会话ID
     * @param message 消息
     * @param isUser 是否为用户消息
     * @param audioUrl 音频URL
     * @param voiceDuration 语音时长（秒）
     */
    void saveMessage(String conversationId, Message message, boolean isUser, String audioUrl, Integer voiceDuration);
    
    /**
     * 更新消息的audioUrl和语音时长
     * 
     * @param conversationId 会话ID
     * @param messageContent 消息内容
     * @param audioUrl 音频URL
     */
    void updateMessageAudioUrl(String conversationId, String messageContent, String audioUrl);
    
    /**
     * 更新消息的audioUrl和语音时长
     * 
     * @param conversationId 会话ID
     * @param messageContent 消息内容
     * @param audioUrl 音频URL
     * @param voiceDuration 语音时长（秒）
     */
    void updateMessageAudioInfo(String conversationId, String messageContent, String audioUrl, Integer voiceDuration);
    
    /**
     * 更新用户消息的语音时长
     * 
     * @param conversationId 会话ID
     * @param messageContent 消息内容
     * @param voiceDuration 语音时长（秒）
     * @return 是否成功
     */
    boolean updateUserMessageVoiceDuration(String conversationId, String messageContent, Integer voiceDuration);
    
    /**
     * 从Redis获取消息历史
     * 
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<StoredMessage> getMessages(String conversationId);
    
    /**
     * 清空会话消息
     * 
     * @param conversationId 会话ID
     */
    void clearMessages(String conversationId);
}
