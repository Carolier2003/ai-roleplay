#!/bin/bash

echo "🚀 开始测试AI角色扮演系统启动状态..."
echo ""

# 检查应用健康状态
echo "1. 检查应用健康状态..."
HEALTH_RESULT=$(curl -s "http://localhost:18080/api/health" 2>/dev/null)
if [ $? -eq 0 ] && [ ! -z "$HEALTH_RESULT" ]; then
    echo "✅ 应用健康检查成功: $HEALTH_RESULT"
else
    echo "❌ 应用健康检查失败 - 应用可能还在启动中或启动失败"
    echo "   请等待片刻或检查启动日志"
    exit 1
fi

echo ""

# 验证模板系统
echo "2. 验证Prompt模板系统..."
TEMPLATE_RESULT=$(curl -s "http://localhost:18080/api/characters/template/validate" 2>/dev/null)
if [ $? -eq 0 ] && [ ! -z "$TEMPLATE_RESULT" ]; then
    echo "✅ 模板系统验证成功"
    echo "   响应: $TEMPLATE_RESULT"
else
    echo "❌ 模板系统验证失败"
fi

echo ""

# 获取角色列表
echo "3. 获取角色列表..."
CHARACTERS_RESULT=$(curl -s "http://localhost:18080/api/characters" 2>/dev/null)
if [ $? -eq 0 ] && [ ! -z "$CHARACTERS_RESULT" ]; then
    echo "✅ 角色列表获取成功"
    echo "   发现角色数量: $(echo "$CHARACTERS_RESULT" | grep -o '"id"' | wc -l)"
else
    echo "❌ 角色列表获取失败"
fi

echo ""

# 测试基础对话
echo "4. 测试基础对话功能..."
CHAT_RESULT=$(curl -s -X POST "http://localhost:18080/api/chat/message" \
    -H "Content-Type: application/json" \
    -d '{"message":"你好","userId":"test-user"}' 2>/dev/null)
if [ $? -eq 0 ] && [ ! -z "$CHAT_RESULT" ]; then
    echo "✅ 基础对话测试成功"
    echo "   AI回复了: $(echo "$CHAT_RESULT" | grep -o '"content":"[^"]*"' | head -1)"
else
    echo "❌ 基础对话测试失败"
fi

echo ""
echo "🎉 Prompt工程功能测试完成！"
echo ""
echo "📋 下一步测试建议:"
echo "   - 与哈利·波特对话: characterId=1"
echo "   - 与苏格拉底对话: characterId=2"  
echo "   - 与爱因斯坦对话: characterId=3"
echo ""
echo "📖 使用test-prompt-api.http文件进行详细测试"
