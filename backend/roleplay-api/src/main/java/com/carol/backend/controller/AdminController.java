package com.carol.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carol.backend.dto.ApiResponse;
import com.carol.backend.entity.User;
import com.carol.backend.mapper.UserMapper;
import com.carol.backend.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 * 处理用户管理等后台功能
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final com.carol.backend.mapper.CharacterMapper characterMapper;
    private final com.carol.backend.mapper.CharacterKnowledgeMapper characterKnowledgeMapper;

    /**
     * 检查当前用户是否为管理员
     */
    private void checkAdmin() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("未登录");
        }
        User user = userMapper.selectById(currentUserId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("无权限访问");
        }
    }

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<com.carol.backend.dto.AdminStatsResponse>> getStats() {
        checkAdmin();

        com.carol.backend.dto.AdminStatsResponse response = new com.carol.backend.dto.AdminStatsResponse();
        response.setUserCount(userMapper.selectCount(null));
        response.setCharacterCount(characterMapper.selectCount(null));
        response.setKnowledgeCount(characterKnowledgeMapper.selectCount(null));
        response.setKnowledgeDistribution(characterKnowledgeMapper.selectKnowledgeDistribution());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<User>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        
        checkAdmin();
        
        Page<User> userPage = new Page<>(page, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like(User::getUserAccount, keyword)
                    .or().like(User::getDisplayName, keyword)
                    .or().like(User::getEmail, keyword);
        }
        
        queryWrapper.orderByDesc(User::getCreatedAt);
        
        // 排除密码字段
        queryWrapper.select(User.class, info -> !info.getColumn().equals("user_password"));
        
        Page<User> result = userMapper.selectPage(userPage, queryWrapper);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<String>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        checkAdmin();
        
        String role = request.get("role");
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("无效的角色"));
        }
        
        User user = new User();
        user.setUserId(id);
        user.setRole(role);
        
        userMapper.updateById(user);
        return ResponseEntity.ok(ApiResponse.success("角色更新成功"));
    }

    /**
     * 更新用户状态 (封禁/解封)
     */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        
        checkAdmin();
        
        Integer status = request.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("无效的状态"));
        }
        
        User user = new User();
        user.setUserId(id);
        user.setStatus(status);
        
        userMapper.updateById(user);
        return ResponseEntity.ok(ApiResponse.success("状态更新成功"));
    }
}
