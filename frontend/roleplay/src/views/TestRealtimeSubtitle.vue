<template>
  <div class="test-page">
    <div class="header">
      <h1>🎤 实时语音转文字字幕测试</h1>
      <p>测试SSE长连接 + 连续音频流的实时字幕功能</p>
    </div>
    
    <div class="content">
      <!-- 实时字幕组件 -->
      <RealtimeSubtitle 
        @subtitle-update="onSubtitleUpdate"
        @listening-change="onListeningChange"
        @error="onError"
      />
      
      <!-- 调试信息 -->
      <div class="debug-panel">
        <h3>🔍 调试信息</h3>
        <div class="debug-item">
          <strong>监听状态:</strong> 
          <span :class="{ 'status-active': isListening, 'status-inactive': !isListening }">
            {{ isListening ? '正在监听' : '未监听' }}
          </span>
        </div>
        <div class="debug-item">
          <strong>最新字幕:</strong> 
          <span class="latest-text">{{ latestText || '暂无' }}</span>
        </div>
        <div class="debug-item">
          <strong>字幕类型:</strong> 
          <span :class="{ 'type-final': isLatestFinal, 'type-partial': !isLatestFinal }">
            {{ isLatestFinal ? '最终结果' : '临时结果' }}
          </span>
        </div>
        <div class="debug-item">
          <strong>更新次数:</strong> 
          <span>{{ updateCount }}</span>
        </div>
        <div class="debug-item">
          <strong>错误信息:</strong> 
          <span class="error-text">{{ lastError || '无' }}</span>
        </div>
      </div>
      
      <!-- 使用说明 -->
      <div class="instructions">
        <h3>📋 使用说明</h3>
        <ol>
          <li>确保已登录系统</li>
          <li>点击"开始语音识别"按钮</li>
          <li>允许浏览器访问麦克风</li>
          <li>开始说话，观察实时字幕效果</li>
          <li>临时结果会实时更新，最终结果会固定显示</li>
          <li>支持连续语音识别，自动分段</li>
        </ol>
      </div>
      
      <!-- 技术特点 -->
      <div class="features">
        <h3>✨ 技术特点</h3>
        <ul>
          <li><strong>SSE长连接:</strong> 建立一次连接，持续接收实时推送</li>
          <li><strong>流式音频传输:</strong> 连续发送音频chunk，非定时轮询</li>
          <li><strong>实时结果更新:</strong> 临时结果实时更新，最终结果确认替换</li>
          <li><strong>智能段落分割:</strong> 基于静音检测自动分段</li>
          <li><strong>状态管理:</strong> 暂停/恢复无需重建连接</li>
          <li><strong>错误处理:</strong> 完善的错误提示和恢复机制</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import RealtimeSubtitle from '@/components/RealtimeSubtitle.vue'

// 状态管理
const isListening = ref(false)
const latestText = ref('')
const isLatestFinal = ref(false)
const updateCount = ref(0)
const lastError = ref('')

// 事件处理
const onSubtitleUpdate = (text: string, isFinal: boolean) => {
  console.log('[TestPage] 字幕更新:', { text, isFinal })
  
  latestText.value = text
  isLatestFinal.value = isFinal
  updateCount.value++
}

const onListeningChange = (listening: boolean) => {
  console.log('[TestPage] 监听状态变化:', listening)
  isListening.value = listening
}

const onError = (error: string) => {
  console.error('[TestPage] 字幕错误:', error)
  lastError.value = error
}
</script>

<style scoped>
.test-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

.header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header h1 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 28px;
}

.header p {
  margin: 0;
  color: #666;
  font-size: 16px;
}

.content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  align-items: start;
}

@media (max-width: 768px) {
  .content {
    grid-template-columns: 1fr;
  }
}

.debug-panel {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.debug-panel h3 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 18px;
}

.debug-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.debug-item:last-child {
  border-bottom: none;
}

.debug-item strong {
  color: #555;
  min-width: 80px;
}

.status-active {
  color: #18a058;
  font-weight: 500;
}

.status-inactive {
  color: #999;
}

.latest-text {
  color: #333;
  font-family: monospace;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-final {
  color: #18a058;
  font-weight: 500;
}

.type-partial {
  color: #f0a020;
  font-weight: 500;
}

.error-text {
  color: #d03050;
  font-family: monospace;
  font-size: 12px;
}

.instructions {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.instructions h3 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 18px;
}

.instructions ol {
  margin: 0;
  padding-left: 20px;
}

.instructions li {
  margin-bottom: 8px;
  color: #555;
  line-height: 1.5;
}

.features {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.features h3 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 18px;
}

.features ul {
  margin: 0;
  padding-left: 20px;
}

.features li {
  margin-bottom: 8px;
  color: #555;
  line-height: 1.5;
}

.features strong {
  color: #18a058;
}
</style>
