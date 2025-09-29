package com.carol.backend.service;

import com.carol.backend.dto.RegisterRequest;
import com.carol.backend.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {
    
    @Test
    void testPasswordValidation() {

        // 测试有效密码
        String validPassword = "Test123!";
        assertTrue(validPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,12}$"));
        
        // 测试无效密码 - 没有大写字母
        String invalidPassword1 = "test123!";
        assertFalse(invalidPassword1.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,12}$"));
        
        // 测试无效密码 - 没有小写字母
        String invalidPassword2 = "TEST123!";
        assertFalse(invalidPassword2.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,12}$"));
        
        // 测试无效密码 - 没有特殊字符
        String invalidPassword3 = "Test1234";
        assertFalse(invalidPassword3.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,12}$"));
    }
    
    @Test
    void testUserAccountValidation() {
        // 测试有效账号
        String validAccount = "12345678";
        assertTrue(validAccount.matches("^\\d{8,12}$"));
        
        String validAccount2 = "123456789012";
        assertTrue(validAccount2.matches("^\\d{8,12}$"));
        
        // 测试无效账号 - 太短
        String invalidAccount1 = "1234567";
        assertFalse(invalidAccount1.matches("^\\d{8,12}$"));
        
        // 测试无效账号 - 太长
        String invalidAccount2 = "1234567890123";
        assertFalse(invalidAccount2.matches("^\\d{8,12}$"));
        
        // 测试无效账号 - 包含字母
        String invalidAccount3 = "12345678a";
        assertFalse(invalidAccount3.matches("^\\d{8,12}$"));
    }
}
