#!/bin/bash

# AI角色扮演系统 - 知识库导入脚本（Shell版本）
# 使用curl直接调用API，无需Python环境
#
# @author jianjl
# @version 1.0
# @description 简单的Shell脚本，使用curl上传JSON文件导入知识库
# @date 2025-01-15

# 默认配置
BACKEND_URL="${BACKEND_URL:-http://localhost:18080}"
CHARACTER_ID="${CHARACTER_ID:-1}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 显示帮助信息
show_help() {
    echo -e "${BLUE}📚 AI角色扮演系统 - 知识库导入工具${NC}"
    echo "=========================================="
    echo ""
    echo "用法:"
    echo "  ./import-knowledge.sh <json_file> [character_id]"
    echo ""
    echo "参数:"
    echo "  json_file      要导入的JSON文件路径（必需）"
    echo "  character_id  角色ID (1-5，可选，默认1)"
    echo ""
    echo "环境变量:"
    echo "  BACKEND_URL   后端服务地址（默认: http://localhost:18080）"
    echo ""
    echo "角色列表:"
    echo "  1 - 哈利·波特"
    echo "  2 - 苏格拉底"
    echo "  3 - 爱因斯坦"
    echo "  4 - 江户川柯南"
    echo "  5 - 泰拉瑞亚向导"
    echo ""
    echo "示例:"
    echo "  ./import-knowledge.sh scripts/data/harry_potter/knowledge_base.json 1"
    echo "  BACKEND_URL=http://localhost:18080 ./import-knowledge.sh data.json 2"
    echo ""
}

# 检查参数
if [ $# -eq 0 ] || [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_help
    exit 0
fi

JSON_FILE="$1"
CHARACTER_ID="${2:-$CHARACTER_ID}"

# 验证文件是否存在
if [ ! -f "$JSON_FILE" ]; then
    echo -e "${RED}❌ 错误: 文件不存在: $JSON_FILE${NC}"
    exit 1
fi

# 验证文件格式
if [[ ! "$JSON_FILE" =~ \.json$ ]]; then
    echo -e "${RED}❌ 错误: 只支持JSON文件格式${NC}"
    exit 1
fi

# 验证角色ID
if ! [[ "$CHARACTER_ID" =~ ^[1-5]$ ]]; then
    echo -e "${RED}❌ 错误: 角色ID必须是1-5之间的数字${NC}"
    exit 1
fi

# 检查curl是否安装
if ! command -v curl &> /dev/null; then
    echo -e "${RED}❌ 错误: 未找到curl命令，请先安装curl${NC}"
    exit 1
fi

# 检查后端服务
echo -e "${BLUE}🔍 检查后端服务: $BACKEND_URL${NC}"
if ! curl -s -f "$BACKEND_URL/api/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ 错误: 无法连接到后端服务: $BACKEND_URL${NC}"
    echo -e "${YELLOW}💡 提示: 请确保后端服务已启动${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 后端服务正常${NC}"

# 显示导入信息
echo ""
echo -e "${BLUE}📋 导入信息${NC}"
echo "  文件: $JSON_FILE"
echo "  角色ID: $CHARACTER_ID"
echo "  后端地址: $BACKEND_URL"
echo ""

# 获取文件大小
FILE_SIZE=$(stat -f%z "$JSON_FILE" 2>/dev/null || stat -c%s "$JSON_FILE" 2>/dev/null)
FILE_SIZE_MB=$(echo "scale=2; $FILE_SIZE / 1024 / 1024" | bc)

echo -e "${YELLOW}📤 开始上传文件 (大小: ${FILE_SIZE_MB}MB)...${NC}"

# 执行导入
RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST \
    -F "file=@$JSON_FILE" \
    -F "characterId=$CHARACTER_ID" \
    "$BACKEND_URL/api/knowledge/import/file")

# 分离响应体和状态码
HTTP_BODY=$(echo "$RESPONSE" | head -n -1)
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

# 检查HTTP状态码
if [ "$HTTP_CODE" -eq 200 ]; then
    # 解析JSON响应
    SUCCESS=$(echo "$HTTP_BODY" | grep -o '"success":[^,]*' | cut -d':' -f2 | tr -d ' ')
    MESSAGE=$(echo "$HTTP_BODY" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)
    IMPORTED_COUNT=$(echo "$HTTP_BODY" | grep -o '"imported_count":[^,}]*' | cut -d':' -f2 | tr -d ' ')
    
    if [ "$SUCCESS" = "true" ]; then
        echo -e "${GREEN}✅ 导入成功！${NC}"
        echo -e "${GREEN}   消息: $MESSAGE${NC}"
        if [ -n "$IMPORTED_COUNT" ]; then
            echo -e "${GREEN}   导入数量: $IMPORTED_COUNT 条知识${NC}"
        fi
        echo ""
        echo -e "${GREEN}🎉 知识库导入完成！现在可以测试RAG增强对话了${NC}"
        exit 0
    else
        echo -e "${RED}❌ 导入失败${NC}"
        echo -e "${RED}   消息: $MESSAGE${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ 导入失败 (HTTP $HTTP_CODE)${NC}"
    echo -e "${RED}   响应: $HTTP_BODY${NC}"
    exit 1
fi
