# 🛠️ 开发工具集

## 目录结构

- **deployment/** - 系统部署工具
- **testing/** - 测试和验证工具
- **debugging/** - 调试和诊断工具
- **management/** - 系统管理工具
- **development/** - 开发辅助工具

## 快速使用

### 🚀 部署工具
```bash
# 环境检查
deployment/check_environment.py

# 一键部署
deployment/quick_deploy.sh

# 知识库部署
deployment/deploy_knowledge_base.py

# 部署验证
deployment/validate_import.py

# 数据目录切换
deployment/switch_to_organized_data.sh
```

### 🧪 测试工具
```bash
# 性能测试
testing/performance_comparison_test.py

# 性能测试脚本
testing/run_performance_test.sh

# 综合功能测试
testing/comprehensive_character_test.py

# 泰拉瑞亚角色测试
testing/test_terraria_guide_rag.py

# 启动测试
testing/test-startup.sh
```

### 🔍 调试工具
```bash
# 数据库记录检查
debugging/check_database_records.py

# 知识库调试
debugging/debug_knowledge.py
```

### ⚙️ 管理工具
```bash
# 系统管理
management/manage_knowledge.sh

# 服务启动/停止
management/start_backend.sh
management/stop_backend.sh

# 开发环境启动
management/start-dev.sh

# 知识库清理
management/clear_knowledge_base.py

# 日志清理
management/clean_logs.sh
```

### 🔧 开发工具
```bash
# 数据爬取 (已集成到scripts/crawlers/)
# 请使用 scripts/crawlers/ 目录下的爬虫工具
```

## 便民链接

项目根目录提供了常用工具的快捷方式：

```bash
# 部署相关
./quick_deploy.sh         # 一键部署知识库

# 管理相关  
./manage.sh               # 综合管理工具
./start-dev.sh           # 启动开发环境
./start_backend.sh       # 启动后端服务
./stop_backend.sh        # 停止后端服务
```

## 工具依赖

### Python工具
大部分Python工具需要安装依赖：
```bash
pip install -r deployment/requirements.txt
```

### Shell脚本
所有shell脚本都具有可执行权限，可直接运行：
```bash
chmod +x tools/**/*.sh  # 如果需要的话
```

## 使用建议

### 新手入门
1. 使用 `deployment/check_environment.py` 检查环境
2. 运行 `deployment/quick_deploy.sh` 快速部署
3. 使用 `deployment/validate_import.py` 验证结果

### 日常开发
1. `management/start_backend.sh` - 启动后端服务
2. `testing/run_performance_test.sh` - 性能测试
3. `management/clean_logs.sh` - 清理日志

### 问题排查
1. `debugging/` 目录下的调试工具
2. `management/manage_knowledge.sh` 系统状态检查
3. 查看相关日志文件

## 📝 更新日志

### 2025-09-27 清理更新
- 删除了过时的爬虫脚本 `development/run_crawler.sh`
- 删除了引用不存在文件的调试脚本 `debugging/diagnose_terraria_issue.py`
- 删除了引用不存在文件的修复脚本 `management/fix_terraria_data.py`
- 删除了重复的测试脚本 `testing/test_terraria_guide.py`
- 更新了文档说明，指向新的爬虫工具位置
