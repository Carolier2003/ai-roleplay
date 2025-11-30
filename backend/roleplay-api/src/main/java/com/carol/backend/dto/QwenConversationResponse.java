package com.carol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenConversationResponse {
    private String conversationId;
    private Long createdAt;
    private String title;
}
