#!/bin/bash
# 后端服务停止脚本

echo "🛑 停止AI角色扮演后端服务..."

pkill -f "java.*18080" && echo "✅ 服务已停止" || echo "ℹ️ 没有发现运行中的服务"
