#!/bin/bash
# 日志清理脚本

LOG_DIR="logs"
DAYS_TO_KEEP=7

echo "🧹 清理 ${DAYS_TO_KEEP} 天前的日志文件..."

if [ -d "$LOG_DIR" ]; then
    # 删除7天前的日志文件
    find "$LOG_DIR" -name "*.log" -mtime +$DAYS_TO_KEEP -delete
    
    # 压缩3天前的日志文件
    find "$LOG_DIR" -name "*.log" -mtime +3 ! -name "*.gz" -exec gzip {} \;
    
    echo "✅ 日志清理完成"
    
    # 显示当前日志大小
    du -sh "$LOG_DIR"
else
    echo "❌ 日志目录不存在: $LOG_DIR"
fi
