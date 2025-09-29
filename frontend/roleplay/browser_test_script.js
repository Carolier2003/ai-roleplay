
// 在浏览器控制台执行以下代码来模拟发送消息
console.log("🔍 模拟用户发送消息...");

// 1. 首先检查当前认证状态
const authStore = JSON.parse(localStorage.getItem("ACCESS_TOKEN") || "null");
console.log("当前认证状态:", authStore ? "已登录" : "未登录");

// 2. 模拟点击输入框并发送消息
const inputElement = document.querySelector("input[placeholder*=\"消息\"], textarea[placeholder*=\"消息\"], input[type=\"text\"]");
if (inputElement) {
    inputElement.value = "测试消息 - 检查后端响应";
    inputElement.dispatchEvent(new Event("input", { bubbles: true }));
    console.log("✅ 已输入测试消息");
    
    // 查找发送按钮
    const sendButton = document.querySelector("button[type=\"submit\"], button:contains(\"发送\"), .send-button");
    if (sendButton) {
        console.log("🚀 点击发送按钮...");
        sendButton.click();
    } else {
        // 尝试按回车键
        inputElement.dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", bubbles: true }));
        console.log("⌨️ 按下回车键发送...");
    }
} else {
    console.log("❌ 未找到输入框");
}

