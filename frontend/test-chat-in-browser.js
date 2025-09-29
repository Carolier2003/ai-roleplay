// 在浏览器控制台中运行这个脚本来测试聊天布局

// 1. 启用调试模式
localStorage.setItem('CHAT_DEBUG', 'true');
console.log('✅ 调试模式已启用');

// 2. 设置测试 token
const testToken = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInR5cGUiOiJhY2Nlc3MiLCJ1c2VySWQiOjEsImlhdCI6MTc1ODc4NDA3NSwiZXhwIjoxNzU4ODcwNDc1fQ.YutoJTdNbNko1sdMxQ5HraAwtO9P5l0P7KX-BBNAP-sMYPLsjjhLqnV2S5wfHg9TyAFBqe5BVNbCitQ6jJAczg';
localStorage.setItem('ACCESS_TOKEN', testToken);
console.log('✅ 测试 token 已设置');

// 3. 刷新页面以应用调试模式
setTimeout(() => {
    console.log('🔄 刷新页面以应用调试模式...');
    window.location.reload();
}, 1000);

// 4. 页面刷新后运行的测试函数
function runChatTest() {
    console.log('🧪 开始聊天布局测试...');
    
    // 等待页面加载完成
    setTimeout(() => {
        // 查找输入框
        const input = document.querySelector('textarea, input[type="text"]');
        if (input) {
            console.log('✅ 找到输入框:', input);
            
            // 模拟输入消息
            input.value = 'hi';
            input.dispatchEvent(new Event('input', { bubbles: true }));
            
            // 查找发送按钮
            const sendButton = document.querySelector('button[type="submit"], .send-btn, button:contains("发送")');
            if (sendButton) {
                console.log('✅ 找到发送按钮:', sendButton);
                sendButton.click();
                console.log('📤 已发送测试消息: "hi"');
                
                // 等待消息显示
                setTimeout(() => {
                    checkMessageLayout();
                }, 2000);
            } else {
                console.log('❌ 未找到发送按钮');
                // 尝试按回车键发送
                input.dispatchEvent(new KeyboardEvent('keypress', { key: 'Enter', bubbles: true }));
                console.log('⌨️ 尝试按回车键发送');
                
                setTimeout(() => {
                    checkMessageLayout();
                }, 2000);
            }
        } else {
            console.log('❌ 未找到输入框');
        }
    }, 2000);
}

// 5. 检查消息布局
function checkMessageLayout() {
    console.log('🔍 检查消息布局...');
    
    const messages = document.querySelectorAll('.message');
    console.log(`📊 找到 ${messages.length} 条消息`);
    
    messages.forEach((msg, index) => {
        const isUser = msg.dataset.isUser === 'true' || msg.classList.contains('self');
        const debugInfo = msg.querySelector('.debug-info');
        const position = msg.classList.contains('self') ? '右侧' : '左侧';
        
        console.log(`消息 ${index + 1}:`, {
            element: msg,
            isUser: isUser,
            classes: msg.className,
            position: position,
            debugInfo: debugInfo ? debugInfo.textContent : '无调试信息',
            content: msg.textContent.trim().substring(0, 50) + '...'
        });
    });
    
    // 检查是否有布局问题
    const userMessages = document.querySelectorAll('.message.self');
    const aiMessages = document.querySelectorAll('.message:not(.self)');
    
    console.log('📈 布局统计:', {
        总消息数: messages.length,
        用户消息数: userMessages.length,
        AI消息数: aiMessages.length
    });
    
    // 检查用户消息是否在右侧
    userMessages.forEach((msg, index) => {
        const style = window.getComputedStyle(msg.querySelector('.message-content'));
        const marginLeft = style.marginLeft;
        console.log(`用户消息 ${index + 1} 样式:`, {
            marginLeft: marginLeft,
            textAlign: style.textAlign,
            justifyContent: style.justifyContent,
            是否右对齐: marginLeft === 'auto' || style.marginLeft !== '0px'
        });
    });
}

// 如果页面已经加载完成，直接运行测试
if (document.readyState === 'complete') {
    runChatTest();
} else {
    // 否则等待页面加载完成
    window.addEventListener('load', runChatTest);
}

console.log('🚀 聊天布局测试脚本已准备就绪');
