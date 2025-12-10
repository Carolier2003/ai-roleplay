package com.carol.backend.service.impl;

import com.carol.backend.dto.QwenConversationInfo;
import com.carol.backend.dto.QwenConversationResponse;
import com.carol.backend.service.QwenConversationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QwenConversationServiceImpl implements QwenConversationService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CONVERSATION_KEY_PREFIX = "qwen:conversations:";

    @Override
    public QwenConversationResponse createConversation(Long userId) {
        String conversationId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        QwenConversationInfo info = QwenConversationInfo.builder()
                .conversationId(conversationId)
                .title("新对话")
                .createdAt(now)
                .lastActiveTime(now)
                .messageCount(0)
                .lastMessage("")
                .build();

        saveConversationInfo(userId, info);

        return QwenConversationResponse.builder()
                .conversationId(conversationId)
                .createdAt(now)
                .title("新对话")
                .build();
    }

    @Override
    public List<QwenConversationInfo> listConversations(Long userId) {
        String key = CONVERSATION_KEY_PREFIX + userId;
        List<Object> values = redisTemplate.opsForHash().values(key);

        List<QwenConversationInfo> conversations = new ArrayList<>();
        for (Object value : values) {
            try {
                conversations.add(objectMapper.readValue((String) value, QwenConversationInfo.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to parse conversation info", e);
            }
        }

        return conversations.stream()
                .sorted(Comparator.comparing(QwenConversationInfo::getLastActiveTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public QwenConversationInfo getConversationInfo(Long userId, String conversationId) {
        String key = CONVERSATION_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForHash().get(key, conversationId);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue((String) value, QwenConversationInfo.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse conversation info", e);
            return null;
        }
    }

    @Override
    public void deleteConversation(Long userId, String conversationId) {
        String key = CONVERSATION_KEY_PREFIX + userId;
        redisTemplate.opsForHash().delete(key, conversationId);
    }

    @Override
    public void renameConversation(Long userId, String conversationId, String newTitle) {
        QwenConversationInfo info = getConversationInfo(userId, conversationId);
        if (info != null) {
            info = QwenConversationInfo.builder()
                    .conversationId(info.getConversationId())
                    .title(newTitle)
                    .lastMessage(info.getLastMessage())
                    .lastActiveTime(info.getLastActiveTime())
                    .createdAt(info.getCreatedAt())
                    .messageCount(info.getMessageCount())
                    .build();
            saveConversationInfo(userId, info);
        }
    }

    @Override
    public void updateLastActiveTime(Long userId, String conversationId) {
        QwenConversationInfo info = getConversationInfo(userId, conversationId);
        if (info != null) {
            info = QwenConversationInfo.builder()
                    .conversationId(info.getConversationId())
                    .title(info.getTitle())
                    .lastMessage(info.getLastMessage())
                    .lastActiveTime(System.currentTimeMillis())
                    .createdAt(info.getCreatedAt())
                    .messageCount(info.getMessageCount())
                    .build();
            saveConversationInfo(userId, info);
        }
    }

    @Override
    public void generateTitle(Long userId, String conversationId, String firstMessage) {
        QwenConversationInfo info = getConversationInfo(userId, conversationId);
        if (info != null && "新对话".equals(info.getTitle())) {
            // Only generate title if it's still the default "新对话"
            String title = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
            renameConversation(userId, conversationId, title);
            log.info("[generateTitle] Auto-generated title for conversation {}: {}", conversationId, title);
        }
    }

    private void saveConversationInfo(Long userId, QwenConversationInfo info) {
        String key = CONVERSATION_KEY_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForHash().put(key, info.getConversationId(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize conversation info", e);
        }
    }
}
