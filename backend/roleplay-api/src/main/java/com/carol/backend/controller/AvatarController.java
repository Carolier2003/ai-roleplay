package com.carol.backend.controller;

import com.carol.backend.dto.avatar.AvatarUploadResponse;
import com.carol.backend.dto.avatar.CropData;
import com.carol.backend.service.IAvatarService;
import com.carol.backend.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * 头像管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/avatar")
@RequiredArgsConstructor
@Validated
public class AvatarController {
    
    private final IAvatarService avatarService;
    private final ObjectMapper objectMapper;
    
    /**
     * 上传头像
     */
    @PostMapping("/upload")
    public AvatarUploadResponse uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "cropData", required = false) String cropDataJson) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[uploadAvatar] 用户上传头像, userId={}, fileName={}, size={} bytes", 
            userId, file.getOriginalFilename(), file.getSize());
        
        try {
            // 解析裁剪参数
            CropData cropData = null;
            if (cropDataJson != null && !cropDataJson.trim().isEmpty()) {
                cropData = objectMapper.readValue(cropDataJson, CropData.class);
                log.info("[uploadAvatar] 裁剪参数: {}", cropData);
            }
            
            AvatarUploadResponse response = avatarService.uploadAvatar(userId, file, cropData);
            
            log.info("[uploadAvatar] 头像上传成功, userId={}, avatarUrl={}", 
                userId, response.getAvatarUrl());
            return response;
            
        } catch (Exception e) {
            log.error("[uploadAvatar] 头像上传失败, userId={}: {}", userId, e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    /**
     * 删除头像
     */
    @DeleteMapping
    public Boolean deleteAvatar() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[deleteAvatar] 用户删除头像, userId={}", userId);
        
        boolean success = avatarService.deleteAvatar(userId);
        
        log.info("[deleteAvatar] 头像删除{}, userId={}", 
            success ? "成功" : "失败", userId);
        return success;
    }
    
    /**
     * 获取当前用户头像URL
     */
    @GetMapping
    public String getAvatarUrl() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[getAvatarUrl] 获取用户头像URL, userId={}", userId);
        
        String avatarUrl = avatarService.getAvatarUrl(userId);
        
        log.info("[getAvatarUrl] 获取头像URL成功, userId={}, avatarUrl={}", 
            userId, avatarUrl != null ? avatarUrl : "无头像");
        return avatarUrl;
    }
}
