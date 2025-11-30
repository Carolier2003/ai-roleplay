package com.carol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenConversationInfo {
    private String conversationId;
    private String title;
    private String lastMessage;
    private Long lastActiveTime;
    private Long createdAt;
    private Integer messageCount;
}
