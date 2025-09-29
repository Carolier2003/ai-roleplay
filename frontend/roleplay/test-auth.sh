#!/bin/bash

echo "🔧 认证功能验收测试脚本"
echo "=================================="

# 检查 localStorage 中的 ACCESS_TOKEN
echo "📋 检查 localStorage 配置..."
echo "打开浏览器开发者工具，在控制台执行："
echo "localStorage.getItem('ACCESS_TOKEN')"
echo ""

# 测试 curl 命令
echo "🌐 测试 API 调用..."
echo "复制粘贴以下命令到终端："
echo ""
echo "# 获取 ACCESS_TOKEN (在浏览器控制台执行)"
echo "const token = localStorage.getItem('ACCESS_TOKEN');"
echo "console.log('Bearer ' + token);"
echo ""
echo "# 使用 token 测试 API (替换 YOUR_TOKEN_HERE)"
echo "curl -H \"Authorization: Bearer YOUR_TOKEN_HERE\" http://localhost:18080/api/chat/stream"
echo ""

# 验收标准
echo "✅ 验收标准："
echo "1. 登录后 localStorage 中应该有 ACCESS_TOKEN"
echo "2. axios 请求应该自动附加 Bearer token"
echo "3. 401 错误时应该自动调用 refresh"
echo "4. refresh 失败时应该重定向到 /login"
echo "5. 登出时应该清除所有数据并重定向"
echo "6. 所有 Vitest 测试应该通过 ✓"
echo ""

echo "🧪 运行测试："
echo "npm run test:run"
echo ""

echo "📝 提交信息："
echo "fix(auth): attach Bearer token & auto refresh"
echo ""
echo "🎯 验收完成条件："
echo "- 所有测试通过 ✅"
echo "- localStorage 使用 ACCESS_TOKEN key ✅"
echo "- curl 命令返回 200 状态码"
