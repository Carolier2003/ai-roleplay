#!/bin/bash

# AI角色扮演系统 - 知识库清空工具
# 快速访问脚本

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLEAR_SCRIPT="$SCRIPT_DIR/tools/management/clear_knowledge_base.py"

# 检查脚本是否存在
if [ ! -f "$CLEAR_SCRIPT" ]; then
    echo "❌ 清空脚本不存在: $CLEAR_SCRIPT"
    exit 1
fi

# 显示帮助信息
show_help() {
    echo "🗑️ AI角色扮演系统 - 知识库清空工具"
    echo "================================"
    echo ""
    echo "用法:"
    echo "  ./clear-knowledge.sh [选项]"
    echo ""
    echo "选项:"
    echo "  --stats              查看知识库统计信息"
    echo "  --character <id>     清空指定角色的知识库 (1-5)"
    echo "  --all               清空所有角色的知识库"
    echo "  --force             强制执行，跳过确认"
    echo "  --help              显示此帮助信息"
    echo ""
    echo "角色列表:"
    echo "  1 - 哈利·波特"
    echo "  2 - 苏格拉底"
    echo "  3 - 爱因斯坦"
    echo "  4 - 江户川柯南"
    echo "  5 - 泰拉瑞亚向导"
    echo ""
    echo "示例:"
    echo "  ./clear-knowledge.sh --stats                # 查看统计信息"
    echo "  ./clear-knowledge.sh --character 1          # 清空哈利·波特的知识库"
    echo "  ./clear-knowledge.sh --all                  # 清空所有知识库（需确认）"
    echo "  ./clear-knowledge.sh --all --force          # 强制清空所有知识库"
}

# 如果没有参数，显示帮助
if [ $# -eq 0 ]; then
    show_help
    exit 0
fi

# 解析参数
STATS_ONLY=false
CHARACTER_ID=""
CLEAR_ALL=false
FORCE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --stats)
            STATS_ONLY=true
            shift
            ;;
        --character)
            CHARACTER_ID="$2"
            shift 2
            ;;
        --all)
            CLEAR_ALL=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
        --help|-h)
            show_help
            exit 0
            ;;
        *)
            echo "❌ 未知参数: $1"
            echo "使用 --help 查看帮助信息"
            exit 1
            ;;
    esac
done

# 构建Python脚本参数
PYTHON_ARGS=""

if [ "$STATS_ONLY" = true ]; then
    PYTHON_ARGS="$PYTHON_ARGS --stats"
fi

if [ -n "$CHARACTER_ID" ]; then
    # 验证角色ID
    if [[ ! "$CHARACTER_ID" =~ ^[1-5]$ ]]; then
        echo "❌ 无效的角色ID: $CHARACTER_ID"
        echo "请使用 1-5 之间的数字"
        exit 1
    fi
    PYTHON_ARGS="$PYTHON_ARGS --character $CHARACTER_ID"
fi

if [ "$FORCE" = true ]; then
    PYTHON_ARGS="$PYTHON_ARGS --force"
fi

# 执行Python脚本
echo "🚀 执行知识库清空工具..."
python3 "$CLEAR_SCRIPT" $PYTHON_ARGS
