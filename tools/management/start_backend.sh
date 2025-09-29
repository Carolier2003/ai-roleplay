#!/bin/bash
# 后端服务启动脚本

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

LOG_DIR="logs/backend"
mkdir -p "$LOG_DIR"

LOG_FILE="$LOG_DIR/backend-$(date +%Y%m%d-%H%M).log"
JAR_FILE="backend/roleplay-api/target/roleplay-api-0.0.1-SNAPSHOT.jar"

echo "🚀 启动AI角色扮演后端服务..."
echo "日志文件: $LOG_FILE"

if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR文件不存在: $JAR_FILE"
    echo "请先运行: cd backend/roleplay-api && mvn clean package spring-boot:repackage"
    exit 1
fi

# 停止现有服务
echo "停止现有服务..."
pkill -f "java.*18080" 2>/dev/null || true
sleep 2

# 启动新服务
echo "启动服务 (端口: 18080)..."
nohup java -jar "$JAR_FILE" --server.port=18080 > "$LOG_FILE" 2>&1 &

echo "等待服务启动..."
sleep 8

# 检查服务状态
if curl -s http://localhost:18080/api/health > /dev/null; then
    echo "✅ 服务启动成功!"
    echo "🔗 健康检查: http://localhost:18080/api/health"
    echo "📋 日志文件: $LOG_FILE"
else
    echo "❌ 服务启动失败，请检查日志: $LOG_FILE"
    exit 1
fi
