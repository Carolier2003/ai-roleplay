#!/bin/bash
# 性能测试脚本 - 使用规范日志路径

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

LOG_DIR="logs/performance"
mkdir -p "$LOG_DIR"

LOG_FILE="$LOG_DIR/perf-test-$(date +%Y%m%d-%H%M).log"

echo "🚀 开始性能测试..."
echo "日志文件: $LOG_FILE"

# 检查服务是否运行
if ! curl -s http://localhost:18080/api/health > /dev/null; then
    echo "❌ 后端服务未运行，请先启动服务"
    echo "使用: ./start-dev.sh (推荐) 或 ./scripts/start_backend.sh"
    exit 1
fi

echo "✅ 后端服务正常，开始性能测试..."

# 运行性能测试
python performance_comparison_test.py > "$LOG_FILE" 2>&1

echo "📊 性能测试完成，结果保存在: $LOG_FILE"
echo "查看结果: cat $LOG_FILE"
