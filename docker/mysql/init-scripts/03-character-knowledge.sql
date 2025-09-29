-- 角色知识表
CREATE TABLE IF NOT EXISTS `character_knowledge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `character_id` BIGINT NOT NULL COMMENT '角色ID',
    `title` VARCHAR(200) NOT NULL COMMENT '知识标题',
    `content` TEXT NOT NULL COMMENT '知识内容',
    `knowledge_type` VARCHAR(50) NOT NULL COMMENT '知识类型',
    `importance_score` INT DEFAULT 5 COMMENT '重要性评分 (1-10)',
    `source` VARCHAR(100) COMMENT '数据来源',
    `source_url` VARCHAR(500) COMMENT '原始链接',
    `vector_id` VARCHAR(100) COMMENT '向量数据库ID',
    `tags` JSON COMMENT '标签列表',
    `language` VARCHAR(10) DEFAULT 'zh' COMMENT '语言',
    `status` TINYINT DEFAULT 1 COMMENT '状态 (1-启用, 0-禁用)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    INDEX `idx_character_id` (`character_id`),
    INDEX `idx_knowledge_type` (`knowledge_type`),
    INDEX `idx_importance_score` (`importance_score`),
    INDEX `idx_vector_id` (`vector_id`),
    INDEX `idx_status_deleted` (`status`, `deleted`),
    INDEX `idx_created_at` (`created_at`),
    FULLTEXT KEY `idx_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色知识表';

-- 插入哈利·波特角色的基础知识（示例数据）
INSERT INTO `character_knowledge` (`character_id`, `title`, `content`, `knowledge_type`, `importance_score`, `source`, `tags`) VALUES
(1, '哈利·波特基本信息', '哈利·詹姆斯·波特，生于1980年7月31日，是系列小说的主人公。父母是詹姆斯·波特和莉莉·波特，在他一岁时被伏地魔杀害。哈利额头上有一道闪电形伤疤，这是伏地魔企图杀死他时留下的。', 'BASIC_INFO', 10, 'system', '["主人公", "基本信息", "生平"]'),
(1, '哈利·波特性格特征', '哈利·波特勇敢、忠诚、善良，具有强烈的正义感。他不畏强权，敢于挑战权威，同时也很谦逊，不喜欢成为众人瞩目的焦点。在朋友面前，他是可靠的伙伴；面对敌人，他毫不退缩。', 'PERSONALITY', 9, 'system', '["性格", "勇敢", "正义"]'),
(1, '霍格沃茨魔法学校', '霍格沃茨魔法学校是英国最著名的魔法学校，位于苏格兰高地。学校分为四个学院：格兰芬多、拉文克劳、赫奇帕奇和斯莱特林。哈利·波特被分配到格兰芬多学院。', 'KNOWLEDGE', 8, 'system', '["霍格沃茨", "学校", "格兰芬多"]');

-- 为哈利·波特知识库创建一些统计视图
CREATE OR REPLACE VIEW `character_knowledge_stats` AS
SELECT 
    `character_id`,
    COUNT(*) as `total_knowledge`,
    COUNT(CASE WHEN `knowledge_type` = 'BASIC_INFO' THEN 1 END) as `basic_info_count`,
    COUNT(CASE WHEN `knowledge_type` = 'PERSONALITY' THEN 1 END) as `personality_count`,
    COUNT(CASE WHEN `knowledge_type` = 'KNOWLEDGE' THEN 1 END) as `knowledge_count`,
    COUNT(CASE WHEN `knowledge_type` = 'EVENTS' THEN 1 END) as `events_count`,
    COUNT(CASE WHEN `knowledge_type` = 'RELATIONSHIPS' THEN 1 END) as `relationships_count`,
    COUNT(CASE WHEN `knowledge_type` = 'ABILITIES' THEN 1 END) as `abilities_count`,
    COUNT(CASE WHEN `knowledge_type` = 'QUOTES' THEN 1 END) as `quotes_count`,
    AVG(`importance_score`) as `avg_importance`,
    COUNT(CASE WHEN `vector_id` IS NOT NULL THEN 1 END) as `vectorized_count`,
    MAX(`updated_at`) as `last_updated`
FROM `character_knowledge` 
WHERE `status` = 1 AND `deleted` = 0
GROUP BY `character_id`;

-- 创建知识类型枚举说明
CREATE TABLE IF NOT EXISTS `knowledge_type_dict` (
    `type_code` VARCHAR(50) PRIMARY KEY COMMENT '类型编码',
    `type_name` VARCHAR(100) NOT NULL COMMENT '类型名称',
    `description` TEXT COMMENT '类型描述',
    `display_order` INT DEFAULT 0 COMMENT '显示顺序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识类型字典表';

INSERT INTO `knowledge_type_dict` (`type_code`, `type_name`, `description`, `display_order`) VALUES
('BASIC_INFO', '基本信息', '角色的基础信息，如姓名、年龄、出生日期、家庭背景等', 1),
('PERSONALITY', '性格特征', '角色的性格、品格、价值观、行为习惯等', 2),
('KNOWLEDGE', '专业知识', '角色掌握的专业技能、学术知识、魔法知识等', 3),
('EVENTS', '重要事件', '角色经历的重要事件、里程碑、转折点等', 4),
('RELATIONSHIPS', '人际关系', '角色的朋友、敌人、家人、导师等重要关系', 5),
('ABILITIES', '能力技能', '角色的特殊能力、技能、天赋等', 6),
('QUOTES', '经典语录', '角色的经典台词、名言、口头禅等', 7);
