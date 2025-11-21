package com.carol.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carol.backend.entity.Character;

import java.util.List;

/**
 * 角色服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 角色相关的业务逻辑服务
 * @date 2025-01-15
 */
public interface CharacterService extends IService<Character> {

    /**
     * 获取所有角色列表
     */
    List<Character> getAllCharacters();

    /**
     * 根据ID获取角色详情
     */
    Character getCharacterById(Long id);

    /**
     * 根据名称搜索角色
     */
    List<Character> searchCharactersByName(String name);

    /**
     * 获取指定专业领域的角色
     */
    List<Character> getCharactersByExpertise(String expertiseArea);

    /**
     * 获取热门角色
     */
    List<Character> getPopularCharacters(int limit);

    /**
     * 验证角色是否存在且可用
     */
    boolean isCharacterAvailable(Long characterId);

    /**
     * 创建新角色
     */
    Character createCharacter(Character character);

    /**
     * 更新角色信息
     */
    Character updateCharacter(Character character);
}
