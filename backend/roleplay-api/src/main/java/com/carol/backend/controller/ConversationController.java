package com.carol.backend.controller;

import com.carol.backend.entity.Conversation;
import com.carol.backend.entity.ConversationMessage;
import com.carol.backend.mapper.ConversationMapper;
import com.carol.backend.mapper.ConversationMessageMapper;
import com.carol.backend.service.IConversationSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 对话数据仓库控制器
 * 提供对话同步、导出、分析等功能
 * 
 * @author carol
 */
@Slf4j
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "对话数据仓库", description = "对话同步、导出、分析相关接口")
public class ConversationController {
    
    private final IConversationSyncService syncService;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    
    // 配置ObjectMapper支持JavaTime
    private final ObjectMapper objectMapper = new ObjectMapper() {{
        registerModule(new JavaTimeModule());
    }};
    
    /**
     * 同步指定会话的对话数据
     */
    @PostMapping("/sync/{sessionId}")
    @Operation(summary = "同步指定会话", description = "将Redis中的指定会话数据同步到MySQL数据仓库")
    public ResponseEntity<Map<String, Object>> syncConversation(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        log.info("收到同步会话请求: sessionId={}", sessionId);
        
        try {
            boolean success = syncService.syncConversation(sessionId);
            
            String message = success ? "同步成功" : "同步失败 - 请检查日志获取详细信息";
            
            Map<String, Object> response = Map.of(
                "success", success,
                "sessionId", sessionId,
                "message", message,
                "timestamp", LocalDateTime.now()
            );
            
            log.info("同步会话响应: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("同步会话异常: sessionId={}, error={}", sessionId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "sessionId", sessionId,
                "message", "同步异常: " + e.getMessage(),
                "error", e.getClass().getSimpleName(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 批量同步所有对话数据
     */
    @PostMapping("/sync/all")
    @Operation(summary = "批量同步所有对话", description = "将Redis中的所有对话数据同步到MySQL数据仓库")
    public ResponseEntity<Map<String, Object>> syncAllConversations() {
        
        log.info("收到批量同步请求");
        
        try {
            Map<String, Object> result = syncService.syncAllConversations();
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("批量同步失败: error={}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "批量同步失败: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
    
    /**
     * 获取对话列表
     */
    @GetMapping
    @Operation(summary = "获取对话列表", description = "分页查询对话列表")
    public ResponseEntity<Map<String, Object>> getConversations(
            @Parameter(description = "角色ID") @RequestParam(required = false) Long characterId,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "20") int size) {
        
        try {
            // 这里可以实现分页查询逻辑
            // 为简化，返回基本结构
            
            return ResponseEntity.ok(Map.of(
                "conversations", List.of(),
                "total", 0,
                "page", page,
                "size", size,
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("查询对话列表失败: error={}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "查询失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取对话详情
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "获取对话详情", description = "根据会话ID获取对话详情和消息列表")
    public ResponseEntity<Map<String, Object>> getConversationDetail(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        try {
            Conversation conversation = conversationMapper.selectBySessionId(sessionId);
            if (conversation == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<ConversationMessage> messages = conversationMessageMapper
                .selectByConversationId(conversation.getId());
            
            return ResponseEntity.ok(Map.of(
                "conversation", conversation,
                "messages", messages,
                "messageCount", messages.size(),
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("查询对话详情失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "查询失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 导出对话数据为JSON格式
     */
    @GetMapping("/{sessionId}/export/json")
    @Operation(summary = "导出对话JSON", description = "将对话数据导出为JSON格式")
    public ResponseEntity<String> exportConversationJson(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        try {
            Conversation conversation = conversationMapper.selectBySessionId(sessionId);
            if (conversation == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<ConversationMessage> messages = conversationMessageMapper
                .selectByConversationId(conversation.getId());
            
            // 构建导出数据
            Map<String, Object> exportData = Map.of(
                "conversation", conversation,
                "messages", messages,
                "exportTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "exportFormat", "JSON"
            );
            
            // 更新导出次数
            conversation.setExportCount((conversation.getExportCount() != null ? 
                conversation.getExportCount() : 0) + 1);
            conversationMapper.updateById(conversation);
            
            // 使用ObjectMapper序列化
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(exportData);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"conversation_" + sessionId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
                
        } catch (Exception e) {
            log.error("导出对话JSON失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(500).body("{\"error\":\"导出失败: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * 导出对话数据为Markdown格式
     */
    @GetMapping("/{sessionId}/export/markdown")
    @Operation(summary = "导出对话Markdown", description = "将对话数据导出为Markdown格式")
    public ResponseEntity<String> exportConversationMarkdown(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        try {
            Conversation conversation = conversationMapper.selectBySessionId(sessionId);
            if (conversation == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<ConversationMessage> messages = conversationMessageMapper
                .selectByConversationId(conversation.getId());
            
            StringBuilder markdown = new StringBuilder();
            markdown.append("# ").append(conversation.getTitle()).append("\n\n");
            markdown.append("**会话ID**: ").append(sessionId).append("\n");
            markdown.append("**用户**: ").append(conversation.getUserId()).append("\n");
            markdown.append("**开始时间**: ").append(conversation.getStartTime()).append("\n");
            markdown.append("**消息数量**: ").append(messages.size()).append("\n\n");
            
            markdown.append("## 对话内容\n\n");
            
            for (ConversationMessage message : messages) {
                String messageTypeText = getMessageTypeText(message.getMessageType());
                markdown.append("### ").append(messageTypeText).append("\n");
                markdown.append(message.getContent()).append("\n\n");
            }
            
            markdown.append("---\n");
            markdown.append("*导出时间: ").append(LocalDateTime.now()).append("*\n");
            
            // 更新导出次数
            conversation.setExportCount((conversation.getExportCount() != null ? 
                conversation.getExportCount() : 0) + 1);
            conversationMapper.updateById(conversation);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"conversation_" + sessionId + ".md\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(markdown.toString());
                
        } catch (Exception e) {
            log.error("导出对话Markdown失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(500).body("导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取对话分析数据
     */
    @GetMapping("/analytics")
    @Operation(summary = "获取对话分析", description = "获取指定时间范围内的对话分析数据")
    public ResponseEntity<Map<String, Object>> getConversationAnalytics(
            @Parameter(description = "开始日期") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        try {
            List<Map<String, Object>> analytics = conversationMapper
                .getConversationAnalytics(startDate.toString(), endDate.toString());
            
            return ResponseEntity.ok(Map.of(
                "analytics", analytics,
                "period", Map.of("start", startDate, "end", endDate),
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("获取对话分析失败: error={}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "分析失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取消息类型文本
     */
    private String getMessageTypeText(Integer messageType) {
        if (messageType == null) return "未知";
        
        switch (messageType) {
            case 1: return "👤 用户";
            case 2: return "🤖 AI助手";
            case 3: return "⚙️ 系统";
            default: return "❓ 未知";
        }
    }
}
