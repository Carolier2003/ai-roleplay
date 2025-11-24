package com.carol.backend.controller;

import com.carol.backend.entity.CharacterKnowledge;
import com.carol.backend.service.ICharacterKnowledgeRAGService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识管理控制器
 * 提供知识导入、检索、管理等API
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private ICharacterKnowledgeRAGService ragService;

    /**
     * 批量导入知识
     */
    @PostMapping("/import/batch")
    public ResponseEntity<?> importKnowledgeBatch(@RequestBody ImportBatchRequest request) {
        try {
            log.info("接收批量知识导入请求，角色ID: {}, 知识数量: {}", 
                request.getCharacterId(), request.getKnowledgeList().size());
            
            // 转换为实体对象
            List<CharacterKnowledge> knowledgeList = new ArrayList<>();
            for (KnowledgeItem item : request.getKnowledgeList()) {
                CharacterKnowledge knowledge = convertToEntity(item, request.getCharacterId());
                knowledgeList.add(knowledge);
            }
            
            // 导入到RAG系统
            ragService.importKnowledgeBatch(knowledgeList);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "知识导入成功");
            response.put("imported_count", knowledgeList.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量导入知识失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 单个知识导入
     */
    @PostMapping("/import/single")
    public ResponseEntity<?> importSingleKnowledge(@RequestBody ImportSingleRequest request) {
        try {
            CharacterKnowledge knowledge = convertToEntity(request.getKnowledge(), request.getCharacterId());
            ragService.importSingleKnowledge(knowledge);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "知识导入成功");
            response.put("knowledge_id", knowledge.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("单个知识导入失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 文本格式知识导入（支持脚本调用）
     */
    @PostMapping("/import/text")
    public ResponseEntity<?> importTextKnowledge(@RequestBody TextImportRequest request) {
        try {
            log.info("接收文本知识导入请求，角色ID: {}", request.getCharacterId());
            
            List<CharacterKnowledge> knowledgeList = new ArrayList<>();
            
            for (TextKnowledgeItem item : request.getKnowledgeItems()) {
                CharacterKnowledge knowledge = new CharacterKnowledge();
                knowledge.setCharacterId(request.getCharacterId());
                knowledge.setTitle(item.getTitle());
                knowledge.setContent(item.getContent());
                knowledge.setKnowledgeType(item.getKnowledgeType() != null ? item.getKnowledgeType() : "KNOWLEDGE");
                knowledge.setImportanceScore(item.getImportanceScore() != null ? item.getImportanceScore() : 5);
                knowledge.setSource(item.getSource() != null ? item.getSource() : "script_import");
                knowledge.setSourceUrl(item.getSourceUrl());
                knowledge.setLanguage("zh");
                knowledge.setStatus(1);
                
                // 设置标签
                if (item.getTags() != null && !item.getTags().isEmpty()) {
                    knowledge.setTagList(item.getTags());
                }
                
                knowledgeList.add(knowledge);
            }
            
            ragService.importKnowledgeBatch(knowledgeList);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文本知识导入成功");
            response.put("imported_count", knowledgeList.size());
            response.put("character_id", request.getCharacterId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("文本知识导入失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 检索相关知识
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchKnowledge(@RequestBody SearchRequest request) {
        try {
            List<CharacterKnowledge> knowledgeList = ragService.searchRelevantKnowledge(
                request.getCharacterId(), 
                request.getQuery(), 
                request.getTopK() != null ? request.getTopK() : 5
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("knowledge_list", knowledgeList);
            response.put("count", knowledgeList.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("知识检索失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检索失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取角色知识统计
     */
    @GetMapping("/stats/{characterId}")
    public ResponseEntity<?> getKnowledgeStats(@PathVariable Long characterId) {
        try {
            Map<String, Object> stats = ragService.getKnowledgeStats(characterId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取知识统计失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取统计失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 重新同步到向量数据库
     */
    @PostMapping("/resync/{characterId}")
    public ResponseEntity<?> resyncVectorStore(@PathVariable Long characterId) {
        try {
            ragService.resyncToVectorStore(characterId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "重新同步成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("重新同步失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "同步失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取核心知识
     */
    @GetMapping("/core/{characterId}")
    public ResponseEntity<?> getCoreKnowledge(@PathVariable Long characterId, @RequestParam(defaultValue = "10") int limit) {
        try {
            List<CharacterKnowledge> knowledgeList = ragService.getCoreKnowledge(characterId, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("knowledge_list", knowledgeList);
            response.put("count", knowledgeList.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取核心知识失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取知识列表（分页）
     */
    @GetMapping("/list")
    public ResponseEntity<?> getKnowledgeList(
            @RequestParam(required = false) Long characterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<CharacterKnowledge> result = 
                    ragService.getKnowledgeList(characterId, page, size, keyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取知识列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新知识
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateKnowledge(@PathVariable Long id, @RequestBody CharacterKnowledge knowledge) {
        try {
            knowledge.setId(id);
            ragService.updateKnowledge(knowledge);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("更新知识失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除知识
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteKnowledge(@PathVariable Long id) {
        try {
            ragService.deleteKnowledge(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("删除知识失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== 私有方法 ====================

    private CharacterKnowledge convertToEntity(KnowledgeItem item, Long characterId) {
        CharacterKnowledge knowledge = new CharacterKnowledge();
        knowledge.setCharacterId(characterId);
        knowledge.setTitle(item.getTitle());
        knowledge.setContent(item.getContent());
        knowledge.setKnowledgeType(item.getKnowledgeType());
        knowledge.setImportanceScore(item.getImportanceScore());
        knowledge.setSource(item.getSource());
        knowledge.setSourceUrl(item.getSourceUrl());
        knowledge.setLanguage("zh");
        knowledge.setStatus(1);
        
        if (item.getTags() != null) {
            knowledge.setTagList(item.getTags());
        }
        
        return knowledge;
    }

    // ==================== 请求/响应对象 ====================

    public static class ImportBatchRequest {
        private Long characterId;
        private List<KnowledgeItem> knowledgeList;

        // Getters and Setters
        public Long getCharacterId() { return characterId; }
        public void setCharacterId(Long characterId) { this.characterId = characterId; }
        public List<KnowledgeItem> getKnowledgeList() { return knowledgeList; }
        public void setKnowledgeList(List<KnowledgeItem> knowledgeList) { this.knowledgeList = knowledgeList; }
    }

    public static class ImportSingleRequest {
        private Long characterId;
        private KnowledgeItem knowledge;

        // Getters and Setters
        public Long getCharacterId() { return characterId; }
        public void setCharacterId(Long characterId) { this.characterId = characterId; }
        public KnowledgeItem getKnowledge() { return knowledge; }
        public void setKnowledge(KnowledgeItem knowledge) { this.knowledge = knowledge; }
    }

    public static class TextImportRequest {
        private Long characterId;
        private List<TextKnowledgeItem> knowledgeItems;

        // Getters and Setters
        public Long getCharacterId() { return characterId; }
        public void setCharacterId(Long characterId) { this.characterId = characterId; }
        public List<TextKnowledgeItem> getKnowledgeItems() { return knowledgeItems; }
        public void setKnowledgeItems(List<TextKnowledgeItem> knowledgeItems) { this.knowledgeItems = knowledgeItems; }
    }

    public static class SearchRequest {
        private Long characterId;
        private String query;
        private Integer topK;

        // Getters and Setters
        public Long getCharacterId() { return characterId; }
        public void setCharacterId(Long characterId) { this.characterId = characterId; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getTopK() { return topK; }
        public void setTopK(Integer topK) { this.topK = topK; }
    }

    public static class KnowledgeItem {
        private String title;
        private String content;
        private String knowledgeType;
        private Integer importanceScore;
        private String source;
        private String sourceUrl;
        private List<String> tags;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getKnowledgeType() { return knowledgeType; }
        public void setKnowledgeType(String knowledgeType) { this.knowledgeType = knowledgeType; }
        public Integer getImportanceScore() { return importanceScore; }
        public void setImportanceScore(Integer importanceScore) { this.importanceScore = importanceScore; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public static class TextKnowledgeItem {
        private String title;
        private String content;
        private String knowledgeType;
        private Integer importanceScore;
        private String source;
        private String sourceUrl;
        private List<String> tags;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getKnowledgeType() { return knowledgeType; }
        public void setKnowledgeType(String knowledgeType) { this.knowledgeType = knowledgeType; }
        public Integer getImportanceScore() { return importanceScore; }
        public void setImportanceScore(Integer importanceScore) { this.importanceScore = importanceScore; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
}
