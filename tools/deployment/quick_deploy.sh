#!/bin/bash
# 快速部署脚本 - 在新环境中一键部署知识库

set -e  # 遇到错误立即退出

echo "🚀 AI角色扮演系统 - 快速部署脚本"
echo "================================"

# 检查Python环境
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 未安装，请先安装Python3"
    exit 1
fi

# 检查依赖
echo "📦 检查Python依赖..."
python3 -c "import requests" 2>/dev/null || {
    echo "📦 需要安装requests库..."
    echo "🔧 尝试安装（可能需要虚拟环境）..."
    
    # 尝试不同的安装方式
    if pip3 install requests --user 2>/dev/null; then
        echo "✅ 使用--user参数安装成功"
    elif pip3 install requests --break-system-packages 2>/dev/null; then
        echo "✅ 使用--break-system-packages参数安装成功"
    else
        echo "❌ 无法自动安装requests库"
        echo "💡 请手动安装："
        echo "   pip3 install requests --user"
        echo "   或者使用虚拟环境："
        echo "   python3 -m venv venv && source venv/bin/activate && pip install requests"
        exit 1
    fi
}

# 检查项目结构 - 智能检测运行位置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT=""
DEPLOY_SCRIPT=""
DATA_DIR=""

# 方案1：从项目根目录运行（通过符号链接）
if [ -f "deploy.py" ] && [ -d "scripts/data" ]; then
    PROJECT_ROOT="$(pwd)"
    DEPLOY_SCRIPT="deploy.py"
    DATA_DIR="scripts/data"
    echo "✅ 检测到从项目根目录运行"
# 方案2：从tools/deployment目录运行
elif [ -f "deploy_knowledge_base.py" ] && [ -d "../../scripts/data" ]; then
    PROJECT_ROOT="$(cd ../.. && pwd)"
    DEPLOY_SCRIPT="deploy_knowledge_base.py"
    DATA_DIR="../../scripts/data"
    echo "✅ 检测到从tools/deployment目录运行"
else
    echo "❌ 找不到部署脚本或数据目录"
    echo "请从以下位置之一运行："
    echo "  1. 项目根目录: ./quick_deploy.sh"
    echo "  2. tools/deployment/目录: ./quick_deploy.sh"
    exit 1
fi

echo "📁 项目根目录: $PROJECT_ROOT"
echo "📄 部署脚本: $DEPLOY_SCRIPT"
echo "📂 数据目录: $DATA_DIR"

# 检查后端服务
echo "🔍 检查后端服务..."
if curl -s http://localhost:18080/api/health > /dev/null; then
    echo "✅ 后端服务正常运行"
else
    echo "❌ 后端服务未运行，请先启动："
    echo "   cd backend/roleplay-api"
    echo "   java -jar target/roleplay-api-0.0.1-SNAPSHOT.jar --server.port=18080"
    exit 1
fi

# 询问是否清理现有数据
echo ""
read -p "🤔 是否清理现有知识库数据？(y/N): " clear_data

# 运行部署
echo ""
echo "🚀 开始部署知识库..."
if [ "$clear_data" = "y" ] || [ "$clear_data" = "Y" ]; then
    python3 "$DEPLOY_SCRIPT" --clear --yes
else
    python3 "$DEPLOY_SCRIPT" --yes
fi

echo ""
echo "🎉 部署完成！"
echo "📋 查看详细报告: cat knowledge_import_report.md"
echo "📋 查看导入日志: cat knowledge_import.log"
echo ""
echo "💡 提示："
echo "   • 测试聊天: curl -X POST http://localhost:18080/api/chat/message -H 'Content-Type: application/json' -d '{\"message\":\"你好\",\"characterId\":1}'"
echo "   • 查看角色: curl http://localhost:18080/api/characters"
