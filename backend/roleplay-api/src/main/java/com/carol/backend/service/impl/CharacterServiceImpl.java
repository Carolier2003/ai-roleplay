package com.carol.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carol.backend.entity.Character;
import com.carol.backend.mapper.CharacterMapper;
import com.carol.backend.service.CharacterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色服务实现类
 * 
 * @author carol
 */
@Slf4j
@Service
public class CharacterServiceImpl extends ServiceImpl<CharacterMapper, Character> implements CharacterService {

    @Override
    public List<Character> getAllCharacters() {
        log.info("获取所有角色列表");
        try {
            return baseMapper.selectAllCharactersOrderByCreated();
        } catch (Exception e) {
            log.error("获取角色列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取角色列表失败: " + e.getMessage());
        }
    }

    @Override
    public Character getCharacterById(Long id) {
        log.info("根据ID获取角色详情: {}", id);
        
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("角色ID无效");
        }
        
        try {
            Character character = baseMapper.selectById(id);
            if (character == null) {
                throw new RuntimeException("角色不存在: ID=" + id);
            }
            
            log.info("成功获取角色: {} ({})", character.getName(), id);
            return character;
        } catch (Exception e) {
            log.error("获取角色详情失败: ID={}, error={}", id, e.getMessage(), e);
            throw new RuntimeException("获取角色详情失败: " + e.getMessage());
        }
    }

    @Override
    public List<Character> searchCharactersByName(String name) {
        log.info("根据名称搜索角色: {}", name);
        
        if (name == null || name.trim().isEmpty()) {
            return getAllCharacters();
        }
        
        try {
            List<Character> characters = baseMapper.selectByNameLike(name.trim());
            log.info("搜索到 {} 个角色", characters.size());
            return characters;
        } catch (Exception e) {
            log.error("搜索角色失败: name={}, error={}", name, e.getMessage(), e);
            throw new RuntimeException("搜索角色失败: " + e.getMessage());
        }
    }

    @Override
    public List<Character> getCharactersByExpertise(String expertiseArea) {
        log.info("获取专业领域角色: {}", expertiseArea);
        
        if (expertiseArea == null || expertiseArea.trim().isEmpty()) {
            return getAllCharacters();
        }
        
        try {
            return baseMapper.selectByExpertiseArea(expertiseArea.trim());
        } catch (Exception e) {
            log.error("获取专业领域角色失败: expertise={}, error={}", expertiseArea, e.getMessage(), e);
            throw new RuntimeException("获取专业领域角色失败: " + e.getMessage());
        }
    }

    @Override
    public List<Character> getPopularCharacters(int limit) {
        log.info("获取热门角色: limit={}", limit);
        
        if (limit <= 0) {
            limit = 10; // 默认返回10个
        }
        
        try {
            return baseMapper.selectPopularCharacters(limit);
        } catch (Exception e) {
            log.error("获取热门角色失败: limit={}, error={}", limit, e.getMessage(), e);
            throw new RuntimeException("获取热门角色失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isCharacterAvailable(Long characterId) {
        if (characterId == null || characterId <= 0) {
            return false;
        }
        
        try {
            Character character = baseMapper.selectById(characterId);
            return character != null && character.isComplete();
        } catch (Exception e) {
            log.error("检查角色可用性失败: ID={}, error={}", characterId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Character createCharacter(Character character) {
        log.info("创建新角色: {}", character.getName());
        
        if (character == null) {
            throw new IllegalArgumentException("角色信息不能为空");
        }
        
        if (character.getName() == null || character.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        
        try {
            // 设置创建时间
            character.setCreatedAt(LocalDateTime.now());
            character.setUpdatedAt(LocalDateTime.now());
            
            // 设置默认值
            if (character.getVoiceStyle() == null) {
                character.setVoiceStyle("default");
            }
            
            baseMapper.insert(character);
            log.info("成功创建角色: {} (ID={})", character.getName(), character.getId());
            return character;
        } catch (Exception e) {
            log.error("创建角色失败: name={}, error={}", character.getName(), e.getMessage(), e);
            throw new RuntimeException("创建角色失败: " + e.getMessage());
        }
    }

    @Override
    public Character updateCharacter(Character character) {
        log.info("更新角色信息: ID={}", character.getId());
        
        if (character == null || character.getId() == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        
        // 检查角色是否存在
        Character existingCharacter = getCharacterById(character.getId());
        if (existingCharacter == null) {
            throw new RuntimeException("角色不存在: ID=" + character.getId());
        }
        
        try {
            // 设置更新时间
            character.setUpdatedAt(LocalDateTime.now());
            
            baseMapper.updateById(character);
            log.info("成功更新角色: {} (ID={})", character.getName(), character.getId());
            return getCharacterById(character.getId());
        } catch (Exception e) {
            log.error("更新角色失败: ID={}, error={}", character.getId(), e.getMessage(), e);
            throw new RuntimeException("更新角色失败: " + e.getMessage());
        }
    }
}
