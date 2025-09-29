package com.carol.backend.controller;

import com.carol.backend.entity.Character;
import com.carol.backend.service.CharacterService;
import com.carol.backend.service.PromptTemplateService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器
 * 提供角色相关的API接口
 * 
 * @author carol
 */
@Slf4j
@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;
    private final PromptTemplateService promptTemplateService;

    @Autowired
    public CharacterController(CharacterService characterService, 
                              PromptTemplateService promptTemplateService) {
        this.characterService = characterService;
        this.promptTemplateService = promptTemplateService;
    }

    /**
     * 获取所有角色列表
     * GET /api/characters
     */
    @GetMapping
    public ResponseEntity<List<Character>> getAllCharacters() {
        log.info("获取所有角色列表");
        
        try {
            List<Character> characters = characterService.getAllCharacters();
            log.info("成功获取 {} 个角色", characters.size());
            return ResponseEntity.ok(characters);
        } catch (Exception e) {
            log.error("获取角色列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取角色列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取角色详情
     * GET /api/characters/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Character> getCharacterById(@PathVariable Long id) {
        log.info("获取角色详情: ID={}", id);
        
        try {
            Character character = characterService.getCharacterById(id);
            log.info("成功获取角色: {} (ID={})", character.getName(), id);
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            log.warn("参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("获取角色详情失败: ID={}, error={}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 搜索角色
     * GET /api/characters/search?name=xxx&expertise=xxx
     */
    @GetMapping("/search")
    public ResponseEntity<List<Character>> searchCharacters(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String expertise) {
        
        log.info("搜索角色: name={}, expertise={}", name, expertise);
        
        try {
            List<Character> characters;
            
            if (name != null && !name.trim().isEmpty()) {
                characters = characterService.searchCharactersByName(name);
            } else if (expertise != null && !expertise.trim().isEmpty()) {
                characters = characterService.getCharactersByExpertise(expertise);
            } else {
                characters = characterService.getAllCharacters();
            }
            
            log.info("搜索到 {} 个角色", characters.size());
            return ResponseEntity.ok(characters);
        } catch (Exception e) {
            log.error("搜索角色失败: name={}, expertise={}, error={}", name, expertise, e.getMessage(), e);
            throw new RuntimeException("搜索角色失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门角色
     * GET /api/characters/popular?limit=10
     */
    @GetMapping("/popular")
    public ResponseEntity<List<Character>> getPopularCharacters(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("获取热门角色: limit={}", limit);
        
        try {
            List<Character> characters = characterService.getPopularCharacters(limit);
            log.info("获取到 {} 个热门角色", characters.size());
            return ResponseEntity.ok(characters);
        } catch (Exception e) {
            log.error("获取热门角色失败: limit={}, error={}", limit, e.getMessage(), e);
            throw new RuntimeException("获取热门角色失败: " + e.getMessage());
        }
    }

    /**
     * 获取角色的系统提示词
     * GET /api/characters/{id}/prompt
     */
    @GetMapping("/{id}/prompt")
    public ResponseEntity<Map<String, Object>> getCharacterPrompt(@PathVariable Long id) {
        log.info("获取角色系统提示词: ID={}", id);
        
        try {
            Character character = characterService.getCharacterById(id);
            String promptText = promptTemplateService.getCharacterPromptText(character);
            
            Map<String, Object> response = new HashMap<>();
            response.put("characterId", id);
            response.put("characterName", character.getName());
            response.put("promptText", promptText);
            response.put("isComplete", character.isComplete());
            
            log.info("成功获取角色 {} 的系统提示词", character.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取角色系统提示词失败: ID={}, error={}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 验证角色是否可用
     * GET /api/characters/{id}/available
     */
    @GetMapping("/{id}/available")
    public ResponseEntity<Map<String, Object>> checkCharacterAvailable(@PathVariable Long id) {
        log.info("检查角色可用性: ID={}", id);
        
        boolean isAvailable = characterService.isCharacterAvailable(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("characterId", id);
        response.put("available", isAvailable);
        
        if (isAvailable) {
            try {
                Character character = characterService.getCharacterById(id);
                response.put("characterName", character.getName());
                response.put("isComplete", character.isComplete());
            } catch (Exception e) {
                log.warn("获取角色信息失败，但角色可用性检查通过: ID={}", id);
            }
        }
        
        log.info("角色可用性检查结果: ID={}, available={}", id, isAvailable);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建新角色（管理功能）
     * POST /api/characters
     */
    @PostMapping
    public ResponseEntity<Character> createCharacter(@Valid @RequestBody Character character) {
        log.info("创建新角色: {}", character.getName());
        
        try {
            Character createdCharacter = characterService.createCharacter(character);
            log.info("成功创建角色: {} (ID={})", createdCharacter.getName(), createdCharacter.getId());
            return ResponseEntity.ok(createdCharacter);
        } catch (IllegalArgumentException e) {
            log.warn("创建角色参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("创建角色失败: name={}, error={}", character.getName(), e.getMessage(), e);
            throw new RuntimeException("创建角色失败: " + e.getMessage());
        }
    }

    /**
     * 更新角色信息（管理功能）
     * PUT /api/characters/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Character> updateCharacter(@PathVariable Long id, 
                                                   @Valid @RequestBody Character character) {
        log.info("更新角色信息: ID={}", id);
        
        try {
            character.setId(id);
            Character updatedCharacter = characterService.updateCharacter(character);
            log.info("成功更新角色: {} (ID={})", updatedCharacter.getName(), id);
            return ResponseEntity.ok(updatedCharacter);
        } catch (IllegalArgumentException e) {
            log.warn("更新角色参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("更新角色失败: ID={}, error={}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 验证模板系统
     * GET /api/characters/template/validate
     */
    @GetMapping("/template/validate")
    public ResponseEntity<Map<String, Object>> validateTemplates() {
        log.info("验证Prompt模板系统");
        
        boolean isValid = promptTemplateService.validateTemplates();
        
        Map<String, Object> response = new HashMap<>();
        response.put("templatesValid", isValid);
        response.put("message", isValid ? "模板系统正常" : "模板系统存在问题");
        
        log.info("模板系统验证结果: {}", isValid);
        return ResponseEntity.ok(response);
    }
}
