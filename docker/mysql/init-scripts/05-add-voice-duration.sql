-- 添加语音时长字段到 conversation_messages 表
-- 执行时间：2025-09-28

USE `ai_roleplay`;

-- 添加 voice_duration 字段
ALTER TABLE `conversation_messages` 
ADD COLUMN `voice_duration` INT DEFAULT NULL COMMENT '语音时长（秒）' 
AFTER `audio_url`;

-- 添加索引以优化查询
ALTER TABLE `conversation_messages` 
ADD INDEX `idx_voice_duration` (`voice_duration`);

-- 验证字段添加成功
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'ai_roleplay' 
AND TABLE_NAME = 'conversation_messages' 
AND COLUMN_NAME = 'voice_duration';
