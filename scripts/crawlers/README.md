# 🕷️ AI角色扮演系统爬虫工具集

这个工具集包含多个角色的数据爬虫，可以从各种维基和网站爬取角色和世界观数据，并导入到我们的RAG知识库系统中。

## 📋 支持的角色

- 🧙‍♂️ **哈利·波特** - 从哈利·波特中文维基爬取魔法世界数据
- 🏛️ **苏格拉底** - 从哲学维基爬取古希腊哲学数据  
- 🔬 **爱因斯坦** - 从科学维基爬取物理学和相对论数据
- 🕵️ **江户川柯南** - 从名侦探柯南维基爬取推理数据
- ⚔️ **泰拉瑞亚向导** - 从泰拉瑞亚维基爬取游戏数据

## 📋 功能特性

- 🔍 **智能爬取**: 按优先级爬取核心角色、霍格沃茨、魔法世界等数据
- 📚 **数据处理**: 自动清洗、分块、分类和重要性评分
- 🚀 **一键导入**: 直接导入到RAG系统，支持向量搜索
- 📊 **详细统计**: 提供爬取和处理的详细报告
- ⚡ **批量操作**: 支持批量爬取和导入，提高效率

## 🛠️ 安装和使用

### 1. 环境准备

```bash
# 进入爬虫目录
cd /Users/carol/develop/ai-roleplay/scripts/crawlers

# 创建虚拟环境
python3 -m venv venv
source venv/bin/activate

# 安装依赖
pip install -r common/requirements.txt
```

### 2. 运行特定角色爬虫

```bash
# 哈利·波特数据爬取
cd harry_potter
python3 crawler.py

# 苏格拉底数据爬取  
cd ../socrates
python3 crawler.py

# 爱因斯坦数据爬取
cd ../einstein
python3 crawler.py

# 江户川柯南数据爬取
cd ../conan
python3 crawler.py

# 泰拉瑞亚数据爬取
cd ../terraria
python3 terraria_weapons_crawler_v2.py
python3 tool_spider_fixed.py
```

### 3. 数据导入到RAG系统

```bash
# 导入处理后的数据到RAG系统 (需要后端服务运行)
cd /Users/carol/develop/ai-roleplay/tools/deployment
python3 import_to_rag.py
```

## 📁 项目结构

```
crawlers/
├── common/                         # 通用工具
│   ├── base_crawler.py            # 基础爬虫类
│   └── requirements.txt           # Python依赖
├── harry_potter/                   # 哈利·波特爬虫
│   ├── crawler.py                 # 主爬虫脚本
│   └── expanded_crawled_data/     # 爬取数据
├── socrates/                       # 苏格拉底爬虫
│   └── crawler.py                 # 主爬虫脚本
├── einstein/                       # 爱因斯坦爬虫
│   └── crawler.py                 # 主爬虫脚本
├── conan/                          # 江户川柯南爬虫
│   ├── crawler.py                 # 主爬虫脚本
│   └── smart_batch_crawler.py     # 批量爬虫
├── terraria/                       # 泰拉瑞亚爬虫
│   ├── terraria_weapons_crawler_v2.py  # 武器爬虫
│   └── tool_spider_fixed.py       # 工具爬虫
└── README.md                       # 说明文档
```

## 📁 数据输出结构

```
scripts/data/
├── harry_potter/                   # 哈利·波特数据
├── socrates/                       # 苏格拉底数据
├── einstein/                       # 爱因斯坦数据
├── conan/                          # 江户川柯南数据
└── terraria/                       # 泰拉瑞亚数据
```

## 🎯 爬取策略

### 数据分类
- `PERSONALITY` - 性格特征 (主要角色数据)
- `BASIC_INFO` - 基本信息 (世界观设定)
- `KNOWLEDGE` - 专业知识 (专业领域知识)
- `EVENTS` - 重要事件 (关键情节)

### 重要性评分
- **🔥 最高优先级 (10分)**: 核心角色和关键概念
- **⭐ 高优先级 (8-9分)**: 重要设定和专业知识
- **📚 中优先级 (6-7分)**: 一般知识和背景信息
- **📖 低优先级 (4-5分)**: 补充信息和细节

## 🔧 配置说明

### 通用配置
每个爬虫都可以在对应的 `crawler.py` 中修改配置:

```python
# 调整爬取延迟
DELAY_BETWEEN_REQUESTS = 1  # 秒

# 调整重试次数
MAX_RETRIES = 3

# 调整内容块大小
MAX_CHUNK_SIZE = 500  # 字符
```

### 角色特定配置
- **哈利·波特**: 配置魔法世界相关URL
- **苏格拉底**: 配置哲学维基URL
- **爱因斯坦**: 配置科学维基URL
- **江户川柯南**: 配置推理相关URL
- **泰拉瑞亚**: 配置游戏维基URL

## 📊 预期效果

### 爬取数量
- **哈利·波特**: ~36个页面，处理后约100-200个知识条目
- **苏格拉底**: ~15个页面，处理后约50-80个知识条目
- **爱因斯坦**: ~20个页面，处理后约60-100个知识条目
- **江户川柯南**: ~25个页面，处理后约80-120个知识条目
- **泰拉瑞亚**: ~50个页面，处理后约150-200个知识条目

### 处理结果
- 每个长页面会被智能拆分为多个知识块
- 自动添加上下文信息和关键词标签
- 根据内容重要性自动评分 (1-10分)
- 平均每个知识块 300-500 字符

## 🧪 测试RAG效果

导入完成后，可以测试RAG增强对话:

```bash
# 测试哈利·波特角色对话
curl -X POST "http://localhost:18080/api/chat/message" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "message": "哈利，能告诉我关于分院帽的故事吗？",
    "characterId": 1
  }'

# 测试苏格拉底角色对话
curl -X POST "http://localhost:18080/api/chat/message" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "message": "苏格拉底，什么是真正的智慧？",
    "characterId": 2
  }'
```

**预期效果对比**:

❌ **无RAG**: 通用回答，缺乏角色特色

✅ **有RAG**: 基于角色知识库的个性化回答，包含丰富的背景信息和角色特色

## ⚠️ 注意事项

### 版权说明
- 爬取的数据来自各种维基和网站
- 遵循各网站的robots.txt和使用条款
- 数据仅用于学习和研究目的

### 技术要求
- Python 3.7+
- 稳定的网络连接
- 后端服务需要运行 (导入时)
- 大约 10-30 分钟完成完整流程

### 常见问题

**Q: 爬取失败怎么办？**
A: 检查网络连接，某些页面可能暂时无法访问，重新运行即可。

**Q: 导入失败怎么办？**  
A: 确保后端服务正在运行 (localhost:18080)，检查API接口是否正常。

**Q: 如何添加更多角色？**
A: 创建新的爬虫文件夹，参考现有爬虫的结构和代码。

**Q: 如何调整重要性评分？**
A: 修改对应爬虫中的评分逻辑。

## 🚀 扩展功能

### 添加新角色
1. 创建新的爬虫文件夹
2. 实现爬虫逻辑
3. 配置数据处理和导入流程

### 多语言支持
- 当前主要支持中文维基
- 可以扩展到其他语言维基
- 修改对应爬虫的 `base_url` 即可

### 自动化更新
- 可以设置定时任务自动更新知识库
- 支持增量更新，避免重复爬取

## 📞 支持

如果遇到问题，请检查:
1. Python环境和依赖包
2. 网络连接状态  
3. 后端服务运行状态
4. 日志文件中的错误信息

---

*📅 最后更新: 2025-09-27*  
*🕷️ 爬虫工具集清理完成！*
