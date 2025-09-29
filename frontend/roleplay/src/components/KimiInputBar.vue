<template>
  <div class="kimi-input-bar">
    <div class="input-container">
      <div class="input-wrapper">
        <textarea
          ref="textareaRef"
          v-model="inputText"
          :placeholder="placeholder"
          :disabled="disabled"
          class="message-input"
          @keydown="handleKeydown"
          @input="handleInput"
          rows="1"
        />
        
        <!-- 发送按钮 -->
        <button
          :disabled="!canSend"
          @click="handleSend"
          class="send-button"
          :class="{ active: canSend }"
        >
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path d="M2,21L23,12L2,3V10L17,12L2,14V21Z" fill="currentColor"/>
          </svg>
        </button>
      </div>
      
      <!-- 输入提示 -->
      <div class="input-hint">
        <span>按 Enter 发送，Shift + Enter 换行</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'

interface Props {
  disabled?: boolean
  placeholder?: string
}

interface Emits {
  send: [content: string]
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  placeholder: '输入消息...'
})

const emit = defineEmits<Emits>()

const textareaRef = ref<HTMLTextAreaElement>()
const inputText = ref('')

// 计算属性
const canSend = computed(() => {
  return inputText.value.trim().length > 0 && !props.disabled
})

// 自动调整textarea高度
const adjustTextareaHeight = () => {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    const scrollHeight = textareaRef.value.scrollHeight
    const maxHeight = 120 // 最大高度约5行
    textareaRef.value.style.height = Math.min(scrollHeight, maxHeight) + 'px'
  }
}

// 处理输入
const handleInput = () => {
  adjustTextareaHeight()
}

// 处理键盘事件
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    if (e.shiftKey) {
      // Shift + Enter 换行，不做处理
      return
    } else {
      // Enter 发送消息
      e.preventDefault()
      handleSend()
    }
  }
}

// 发送消息
const handleSend = () => {
  if (!canSend.value) return
  
  const content = inputText.value.trim()
  if (!content) return
  
  emit('send', content)
  inputText.value = ''
  
  // 重置textarea高度
  nextTick(() => {
    adjustTextareaHeight()
    textareaRef.value?.focus()
  })
}

// 监听输入文本变化，调整高度
watch(inputText, () => {
  nextTick(() => {
    adjustTextareaHeight()
  })
})

// 组件挂载后聚焦
nextTick(() => {
  textareaRef.value?.focus()
  adjustTextareaHeight()
})
</script>

<style scoped>
.kimi-input-bar {
  padding: 16px 20px;
  background: transparent;
}

.input-container {
  max-width: 768px;
  margin: 0 auto;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 24px;
  padding: 12px 16px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.input-wrapper:focus-within {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.1);
}

:global(.dark) .input-wrapper {
  background: #374151;
  border-color: #4b5563;
}

:global(.dark) .input-wrapper:focus-within {
  border-color: #1677ff;
}

.message-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
  color: #1f2937;
  resize: none;
  min-height: 20px;
  max-height: 120px;
  font-family: inherit;
  transition: color 0.3s ease;
}

.message-input::placeholder {
  color: #9ca3af;
  transition: color 0.3s ease;
}

:global(.dark) .message-input {
  color: #f9fafb;
}

:global(.dark) .message-input::placeholder {
  color: #6b7280;
}

.message-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-button {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: #f3f4f6;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  outline: none;
}

.send-button:hover:not(:disabled) {
  background: #e5e7eb;
  transform: scale(1.05);
}

.send-button.active {
  background: #1677ff;
  color: white;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
}

.send-button.active:hover {
  background: #1366d9;
  transform: scale(1.05);
}

.send-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

:global(.dark) .send-button {
  background: #4b5563;
  color: #9ca3af;
}

:global(.dark) .send-button:hover:not(:disabled) {
  background: #6b7280;
}

.input-hint {
  margin-top: 8px;
  text-align: center;
}

.input-hint span {
  font-size: 11px;
  color: #9ca3af;
  transition: color 0.3s ease;
}

:global(.dark) .input-hint span {
  color: #6b7280;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .kimi-input-bar {
    padding: 12px 16px;
  }
  
  .input-wrapper {
    padding: 10px 14px;
  }
  
  .send-button {
    width: 32px;
    height: 32px;
  }
  
  .send-button svg {
    width: 16px;
    height: 16px;
  }
  
  .message-input {
    font-size: 13px;
  }
  
  .input-hint {
    margin-top: 6px;
  }
  
  .input-hint span {
    font-size: 10px;
  }
}

/* 动画效果 */
@keyframes sendPulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(0.95);
  }
  100% {
    transform: scale(1);
  }
}

.send-button.active:active {
  animation: sendPulse 0.2s ease;
}
</style>
