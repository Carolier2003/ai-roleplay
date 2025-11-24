# 🎭 AI角色扮演系统

基于Spring AI Alibaba的智能角色扮演对话系统，支持多角色切换、RAG知识增强、语音对话和TTS语音合成等完整功能。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI%20Alibaba-1.0.0--M6-blue.svg)](https://github.com/alibaba/spring-ai-alibaba)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## ✨ 核心特性

- 🎭 **多角色扮演**：5个独特AI角色，每个都有专属知识库和语音风格
- 🧠 **RAG知识增强**：基于Redis Vector Store的精准知识检索
- 🗣️ **完整语音体验**：ASR语音识别 → AI回复 → TTS语音合成的端到端流程
- 🔊 **智能TTS技术**：突破600字符限制，支持长文本分段合成
- 🔐 **企业级安全**：JWT认证 + Spring Security完整权限控制
- ⚡ **高性能处理**：并发优化，支持多用户实时对话

## 🚀 快速开始

### 一键启动（推荐）

```bash
# 1. 克隆项目
git clone <项目地址>
cd ai-roleplay

# 2. 配置API密钥
export AI_DASHSCOPE_API_KEY="your-api-key-here"

# 3. 一键启动完整环境
./start-dev.sh

# 4. 访问应用
# 前端: http://localhost:5173
# 后端: http://localhost:18080
```

### 手动启动

```bash
# 1. 启动基础服务
docker-compose up -d mysql redis

# 2. 启动后端
./start_backend.sh

# 3. 启动前端
cd frontend/roleplay
npm install && npm run dev

# 4. 部署知识库（首次运行）
python3 scripts/deploy_knowledge_base.py --yes
```

## 📁 项目结构

```
ai-roleplay/
├── README.md                 # 项目说明
├── requirements.txt          # 🐍 Python依赖配置
├── docker-compose.yml        # 🐳 Docker服务配置
├── backend/                  # Spring Boot后端应用
├── docs/                     # 📚 项目文档
│   ├── api/                 # API接口文档
│   ├── prompt/              # Prompt工程文档  
│   ├── rag/                 # RAG技术文档
│   ├── deployment/          # 部署指南
│   ├── reports/             # 开发报告
│   └── technical/           # 技术方案
├── tools/                    # 🛠️ 开发工具
│   ├── deployment/          # 部署工具
│   ├── testing/             # 测试工具
│   ├── debugging/           # 调试工具
│   └── management/          # 管理工具
├── tests/                    # 🧪 测试文件
│   ├── api/                 # API测试
│   ├── performance/         # 性能测试
│   └── integration/         # 集成测试
├── docker/                   # 🐳 Docker配置文件
│   ├── mysql/               # MySQL配置
│   └── redis/               # Redis配置
├── scripts/                  # 数据和脚本
└── logs/                     # 日志文件
```

## 🎭 支持的角色

| 角色 | 专业领域 | 知识库规模 | 语音风格 | 特色功能 |
|------|----------|------------|----------|----------|
| 🧙‍♂️ **哈利·波特** | 魔法世界 | 65条知识 | Ethan (阳光活力) | 魔法咒语、霍格沃茨生活 |
| 🏛️ **苏格拉底** | 古希腊哲学 | 48条知识 | Elias (学者风范) | 启发式思辨、哲学对话 |
| 🔬 **爱因斯坦** | 物理学理论 | 52条知识 | Marcus (陕西话特色) | 相对论、科学探索 |
| 🕵️ **江户川柯南** | 推理侦探 | 71条知识 | Dylan (北京话) | 逻辑推理、案例分析 |
| ⚔️ **泰拉瑞亚向导** | 游戏攻略 | 745条完整数据 | Ryan (戏感炸裂) | 武器制作、Boss攻略 |

## 🔧 核心功能

### 🗣️ 语音对话系统
- **ASR语音识别**：支持实时语音转文字
- **TTS语音合成**：智能分段处理，突破600字符限制
- **角色音色映射**：每个角色专属语音风格
- **端到端体验**：语音输入 → AI回复 → 语音输出

### 🧠 RAG知识增强
- **Redis Vector Store**：高性能向量检索
- **角色知识隔离**：每个角色独立知识库
- **智能相似度匹配**：精准知识检索
- **反幻觉机制**：基于知识库的准确回答

### 🎭 角色扮演系统
- **动态提示词**：基于StringTemplate的模板系统
- **个性化人格**：每个角色独特的说话风格
- **上下文记忆**：Redis ChatMemory维护对话历史
- **流式响应**：SSE实时对话体验

### 🔐 安全认证
- **JWT Token认证**：无状态身份验证
- **Spring Security**：企业级安全框架
- **数据隔离**：用户和角色数据完全隔离
- **权限控制**：细粒度API访问控制

## 🛠️ 技术栈

### 后端技术
- **Spring Boot 3.5.6** - 企业级Java框架
- **Spring AI Alibaba 1.0.0-M6** - AI集成框架
- **Spring Security 6.x** - 安全认证框架
- **MySQL 8.0** - 关系型数据库
- **Redis 7.x** - 缓存和向量存储
- **DashScope API** - 阿里云AI服务

### 前端技术
- **Vue.js 3.x** - 渐进式前端框架
- **TypeScript 5.x** - 类型安全的JavaScript
- **Element Plus** - Vue 3 UI组件库
- **Pinia** - Vue 3 状态管理
- **Vite 4.x** - 现代化构建工具

### AI服务
- **通义千问 (qwen-plus)** - 大语言模型
- **DashScope ASR** - 语音识别服务
- **DashScope TTS** - 语音合成服务
- **Redis Vector Store** - 向量数据库

## 📊 系统架构

### 整体架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Vue.js 前端    │    │  Spring Boot    │    │   阿里云AI服务   │
│                 │    │     后端API     │    │                 │
│ • 角色选择界面   │◄──►│ • RESTful API   │◄──►│ • 通义千问LLM    │
│ • 实时对话界面   │    │ • JWT认证       │    │ • ASR语音识别    │
│ • 语音播放控制   │    │ • 流式响应      │    │ • TTS语音合成    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   数据存储层     │
                       │                 │
                       │ • MySQL 8.0     │
                       │ • Redis Cache   │
                       │ • Vector Store  │
                       └─────────────────┘
```

### 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户交互层                                │
├─────────────────────────────────────────────────────────────────┤
│  Web前端 (Vue.js)     │  API客户端          │                    │
│  - 角色选择界面        │  - 第三方集成        │                    │
│  - 实时对话界面        │  - 开发者工具        │                    │
│  - 语音播放控制        │  - 测试工具          │                    │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        应用服务层                                │
├─────────────────────────────────────────────────────────────────┤
│  Spring Boot API      │  认证服务          │  语音处理服务        │
│  - RESTful接口        │  - JWT Token       │  - ASR语音识别       │
│  - 流式响应           │  - 用户管理        │  - TTS语音合成       │
│  - 异常处理           │  - 权限控制        │  - 音频处理          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        业务逻辑层                                │
├─────────────────────────────────────────────────────────────────┤
│  对话编排服务         │  RAG增强服务       │  角色管理服务        │
│  - 会话管理           │  - 知识检索        │  - 角色配置          │
│  - 上下文维护         │  - 向量相似度      │  - 人格模板          │
│  - 流程控制           │  - 结果排序        │  - 音色映射          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AI服务层                                 │
├─────────────────────────────────────────────────────────────────┤
│  Spring AI Alibaba    │  DashScope API     │  提示词引擎          │
│  - ChatClient         │  - 通义千问        │  - 模板渲染          │
│  - 流式处理           │  - 语音服务        │  - 动态组装          │
│  - 记忆管理           │  - 音频合成        │  - 上下文注入        │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据存储层                                │
├─────────────────────────────────────────────────────────────────┤
│  MySQL数据库          │  Redis缓存         │  Redis向量存储       │
│  - 用户数据           │  - 会话记忆        │  - 知识向量          │
│  - 角色信息           │  - 热点数据        │  - 相似度检索        │
│  - 对话历史           │  - 分布式锁        │  - 角色隔离          │
└─────────────────────────────────────────────────────────────────┘
```

### 数据流架构

```
用户输入 → 前端处理 → 认证授权 → 业务逻辑
    ↓
语音识别(ASR) → 文本处理 → RAG知识检索 → 提示词组装
    ↓
AI模型调用 → 响应生成 → TTS语音合成 → 结果返回
    ↓
数据持久化 → 缓存更新 → 日志记录
```

## 🚀 环境要求

### 必需软件
- **Java 17+** (推荐 OpenJDK 17)
- **Node.js 16+** (推荐 18+)
- **Python 3.8+** (用于知识库部署)
- **Docker 20+** (用于数据库服务)
- **Maven 3.8+** (后端构建)

### API密钥配置

#### DashScope API密钥（必需）

```bash
# 环境变量方式（推荐）
export AI_DASHSCOPE_API_KEY="your-api-key-here"

# 获取API密钥：
# 1. 访问 https://dashscope.console.aliyun.com/
# 2. 创建应用并获取API Key
# 3. 确保开通通义千问、ASR、TTS服务
```

#### OSS对象存储配置（可选）

系统使用阿里云OSS存储用户头像和音频文件：

```bash
# 环境变量配置（推荐）
export ALIYUN_OSS_ENDPOINT="https://oss-cn-guangzhou.aliyuncs.com"
export ALIYUN_OSS_ACCESS_KEY_ID="your-access-key-id"
export ALIYUN_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export ALIYUN_OSS_BUCKET_NAME="your-bucket-name"
```

**OSS配置步骤**：
1. 访问 [阿里云OSS控制台](https://oss.console.aliyun.com/)
2. 创建存储桶（Bucket）
3. 获取访问密钥（AccessKey）
4. 配置桶的读写权限

**注意事项**：
- OSS配置是可选的，如果不配置，头像上传功能将不可用
- 音频文件可以选择本地存储或OSS存储
- 建议生产环境使用OSS，开发环境可以跳过此配置

## 📖 详细运行指南

### 1. 环境准备

#### 检查Java环境
```bash
java -version
# 应显示 Java 17 或更高版本
```

#### 检查Node.js环境
```bash
node -v
npm -v
# Node.js 16+ 和对应的npm版本
```

#### 检查Docker环境
```bash
docker --version
docker-compose --version
```

### 2. 数据库服务启动

```bash
# 启动MySQL和Redis
docker-compose up -d mysql redis

# 验证服务状态
docker-compose ps

# 查看日志（如有问题）
docker-compose logs mysql
docker-compose logs redis
```

**默认数据库配置**：
- MySQL: `localhost:3306`, 用户: `roleplay`, 密码: `roleplay123`, 数据库: `ai_roleplay`
- Redis: `localhost:6379`, 无密码

### 3. 后端服务启动

```bash
# 方式1: 使用启动脚本（推荐）
./start_backend.sh

# 方式2: 手动启动
cd backend/roleplay-api
mvn clean package -DskipTests
java -jar target/roleplay-api-0.0.1-SNAPSHOT.jar

# 验证后端服务
curl http://localhost:18080/api/health
# 应返回: {"status":"UP","timestamp":"..."}
```

**后端服务端口**: `18080`

### 4. 前端服务启动

```bash
cd frontend/roleplay

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev

# 或者构建生产版本
npm run build
npm run preview
```

**前端服务端口**: `5173`

### 5. 知识库部署（首次运行必需）

```bash
# 自动部署所有角色知识库
python3 scripts/deploy_knowledge_base.py --yes

# 验证部署结果
python3 scripts/validate_import.py
```

## 🧪 功能测试

### 基础API测试

```bash
# 1. 健康检查
curl http://localhost:18080/api/health

# 2. 获取角色列表
curl http://localhost:18080/api/characters

# 3. 测试对话功能
curl -X POST http://localhost:18080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "characterId": 1,
    "message": "你好，哈利·波特！",
    "userId": "test_user"
  }'

# 4. 测试知识搜索
curl -X POST http://localhost:18080/api/knowledge/search \
  -H "Content-Type: application/json" \
  -d '{
    "characterId": 1,
    "query": "魔法",
    "topK": 3
  }'
```

### 语音功能测试

```bash
# 1. 测试TTS语音合成
curl -X POST http://localhost:18080/api/tts/synthesize \
  -H "Content-Type: application/json" \
  -d '{
    "text": "你好，我是哈利·波特！",
    "voice": "Ethan",
    "model": "qwen3-tts-flash-realtime"
  }'

# 2. 测试角色音色映射
curl http://localhost:18080/api/tts/character/1/recommended-voice
```

### 前端功能测试

1. 访问 http://localhost:5173
2. 选择一个角色（如哈利·波特）
3. 发送消息测试对话功能
4. 点击语音按钮测试TTS功能
5. 查看历史消息记录

## 🚨 故障排除

### 常见问题

#### 1. 后端启动失败
```
错误: 无法连接数据库
```
**解决方案**:
- 确保MySQL服务已启动: `docker-compose ps`
- 检查数据库配置: `backend/roleplay-api/src/main/resources/application.yaml`
- 查看详细错误: `tail -f logs/backend/backend-*.log`

#### 2. API密钥错误
```
错误: DashScope API调用失败
```
**解决方案**:
- 检查API密钥配置是否正确
- 确认API密钥有效且有足够余额
- 验证网络连接到阿里云服务

#### 3. 前端无法连接后端
```
错误: Network Error
```
**解决方案**:
- 确认后端服务在18080端口运行
- 检查CORS配置
- 查看浏览器开发者工具的网络请求

#### 4. 知识库搜索无结果
```
警告: 知识搜索返回空结果
```
**解决方案**:
- 确认知识库已正确部署: `python3 scripts/validate_import.py`
- 检查Redis服务状态
- 重新部署知识库: `python3 scripts/deploy_knowledge_base.py --clear --yes`

#### 5. 头像上传失败
```
错误: 头像上传失败，OSS连接错误
```
**解决方案**:
- 检查OSS配置是否正确
- 验证AccessKey权限是否足够
- 确认存储桶名称和地域正确
- 检查网络连接到阿里云OSS服务

### 日志查看

```bash
# 后端服务日志
tail -f logs/backend/backend-*.log

# 知识库部署日志
cat knowledge_import.log

# Docker服务日志
docker-compose logs mysql
docker-compose logs redis

# 前端开发服务器日志
# 直接在启动前端的终端查看
```

## 🏗️ 架构设计

### 系统概述

基于Spring AI Alibaba的智能角色扮演系统，采用前后端分离架构，集成RAG知识库增强、语音对话、JWT认证等完整功能。系统支持多角色扮演，每个角色拥有独立的知识库和个性化语音风格。

**🎯 设计目标**：
- 🎭 **沉浸式角色扮演**：每个AI角色具有独特人格和专业知识
- 🧠 **智能知识增强**：基于RAG技术的精准知识检索
- 🗣️ **完整语音体验**：ASR识别 → AI回复 → TTS合成的端到端语音对话
- ⚡ **高性能处理**：支持并发用户和实时响应
- 🔐 **企业级安全**：完整的认证授权和数据隔离

### 团队分工

#### 开发团队
- **翦嘉乐 (jianjl)** - 后端架构师 & 核心开发
- **许智超 (xuzhichao)** - 全栈开发 & 用户体验

#### 详细分工

**🔧 翦嘉乐 (jianjl) - 后端核心架构**
- **🏗️ 系统架构**：Spring AI Alibaba框架搭建、项目初始化
- **🤖 AI服务集成**：通义千问模型集成、流式对话实现
- **🧠 RAG知识库**：Redis Vector Store实现、知识检索交互设计
- **🗣️ 语音处理**：TTS语音合成、智能分段算法、音频播放联调
- **📊 数据管理**：MySQL数据库设计、Redis缓存架构
- **🔍 知识爬虫**：多角色知识库数据采集和处理
- **🔐 认证系统**：JWT认证的前后端集成
- **🖼️ 文件存储**：OSS集成后端实现
- **⚡ 性能优化**：并发处理、缓存策略、数据库优化
- **📝 文档编写**：技术文档、API文档、部署指南

**🎨 许智超 (xuzhichao) - 全栈开发 & 用户体验**
- **🖥️ 前端架构**：Vue.js项目搭建、组件化设计
- **🎭 用户界面**：角色选择界面、聊天界面、用户中心
- **🔐 用户认证**：登录注册功能、JWT token管理
- **🗣️ 音频交互**：录音转文字、语音播放控制、音频暂停
- **🖼️ 媒体功能**：头像上传、图片处理、OSS集成
- **💬 聊天体验**：流式对话显示、消息管理、历史记录
- **🎵 音色控制**：多音色支持、语音播放优化
- **🤖 AI服务协作**：流式对话前后端联调
- **🧠 RAG功能集成**：知识检索功能的前端展示和交互
- **📊 数据同步**：Redis到MySQL的数据同步功能设计
- **🔧 用户体验**：界面优化、交互改进、错误处理

**📊 贡献统计**：
| 开发者 | 提交次数 | 净增加行 | 主要领域 |
|--------|----------|----------|----------|
| 翦嘉乐 | 64 | 66,356 | 后端架构、AI集成、协作联调 |
| 许智超 | 34 | 27,594 | 全栈开发、用户体验、功能集成 |

### 数据库设计

#### 核心数据模型

**用户系统**：
```sql
-- 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_account VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,        -- BCrypt加密
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    status TINYINT DEFAULT 1,              -- 1=正常 0=禁用
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**角色系统**：
```sql
-- 角色表
CREATE TABLE characters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    background_story TEXT,                 -- 角色背景故事
    personality_traits TEXT,               -- 性格特征
    speaking_style VARCHAR(100),           -- 说话风格
    expertise_area VARCHAR(100),           -- 专业领域
    voice_style VARCHAR(50) DEFAULT 'Cherry', -- 默认音色
    status TINYINT DEFAULT 1,              -- 1=启用 0=禁用
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 角色知识库表
CREATE TABLE character_knowledge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    character_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    knowledge_type VARCHAR(50),            -- 知识类型
    importance_score INT DEFAULT 5,        -- 重要性评分(1-10)
    source VARCHAR(100),                   -- 知识来源
    source_url VARCHAR(500),               -- 来源URL
    vector_id VARCHAR(100),                -- 向量存储ID
    tags JSON,                             -- 标签数组
    language VARCHAR(10) DEFAULT 'zh',     -- 语言
    status TINYINT DEFAULT 1,              -- 1=启用 0=禁用
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (character_id) REFERENCES characters(id),
    INDEX idx_character_id (character_id),
    INDEX idx_knowledge_type (knowledge_type),
    INDEX idx_status (status)
);
```

**对话系统**：
```sql
-- 对话会话表
CREATE TABLE conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    user_id BIGINT,
    character_id BIGINT,
    title VARCHAR(200),
    context_summary TEXT,                  -- 对话摘要
    message_count INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_minutes INT DEFAULT 0,
    quality_score DECIMAL(3,2),            -- 对话质量评分
    feedback_rating TINYINT,               -- 用户反馈评分
    tags JSON,                             -- 对话标签
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (character_id) REFERENCES characters(id),
    INDEX idx_session_id (session_id),
    INDEX idx_user_character (user_id, character_id),
    INDEX idx_created_at (created_at)
);

-- 对话消息表
CREATE TABLE conversation_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    message_index INT NOT NULL,
    message_type TINYINT NOT NULL,         -- 1=用户 2=AI 3=系统
    content LONGTEXT NOT NULL,
    content_length INT DEFAULT 0,
    response_time_ms INT,                  -- 响应时间
    token_count INT DEFAULT 0,
    model_name VARCHAR(50),
    rag_knowledge_used TINYINT DEFAULT 0,  -- 是否使用RAG
    rag_knowledge_count INT DEFAULT 0,     -- 使用的知识条数
    sentiment_score DECIMAL(3,2),          -- 情感分析评分
    audio_url VARCHAR(500),                -- TTS音频URL
    audio_duration INT,                    -- 音频时长(秒)
    voice_style VARCHAR(50),              -- 使用的音色
    language VARCHAR(10) DEFAULT 'zh',
    metadata JSON,                         -- 扩展元数据
    message_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    INDEX idx_session_id (session_id),
    INDEX idx_message_type (message_type),
    INDEX idx_message_timestamp (message_timestamp)
);
```

### Redis数据结构

#### 会话记忆存储
```redis
# Spring AI ChatMemory格式
Key: spring_ai_alibaba_chat_memory:{sessionId}
Type: List
Value: [
  {
    "messageType": "USER",
    "content": "用户消息内容",
    "timestamp": "2024-01-01T10:00:00Z"
  },
  {
    "messageType": "ASSISTANT", 
    "content": "AI回复内容",
    "timestamp": "2024-01-01T10:00:05Z"
  }
]
TTL: 7天
```

#### 向量存储
```redis
# Redis Vector Store格式
Key: vector_store:knowledge_{id}
Type: Hash
Fields:
  - content: "知识内容文本"
  - embedding: [0.1, 0.2, 0.3, ...] # 向量数据
  - character_id: "1"
  - title: "知识标题"
  - knowledge_type: "人物"
  - importance_score: "8"
```

### 核心业务流程

#### 对话处理流程

1. **用户认证和权限检查**
2. **会话管理**：生成或获取会话ID
3. **角色信息获取**：验证角色状态
4. **RAG知识检索**：检索相关角色知识
5. **提示词组装**：构建系统提示词和用户消息
6. **AI模型调用**：调用通义千问生成回复
7. **TTS语音合成**（可选）：生成语音音频
8. **数据持久化**：保存对话记录
9. **响应构建**：返回完整响应

#### RAG知识检索流程

1. **参数优化**：根据角色知识库大小动态调整检索参数
2. **向量检索**：在Redis Vector Store中搜索相似文档
3. **角色隔离**：使用filterExpression确保只检索当前角色的知识
4. **批量数据库查询**：避免N+1问题
5. **结果排序和过滤**：按重要性评分排序并限制数量

#### TTS语音合成流程

1. **文本预处理**：清理和格式化文本
2. **长文本检测**：判断是否超过600字符限制
3. **智能分段**：长文本自动分段处理
4. **并发合成**：多段文本并发调用TTS服务
5. **音频拼接**：合并多段音频为完整音频
6. **结果封装**：返回Base64编码的音频数据

### 安全架构设计

#### 认证授权流程

1. **公开接口放行**：白名单路径直接通过
2. **提取JWT Token**：从Authorization头获取
3. **Token验证**：验证签名和过期时间
4. **用户信息提取**：从Token中获取用户账号和ID
5. **设置安全上下文**：将用户信息设置到Spring Security上下文

#### 数据安全策略

- **角色数据隔离**：每个用户只能访问授权的角色
- **对话数据隔离**：会话ID包含用户ID，确保数据隔离
- **敏感数据脱敏**：自动脱敏手机号、身份证等敏感信息
- **密码加密**：使用BCrypt加密存储用户密码

### 性能优化策略

#### 缓存策略

- **角色信息缓存**：1小时过期，减少数据库查询
- **热门知识缓存**：30分钟过期，提升检索性能
- **会话记忆缓存**：7天过期，快速恢复对话上下文

#### 并发处理优化

- **TTS专用线程池**：4-8线程，处理语音合成任务
- **RAG检索线程池**：2-4线程，处理知识检索任务
- **异步处理**：使用CompletableFuture实现异步任务

#### 数据库优化

- **关键索引**：为常用查询字段创建复合索引
- **查询优化**：避免N+1问题，使用批量查询
- **连接池配置**：合理配置数据库连接池大小

## 🎯 性能指标

- ⚡ **对话响应时间**: < 3秒
- 🗣️ **语音识别准确率**: > 90%
- 🔊 **TTS合成质量**: 自然清晰
- 📊 **并发用户支持**: > 10用户
- 🚀 **长文本TTS**: 突破600字符限制
- ⚙️ **并发处理优化**: 4-8倍性能提升

## 🔒 安全注意事项

### 生产环境部署

1. **修改默认密码**:
   - 数据库密码
   - Redis密码（如需要）

2. **配置HTTPS**:
   - 前端使用HTTPS
   - 后端API使用HTTPS

3. **API密钥安全**:
   - 使用环境变量而非配置文件
   - 定期轮换API密钥

4. **网络安全**:
   - 配置防火墙规则
   - 限制数据库访问IP

### 数据备份

```bash
# MySQL数据备份
docker exec mysql mysqldump -u roleplay -proleplay123 ai_roleplay > backup.sql

# Redis数据备份
docker exec redis redis-cli BGSAVE
```

## 📖 文档导航

| 文档类型 | 文件路径 | 说明 |
|----------|----------|------|
| 📚 **API文档** | [`docs/api/`](docs/api/) | 接口文档和使用指南 |
| 🛠️ **部署指南** | [`docs/deployment/`](docs/deployment/) | 环境搭建和部署流程 |
| 🧠 **RAG技术** | [`docs/rag/`](docs/rag/) | 向量检索和知识库实现 |
| 📊 **技术方案** | [`docs/technical/`](docs/technical/) | 系统架构和技术选型 |
| 🎨 **Prompt工程** | [`docs/prompt/`](docs/prompt/) | 提示词设计和优化 |

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 技术支持

- 📧 **问题反馈**: 通过 GitHub Issues 提交
- 📖 **文档查看**: [`docs/`](docs/) 目录
- 🔧 **环境检查**: `python3 scripts/check_environment.py`

---

## 🎉 项目亮点

### 🚀 技术创新
- **突破性TTS技术**: 智能分段算法突破DashScope 600字符限制
- **RAG知识隔离**: 每个角色专属知识库，精准检索无串扰
- **端到端语音体验**: ASR → AI → TTS 完整语音对话链路
- **并发性能优化**: ThreadPoolTaskExecutor实现4-8倍性能提升

### 🎭 用户体验
- **沉浸式角色扮演**: 5个独特AI角色，各具专业知识和语音风格
- **智能对话记忆**: Redis ChatMemory维护上下文连贯性
- **实时流式响应**: SSE技术提供流畅对话体验
- **企业级安全**: JWT + Spring Security完整权限控制

### 🏗️ 架构优势
- **现代化技术栈**: 基于最新的Spring AI和Vue 3技术
- **高性能架构**: 多级缓存，异步处理，并发优化
- **企业级安全**: JWT认证，数据隔离，权限控制
- **智能AI集成**: RAG增强，语音处理，流式响应
- **扩展性强**: 模块化设计，便于功能扩展
- **运维友好**: 容器化部署，日志完善，健康检查

**🌟 这是一个技术先进、功能完整、用户体验优秀的现代化AI角色扮演系统！**
