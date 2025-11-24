package com.carol.backend.service;

import com.carol.backend.entity.CharacterKnowledge;

import java.util.List;
import java.util.Map;

/**
 * 角色知识RAG服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责知识的向量化、检索和管理
 * @date 2025-01-15
 */
public interface ICharacterKnowledgeRAGService {
    
    /**
     * 批量导入知识到RAG系统
     * 
     * @param knowledgeList 知识列表
     */
    void importKnowledgeBatch(List<CharacterKnowledge> knowledgeList);
    
    /**
     * 单个知识导入
     * 
     * @param knowledge 知识实体
     */
    void importSingleKnowledge(CharacterKnowledge knowledge);
    
    /**
     * 基于问题检索相关知识
     * 
     * @param characterId 角色ID
     * @param query 查询文本
     * @param topK 返回数量
     * @return 相关知识列表
     */
    List<CharacterKnowledge> searchRelevantKnowledge(Long characterId, String query, int topK);
    
    /**
     * 获取角色的核心知识
     * 
     * @param characterId 角色ID
     * @param limit 限制数量
     * @return 核心知识列表
     */
    List<CharacterKnowledge> getCoreKnowledge(Long characterId, int limit);
    
    /**
     * 根据类型获取知识
     * 
     * @param characterId 角色ID
     * @param knowledgeType 知识类型
     * @return 知识列表
     */
    List<CharacterKnowledge> getKnowledgeByType(Long characterId, String knowledgeType);
    
    /**
     * 获取知识统计信息
     * 
     * @param characterId 角色ID
     * @return 统计信息
     */
    Map<String, Object> getKnowledgeStats(Long characterId);
    
    /**
     * 重新同步知识到向量数据库
     * 
     * @param characterId 角色ID
     */
    void resyncToVectorStore(Long characterId);
    
    /**
     * 删除知识（包括向量数据）
     * 
     * @param knowledgeId 知识ID
     */
    void deleteKnowledge(Long knowledgeId);

    /**
     * 更新知识（同步更新向量数据）
     * 
     * @param knowledge 知识实体
     */
    void updateKnowledge(CharacterKnowledge knowledge);

    /**
     * 获取知识列表（分页）
     * 
     * @param characterId 角色ID
     * @param page 页码
     * @param size 每页数量
     * @param keyword 关键词
     * @return 知识分页列表
     */
    com.baomidou.mybatisplus.core.metadata.IPage<CharacterKnowledge> getKnowledgeList(Long characterId, int page, int size, String keyword);
}
