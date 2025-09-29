-- 创建AI角色扮演系统数据库表结构

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 角色信息表
CREATE TABLE `characters` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `description` TEXT COMMENT '角色描述',
    `background_story` TEXT COMMENT '背景故事',
    `personality_traits` TEXT COMMENT '性格特征',
    `speaking_style` VARCHAR(100) DEFAULT NULL COMMENT '说话风格',
    `expertise_area` VARCHAR(100) DEFAULT NULL COMMENT '专业领域',
    `voice_style` VARCHAR(50) DEFAULT 'default' COMMENT '语音风格',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 1-启用 0-禁用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_name` (`name`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';

-- 对话会话表（数据仓库版本 - 从Redis同步的历史数据）
CREATE TABLE `conversations` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID（对应Redis的conversationId）',
    `character_id` BIGINT(20) NOT NULL COMMENT '角色ID',
    `user_id` VARCHAR(100) DEFAULT 'anonymous' COMMENT '用户ID（逻辑关联users.user_account，支持匿名用户）',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '对话标题（自动生成或用户设置）',
    `context_summary` TEXT COMMENT '对话内容摘要',
    `message_count` INT DEFAULT 0 COMMENT '消息总数',
    `total_tokens` INT DEFAULT 0 COMMENT '总Token消耗',
    `start_time` TIMESTAMP NULL DEFAULT NULL COMMENT '对话开始时间',
    `end_time` TIMESTAMP NULL DEFAULT NULL COMMENT '对话结束时间',
    `duration_minutes` INT DEFAULT 0 COMMENT '对话持续时间（分钟）',
    `sync_source` VARCHAR(20) DEFAULT 'redis' COMMENT '同步来源: redis, manual',
    `sync_status` TINYINT(1) DEFAULT 1 COMMENT '同步状态: 1-已同步 2-同步失败',
    `quality_score` DECIMAL(3,2) DEFAULT NULL COMMENT '对话质量评分（1-5）',
    `feedback_rating` TINYINT(1) DEFAULT NULL COMMENT '用户评分: 1-5',
    `tags` JSON DEFAULT NULL COMMENT '对话标签',
    `export_count` INT DEFAULT 0 COMMENT '导出次数',
    `last_sync_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后同步时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    INDEX `idx_character_id` (`character_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_start_time` (`start_time`),
    INDEX `idx_sync_status` (`sync_status`),
    INDEX `idx_quality_score` (`quality_score`),
    INDEX `idx_tags` ((CAST(`tags` AS CHAR(255) ARRAY))),
    FOREIGN KEY (`character_id`) REFERENCES `characters`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话数据仓库表';

-- 对话消息数据仓库表
CREATE TABLE `conversation_messages` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` BIGINT(20) NOT NULL COMMENT '会话ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话标识',
    `message_index` INT NOT NULL COMMENT '消息在会话中的序号',
    `message_type` TINYINT(1) NOT NULL COMMENT '消息类型: 1-用户 2-AI 3-系统',
    `content` LONGTEXT NOT NULL COMMENT '消息内容',
    `content_length` INT DEFAULT 0 COMMENT '内容字符数',
    `audio_url` VARCHAR(500) DEFAULT NULL COMMENT '语音文件URL',
    `voice_duration` INT DEFAULT NULL COMMENT '语音时长（秒）',
    `response_time_ms` INT DEFAULT NULL COMMENT 'AI响应时间（毫秒）',
    `token_count` INT DEFAULT 0 COMMENT 'Token消耗数量',
    `model_name` VARCHAR(50) DEFAULT NULL COMMENT '使用的模型名称',
    `temperature` DECIMAL(3,2) DEFAULT NULL COMMENT '模型温度参数',
    `rag_knowledge_used` TINYINT(1) DEFAULT 0 COMMENT '是否使用了RAG知识',
    `rag_knowledge_count` INT DEFAULT 0 COMMENT '使用的RAG知识条目数',
    `sentiment_score` DECIMAL(3,2) DEFAULT NULL COMMENT '情感分析得分（-1到1）',
    `language` VARCHAR(10) DEFAULT 'zh' COMMENT '消息语言',
    `metadata` JSON DEFAULT NULL COMMENT '消息元数据',
    `sync_source` VARCHAR(20) DEFAULT 'redis' COMMENT '同步来源',
    `message_timestamp` TIMESTAMP NOT NULL COMMENT '消息原始时间戳',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conversation_id` (`conversation_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_message_type` (`message_type`),
    INDEX `idx_message_timestamp` (`message_timestamp`),
    INDEX `idx_token_count` (`token_count`),
    INDEX `idx_rag_knowledge_used` (`rag_knowledge_used`),
    INDEX `idx_sentiment_score` (`sentiment_score`),
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息数据仓库表';

-- 用户表
CREATE TABLE `users` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_account` VARCHAR(50) NOT NULL COMMENT '用户账号（登录名）',
    `user_password` VARCHAR(255) NOT NULL COMMENT '用户密码（加密存储）',
    `username` VARCHAR(100) DEFAULT NULL COMMENT '用户昵称/显示名',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
    `last_login_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后登录时间',
    `login_count` INT DEFAULT 0 COMMENT '登录次数',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account` (`user_account`),
    INDEX `idx_email` (`email`),
    INDEX `idx_status` (`status`),
    INDEX `idx_last_login` (`last_login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色知识表
CREATE TABLE `character_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `character_id` bigint NOT NULL COMMENT '角色ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识标题',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识内容',
  `knowledge_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识类型',
  `importance_score` int DEFAULT '5' COMMENT '重要性评分 (1-10)',
  `source` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据来源',
  `source_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始链接',
  `vector_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '向量数据库ID',
  `tags` json DEFAULT NULL COMMENT '标签列表',
  `language` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'zh' COMMENT '语言',
  `status` tinyint DEFAULT '1' COMMENT '状态 (1-启用, 0-禁用)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_character_id` (`character_id`),
  KEY `idx_knowledge_type` (`knowledge_type`),
  KEY `idx_importance_score` (`importance_score`),
  KEY `idx_vector_id` (`vector_id`),
  KEY `idx_status_deleted` (`status`,`deleted`),
  KEY `idx_created_at` (`created_at`),
  FULLTEXT KEY `idx_title_content` (`title`,`content`)
) ENGINE=InnoDB AUTO_INCREMENT=2734 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色知识表';

SET FOREIGN_KEY_CHECKS = 1;


-- 插入示例用户数据
INSERT INTO `users` (`user_account`, `user_password`, `username`, `email`, `status`) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFDYnD4bcLCJZ3LfkQOL6H.', '测试用户', 'test@example.com', 1),
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFDYnD4bcLCJZ3LfkQOL6H.', '管理员', 'admin@example.com', 1),
('demo', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFDYnD4bcLCJZ3LfkQOL6H.', '演示用户', 'demo@example.com', 1);

-- 插入示例角色数据
INSERT INTO `characters` (`name`, `description`, `background_story`, `personality_traits`, `speaking_style`, `expertise_area`, `voice_style`) VALUES
('哈利·波特', '英国著名的年轻巫师，霍格沃茨魔法学校的学生', '出生在巫师家庭，1岁时父母被伏地魔杀害，被姨妈收养。11岁时收到霍格沃茨入学通知书，发现自己是巫师。在学校里与赫敏、罗恩成为好友，多次与伏地魔及其手下战斗。', '勇敢、善良、忠诚、有时冲动、富有正义感', '年轻人的语调，略带英式口音，语气坚定但不失温和', '魔法、防御黑魔法、魁地奇运动', 'male'),
('苏格拉底', '古希腊哲学家，西方哲学的奠基者之一', '生活在公元前5世纪的雅典，以其独特的对话式教学方法而闻名。他相信"未经审视的生活不值得过"，通过不断的提问来引导学生思考。最终因被指控腐蚀青年和不信神而被判死刑。', '睿智、谦逊、好奇、善于提问、追求真理', '古典哲学家的语调，喜欢通过提问来引导思考', '哲学、伦理学、逻辑学、教育学', 'male'),
('爱因斯坦', '20世纪最伟大的物理学家之一', '出生于德国，后移居美国。提出了相对论理论，获得了诺贝尔物理学奖。不仅在科学上有重大贡献，也关心社会问题，支持和平主义和民权运动。', '天才、好奇、想象力丰富、幽默、关心人类', '科学家的严谨语调，但也不失幽默感，喜欢用比喻解释复杂概念', '物理学、数学、科学哲学', 'male'),
('江户川柯南', '表面是小学生，实际是高中生侦探工藤新一', '原本是17岁的高中生名侦探工藤新一，因为目击黑暗组织的交易被发现，被迫服下神秘药物APTX4869，身体缩小成7岁小学生的模样。为了追查黑暗组织的真相，化名江户川柯南，寄住在毛利小五郎侦探事务所。', '聪明睿智、观察力敏锐、正义感强、推理能力超群、有时显得过于成熟', '成熟的推理思维但用童声表达，逻辑清晰，善于抓住细节', '推理侦探、犯罪学、法医学、化学', 'child'),
('泰拉瑞亚向导', '泰拉瑞亚世界的万能向导和知识守护者', '我是泰拉瑞亚世界中的向导NPC，掌握着这个沙盒世界的一切知识。从最基础的木剑制作到最高端的泰拉刃锻造，从简单的房屋建造到复杂的机械装置，从普通的史莱姆到最强的月亮领主，我都了如指掌。我的使命是帮助新玩家适应这个世界，指导他们的冒险之旅。', '热情友善、博学多才、耐心细致、乐于助人、对冒险充满热情', '游戏NPC的友好语调，专业而亲切，善于用简单易懂的方式解释复杂的游戏机制', '泰拉瑞亚游戏机制、合成配方、BOSS攻略、建筑设计、装备属性', 'male');

-- 创建对话数据仓库统计视图
CREATE VIEW `conversation_analytics` AS
SELECT 
    c.character_id,
    ch.name as character_name,
    COUNT(DISTINCT c.session_id) as total_conversations,
    COUNT(cm.id) as total_messages,
    AVG(c.message_count) as avg_messages_per_conversation,
    SUM(c.total_tokens) as total_tokens_used,
    AVG(c.total_tokens) as avg_tokens_per_conversation,
    AVG(c.duration_minutes) as avg_duration_minutes,
    AVG(c.quality_score) as avg_quality_score,
    AVG(c.feedback_rating) as avg_feedback_rating,
    DATE(c.start_time) as conversation_date
FROM conversations c
LEFT JOIN characters ch ON c.character_id = ch.id
LEFT JOIN conversation_messages cm ON c.id = cm.conversation_id
WHERE c.sync_status = 1
GROUP BY c.character_id, ch.name, DATE(c.start_time);

SET FOREIGN_KEY_CHECKS = 1;
