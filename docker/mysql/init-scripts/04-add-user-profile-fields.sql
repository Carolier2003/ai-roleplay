-- 添加用户个人资料相关字段
-- 执行时间：2025-09-26

USE ai_roleplay;

-- 检查并添加 bio 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'ai_roleplay' 
     AND TABLE_NAME = 'users' 
     AND COLUMN_NAME = 'bio') = 0,
    'ALTER TABLE users ADD COLUMN bio TEXT DEFAULT NULL COMMENT "个人简介"',
    'SELECT "bio 字段已存在" AS message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 gender 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'ai_roleplay' 
     AND TABLE_NAME = 'users' 
     AND COLUMN_NAME = 'gender') = 0,
    'ALTER TABLE users ADD COLUMN gender VARCHAR(1) DEFAULT "U" COMMENT "性别 (M: 男性, F: 女性, U: 未知)"',
    'SELECT "gender 字段已存在" AS message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 birthday 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'ai_roleplay' 
     AND TABLE_NAME = 'users' 
     AND COLUMN_NAME = 'birthday') = 0,
    'ALTER TABLE users ADD COLUMN birthday VARCHAR(10) DEFAULT NULL COMMENT "生日 (格式: YYYY-MM-DD)"',
    'SELECT "birthday 字段已存在" AS message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 phone_number 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'ai_roleplay' 
     AND TABLE_NAME = 'users' 
     AND COLUMN_NAME = 'phone_number') = 0,
    'ALTER TABLE users ADD COLUMN phone_number VARCHAR(20) DEFAULT NULL COMMENT "手机号码"',
    'SELECT "phone_number 字段已存在" AS message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = 'ai_roleplay' 
     AND TABLE_NAME = 'users' 
     AND INDEX_NAME = 'idx_gender') = 0,
    'ALTER TABLE users ADD INDEX idx_gender (gender)',
    'SELECT "idx_gender 索引已存在" AS message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = 'ai_roleplay' 
     AND TABLE_NAME = 'users' 
     AND INDEX_NAME = 'idx_phone_number') = 0,
    'ALTER TABLE users ADD INDEX idx_phone_number (phone_number)',
    'SELECT "idx_phone_number 索引已存在" AS message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 显示表结构确认
SHOW COLUMNS FROM users;
