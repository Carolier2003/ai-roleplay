package com.carol.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carol.backend.entity.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问层
 */
@Mapper
@Repository
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户账号查询用户
     */
    @Select("SELECT id AS userId, user_account, user_password, username AS displayName, email, avatar_url, status, last_login_at, login_count, created_at, updated_at, role FROM users WHERE user_account = #{userAccount} AND status = 1")
    User findByUserAccount(@Param("userAccount") String userAccount);
    
    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT id AS userId, user_account, user_password, username AS displayName, email, avatar_url, status, last_login_at, login_count, created_at, updated_at, role FROM users WHERE email = #{email} AND status = 1")
    User findByEmail(@Param("email") String email);
    
    /**
     * 检查用户账号是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE user_account = #{userAccount} AND status = 1")
    int countByUserAccount(@Param("userAccount") String userAccount);
    
    /**
     * 根据邮箱查询用户（包含所有状态）
     */
    @Select("SELECT id AS userId, user_account, user_password, username AS displayName, email, avatar_url, status, last_login_at, login_count, created_at, updated_at, role FROM users WHERE email = #{email}")
    User selectByEmail(@Param("email") String email);
    
    /**
     * 根据用户ID查询用户（自定义方法，避免SELECT *）
     */
    @Select("SELECT id AS userId, user_account, user_password, username AS displayName, email, avatar_url, status, last_login_at, login_count, created_at, updated_at, role FROM users WHERE id = #{userId}")
    User selectByUserId(@Param("userId") Long userId);
}
