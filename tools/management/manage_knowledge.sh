#!/bin/bash
# 知识库管理脚本 - 提供常用管理功能

show_help() {
    echo "🎯 AI角色扮演系统 - 知识库管理工具"
    echo "=================================="
    echo ""
    echo "用法: $0 [命令] [选项]"
    echo ""
    echo "命令:"
    echo "  check      检查环境和数据完整性"
    echo "  deploy     部署知识库（交互式）"
    echo "  quick      快速部署（自动化）"
    echo "  clear      清理并重新部署"
    echo "  validate   验证导入结果"
    echo "  status     查看系统状态"
    echo "  logs       查看相关日志"
    echo ""
    echo "示例:"
    echo "  $0 check                # 检查环境"
    echo "  $0 deploy               # 交互式部署"
    echo "  $0 quick                # 快速部署"
    echo "  $0 clear                # 清理重部署"
    echo "  $0 validate             # 验证结果"
}

check_environment() {
    echo "🔍 检查部署环境..."
    python3 check-env.py
}

deploy_interactive() {
    echo "🚀 交互式部署知识库..."
    python3 deploy.py
}

deploy_quick() {
    echo "⚡ 快速部署知识库..."
    python3 deploy.py --yes
}

deploy_clear() {
    echo "🧹 清理并重新部署..."
    python3 deploy.py --clear --yes
}

validate_results() {
    echo "🧪 验证导入结果..."
    python3 validate_import.py
}

show_status() {
    echo "📊 系统状态检查..."
    echo ""
    
    echo "1. 后端服务状态:"
    if curl -s http://localhost:18080/api/health > /dev/null; then
        echo "   ✅ 后端服务正常运行"
        curl -s http://localhost:18080/api/health | python3 -m json.tool
    else
        echo "   ❌ 后端服务未响应"
    fi
    
    echo ""
    echo "2. 项目文件检查:"
    for file in deploy.py check-env.py validate_import.py; do
        if [ -f "$file" ]; then
            echo "   ✅ $file"
        else
            echo "   ❌ $file 缺失"
        fi
    done
    
    echo ""
    echo "3. 数据目录检查:"
    if [ -d "scripts/data" ]; then
        echo "   ✅ scripts/data 存在"
        for char in harry_potter socrates einstein conan terraria; do
            if [ -d "scripts/data/$char" ]; then
                echo "   ✅ $char 数据存在"
            else
                echo "   ❌ $char 数据缺失"
            fi
        done
    else
        echo "   ❌ scripts/data 目录缺失"
    fi
}

show_logs() {
    echo "📋 相关日志查看..."
    echo ""
    
    if [ -f "knowledge_import.log" ]; then
        echo "🔍 最新导入日志 (最后20行):"
        tail -20 knowledge_import.log
    else
        echo "❌ 导入日志不存在"
    fi
    
    echo ""
    if [ -f "knowledge_import_report.md" ]; then
        echo "📊 导入报告摘要:"
        head -20 knowledge_import_report.md
    else
        echo "❌ 导入报告不存在"
    fi
    
    echo ""
    echo "💡 查看完整日志:"
    echo "   导入日志: cat knowledge_import.log"
    echo "   导入报告: cat knowledge_import_report.md"
    echo "   后端日志: tail -f logs/backend/backend-*.log"
}

# 主程序
case "$1" in
    check)
        check_environment
        ;;
    deploy)
        deploy_interactive
        ;;
    quick)
        deploy_quick
        ;;
    clear)
        deploy_clear
        ;;
    validate)
        validate_results
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    help|--help|-h)
        show_help
        ;;
    "")
        show_help
        ;;
    *)
        echo "❌ 未知命令: $1"
        echo "💡 查看帮助: $0 help"
        exit 1
        ;;
esac
