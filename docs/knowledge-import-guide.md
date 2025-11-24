# 📚 RAG知识库导入指南

## 概述

现在提供了三种方式导入RAG知识库数据，**无需Python环境**即可完成导入：

1. **Web界面导入**（推荐）✨ - 最简单直观的方式
2. **Shell脚本导入** - 命令行快速导入
3. **Python脚本导入** - 保留原有方式作为备选

---

## 方式一：Web界面导入（推荐）✨

### 使用步骤

1. **启动服务**
   ```bash
   # 启动后端和前端
   ./start-dev.sh
   ```

2. **访问管理页面**
   - 打开浏览器访问：`http://localhost:5173/knowledge`
   - 或在前端导航栏添加链接

3. **选择角色和文件**
   - 选择要导入的角色（1-5）
   - 拖拽或点击选择JSON文件
   - 点击"开始导入"按钮

4. **查看导入结果**
   - 页面会显示导入进度和结果
   - 成功后会显示导入的知识条数

### 特点

- ✅ 无需安装Python环境
- ✅ 可视化界面，操作简单
- ✅ 支持拖拽上传
- ✅ 实时显示导入进度和结果
- ✅ 自动识别多种JSON格式

---

## 方式二：Shell脚本导入

### 使用步骤

```bash
# 1. 赋予执行权限（首次使用）
chmod +x import-knowledge.sh

# 2. 执行导入
./import-knowledge.sh <json_file> [character_id]

# 示例
./import-knowledge.sh scripts/data/harry_potter/knowledge_base.json 1
./import-knowledge.sh scripts/data/terraria/terraria_weapons.json 5
```

### 参数说明

- `json_file`: 要导入的JSON文件路径（必需）
- `character_id`: 角色ID (1-5，可选，默认1)

### 环境变量

```bash
# 自定义后端地址
BACKEND_URL=http://localhost:18080 ./import-knowledge.sh data.json 1
```

### 角色ID列表

| ID | 角色名称 |
|----|---------|
| 1 | 哈利·波特 |
| 2 | 苏格拉底 |
| 3 | 爱因斯坦 |
| 4 | 江户川柯南 |
| 5 | 泰拉瑞亚向导 |

### 特点

- ✅ 无需Python环境，只需要curl
- ✅ 命令行快速导入
- ✅ 支持自动化脚本调用
- ✅ 显示详细的导入结果

---

## 方式三：Python脚本导入（备选）

如果前两种方式都不适用，仍可使用原有的Python脚本：

```bash
# 使用原有的导入脚本
python3 tools/deployment/import_to_rag.py <json_file>

# 或使用完整部署脚本
python3 tools/deployment/deploy_knowledge_base.py --yes
```

---

## JSON文件格式要求

### 标准格式（推荐）

```json
[
  {
    "title": "知识标题",
    "content": "知识内容",
    "knowledgeType": "KNOWLEDGE",
    "importanceScore": 5,
    "source": "harry_potter_wiki",
    "sourceUrl": "https://example.com",
    "tags": ["标签1", "标签2"]
  },
  {
    "title": "另一个知识",
    "content": "内容..."
  }
]
```

### 支持的字段映射

后端会自动识别以下字段：

- **标题**: `title`, `name`, `武器名`, `工具名`, `NPC名称`, `Boss中文名`, `事件中文名`
- **内容**: `content`, `description`, `详细描述`
- **类型**: `knowledgeType`, `knowledge_type`, `type`
- **重要性**: `importanceScore`, `importance_score`, `importance`
- **来源**: `source`
- **URL**: `sourceUrl`, `source_url`, `url`
- **标签**: `tags` (数组格式)

### 特殊格式支持

#### 泰拉瑞亚格式

```json
{
  "武器名": "木剑",
  "属性": {
    "伤害": 7,
    "击退": 4
  },
  "合成表": [
    {
      "产物": "木剑",
      "材料": ["木材 x7"],
      "制作站": "工作台"
    }
  ]
}
```

系统会自动识别并转换为标准格式。

---

## API接口说明

### 文件上传接口

**接口路径**: `POST /api/knowledge/import/file`

**请求参数**:
- `file`: 文件（multipart/form-data）
- `characterId`: 角色ID（Long）

**响应示例**:
```json
{
  "success": true,
  "message": "文件导入成功",
  "imported_count": 65,
  "character_id": 1,
  "filename": "knowledge_base.json"
}
```

---

## 常见问题

### Q: 导入失败怎么办？

A: 检查以下几点：
1. 后端服务是否正常运行
2. JSON文件格式是否正确
3. 文件大小是否超过限制（建议<10MB）
4. 查看后端日志获取详细错误信息

### Q: 支持哪些JSON格式？

A: 支持：
- JSON数组格式：`[{...}, {...}]`
- 单个JSON对象：`{...}`
- JSONLines格式（每行一个JSON对象）

### Q: 如何批量导入多个文件？

A: 
- Web界面：逐个上传
- Shell脚本：编写循环脚本批量调用
- Python脚本：使用 `deploy_knowledge_base.py` 自动导入所有角色

### Q: 导入后如何验证？

A: 
1. 使用知识搜索API测试
2. 在聊天界面测试RAG增强对话
3. 查看知识统计：`GET /api/knowledge/stats/{characterId}`

---

## 性能建议

1. **大文件导入**：建议分批导入，每批不超过1000条
2. **并发导入**：避免同时导入多个大文件
3. **网络环境**：确保网络稳定，大文件上传可能需要较长时间

---

## 更新日志

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2025-01-15 | 新增Web界面和Shell脚本导入方式 |

---

## 相关文件

- 后端接口：`backend/roleplay-api/src/main/java/com/carol/backend/controller/KnowledgeController.java`
- 前端页面：`frontend/roleplay/src/views/KnowledgeManagement.vue`
- Shell脚本：`import-knowledge.sh`
- Python脚本：`tools/deployment/import_to_rag.py`
