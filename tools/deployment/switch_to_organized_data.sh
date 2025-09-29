#!/bin/bash
# 数据目录切换脚本

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🔄 切换到整理后的数据目录..."

# 确认操作
echo "此操作将："
echo "1. 将当前 data/ 目录重命名为 data_original_backup/"
echo "2. 将 data_organized/ 目录重命名为 data/"
echo ""
read -p "是否继续？(y/N): " confirm

if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
    # 备份原始目录
    if [ -d "data" ] && [ ! -d "data_original_backup" ]; then
        mv data data_original_backup
        echo "✅ 原始数据已备份到: data_original_backup/"
    fi
    
    # 切换到整理后的目录
    if [ -d "data_organized" ]; then
        mv data_organized data
        echo "✅ 已切换到整理后的数据目录"
        
        echo ""
        echo "📁 新的数据目录结构:"
        find data/ -mindepth 1 -maxdepth 1 -type d | while read dir; do
            file_count=$(find "$dir" -type f | wc -l)
            echo "   📂 $(basename "$dir"): $file_count 个文件"
        done
    else
        echo "❌ 找不到 data_organized 目录"
        exit 1
    fi
    
    echo ""
    echo "🎉 数据目录整理完成！"
else
    echo "❌ 操作已取消"
fi
