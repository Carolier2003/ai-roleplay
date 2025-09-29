#!/usr/bin/env node

const fs = require('fs');
const FormData = require('form-data');
const axios = require('axios');

// 直接测试OSS上传功能（绕过认证）
async function testOssUpload() {
    console.log('🧪 开始测试OSS上传功能...\n');
    
    try {
        // 1. 测试OSS配置
        console.log('📝 步骤1: 测试OSS配置');
        const configResponse = await axios.get('http://localhost:18080/api/public/test/oss-config');
        console.log('✅ OSS配置测试成功');
        console.log('📊 配置信息:', JSON.stringify(configResponse.data, null, 2));
        
        // 2. 测试OSS连接
        console.log('\n📝 步骤2: 测试OSS连接');
        const connectionResponse = await axios.get('http://localhost:18080/api/public/test/oss-connection');
        console.log('✅ OSS连接测试成功');
        console.log('📊 连接结果:', JSON.stringify(connectionResponse.data, null, 2));
        
        console.log('\n🎉 OSS基础功能测试完成！');
        console.log('\n💡 要测试完整的头像上传功能，请：');
        console.log('1. 在浏览器中访问: http://localhost:5185');
        console.log('2. 注册或登录账号');
        console.log('3. 点击用户头像 -> 个人中心');
        console.log('4. 使用头像上传功能');
        
    } catch (error) {
        console.error('❌ 测试失败:', error.response?.data || error.message);
    }
}

// 运行测试
testOssUpload().catch(console.error);
