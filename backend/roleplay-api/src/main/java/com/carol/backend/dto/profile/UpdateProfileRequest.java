package com.carol.backend.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Pattern;

/**
 * 更新个人资料请求DTO
 */
@Data
public class UpdateProfileRequest {
    
    /**
     * 显示名称
     */
    @NotBlank(message = "显示名称不能为空")
    @Length(min = 1, max = 50, message = "显示名称长度必须在1-50个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\s]+$", message = "显示名称只能包含中文、英文、数字、下划线、连字符和空格")
    private String displayName;
    
    /**
     * 邮箱地址
     */
    @Email(message = "邮箱格式不正确")
    @Length(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
    
    /**
     * 头像URL
     */
    @Length(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatarUrl;
    
    // 暂时注释掉数据库中不存在的字段
    // /**
    //  * 个人简介
    //  */
    // @Length(max = 200, message = "个人简介长度不能超过200个字符")
    // private String bio;
    
    // /**
    //  * 性别 (M: 男性, F: 女性, U: 未知)
    //  */
    // @Pattern(regexp = "^[MFU]$", message = "性别只能是M(男性)、F(女性)或U(未知)")
    // private String gender;
    
    // /**
    //  * 生日 (格式: YYYY-MM-DD)
    //  */
    // @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$|^$", message = "生日格式必须为YYYY-MM-DD")
    // private String birthday;
    
    // /**
    //  * 手机号码
    //  */
    // @Pattern(regexp = "^1[3-9]\\d{9}$|^$", message = "手机号码格式不正确")
    // private String phoneNumber;
}
