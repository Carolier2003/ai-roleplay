#!/bin/bash

# AI角色扮演系统开发环境启动脚本

echo "🚀 启动AI角色扮演系统开发环境..."

# 检查Docker是否运行
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 启动Docker Compose
echo "📦 启动Docker容器..."
docker-compose up -d

# 等待容器启动
echo "⏳ 等待容器启动完成..."
sleep 10

# 检查容器状态
echo "🔍 检查容器状态..."
docker-compose ps

# 检查MySQL连接
echo "🗄️ 检查MySQL连接..."
for i in {1..30}; do
    if docker exec ai-roleplay-mysql mysql -u roleplay -proleplay123 -e "SELECT 1" >/dev/null 2>&1; then
        echo "✅ MySQL连接成功"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ MySQL连接失败，请检查配置"
        exit 1
    fi
    sleep 2
done

# 检查Redis连接
echo "🔑 检查Redis连接..."
if docker exec ai-roleplay-redis-stack redis-cli ping >/dev/null 2>&1; then
    echo "✅ Redis连接成功"
else
    echo "❌ Redis连接失败，请检查配置"
    exit 1
fi

# 检查Python环境和依赖
echo "🐍 检查Python环境..."
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3未安装，请先安装Python3"
    exit 1
fi

# 检查Python依赖
echo "📦 检查Python依赖..."
if [ -f "requirements.txt" ]; then
    echo "📋 使用 requirements.txt 检查和安装依赖..."
    # 检查关键依赖是否已安装
    python3 -c "import requests" 2>/dev/null || MISSING_DEPS=1
    python3 -c "import mysql.connector" 2>/dev/null || MISSING_DEPS=1
    
    if [ "$MISSING_DEPS" = "1" ]; then
        echo "📦 安装缺失的依赖..."
        if pip3 install -r requirements.txt --user 2>/dev/null; then
            echo "✅ 使用--user参数安装成功"
        elif pip3 install -r requirements.txt --break-system-packages 2>/dev/null; then
            echo "✅ 使用--break-system-packages参数安装成功"
        else
            echo "❌ 无法自动安装依赖，请手动安装: pip3 install -r requirements.txt --user"
            exit 1
        fi
    else
        echo "✅ Python依赖已满足"
    fi
else
    echo "⚠️ requirements.txt 不存在，只检查 requests..."
    python3 -c "import requests" 2>/dev/null || {
        echo "📦 需要安装requests库..."
        if pip3 install requests --user 2>/dev/null; then
            echo "✅ 使用--user参数安装成功"
        elif pip3 install requests --break-system-packages 2>/dev/null; then
            echo "✅ 使用--break-system-packages参数安装成功"
        else
            echo "❌ 无法自动安装requests库，请手动安装: pip3 install requests --user"
            exit 1
        fi
    }
fi

# 检查环境变量
echo ""
echo "🔑 检查环境变量..."
if [ -z "$AI_DASHSCOPE_API_KEY" ]; then
    echo "⚠️ AI_DASHSCOPE_API_KEY 环境变量未设置"
    echo "💡 提示: 后端服务可能无法正常工作"
    echo "   请设置: export AI_DASHSCOPE_API_KEY=your_api_key"
    echo "   或在启动后手动配置"
else
    echo "✅ AI_DASHSCOPE_API_KEY 已设置"
fi

# 启动后端服务
echo ""
echo "🚀 启动后端服务..."
echo "📁 切换到后端目录..."
cd backend/roleplay-api

# 检查是否需要编译
if [ ! -f "target/roleplay-api-0.0.1-SNAPSHOT.jar" ]; then
    echo "🔨 编译后端项目..."
    
    # 检查Maven是否安装
    if ! command -v mvn &> /dev/null; then
        echo "❌ Maven未安装，请先安装Maven"
        echo "💡 安装建议："
        echo "   macOS: brew install maven"
        echo "   Ubuntu: sudo apt install maven"
        echo "   CentOS: sudo yum install maven"
        exit 1
    fi
    
    echo "✅ Maven已安装，开始编译..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ 后端编译失败"
        exit 1
    fi
else
    echo "✅ JAR文件已存在，跳过编译"
fi

# 启动后端服务（后台运行）
echo "⚡ 启动Spring Boot应用..."
nohup java -jar target/roleplay-api-0.0.1-SNAPSHOT.jar --server.port=18080 > ../../logs/backend-$(date +%Y%m%d-%H%M%S).log 2>&1 &
BACKEND_PID=$!

# 回到项目根目录
cd ../..

# 等待后端服务启动
echo "⏳ 等待后端服务启动完成..."
for i in {1..60}; do
    if curl -s http://localhost:18080/api/health > /dev/null 2>&1; then
        echo "✅ 后端服务启动成功"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "❌ 后端服务启动超时"
        kill $BACKEND_PID 2>/dev/null
        exit 1
    fi
    sleep 2
done

# 导入知识库
echo ""
echo "📚 导入知识库到MySQL和Redis..."
echo "🎯 泰拉瑞亚数据: 5种类型共745条记录（武器已去重优化）"
echo "📁 数据格式: JSONLines（已适配新格式）"
python3 tools/deployment/deploy_knowledge_base.py --yes

if [ $? -eq 0 ]; then
    echo "✅ 知识库导入成功"
else
    echo "❌ 知识库导入失败，请检查日志"
    exit 1
fi

# 显示完成信息
echo ""
echo "🎉 AI角色扮演系统已完全启动！"
echo "=================================="
echo ""
echo "🌐 服务访问地址："
echo "   • 后端API: http://localhost:18080"
echo "   • API健康检查: http://localhost:18080/api/health"
echo "   • 角色列表: http://localhost:18080/api/characters"
echo ""
echo "🔧 数据库连接信息："
echo "   • MySQL: localhost:3306 (数据库: ai_roleplay)"
echo "   • Redis: localhost:6379"
echo ""
echo "🧪 快速测试："
echo "   curl -X POST http://localhost:18080/api/chat/message \\"
echo "        -H 'Content-Type: application/json' \\"
echo "        -d '{\"message\":\"你好\",\"characterId\":1}'"
echo ""
echo "📊 系统状态："
echo "   • Docker容器: 运行中"
echo "   • 后端服务: 运行中 (PID: $BACKEND_PID)"
echo "   • 知识库: 已导入（5角色，泰拉瑞亚745条记录）"
echo ""
echo "📝 日志文件:"
echo "   • 后端日志: logs/backend-$(date +%Y%m%d-%H%M%S).log"
echo "   • 导入日志: knowledge_import.log"
echo ""
