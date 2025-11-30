package com.carol.backend.controller;

import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import com.carol.backend.dto.ChatRequest;
import com.carol.backend.dto.ChatResponse;
import com.carol.backend.dto.TtsSynthesisResponse;
import com.carol.backend.dto.TtsPersistenceResult;
import com.carol.backend.dto.UpdateVoiceDurationRequest;
import com.carol.backend.entity.Character;
import com.carol.backend.entity.CharacterKnowledge;
import com.carol.backend.service.CharacterService;
import com.carol.backend.service.ICharacterKnowledgeRAGService;
import com.carol.backend.service.IPromptTemplateService;
import com.carol.backend.service.IConversationHistoryService;
import com.carol.backend.service.IChatTtsIntegrationService;
import com.carol.backend.service.ITtsSynthesisService;
import com.carol.backend.service.IGuestChatLimitService;
import com.carol.backend.service.CustomMessageStorageService;
import com.carol.backend.service.ITtsAudioPersistenceService;
import com.carol.backend.service.QwenConversationService;
import com.carol.backend.dto.QwenConversationResponse;
import com.carol.backend.dto.QwenConversationInfo;
import com.carol.backend.dto.RenameConversationRequest;
import com.carol.backend.dto.ChatHistoryResponse;
import com.carol.backend.dto.ConversationMessageVO;
import com.carol.backend.util.SecurityUtils;
// 移除了AsyncContext和HttpServletRequest相关import，因为不再需要手动管理异步上下文
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
// 移除SimpleLoggerAdvisor import，因为它在序列化包含java.time.Duration的响应时有问题
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
// 移除了RequestContextHolder相关import，因为在异步回调中使用不安全
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 聊天控制器
 * 实现大模型对话和会话记忆功能，支持用户区分
 * 
 * @author carol
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final MessageWindowChatMemory messageWindowChatMemory;
    private final CharacterService characterService;
    private final IPromptTemplateService promptTemplateService;
    private final ICharacterKnowledgeRAGService ragService;
    private final IConversationHistoryService conversationHistoryService;
    private final IChatTtsIntegrationService chatTtsIntegrationService;
    private final ITtsSynthesisService ttsSynthesisService;
    private final IGuestChatLimitService guestChatLimitService;
    private final CustomMessageStorageService customMessageStorageService;
    private final ITtsAudioPersistenceService ttsAudioPersistenceService;
    private final QwenConversationService qwenConversationService;

    private static final int DEFAULT_MAX_MESSAGES = 100;
    
    // 同步状态缓存：记录已经同步过的conversationId，避免重复检查
    private final java.util.Set<String> syncedConversations = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public ChatController(ChatClient.Builder chatClientBuilder, 
                         MessageWindowChatMemory messageWindowChatMemory,
                         CharacterService characterService,
                         IPromptTemplateService promptTemplateService,
                         ICharacterKnowledgeRAGService ragService,
                         IConversationHistoryService conversationHistoryService,
                         IChatTtsIntegrationService chatTtsIntegrationService,
                         ITtsSynthesisService ttsSynthesisService,
                         IGuestChatLimitService guestChatLimitService,
                         CustomMessageStorageService customMessageStorageService,
                         ITtsAudioPersistenceService ttsAudioPersistenceService,
                         QwenConversationService qwenConversationService) {

        this.characterService = characterService;
        this.promptTemplateService = promptTemplateService;
        this.ragService = ragService;
        this.conversationHistoryService = conversationHistoryService;
        this.guestChatLimitService = guestChatLimitService;
        this.messageWindowChatMemory = messageWindowChatMemory;
        this.chatTtsIntegrationService = chatTtsIntegrationService;
        this.ttsSynthesisService = ttsSynthesisService;
        this.customMessageStorageService = customMessageStorageService;
        this.ttsAudioPersistenceService = ttsAudioPersistenceService;
        this.qwenConversationService = qwenConversationService;

        // 初始化ChatClient，配置默认系统提示和顾问
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个智能的AI助手，请根据用户的问题提供有用、准确、友好的回答。")
                .defaultAdvisors(
                        // 会话记忆顾问
                        MessageChatMemoryAdvisor.builder(this.messageWindowChatMemory).build()
                        // 移除SimpleLoggerAdvisor，因为它在序列化包含java.time.Duration的ChatResponse时会出错
                )
                .build();
    }

    /**
     * 普通聊天接口
     * 支持用户会话记忆和角色扮演
     */
    @PostMapping("/message")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        // ✅ 从JWT中获取userId
        Long userId = SecurityUtils.getCurrentUserId();
        
        log.info("[chat] 收到聊天请求: userId={}, characterId={}, message={}", 
                userId, request.getCharacterId(), request.getMessage());
        
        // 验证用户认证
        if (userId == null) {
            log.warn("[chat] 用户未认证，拒绝请求");
            throw new RuntimeException("用户未认证");
        }
        
        // 生成会话ID
        String conversationId = generateConversationId(request, userId);
        
        try {
            String response;
            ChatResponse.CharacterInfo characterInfo = null;
            ChatResponse.AudioInfo audioInfo = null;

            // 检查是否指定了角色
            if (request.getCharacterId() != null) {
                // 角色扮演对话
                response = handleCharacterChat(request, conversationId);
                
                // 获取角色信息用于响应
                if (request.getCharacterId() == 0L) {
                    characterInfo = ChatResponse.CharacterInfo.builder()
                        .id(0L)
                        .name("Qwen")
                        .avatar("http://oss.kon-carol.xyz/airole0.png")
                        .voice("default")
                        .build();
                } else {
                    Character character = characterService.getCharacterById(request.getCharacterId());
                    String characterVoice = ttsSynthesisService.getRecommendedVoiceForCharacter(request.getCharacterId());
                    characterInfo = ChatResponse.CharacterInfo.builder()
                        .id(character.getId())
                        .name(character.getName())
                        .avatar(character.getAvatarUrl())
                        .voice(characterVoice)
                        .build();
                }
            } else {
                // 普通对话 - 同步历史记录到messageWindowChatMemory
                syncHistoryToMessageWindowChatMemory(conversationId);
                
                // 创建用户消息
                UserMessage userMessage = new UserMessage(request.getMessage());
                
                // 保存用户消息到自定义存储和messageWindowChatMemory
                // 保存用户消息到自定义存储和messageWindowChatMemory
                customMessageStorageService.saveMessage(conversationId, userMessage, true, request.getAudioUrl(), request.getVoiceDuration());
                messageWindowChatMemory.add(conversationId, userMessage);
                
                response = chatClient.prompt(new Prompt(List.of(userMessage)))
                        .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                        .call()
                        .content();
                
                // 保存AI回复到自定义存储和messageWindowChatMemory（暂时不包含audioUrl）
                AssistantMessage assistantMessage = new AssistantMessage(response);
                customMessageStorageService.saveMessage(conversationId, assistantMessage, false);
                messageWindowChatMemory.add(conversationId, assistantMessage);
            }
            
            log.info("AI回复: conversationId={}, response={}", conversationId, response);
            
            // 如果启用了TTS，生成语音
            if (Boolean.TRUE.equals(request.getEnableTts())) {
                try {
                    TtsSynthesisResponse ttsResponse = chatTtsIntegrationService.generateSpeechForChatReply(
                        response,
                        request.getCharacterId(),
                        userId.toString(),
                        request.getLanguageType()
                    );

                    if (ttsResponse != null && ttsResponse.getSuccess()) {
                        audioInfo = ChatResponse.AudioInfo.builder()
                            .audioUrl(ttsResponse.getAudioUrl())
                            .duration(ttsResponse.getDuration())
                            .voice(ttsResponse.getVoice())
                            .languageType(ttsResponse.getLanguageType())
                            .success(true)
                            .characterCount(ttsResponse.getCharacterCount())
                            .estimatedCost(ttsResponse.getEstimatedCost())
                            .processingTime(ttsResponse.getProcessingTime())
                            .build();

                        log.info("TTS语音合成成功: conversationId={}, audioUrl={}", conversationId, ttsResponse.getAudioUrl());
                        
                        // 更新Redis中AI回复消息的audioUrl和语音时长
                        Integer voiceDuration = ttsResponse.getDuration() != null ? 
                            ttsResponse.getDuration().intValue() : null;
                        customMessageStorageService.updateMessageAudioInfo(conversationId, response, 
                            ttsResponse.getAudioUrl(), voiceDuration);

                    } else if (ttsResponse == null) {
                        log.warn("TTS语音合成跳过: conversationId={}, 文本不适合语音合成", conversationId);
                        
                        audioInfo = ChatResponse.AudioInfo.builder()
                            .success(false)
                            .errorMessage("文本不适合语音合成")
                            .build();
                    } else {
                        log.warn("TTS语音合成失败: conversationId={}, error={}",
                                conversationId, ttsResponse.getErrorMessage());

                        audioInfo = ChatResponse.AudioInfo.builder()
                            .success(false)
                            .errorMessage(ttsResponse.getErrorMessage())
                            .build();
                    }
                } catch (Exception ttsError) {
                    log.error("TTS语音合成异常: conversationId={}, error={}", conversationId, ttsError.getMessage(), ttsError);
                    audioInfo = ChatResponse.AudioInfo.builder()
                        .success(false)
                        .errorMessage("语音合成服务异常")
                        .build();
                }
            }

            return ChatResponse.builder()
                .content(response)
                .conversationId(conversationId)
                .character(characterInfo)
                .audio(audioInfo)
                .timestamp(LocalDateTime.now())
                .build();
            
        } catch (Exception e) {
            log.error("聊天处理失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new RuntimeException("聊天处理失败: " + e.getMessage());
        }
    }

    /**
     * 流式聊天接口
     * 实时返回AI回复内容，支持角色扮演
     * ✅ Spring自动将Flux<String>包装为text/event-stream
     */
    @PostMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        // ✅ 从JWT中获取userId（使用新的UserContext）
        Long userId = SecurityUtils.getCurrentUserId();

        // 游客模式聊天限制检查
        if (userId == null) {
            // 游客模式，检查聊天次数限制
            String sessionId = getGuestSessionId(httpRequest);

            if (!guestChatLimitService.canGuestChat(sessionId)) {
                log.warn("[streamChat] 游客聊天次数已达上限: sessionId={}", sessionId);
                // 返回错误信息的流式响应，而不是抛出异常
                return Flux.just("data:error:游客模式每日最多可聊天5次，请登录后继续使用\n\n");
            }

            // 增加游客聊天次数
            guestChatLimitService.incrementGuestChatCount(sessionId);
            log.info("[streamChat] 游客聊天: sessionId={}, 当前次数={}",
                    sessionId, guestChatLimitService.getGuestChatCount(sessionId));
        }
        
        log.info("[streamChat] 🎯 收到流式聊天请求: userId={}, characterId={}, message={}", 
                userId, request.getCharacterId(), request.getMessage());
        
        // 生成会话ID
        String conversationId = generateConversationId(request, userId);
        
        // 检查是否指定了角色
        if (request.getCharacterId() != null) {
            // 角色扮演流式对话
            return handleCharacterStreamChat(request, conversationId)
                    .onErrorResume(throwable -> {
                        log.error("角色流式聊天失败: conversationId={}, error={}", conversationId, throwable.getMessage(), throwable);
                        return Flux.just(
                            "data: {\"error\": \"角色扮演聊天失败: " + throwable.getMessage() + "\"}\n\n",
                            "data: [DONE]\n\n"
                        );
                    });
        } else {
            // 普通流式对话 - 创建用户消息
            UserMessage userMessage = new UserMessage(request.getMessage());
            
            // 保存用户消息到自定义存储
            customMessageStorageService.saveMessage(conversationId, userMessage, true);
            
            // 用于收集完整响应文本
            StringBuilder completeResponse = new StringBuilder();
            
            return chatClient.prompt(new Prompt(List.of(userMessage)))
                    .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .map(chunk -> {
                        // 收集文本块用于后续保存
                        getCompleteResponse(completeResponse).append(chunk);
                        return "data:" + chunk + "\n\n";
                    })
                    .concatWith(
                        // 流式响应完成后，保存完整的AI回复
                        Mono.fromCallable(() -> {
                            String fullResponse = completeResponse.toString();
                            if (!fullResponse.isEmpty()) {
                                AssistantMessage assistantMessage = new AssistantMessage(fullResponse);
                                customMessageStorageService.saveMessage(conversationId, assistantMessage, false);
                                log.info("[streamChat] 保存普通流式回复: conversationId={}, length={}", 
                                        conversationId, fullResponse.length());
                            }
                            return "data:[DONE]\n\n";
                        }).subscribeOn(Schedulers.boundedElastic())
                    )
                    .onErrorResume(throwable -> {
                        log.error("普通流式聊天失败: conversationId={}, error={}", conversationId, throwable.getMessage(), throwable);
                        return Flux.just(
                            "data: {\"error\": \"聊天失败: " + throwable.getMessage() + "\"}\n\n",
                            "data: [DONE]\n\n"
                        );
                    });
        }
    }

    private static StringBuilder getCompleteResponse(StringBuilder completeResponse) {
        return completeResponse;
    }

    /**
     * 获取会话历史记录
     */
    @GetMapping("/history/{conversationId}")
    public List<Message> getChatHistory(@PathVariable String conversationId) {
        log.info("获取会话历史: conversationId={}", conversationId);
        
        try {
            return messageWindowChatMemory.get(conversationId);
        } catch (Exception e) {
            log.error("获取会话历史失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new RuntimeException("获取会话历史失败: " + e.getMessage());
        }
    }

    /**
     * 清除指定会话的历史记录
     */
    @DeleteMapping("/history/{conversationId}")
    public void clearChatHistory(@PathVariable String conversationId) {
        log.info("清除会话历史: conversationId={}", conversationId);
        
        try {
            messageWindowChatMemory.clear(conversationId);
            // 清除同步缓存，允许重新同步
            clearSyncCache(conversationId);
            log.info("会话历史已清除: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("清除会话历史失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new RuntimeException("清除会话历史失败: " + e.getMessage());
        }
    }

    /**
     * 简单聊天接口（GET方式，用于快速测试）
     */
    @GetMapping("/simple")
    public String simpleChat(@RequestParam(value = "message", defaultValue = "你好！") String message) {
        // ✅ 从JWT中获取userId
        Long userId = SecurityUtils.getCurrentUserId();
        
        log.info("[simpleChat] 收到简单聊天请求: userId={}, message={}", userId, message);
        
        // 验证用户认证
        if (userId == null) {
            log.warn("[simpleChat] 用户未认证，拒绝请求");
            return "错误：用户未认证";
        }
        
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        
        ChatResponse response = chat(request);
        return response.getContent();
    }

    /**
     * 处理角色扮演对话
     * 集成RAG知识检索，提供更智能的角色扮演体验
     */
    /**
     * 处理角色扮演对话
     * 集成RAG知识检索，提供更智能的角色扮演体验
     */
    private String handleCharacterChat(ChatRequest request, String conversationId) {
        log.info("[handleCharacterChat] 处理角色扮演对话: characterId={}, conversationId={}, enableRag={}",
                request.getCharacterId(), conversationId, request.getEnableRag());
        
        try {
            Character character;
            Message systemMessage;

            // ✅ 特殊处理 ID=0 的普通助手角色
            if (request.getCharacterId() != null && request.getCharacterId() == 0L) {
                log.info("[handleCharacterChat] 使用普通助手模式 (ID=0)");
                
                // ✅ 更新 Qwen 会话元数据（如果有 conversationId）
                if (request.getConversationId() != null && !request.getConversationId().trim().isEmpty()) {
                    Long userId = SecurityUtils.getCurrentUserId();
                    // 更新最后活跃时间
                    qwenConversationService.updateLastActiveTime(userId, request.getConversationId());
                    // 自动生成标题（仅首次）
                    qwenConversationService.generateTitle(userId, request.getConversationId(), request.getMessage());
                }
                
                // ✅ 强制关闭RAG - AI助手不需要角色知识库
                if (Boolean.TRUE.equals(request.getEnableRag())) {
                    log.info("[handleCharacterChat] AI助手不使用RAG，强制关闭: characterId=0");
                    request.setEnableRag(false);
                }
                
                // 创建虚拟角色对象
                character = new Character();
                character.setId(0L);
                character.setName("Qwen");
                character.setAvatarUrl("http://oss.kon-carol.xyz/airole0.png");
                
                // 直接使用默认系统提示词，不使用RAG
                systemMessage = new org.springframework.ai.chat.messages.SystemMessage(
                    "你是一个乐于助人的 AI 助手。请用简洁、准确的语言回答用户的问题。"
                );
            } else {
                // 1. 获取角色信息
                character = characterService.getCharacterById(request.getCharacterId());
                
                // 2. 检查角色是否可用
                if (!characterService.isCharacterAvailable(request.getCharacterId())) {
                    throw new RuntimeException("角色 " + character.getName() + " 当前不可用，请稍后再试");
                }
                
                // 3. 根据enableRag标志决定是否使用RAG知识检索
                if (Boolean.TRUE.equals(request.getEnableRag())) {
                    // 启用RAG：检索知识并使用增强提示词
                    List<CharacterKnowledge> relevantKnowledge = ragService.searchRelevantKnowledge(
                        request.getCharacterId(),
                        request.getMessage(),
                        5  // 检索top5相关知识
                    );

                    log.info("[handleCharacterChat] RAG模式：检索到 {} 个相关知识条目", relevantKnowledge.size());

                    // 生成包含RAG知识的增强系统提示词
                    systemMessage = promptTemplateService.createCharacterSystemMessageWithRAG(
                        character,
                        relevantKnowledge,
                        Boolean.TRUE.equals(request.getEnableTts())
                    );
                } else {
                    // 禁用RAG：直接使用基础角色提示词
                    log.info("[handleCharacterChat] 基础模式：不使用RAG知识检索");
                    systemMessage = promptTemplateService.createCharacterSystemMessage(
                        character,
                        Boolean.TRUE.equals(request.getEnableTts())
                    );
                }
            }

            // 4. 同步历史记录到messageWindowChatMemory（确保MessageChatMemoryAdvisor能读取到）
            syncHistoryToMessageWindowChatMemory(conversationId);

            // 5. 创建用户消息
            UserMessage userMessage = new UserMessage(request.getMessage());

            // 6. 使用Prompt进行对话
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            // 7. 保存用户消息到自定义存储和messageWindowChatMemory
            customMessageStorageService.saveMessage(conversationId, userMessage, true, request.getAudioUrl(), request.getVoiceDuration());
            // 同时保存到messageWindowChatMemory，确保MessageChatMemoryAdvisor能读取到
            messageWindowChatMemory.add(conversationId, userMessage);

            // 8. 调用ChatClient，包含会话记忆
            String response = chatClient.prompt(prompt)
                    .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            
            // 9. 保存AI回复到自定义存储和messageWindowChatMemory
            AssistantMessage assistantMessage = new AssistantMessage(response);
            customMessageStorageService.saveMessage(conversationId, assistantMessage, false);
            // 同时保存到messageWindowChatMemory
            messageWindowChatMemory.add(conversationId, assistantMessage);

            log.info("[handleCharacterChat] 角色 {} 回复成功: conversationId={}, RAG模式: {}",
                character.getName(), conversationId, request.getEnableRag());
            return response;
            
        } catch (Exception e) {
            log.error("RAG增强角色扮演对话失败: characterId={}, error={}", 
                request.getCharacterId(), e.getMessage(), e);
            throw new RuntimeException("RAG增强角色扮演对话失败: " + e.getMessage());
        }
    }

    /**
     * 处理流式角色扮演对话
     * 集成RAG知识检索，提供更智能的流式角色扮演体验
     * 支持TTS语音合成
     */
    private Flux<String> handleCharacterStreamChat(ChatRequest request, String conversationId) {
        log.info("[handleCharacterStreamChat] 处理流式角色扮演对话: characterId={}, conversationId={}, enableTts={}, enableRag={}",
                request.getCharacterId(), conversationId, request.getEnableTts(), request.getEnableRag());
        
        return Flux.defer(() -> {
            try {
                Character character;
                Message systemMessage;

                // ✅ 特殊处理 ID=0 的普通助手角色
                if (request.getCharacterId() != null && request.getCharacterId() == 0L) {
                    log.info("[handleCharacterStreamChat] 使用Qwen助手模式 (ID=0)");
                    
                    // ✅ 更新 Qwen 会话元数据（如果有 conversationId）
                    if (request.getConversationId() != null && !request.getConversationId().trim().isEmpty()) {
                        Long userId = SecurityUtils.getCurrentUserId();
                        // 更新最后活跃时间
                        qwenConversationService.updateLastActiveTime(userId, request.getConversationId());
                        // 自动生成标题（仅首次）
                        qwenConversationService.generateTitle(userId, request.getConversationId(), request.getMessage());
                    }
                    
                    // ✅ 强制关闭RAG - AI助手不需要角色知识库
                    if (Boolean.TRUE.equals(request.getEnableRag())) {
                        log.info("[handleCharacterStreamChat] AI助手不使用RAG，强制关闭: characterId=0");
                        request.setEnableRag(false);
                    }
                    
                    // 创建虚拟角色对象
                    character = new Character();
                    character.setId(0L);
                    character.setName("Qwen");
                    character.setAvatarUrl("http://oss.kon-carol.xyz/airole0.png");
                    
                    // 直接使用默认系统提示词，不使用RAG
                    systemMessage = new org.springframework.ai.chat.messages.SystemMessage(
                        "你是一个乐于助人的 AI 助手。请用简洁、准确的语言回答用户的问题。"
                    );
                } else {
                    // 1. 获取角色信息
                    character = characterService.getCharacterById(request.getCharacterId());
                    
                    // 2. 检查角色是否可用
                    if (!characterService.isCharacterAvailable(request.getCharacterId())) {
                        log.warn("角色不可用: characterId={}, characterName={}", request.getCharacterId(), character.getName());
                        return Flux.just(
                            "data: {\"error\": \"角色 " + character.getName() + " 当前不可用\"}\n\n",
                            "data: [DONE]\n\n"
                        );
                    }
                    
                    // 3. 根据enableRag标志决定是否使用RAG知识检索
                    if (Boolean.TRUE.equals(request.getEnableRag())) {
                        // 启用RAG：检索知识并使用增强提示词
                        List<CharacterKnowledge> relevantKnowledge = ragService.searchRelevantKnowledge(
                            request.getCharacterId(),
                            request.getMessage(),
                            5  // 检索top5相关知识
                        );

                        log.info("[handleCharacterStreamChat] RAG模式：检索到 {} 个相关知识条目", relevantKnowledge.size());

                        // 生成包含RAG知识的增强系统提示词
                        systemMessage = promptTemplateService.createCharacterSystemMessageWithRAG(
                            character,
                            relevantKnowledge,
                            Boolean.TRUE.equals(request.getEnableTts())
                        );
                    } else {
                        // 禁用RAG：直接使用基础角色提示词
                        log.info("[handleCharacterStreamChat] 基础模式：不使用RAG知识检索");
                        systemMessage = promptTemplateService.createCharacterSystemMessage(
                            character,
                            Boolean.TRUE.equals(request.getEnableTts())
                        );
                    }
                }

                // 4. 同步历史记录到messageWindowChatMemory（确保MessageChatMemoryAdvisor能读取到）
                syncHistoryToMessageWindowChatMemory(conversationId);

                // 5. 创建用户消息
                UserMessage userMessage = new UserMessage(request.getMessage());

                // 6. 使用Prompt进行流式对话
                Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

                // 7. 保存用户消息到自定义存储和messageWindowChatMemory
                // 7. 保存用户消息到自定义存储和messageWindowChatMemory
                customMessageStorageService.saveMessage(conversationId, userMessage, true, request.getAudioUrl(), request.getVoiceDuration());
                // 同时保存到messageWindowChatMemory，确保MessageChatMemoryAdvisor能读取到
                messageWindowChatMemory.add(conversationId, userMessage);

                log.info("[handleCharacterStreamChat] 角色 {} 流式回复开始: conversationId={}, RAG模式: {}",
                    character.getName(), conversationId, request.getEnableRag());
                
                // 如果启用了TTS，需要收集完整响应用于语音合成
                if (Boolean.TRUE.equals(request.getEnableTts())) {
                    return handleStreamingWithTTS(request, conversationId, prompt);
                } else {
                    // 普通流式响应，不需要TTS - 需要收集完整响应用于保存
                    StringBuilder completeResponse = new StringBuilder();
                    
                    return chatClient.prompt(prompt)
                            .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                            .stream()
                            .content()
                            .map(chunk -> {
                                // 收集文本块用于后续保存
                                completeResponse.append(chunk);
                                return "data:" + chunk + "\n\n";
                            })
                            .concatWith(
                                // 流式响应完成后，保存完整的AI回复
                                Mono.fromCallable(() -> {
                                    String fullResponse = completeResponse.toString();
                                    if (!fullResponse.isEmpty()) {
                                        AssistantMessage assistantMessage = new AssistantMessage(fullResponse);
                                        customMessageStorageService.saveMessage(conversationId, assistantMessage, false);
                                        // 同时保存到messageWindowChatMemory
                                        messageWindowChatMemory.add(conversationId, assistantMessage);
                                        log.info("[handleCharacterStreamChat] 保存AI流式回复: conversationId={}, length={}", 
                                                conversationId, fullResponse.length());
                                    }
                                    return "data:[DONE]\n\n";
                                }).subscribeOn(Schedulers.boundedElastic())
                            );
                }
                        
            } catch (Exception e) {
                log.error("RAG增强流式角色扮演对话初始化失败: characterId={}, error={}", 
                        request.getCharacterId(), e.getMessage(), e);
                return Flux.just(
                    "data: {\"error\": \"角色扮演初始化失败: " + e.getMessage() + "\"}\n\n",
                    "data: [DONE]\n\n"
                );
            }
        });
    }
    
    /**
     * 处理带TTS的流式响应
     * 先进行流式文本输出，然后生成TTS音频
     */
    private Flux<String> handleStreamingWithTTS(ChatRequest request, String conversationId, Prompt prompt) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        // 用于收集完整响应文本
        StringBuilder completeResponse = new StringBuilder();
        
        return chatClient.prompt(prompt)
                .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .map(chunk -> {
                    // 收集文本块用于后续TTS处理
                    completeResponse.append(chunk);
                    return "data:" + chunk + "\n\n";
                })
                .concatWith(
                    // 流式文本完成后，处理TTS
                    Mono.fromCallable(() -> {
                        String fullResponse = completeResponse.toString();
                        log.info("流式响应完成，开始TTS处理: conversationId={}, textLength={}", 
                                conversationId, fullResponse.length());
                        
                        try {
                            // 生成TTS音频
                            TtsSynthesisResponse ttsResponse = chatTtsIntegrationService.generateSpeechForChatReply(
                                fullResponse,
                                request.getCharacterId(),
                                userId != null ? userId.toString() : "anonymous",
                                request.getLanguageType()
                            );
                            
                            // 保存完整的AI回复到自定义存储（先不包含audioUrl）
                            if (!fullResponse.isEmpty()) {
                                AssistantMessage assistantMessage = new AssistantMessage(fullResponse);
                                customMessageStorageService.saveMessage(conversationId, assistantMessage, false);
                                // 同时保存到messageWindowChatMemory
                                messageWindowChatMemory.add(conversationId, assistantMessage);
                                log.info("[handleStreamingWithTTS] 保存AI流式回复: conversationId={}, length={}", 
                                        conversationId, fullResponse.length());
                                
                                // 如果TTS成功，持久化音频到OSS并更新消息
                                if (ttsResponse != null && ttsResponse.getSuccess()) {
                                    Integer voiceDuration = ttsResponse.getDuration() != null ? 
                                        ttsResponse.getDuration().intValue() : null;
                                    
                                    // 持久化TTS音频到OSS（下载临时URL并上传到OSS获取永久URL）
                                    String permanentAudioUrl = ttsResponse.getAudioUrl();
                                    try {
                                        TtsPersistenceResult persistenceResult = ttsAudioPersistenceService.persistTtsAudio(
                                            ttsResponse.getAudioUrl(),
                                            userId,
                                            request.getCharacterId()
                                        );
                                        permanentAudioUrl = persistenceResult.getAudioUrl();
                                        
                                        // 如果TTS响应中没有时长，使用持久化计算的时长
                                        if (voiceDuration == null || voiceDuration == 0) {
                                            voiceDuration = persistenceResult.getDuration();
                                        }
                                        
                                        log.info("[handleStreamingWithTTS] TTS音频持久化到OSS成功: conversationId={}, ossUrl={}, duration={}", 
                                                conversationId, permanentAudioUrl, voiceDuration);
                                    } catch (Exception persistError) {
                                        log.error("[handleStreamingWithTTS] TTS音频持久化失败，使用临时URL: conversationId={}, error={}", 
                                                conversationId, persistError.getMessage());
                                        // 持久化失败时仍然使用临时URL，不影响主流程
                                    }
                                    
                                    customMessageStorageService.updateMessageAudioInfo(conversationId, fullResponse, 
                                        permanentAudioUrl, voiceDuration);
                                    log.info("[handleStreamingWithTTS] 更新AI回复音频信息: conversationId={}, audioUrl={}, duration={}", 
                                            conversationId, permanentAudioUrl, voiceDuration);
                                }
                            }
                            
                            if (ttsResponse != null && ttsResponse.getSuccess()) {
                                // 持久化TTS音频到OSS（下载临时URL并上传到OSS获取永久URL）
                                String audioUrlToReturn = ttsResponse.getAudioUrl();
                                Integer durationToReturn = ttsResponse.getDuration() != null ? ttsResponse.getDuration().intValue() : 0;
                                
                                try {
                                    TtsPersistenceResult persistenceResult = ttsAudioPersistenceService.persistTtsAudio(
                                        ttsResponse.getAudioUrl(),
                                        userId,
                                        request.getCharacterId()
                                    );
                                    audioUrlToReturn = persistenceResult.getAudioUrl();
                                    if (durationToReturn == 0 && persistenceResult.getDuration() != null) {
                                        durationToReturn = persistenceResult.getDuration();
                                    }
                                    
                                    log.info("流式TTS合成成功并持久化到OSS: conversationId={}, ossUrl={}, duration={}", 
                                            conversationId, audioUrlToReturn, durationToReturn);
                                } catch (Exception persistError) {
                                    log.error("流式TTS持久化失败，返回临时URL: conversationId={}, error={}", 
                                            conversationId, persistError.getMessage());
                                    // 持久化失败时仍然返回临时URL，不影响前端播放
                                }
                                
                                // 返回TTS信息作为SSE事件（使用OSS永久URL或临时URL）
                                return "data:{\"type\":\"tts\",\"audioUrl\":\"" + audioUrlToReturn + 
                                       "\",\"voice\":\"" + (ttsResponse.getVoice() != null ? ttsResponse.getVoice() : "") + 
                                       "\",\"duration\":" + durationToReturn + 
                                       ",\"success\":true}\n\n";
                            } else if (ttsResponse == null) {
                                log.warn("流式TTS合成跳过: conversationId={}, 文本不适合语音合成", conversationId);
                                
                                return "data:{\"type\":\"tts\",\"success\":false,\"error\":\"文本不适合语音合成\"}\n\n";
                            } else {
                                log.warn("流式TTS合成失败: conversationId={}, error={}", 
                                        conversationId, ttsResponse.getErrorMessage());
                                
                                return "data:{\"type\":\"tts\",\"success\":false,\"error\":\"" + 
                                       ttsResponse.getErrorMessage() + "\"}\n\n";
                            }
                        } catch (Exception ttsError) {
                            log.error("流式TTS处理异常: conversationId={}, error={}", 
                                    conversationId, ttsError.getMessage(), ttsError);
                            
                            return "data:{\"type\":\"tts\",\"success\":false,\"error\":\"TTS处理异常: " + 
                                   ttsError.getMessage() + "\"}\n\n";
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic()) // 在后台线程处理TTS
                )
                .concatWith(Mono.just("data:[DONE]\n\n"));
    }

    /**
     * 生成会话ID
     * 规则: 基于JWT中的userId和characterId生成
     * ✅ 直接使用userId（Long类型），不再需要字符串转换
     */
    private String generateConversationId(ChatRequest request, Long userId) {
        // 基于JWT中的用户ID和角色ID生成会话ID
        String safeUserId = userId != null ? userId.toString() : "anonymous";
        Long characterId = request.getCharacterId();
        
        // 角色对话：使用角色ID
        if (characterId != null && characterId != 0L) {
            String conversationId = String.format("user_%s_char_%d", safeUserId, characterId);
            log.debug("[generateConversationId] 生成角色对话ID: {}", conversationId);
            return conversationId;
        }
        
        // Qwen 会话：使用 conversationId（如果提供）
        if (characterId != null && characterId == 0L && request.getConversationId() != null && !request.getConversationId().trim().isEmpty()) {
            String conversationId = String.format("user_%s_qwen_%s", safeUserId, request.getConversationId());
            log.debug("[generateConversationId] 生成Qwen会话ID: {}", conversationId);
            return conversationId;
        }
        
        // 兼容旧版：通用对话或没有 conversationId 的 Qwen
        String conversationId = String.format("user_%s_general", safeUserId);
        log.debug("[generateConversationId] 生成通用对话ID: {}", conversationId);
        return conversationId;
    }

    /**
     * 获取聊天历史 - 新接口，适配Spring AI Redis存储
     * @param characterId 角色ID，可选参数。如果不提供则查询所有角色的历史记录
     */
    @GetMapping("/history")
    public ChatHistoryResponse getChatHistoryNew(
            @RequestParam(required = false) Long characterId,
            @RequestParam(required = false) String conversationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[getChatHistoryNew] 获取聊天历史: userId={}, characterId={}, conversationId={}", 
                userId, characterId, conversationId);
        
        if (userId == null) {
            log.warn("[getChatHistoryNew] 用户未登录");
            throw new IllegalStateException("用户未登录");
        }
        
        try {
            ChatHistoryResponse response;
            if (characterId != null || conversationId != null) {
                // 查询指定角色或会话的历史记录
                response = conversationHistoryService.getChatHistory(characterId, userId, conversationId);
            } else {
                // 查询所有角色的历史记录
                response = conversationHistoryService.getAllChatHistory(userId);
            }
            
            log.info("[getChatHistoryNew] 查询成功: 返回 {} 条消息, 数据来源: {}", 
                    response.getTotal(), response.getSourceStats());
            return response;
            
        } catch (Exception e) {
            log.error("[getChatHistoryNew] 查询失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询聊天历史失败: " + e.getMessage());
        }
    }

    /**
     * 清空对话 - 新接口
     */
    @DeleteMapping("/conversation/{characterId}")
    public void clearConversationNew(@PathVariable Long characterId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[clearConversationNew] 清空对话: userId={}, characterId={}", userId, characterId);
        
        if (userId == null) {
            log.warn("[clearConversationNew] 用户未登录");
            throw new IllegalStateException("用户未登录");
        }
        
        try {
            // 生成conversationId用于清除同步缓存
            ChatRequest tempRequest = new ChatRequest();
            tempRequest.setCharacterId(characterId);
            String conversationId = generateConversationId(tempRequest, userId);
            
            boolean success = conversationHistoryService.clearConversation(characterId, userId);
            
            if (success) {
                // 清除同步缓存，允许重新同步
                clearSyncCache(conversationId);
                log.info("[clearConversationNew] 对话清空成功");
            } else {
                log.warn("[clearConversationNew] 对话清空失败");
                throw new RuntimeException("对话清空失败");
            }
            
        } catch (Exception e) {
            log.error("[clearConversationNew] 清空失败: {}", e.getMessage(), e);
            throw new RuntimeException("清空对话失败: " + e.getMessage());
        }
    }

    /**
     * 清空所有对话记录 - 新接口
     */
    @DeleteMapping("/conversation/all")
    public void clearAllConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[clearAllConversations] 清空所有对话: userId={}", userId);
        
        if (userId == null) {
            log.warn("[clearAllConversations] 用户未登录");
            throw new IllegalStateException("用户未登录");
        }
        
        try {
            boolean success = conversationHistoryService.clearAllConversations(userId);
            
            if (success) {
                // 清空所有该用户的同步缓存（通过前缀匹配）
                String userIdPrefix = "user_" + userId + "_";
                syncedConversations.removeIf(convId -> convId.startsWith(userIdPrefix));
                log.info("[clearAllConversations] 所有对话清空成功，同步缓存已清除");
            } else {
                log.warn("[clearAllConversations] 所有对话清空失败");
                throw new RuntimeException("所有对话清空失败");
            }
            
        } catch (Exception e) {
            log.error("[clearAllConversations] 清空所有对话失败: {}", e.getMessage(), e);
            throw new RuntimeException("清空所有对话失败: " + e.getMessage());
        }
    }

    /**
     * 获取聊天次数统计
     */
    @GetMapping("/chat-stats")
    public Map<String, Object> getChatStats(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> stats = new HashMap<>();

        if (userId == null) {
            // 游客模式
            String sessionId = getGuestSessionId(httpRequest);
            int guestChatCount = guestChatLimitService.getGuestChatCount(sessionId);

            stats.put("isGuest", true);
            stats.put("chatCount", guestChatCount);
            stats.put("maxChatCount", IGuestChatLimitService.MAX_GUEST_CHAT_COUNT);
            stats.put("remainingChats", IGuestChatLimitService.MAX_GUEST_CHAT_COUNT - guestChatCount);

            log.info("[getChatStats] 游客聊天统计: sessionId={}, chatCount={}", sessionId, guestChatCount);
        } else {
            // 登录用户模式
            stats.put("isGuest", false);
            stats.put("chatCount", 0); // TODO: 从数据库获取用户今日聊天次数
            stats.put("maxChatCount", -1); // 无限制
            stats.put("remainingChats", -1); // 无限制

            log.info("[getChatStats] 用户聊天统计: userId={}", userId);
        }

        return stats;
    }

    /**
     * 获取游客会话ID
     * 使用IP地址和User-Agent生成唯一标识
     */
    private String getGuestSessionId(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // 生成游客会话ID：IP + UserAgent的hash值
        String sessionData = clientIp + "_" + (userAgent != null ? userAgent : "unknown");
        String sessionId = "guest_" + Math.abs(sessionData.hashCode());

        log.debug("[getGuestSessionId] 生成游客会话ID: clientIp={}, sessionId={}", clientIp, sessionId);
        return sessionId;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 更新消息的语音时长
     * 前端录音完成后调用此接口更新消息的语音时长
     */
    @PostMapping("/update-voice-duration")
    public Map<String, Object> updateVoiceDuration(@Valid @RequestBody UpdateVoiceDurationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        log.info("[updateVoiceDuration] 收到更新语音时长请求: userId={}, conversationId={}, voiceDuration={}", 
                userId, request.getConversationId(), request.getVoiceDuration());
        
        // 验证用户认证
        if (userId == null) {
            log.warn("[updateVoiceDuration] 用户未认证，拒绝请求");
            throw new RuntimeException("用户未认证");
        }
        
        try {
            // 如果请求中没有提供conversationId，则根据characterId和userId生成
            String conversationId = request.getConversationId();
            if (conversationId == null || conversationId.trim().isEmpty()) {
                if (request.getCharacterId() != null) {
                    conversationId = String.format("user_%d_char_%d", userId, request.getCharacterId());
                } else {
                    conversationId = String.format("user_%d_general", userId);
                }
                log.info("[updateVoiceDuration] 生成会话ID: {}", conversationId);
            }
            
            // 更新用户消息的语音时长
            boolean success = customMessageStorageService.updateUserMessageVoiceDuration(
                conversationId, 
                request.getMessageContent(), 
                request.getVoiceDuration()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("conversationId", conversationId);
            response.put("voiceDuration", request.getVoiceDuration());
            
            if (success) {
                log.info("[updateVoiceDuration] 语音时长更新成功: conversationId={}, voiceDuration={}", 
                        conversationId, request.getVoiceDuration());
                response.put("message", "语音时长更新成功");
            } else {
                log.warn("[updateVoiceDuration] 语音时长更新失败，未找到匹配的消息: conversationId={}", conversationId);
                response.put("message", "未找到匹配的消息");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("[updateVoiceDuration] 更新语音时长失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新语音时长失败: " + e.getMessage());
        }
    }

    /**
     * 同步历史记录到messageWindowChatMemory（优化版）
     * 只在第一次调用时同步历史记录，后续消息通过messageWindowChatMemory.add()直接保存
     * 优化策略：
     * 1. 使用缓存机制，避免重复检查已同步的会话
     * 2. 只在messageWindowChatMemory为空且customMessageStorageService有历史记录时才同步
     * 3. 同步后标记为已同步，后续消息通过add()方法直接保存，无需同步
     */
    private void syncHistoryToMessageWindowChatMemory(String conversationId) {
        try {
            // 优化1: 如果已经同步过，直接返回（避免重复检查Redis）
            if (syncedConversations.contains(conversationId)) {
                log.debug("[syncHistory] 会话 {} 已同步过，跳过检查", conversationId);
                return;
            }

            // 优化2: 快速检查messageWindowChatMemory中是否已有记录
            List<Message> existingMessages = messageWindowChatMemory.get(conversationId);
            if (!existingMessages.isEmpty()) {
                log.debug("[syncHistory] messageWindowChatMemory中已有 {} 条记录，标记为已同步", existingMessages.size());
                syncedConversations.add(conversationId);
                return;
            }

            // 优化3: 只在messageWindowChatMemory为空时，才从customMessageStorageService同步历史记录
            List<CustomMessageStorageService.StoredMessage> customMessages = 
                customMessageStorageService.getMessages(conversationId);
            
            if (customMessages.isEmpty()) {
                log.debug("[syncHistory] 自定义存储中没有历史记录，标记为已同步");
                syncedConversations.add(conversationId);
                return;
            }

            log.info("[syncHistory] 首次同步历史记录: conversationId={}, count={}", 
                    conversationId, customMessages.size());

            // 将自定义存储的消息转换为Spring AI的Message对象并添加到messageWindowChatMemory
            // 注意：只同步历史记录，当前消息会通过add()方法单独保存
            for (CustomMessageStorageService.StoredMessage storedMessage : customMessages) {
                Message message;
                if (storedMessage.getIsUser()) {
                    message = new UserMessage(storedMessage.getContent());
                } else {
                    message = new AssistantMessage(storedMessage.getContent());
                }
                messageWindowChatMemory.add(conversationId, message);
            }

            // 标记为已同步，后续消息通过add()方法直接保存，无需再次同步
            syncedConversations.add(conversationId);
            
            log.info("[syncHistory] 历史记录同步完成: conversationId={}, count={}", 
                    conversationId, customMessages.size());

        } catch (Exception e) {
            log.error("[syncHistory] 同步历史记录失败: conversationId={}, error={}", 
                    conversationId, e.getMessage(), e);
            // 不抛出异常，避免影响主流程
            // 失败时不标记为已同步，下次会重试
        }
    }
    
    /**
     * 清除会话的同步缓存（用于清空对话时调用）
     */
    private void clearSyncCache(String conversationId) {
        syncedConversations.remove(conversationId);
        log.debug("[syncHistory] 清除同步缓存: conversationId={}", conversationId);
    }
    
    // ==================== Qwen 会话管理 API ====================
    
    /**
     * 创建新的 Qwen 会话
     */
    @PostMapping("/qwen/conversations")
    public QwenConversationResponse createQwenConversation() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[createQwenConversation] 创建新会话: userId={}", userId);
        return qwenConversationService.createConversation(userId);
    }
    
    /**
     * 列出用户的所有 Qwen 会话
     */
    @GetMapping("/qwen/conversations")
    public List<QwenConversationInfo> listQwenConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[listQwenConversations] 列出会话: userId={}", userId);
        return qwenConversationService.listConversations(userId);
    }
    
    /**
     * 获取会话详情
     */
    @GetMapping("/qwen/conversations/{conversationId}")
    public QwenConversationInfo getQwenConversation(@PathVariable String conversationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[getQwenConversation] 获取会话详情: userId={}, conversationId={}", userId, conversationId);
        return qwenConversationService.getConversationInfo(userId, conversationId);
    }
    
    /**
     * 删除 Qwen 会话
     */
    @DeleteMapping("/qwen/conversations/{conversationId}")
    public void deleteQwenConversation(@PathVariable String conversationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[deleteQwenConversation] 删除会话: userId={}, conversationId={}", userId, conversationId);
        qwenConversationService.deleteConversation(userId, conversationId);
    }
    
    /**
     * 重命名 Qwen 会话
     */
    @PatchMapping("/qwen/conversations/{conversationId}")
    public void renameQwenConversation(
            @PathVariable String conversationId,
            @Valid @RequestBody RenameConversationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[renameQwenConversation] 重命名会话: userId={}, conversationId={}, newTitle={}", 
                userId, conversationId, request.getTitle());
        qwenConversationService.renameConversation(userId, conversationId, request.getTitle());
    }
}
