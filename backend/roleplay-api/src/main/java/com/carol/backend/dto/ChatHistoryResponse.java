package com.carol.backend.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 聊天历史查询响应
 * 
 * @author carol
 */
@Data
@Accessors(chain = true)
public class ChatHistoryResponse {
    
    /**
     * 消息列表
     */
    private List<ConversationMessageVO> messages;
    
    /**
     * 消息总数
     */
    private Integer total;
    
    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;
    
    /**
     * 数据来源统计
     */
    private Map<String, Long> sourceStats;
    
    /**
     * 查询时间范围（天数）
     */
    private Integer queryDays;
}
