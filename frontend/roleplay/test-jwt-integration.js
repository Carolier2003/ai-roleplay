#!/usr/bin/env node
/**
 * JWT 集成测试脚本
 * 用于验证前端 JWT 认证流程是否正确
 */

const express = require('express');
const cors = require('cors');
const jwt = require('jsonwebtoken');

const app = express();
const PORT = 18080;
const SECRET = 'test-secret-key';

app.use(cors());
app.use(express.json());

// 模拟用户数据
const mockUsers = [
  {
    id: 1,
    userAccount: 'testuser',
    userPassword: '123456', // 实际应用中应该是加密的
    displayName: '测试用户',
    avatarUrl: null
  }
];

// 模拟角色数据
const mockCharacters = [
  { id: 1, name: '腾讯元宝', avatar: '/src/assets/characters/yuanbao.webp', description: '我是腾讯元宝，你的AI助手' },
  { id: 2, name: '哈利波特', avatar: '/src/assets/characters/harry.webp', description: '霍格沃茨的魔法师' },
  { id: 3, name: '赫敏', avatar: '/src/assets/characters/hermione.webp', description: '聪明的格兰芬多学生' }
];

// JWT 验证中间件
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ code: 401, message: '未提供有效的认证token' });
  }

  jwt.verify(token, SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ code: 403, message: 'Token无效或已过期' });
    }
    req.user = user;
    next();
  });
};

// 登录接口
app.post('/api/auth/login', (req, res) => {
  const { userAccount, userPassword } = req.body;
  
  console.log('[Mock] 收到登录请求:', userAccount);
  
  const user = mockUsers.find(u => u.userAccount === userAccount && u.userPassword === userPassword);
  
  if (!user) {
    return res.status(401).json({
      code: 401,
      message: '用户名或密码错误'
    });
  }
  
  const accessToken = jwt.sign(
    { 
      userId: user.id, 
      userAccount: user.userAccount,
      type: 'access'
    }, 
    SECRET, 
    { expiresIn: '1h' }
  );
  
  const refreshToken = jwt.sign(
    { 
      userId: user.id, 
      userAccount: user.userAccount,
      type: 'refresh'
    }, 
    SECRET, 
    { expiresIn: '7d' }
  );
  
  console.log('[Mock] 登录成功，生成token:', accessToken.substring(0, 20) + '...');
  
  res.json({
    code: 200,
    message: '登录成功',
    data: {
      accessToken,
      refreshToken,
      user: {
        userId: user.id,
        userAccount: user.userAccount,
        displayName: user.displayName,
        avatarUrl: user.avatarUrl
      }
    }
  });
});

// 角色列表接口（需要认证）
app.get('/api/characters', authenticateToken, (req, res) => {
  console.log('[Mock] 获取角色列表，用户:', req.user.userAccount);
  
  res.json({
    code: 200,
    message: '获取成功',
    data: mockCharacters
  });
});

// 流式聊天接口（需要认证）
app.post('/api/chat/stream', authenticateToken, (req, res) => {
  const { characterId, message } = req.body;
  
  console.log('[Mock] 收到流式聊天请求:', {
    userId: req.user.userId,
    characterId,
    message
  });
  
  // 验证请求参数
  if (!message || message.trim() === '') {
    return res.status(400).json({
      code: 400,
      message: '消息内容不能为空'
    });
  }
  
  // 设置 SSE 响应头
  res.writeHead(200, {
    'Content-Type': 'text/plain',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Cache-Control'
  });
  
  // 模拟 AI 响应
  const aiResponse = `你好！我收到了你的消息："${message}"。这是一个模拟的AI回复，用于测试流式响应。`;
  
  // 逐字发送响应
  let index = 0;
  const sendChar = () => {
    if (index < aiResponse.length) {
      res.write(aiResponse[index]);
      index++;
      setTimeout(sendChar, 50); // 50ms 间隔
    } else {
      res.end();
      console.log('[Mock] 流式响应完成');
    }
  };
  
  // 开始发送
  setTimeout(sendChar, 100);
});

// Token 刷新接口
app.post('/api/auth/refresh', (req, res) => {
  const { refreshToken } = req.body;
  
  console.log('[Mock] 收到token刷新请求');
  
  jwt.verify(refreshToken, SECRET, (err, user) => {
    if (err || user.type !== 'refresh') {
      return res.status(403).json({
        code: 403,
        message: 'Refresh token无效'
      });
    }
    
    const accessToken = jwt.sign(
      { 
        userId: user.userId, 
        userAccount: user.userAccount,
        type: 'access'
      }, 
      SECRET, 
      { expiresIn: '1h' }
    );
    
    const newRefreshToken = jwt.sign(
      { 
        userId: user.userId, 
        userAccount: user.userAccount,
        type: 'refresh'
      }, 
      SECRET, 
      { expiresIn: '7d' }
    );
    
    console.log('[Mock] Token刷新成功');
    
    res.json({
      code: 200,
      message: '刷新成功',
      data: {
        accessToken,
        refreshToken: newRefreshToken,
        user: {
          userId: user.userId,
          userAccount: user.userAccount,
          displayName: '测试用户',
          avatarUrl: null
        }
      }
    });
  });
});

// 启动服务器
app.listen(PORT, () => {
  console.log(`🚀 Mock后端服务已启动: http://localhost:${PORT}`);
  console.log('📋 可用接口:');
  console.log('  POST /api/auth/login - 用户登录');
  console.log('  POST /api/auth/refresh - 刷新token'); 
  console.log('  GET  /api/characters - 获取角色列表 (需要认证)');
  console.log('  POST /api/chat/stream - 流式聊天 (需要认证)');
  console.log('');
  console.log('💡 测试账号: testuser / 123456');
  console.log('');
});
