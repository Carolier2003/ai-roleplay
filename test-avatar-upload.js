#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const FormData = require('form-data');
const axios = require('axios');

// 测试头像上传功能
async function testAvatarUpload() {
    console.log('🧪 开始测试头像上传功能...\n');
    
    // 1. 首先测试登录获取token
    console.log('📝 步骤1: 用户登录获取token');
    try {
        const loginResponse = await axios.post('http://localhost:18080/api/auth/login', {
            userAccount: 'test@example.com',
            userPassword: 'password123'
        });
        
        const token = loginResponse.data.data.accessToken;
        console.log('✅ 登录成功，获取到token');
        
        // 2. 创建一个测试图片文件（简单的1x1像素PNG）
        console.log('\n📝 步骤2: 创建测试图片');
        const testImageBuffer = Buffer.from([
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1 pixel
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53, 0xDE,
            0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, // IDAT chunk
            0x08, 0x99, 0x01, 0x01, 0x00, 0x00, 0x00, 0xFF, 0xFF,
            0x00, 0x00, 0x00, 0x02, 0x00, 0x01, 0x73, 0x75, 0x01, 0x18,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, // IEND chunk
            0xAE, 0x42, 0x60, 0x82
        ]);
        
        const testImagePath = '/tmp/test-avatar.png';
        fs.writeFileSync(testImagePath, testImageBuffer);
        console.log('✅ 测试图片创建成功');
        
        // 3. 测试头像上传
        console.log('\n📝 步骤3: 上传头像');
        const formData = new FormData();
        formData.append('file', fs.createReadStream(testImagePath), {
            filename: 'test-avatar.png',
            contentType: 'image/png'
        });
        
        const uploadResponse = await axios.post('http://localhost:18080/api/avatar/upload', formData, {
            headers: {
                ...formData.getHeaders(),
                'Authorization': `Bearer ${token}`
            }
        });
        
        console.log('✅ 头像上传成功！');
        console.log('📊 上传结果:', JSON.stringify(uploadResponse.data, null, 2));
        
        // 4. 测试获取头像URL
        console.log('\n📝 步骤4: 获取头像URL');
        const getAvatarResponse = await axios.get('http://localhost:18080/api/avatar', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        console.log('✅ 获取头像URL成功:', getAvatarResponse.data);
        
        // 5. 清理测试文件
        fs.unlinkSync(testImagePath);
        console.log('\n🧹 清理测试文件完成');
        
        console.log('\n🎉 头像上传功能测试完成！所有功能正常工作。');
        
    } catch (error) {
        console.error('❌ 测试失败:', error.response?.data || error.message);
        
        if (error.response?.status === 400 && error.response?.data?.message?.includes('账号或密码错误')) {
            console.log('\n💡 提示: 请先注册测试账号或使用已有账号');
            console.log('注册命令: curl -X POST "http://localhost:18080/api/auth/register" -H "Content-Type: application/json" -d \'{"userAccount":"test@example.com","userPassword":"password123","confirmPassword":"password123"}\'');
        }
    }
}

// 运行测试
testAvatarUpload().catch(console.error);
