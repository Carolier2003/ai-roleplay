package com.carol.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.carol.backend.entity.CharacterKnowledge;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.mapper.CharacterKnowledgeMapper;
import com.carol.backend.service.ICharacterKnowledgeRAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 角色知识RAG服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责知识的向量化、检索和管理
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterKnowledgeRAGServiceImpl implements ICharacterKnowledgeRAGService {

    private final CharacterKnowledgeMapper knowledgeMapper;
    private final VectorStore vectorStore;

    @Override
    @Transactional
    public void importKnowledgeBatch(List<CharacterKnowledge> knowledgeList) {
        log.info("[importKnowledgeBatch] 开始批量导入知识: count={}", knowledgeList.size());
        
        List<CharacterKnowledge> validKnowledgeList = knowledgeList.stream()
                .filter(CharacterKnowledge::isValid)
                .collect(Collectors.toList());
        
        log.info("[importKnowledgeBatch] 有效知识数量: validCount={}", validKnowledgeList.size());
        
        // 1. 保存到数据库
        for (CharacterKnowledge knowledge : validKnowledgeList) {
            // 设置默认值
            if (knowledge.getStatus() == null) {
                knowledge.setStatus(1);
            }
            if (knowledge.getLanguage() == null) {
                knowledge.setLanguage("zh");
            }
            if (knowledge.getImportanceScore() == null) {
                knowledge.setImportanceScore(5);
            }
            
            knowledgeMapper.insert(knowledge);
        }
        
        // 2. 向量化并存储到Redis Vector Store
        vectorizeKnowledge(validKnowledgeList);
        
        log.info("[importKnowledgeBatch] 知识导入完成: count={}", validKnowledgeList.size());
    }

    @Override
    @Transactional
    public void importSingleKnowledge(CharacterKnowledge knowledge) {
        log.info("[importSingleKnowledge] 开始导入单个知识: knowledgeId={}", knowledge.getId());
        
        if (!knowledge.isValid()) {
            log.warn("[importSingleKnowledge] 知识内容无效: knowledgeId={}", knowledge.getId());
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "知识内容无效");
        }
        
        // 保存到数据库
        knowledgeMapper.insert(knowledge);
        
        // 向量化
        vectorizeKnowledge(List.of(knowledge));
        
        log.info("[importSingleKnowledge] 单个知识导入完成: knowledgeId={}", knowledge.getId());
    }

    /**
     * 向量化知识并存储到向量数据库
     */
    private void vectorizeKnowledge(List<CharacterKnowledge> knowledgeList) {
        log.info("[vectorizeKnowledge] 开始向量化知识: count={}", knowledgeList.size());
        
        List<Document> documents = new ArrayList<>();
        
        for (CharacterKnowledge knowledge : knowledgeList) {
            try {
                // 构建文档内容
                String content = buildDocumentContent(knowledge);
                
                // 构建元数据
                Map<String, Object> metadata = buildDocumentMetadata(knowledge);
                
                // 生成唯一ID
                String documentId = "knowledge_" + knowledge.getId();
                
                // 创建文档
                Document document = new Document(documentId, content, metadata);
                documents.add(document);
                
                // 更新知识的vector_id
                knowledge.setVectorId(documentId);
                
            } catch (Exception e) {
                log.error("[vectorizeKnowledge] 向量化知识失败: knowledgeId={}, error={}", 
                        knowledge.getId(), e.getMessage(), e);
            }
        }
        
        if (!documents.isEmpty()) {
            // 批量添加到向量数据库
            vectorStore.add(documents);
            
            // 更新数据库中的vector_id
            for (CharacterKnowledge knowledge : knowledgeList) {
                if (knowledge.getVectorId() != null) {
                    knowledgeMapper.updateById(knowledge);
                }
            }
            
            log.info("[vectorizeKnowledge] 成功向量化知识数量: count={}", documents.size());
        }
    }

    /**
     * 构建文档内容
     */
    private String buildDocumentContent(CharacterKnowledge knowledge) {
        StringBuilder content = new StringBuilder();
        
        // 标题
        content.append("标题: ").append(knowledge.getTitle()).append("\n");
        
        // 内容 - 限制长度避免超过token限制
        String originalContent = knowledge.getContent();
        String limitedContent = originalContent;
        
        // 限制内容长度（约2000字符，对应大约500-600个token）
        if (originalContent.length() > 2000) {
            limitedContent = originalContent.substring(0, 2000) + "...";
            log.warn("[buildDocumentContent] 知识内容过长已截断: title={}, originalLength={}, truncatedLength={}", 
                knowledge.getTitle(), originalContent.length(), limitedContent.length());
        }
        
        content.append("内容: ").append(limitedContent).append("\n");
        
        // 知识类型
        if (knowledge.getKnowledgeType() != null) {
            content.append("类型: ").append(knowledge.getKnowledgeType()).append("\n");
        }
        
        // 标签
        List<String> tags = knowledge.getTagList();
        if (!tags.isEmpty()) {
            content.append("标签: ").append(String.join(", ", tags)).append("\n");
        }
        
        return content.toString();
    }

    /**
     * 构建文档元数据
     */
    private Map<String, Object> buildDocumentMetadata(CharacterKnowledge knowledge) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("id", knowledge.getId());
        metadata.put("character_id", knowledge.getCharacterId());
        metadata.put("title", knowledge.getTitle());
        metadata.put("knowledge_type", knowledge.getKnowledgeType());
        metadata.put("importance_score", knowledge.getImportanceScore());
        metadata.put("source", knowledge.getSource());
        metadata.put("language", knowledge.getLanguage());
        
        // 标签
        List<String> tags = knowledge.getTagList();
        if (!tags.isEmpty()) {
            metadata.put("tags", tags);
        }
        
        return metadata;
    }

    @Override
    public List<CharacterKnowledge> searchRelevantKnowledge(Long characterId, String query, int topK) {
        log.info("[searchRelevantKnowledge] 检索角色知识: characterId={}, query={}, topK={}", 
                characterId, query, topK);
        
        // 🎯 智能调整查询参数
        int optimizedTopK = getOptimizedTopK(characterId, topK);
        double optimizedThreshold = getOptimizedThreshold(characterId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建优化的检索请求
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(optimizedTopK)
                    .similarityThreshold(optimizedThreshold) // 智能调整相似度阈值
                    .filterExpression("character_id == " + characterId)
                    .build();
            
            log.debug("[searchRelevantKnowledge] 优化参数配置: query={}, topK={}->{}, threshold={}, filter=character_id=={}", 
                    query, topK, optimizedTopK, optimizedThreshold, characterId);
            
            // 执行带超时的向量检索（20秒超时，适应大数据集）
            List<Document> similarDocuments = performTimeoutVectorSearch(searchRequest, 20);
            long vectorSearchTime = System.currentTimeMillis() - startTime;
            log.info("[searchRelevantKnowledge] 向量搜索完成: vectorSearchTime={}ms, documentCount={}", 
                    vectorSearchTime, similarDocuments.size());
            
            if (similarDocuments.isEmpty()) {
                log.warn("[searchRelevantKnowledge] 向量搜索未返回结果: characterId={}, query={}", 
                        characterId, query);
                return new ArrayList<>();
            }
            
            // 批量查询数据库（性能关键优化 - 解决N+1问题）
            List<CharacterKnowledge> knowledgeList = batchQueryKnowledge(similarDocuments);
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[searchRelevantKnowledge] 搜索完成: totalTime={}ms, knowledgeCount={}", 
                    totalTime, knowledgeList.size());
            return knowledgeList;
            
        } catch (Exception e) {
            log.error("[searchRelevantKnowledge] 检索知识失败: characterId={}, query={}, error={}", 
                    characterId, query, e.getMessage(), e);
            // 回退到数据库检索
            return fallbackSearch(characterId, query, topK);
        }
    }

    /**
     * 回退检索方法（当向量检索失败时）
     */
    private List<CharacterKnowledge> fallbackSearch(Long characterId, String query, int topK) {
        log.info("[fallbackSearch] 使用回退检索方法: characterId={}, query={}, topK={}", 
                characterId, query, topK);
        
        // 使用数据库的全文检索或LIKE查询
        List<CharacterKnowledge> titleResults = knowledgeMapper.searchByTitle(characterId, query);
        List<CharacterKnowledge> contentResults = knowledgeMapper.searchByContent(characterId, query, topK);
        
        // 合并结果并去重
        Set<Long> seenIds = new HashSet<>();
        List<CharacterKnowledge> results = new ArrayList<>();
        
        // 优先添加标题匹配的结果
        for (CharacterKnowledge knowledge : titleResults) {
            if (seenIds.add(knowledge.getId()) && results.size() < topK) {
                results.add(knowledge);
            }
        }
        
        // 添加内容匹配的结果
        for (CharacterKnowledge knowledge : contentResults) {
            if (seenIds.add(knowledge.getId()) && results.size() < topK) {
                results.add(knowledge);
            }
        }
        
        return results;
    }

    @Override
    public List<CharacterKnowledge> getCoreKnowledge(Long characterId, int limit) {
        log.info("[getCoreKnowledge] 获取角色核心知识: characterId={}, limit={}", characterId, limit);
        return knowledgeMapper.selectHighImportanceKnowledge(characterId, 8, limit);
    }

    @Override
    public List<CharacterKnowledge> getKnowledgeByType(Long characterId, String knowledgeType) {
        log.info("[getKnowledgeByType] 根据类型获取知识: characterId={}, knowledgeType={}", 
                characterId, knowledgeType);
        return knowledgeMapper.selectByCharacterIdAndType(characterId, knowledgeType);
    }

    @Override
    public Map<String, Object> getKnowledgeStats(Long characterId) {
        log.info("[getKnowledgeStats] 获取知识统计信息: characterId={}", characterId);
        Map<String, Object> stats = new HashMap<>();
        
        // 总数量
        Integer totalCount = knowledgeMapper.countByCharacterId(characterId);
        stats.put("total_count", totalCount);
        
        // 各类型数量
        List<CharacterKnowledge> typeCounts = knowledgeMapper.countByKnowledgeType(characterId);
        Map<String, Integer> typeStats = new HashMap<>();
        for (CharacterKnowledge item : typeCounts) {
            typeStats.put(item.getKnowledgeType(), totalCount); // 这里应该从查询结果中获取count
        }
        stats.put("type_counts", typeStats);
        
        // 向量化状态
        QueryWrapper<CharacterKnowledge> wrapper = new QueryWrapper<>();
        wrapper.eq("character_id", characterId)
               .eq("status", 1)
               .eq("deleted", 0)
               .isNotNull("vector_id");
        Long vectorizedCountLong = knowledgeMapper.selectCount(wrapper);
        Integer vectorizedCount = vectorizedCountLong != null ? vectorizedCountLong.intValue() : 0;
        stats.put("vectorized_count", vectorizedCount);
        stats.put("vectorization_rate", totalCount > 0 ? (double) vectorizedCount / totalCount : 0.0);
        
        return stats;
    }

    @Override
    @Transactional
    public void resyncToVectorStore(Long characterId) {
        log.info("[resyncToVectorStore] 重新同步角色知识到向量数据库: characterId={}", characterId);
        
        // 查询所有需要同步的知识
        QueryWrapper<CharacterKnowledge> wrapper = new QueryWrapper<>();
        wrapper.eq("character_id", characterId)
               .eq("status", 1)
               .eq("deleted", 0);
        
        List<CharacterKnowledge> knowledgeList = knowledgeMapper.selectList(wrapper);
        
        if (!knowledgeList.isEmpty()) {
            // 清空现有的vector_id
            for (CharacterKnowledge knowledge : knowledgeList) {
                knowledge.setVectorId(null);
            }
            
            // 重新向量化
            vectorizeKnowledge(knowledgeList);
        }
        
        log.info("[resyncToVectorStore] 重新同步完成: characterId={}, count={}", 
                characterId, knowledgeList.size());
    }

    @Override
    @Transactional
    public void deleteKnowledge(Long knowledgeId) {
        log.info("[deleteKnowledge] 删除知识: knowledgeId={}", knowledgeId);
        CharacterKnowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge != null) {
            // 从向量数据库删除
            if (knowledge.getVectorId() != null) {
                try {
                    vectorStore.delete(List.of(knowledge.getVectorId()));
                    log.info("[deleteKnowledge] 从向量数据库删除文档成功: vectorId={}", knowledge.getVectorId());
                } catch (Exception e) {
                    log.error("[deleteKnowledge] 从向量数据库删除文档失败: vectorId={}, error={}", 
                            knowledge.getVectorId(), e.getMessage(), e);
                }
            }
            
            // 从数据库逻辑删除
            knowledge.setDeleted(1);
            knowledgeMapper.updateById(knowledge);
            
            log.info("[deleteKnowledge] 删除知识完成: knowledgeId={}", knowledgeId);
        } else {
            log.warn("[deleteKnowledge] 知识不存在: knowledgeId={}", knowledgeId);
        }
    }

    // ==================== 性能优化方法 ====================
    
    /**
     * 🎯 根据角色ID智能调整topK参数
     */
    private int getOptimizedTopK(Long characterId, int requestedTopK) {
        // 根据角色知识库大小智能调整
        int baseTopK;
        switch (characterId.intValue()) {
            case 5: // 泰拉瑞亚向导 - 知识库最大(1205条)，减少topK提升性能
                baseTopK = 3;
                break;
            case 4: // 江户川柯南 - 中等知识库(137条)
                baseTopK = 4;
                break;
            case 1: // 哈利·波特 - 小知识库(45条)
                baseTopK = 4;
                break;
            default: // 其他角色
                baseTopK = 5;
        }
        // 返回请求值和优化值的较小者
        return Math.min(requestedTopK, baseTopK);
    }
    
    /**
     * 🎯 根据角色ID智能调整相似度阈值
     */
    private double getOptimizedThreshold(Long characterId) {
        switch (characterId.intValue()) {
            case 5: // 泰拉瑞亚向导 - 提高阈值减少结果数量
                return 0.7;
            case 4: // 江户川柯南 - 中等阈值
            case 1: // 哈利·波特 - 中等阈值
                return 0.65;
            default: // 其他角色 - 标准阈值
                return 0.6;
        }
    }
    
    /**
     * 🚀 批量查询知识数据 - 解决N+1查询问题的关键优化
     */
    private List<CharacterKnowledge> batchQueryKnowledge(List<Document> documents) {
        if (documents.isEmpty()) {
            return new ArrayList<>();
        }
        
        long startTime = System.currentTimeMillis();
        
        // 1. 批量提取知识ID
        List<Long> knowledgeIds = documents.stream()
                .map(Document::getId)
                .filter(id -> id != null && id.startsWith("knowledge_"))
                .map(id -> {
                    try {
                        return Long.valueOf(id.substring("knowledge_".length()));
                    } catch (NumberFormatException e) {
                        log.warn("[batchQueryKnowledge] 无效的知识ID格式: id={}", id);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (knowledgeIds.isEmpty()) {
            log.warn("[batchQueryKnowledge] 未找到有效的知识ID");
            return new ArrayList<>();
        }
        
        // 2. 批量查询数据库（一次查询代替N次）
        List<CharacterKnowledge> knowledgeList = knowledgeMapper.selectBatchIds(knowledgeIds);
        
        // 3. 过滤有效记录
        List<CharacterKnowledge> validKnowledge = knowledgeList.stream()
                .filter(k -> k != null && k.getStatus() == 1)
                .collect(Collectors.toList());
        
        long batchQueryTime = System.currentTimeMillis() - startTime;
        log.info("[batchQueryKnowledge] 批量查询完成: batchQueryTime={}ms, queryIdCount={}, recordCount={}", 
                batchQueryTime, knowledgeIds.size(), validKnowledge.size());
        
        return validKnowledge;
    }
    
    /**
     * ⏱️ 执行带超时控制的向量搜索
     */
    private List<Document> performTimeoutVectorSearch(SearchRequest searchRequest, int timeoutSeconds) {
        try {
            CompletableFuture<List<Document>> searchFuture = CompletableFuture.supplyAsync(() -> 
                vectorStore.similaritySearch(searchRequest)
            );
            
            return searchFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("[performTimeoutVectorSearch] 向量搜索超时或失败: timeoutSeconds={}, error={}", 
                    timeoutSeconds, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "向量搜索超时", e);
        }
    }
}
