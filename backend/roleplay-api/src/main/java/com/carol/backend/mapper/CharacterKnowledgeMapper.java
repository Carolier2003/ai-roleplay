package com.carol.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carol.backend.entity.CharacterKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色知识数据访问层
 */
@Mapper
public interface CharacterKnowledgeMapper extends BaseMapper<CharacterKnowledge> {

    /**
     * 根据角色ID查询知识列表
     */
    @Select("SELECT * FROM character_knowledge WHERE character_id = #{characterId} AND status = 1 AND deleted = 0 ORDER BY importance_score DESC, created_at DESC")
    List<CharacterKnowledge> selectByCharacterId(@Param("characterId") Long characterId);

    /**
     * 根据角色ID和知识类型查询
     */
    @Select("SELECT * FROM character_knowledge WHERE character_id = #{characterId} AND knowledge_type = #{knowledgeType} AND status = 1 AND deleted = 0 ORDER BY importance_score DESC")
    List<CharacterKnowledge> selectByCharacterIdAndType(@Param("characterId") Long characterId, @Param("knowledgeType") String knowledgeType);

    /**
     * 根据角色ID分页查询知识
     */
    @Select("SELECT * FROM character_knowledge WHERE character_id = #{characterId} AND status = 1 AND deleted = 0 ORDER BY importance_score DESC, created_at DESC")
    IPage<CharacterKnowledge> selectPageByCharacterId(Page<CharacterKnowledge> page, @Param("characterId") Long characterId);

    /**
     * 查询角色的高重要性知识
     */
    @Select("SELECT * FROM character_knowledge WHERE character_id = #{characterId} AND importance_score >= #{minScore} AND status = 1 AND deleted = 0 ORDER BY importance_score DESC LIMIT #{limit}")
    List<CharacterKnowledge> selectHighImportanceKnowledge(@Param("characterId") Long characterId, @Param("minScore") Integer minScore, @Param("limit") Integer limit);

    /**
     * 根据标题搜索知识
     */
    @Select("SELECT * FROM character_knowledge WHERE character_id = #{characterId} AND title LIKE CONCAT('%', #{keyword}, '%') AND status = 1 AND deleted = 0 ORDER BY importance_score DESC")
    List<CharacterKnowledge> searchByTitle(@Param("characterId") Long characterId, @Param("keyword") String keyword);

    /**
     * 根据内容搜索知识
     */
    @Select("SELECT * FROM character_knowledge WHERE character_id = #{characterId} AND content LIKE CONCAT('%', #{keyword}, '%') AND status = 1 AND deleted = 0 ORDER BY importance_score DESC LIMIT #{limit}")
    List<CharacterKnowledge> searchByContent(@Param("characterId") Long characterId, @Param("keyword") String keyword, @Param("limit") Integer limit);

    /**
     * 统计角色知识数量
     */
    @Select("SELECT COUNT(*) FROM character_knowledge WHERE character_id = #{characterId} AND status = 1 AND deleted = 0")
    Integer countByCharacterId(@Param("characterId") Long characterId);

    /**
     * 统计各类型知识数量
     */
    @Select("SELECT knowledge_type, COUNT(*) as count FROM character_knowledge WHERE character_id = #{characterId} AND status = 1 AND deleted = 0 GROUP BY knowledge_type")
    List<CharacterKnowledge> countByKnowledgeType(@Param("characterId") Long characterId);

    /**
     * 查询需要同步到向量库的知识
     */
    @Select("SELECT * FROM character_knowledge WHERE vector_id IS NULL AND status = 1 AND deleted = 0 ORDER BY importance_score DESC")
    List<CharacterKnowledge> selectNeedSyncToVector();

    /**
     * 批量更新向量ID
     */
    void batchUpdateVectorId(@Param("knowledgeList") List<CharacterKnowledge> knowledgeList);

    /**
     * 根据向量ID查询知识
     */
    /**
     * 根据向量ID查询知识
     */
    @Select("SELECT * FROM character_knowledge WHERE vector_id = #{vectorId} AND deleted = 0")
    CharacterKnowledge selectByVectorId(@Param("vectorId") String vectorId);

    /**
     * 统计每个角色的知识库数量
     */
    @Select("SELECT c.name as characterName, COUNT(ck.id) as count " +
            "FROM character_knowledge ck " +
            "LEFT JOIN characters c ON ck.character_id = c.id " +
            "WHERE ck.status = 1 AND ck.deleted = 0 " +
            "GROUP BY ck.character_id, c.name")
    List<com.carol.backend.dto.AdminStatsResponse.CharacterKnowledgeStat> selectKnowledgeDistribution();
}
