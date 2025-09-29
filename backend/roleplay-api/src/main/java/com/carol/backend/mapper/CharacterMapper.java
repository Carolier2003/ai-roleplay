package com.carol.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carol.backend.entity.Character;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper接口
 * 基于MyBatis Plus实现
 * 
 * @author carol
 */
@Mapper
public interface CharacterMapper extends BaseMapper<Character> {

    /**
     * 查询所有可用角色（按创建时间排序）
     */
    @Select("SELECT * FROM characters ORDER BY created_at DESC")
    List<Character> selectAllCharactersOrderByCreated();

    /**
     * 根据名称模糊查询角色
     */
    @Select("SELECT * FROM characters WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<Character> selectByNameLike(String name);

    /**
     * 查询指定专业领域的角色
     */
    @Select("SELECT * FROM characters WHERE expertise_area = #{expertiseArea}")
    List<Character> selectByExpertiseArea(String expertiseArea);

    /**
     * 查询最受欢迎的角色（可以后续扩展为根据对话次数排序）
     */
    @Select("SELECT * FROM characters ORDER BY created_at DESC LIMIT #{limit}")
    List<Character> selectPopularCharacters(int limit);
}
