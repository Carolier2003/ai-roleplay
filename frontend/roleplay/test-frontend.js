// 测试前端登录注册流程
console.log('开始测试前端登录注册流程...');

// 1. 检查页面元素
const inputBox = document.querySelector('textarea');
const sendButton = document.querySelector('button[type="button"]');

if (inputBox && sendButton) {
    console.log('✓ 找到输入框和发送按钮');
    
    // 2. 模拟输入消息
    inputBox.value = '你好，这是一条测试消息';
    inputBox.dispatchEvent(new Event('input', { bubbles: true }));
    
    console.log('✓ 输入测试消息');
    
    // 3. 点击发送按钮（应该触发登录弹窗）
    sendButton.click();
    
    console.log('✓ 点击发送按钮');
    
    // 4. 检查是否出现登录弹窗
    setTimeout(() => {
        const modal = document.querySelector('.n-modal');
        if (modal) {
            console.log('✓ 登录弹窗已显示');
        } else {
            console.log('✗ 登录弹窗未显示');
        }
    }, 1000);
} else {
    console.log('✗ 未找到输入框或发送按钮');
}
