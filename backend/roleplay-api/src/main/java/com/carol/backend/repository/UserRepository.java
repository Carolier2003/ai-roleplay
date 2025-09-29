package com.carol.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carol.backend.entity.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问层
 */
@Mapper
@Repository
public interface UserRepository extends BaseMapper<User> {
    
    /**
     * 根据用户账号查询用户
     */
    @Select("SELECT * FROM user WHERE user_account = #{userAccount} AND status = 1")
    User findByUserAccount(@Param("userAccount") String userAccount);
    
    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM user WHERE email = #{email} AND status = 1")
    User findByEmail(@Param("email") String email);
    
    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM user WHERE phone = #{phone} AND status = 1")
    User findByPhone(@Param("phone") String phone);
    
    /**
     * 检查用户账号是否存在（用户账号、邮箱或手机号）
     */
    @Select("SELECT COUNT(*) FROM user WHERE (user_account = #{userAccount} OR email = #{userAccount} OR phone = #{userAccount}) AND status = 1")
    int countByUserAccount(@Param("userAccount") String userAccount);
}