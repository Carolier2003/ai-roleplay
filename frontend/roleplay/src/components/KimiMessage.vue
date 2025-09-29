<template>
  <div class="kimi-message" :class="{ 'user-message': message.isUser }">
    <div class="message-bubble" :class="{ streaming: message.streaming }">
      <div class="message-content" v-html="renderedContent"></div>
      
      <!-- 消息操作菜单 -->
      <div v-if="!message.isUser && !message.streaming" class="message-actions">
        <n-dropdown
          :options="messageActions"
          @select="handleActionSelect"
          trigger="click"
          placement="bottom-end"
        >
          <n-button text size="small" class="action-trigger">
            <template #icon>
              <svg viewBox="0 0 24 24" width="14" height="14">
                <path d="M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z" fill="currentColor"/>
              </svg>
            </template>
          </n-button>
        </n-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { NButton, NDropdown } from 'naive-ui'
import { marked } from 'marked'
import type { ChatMessage } from '@/stores/chat'

interface Props {
  message: ChatMessage
  character?: any
}

interface Emits {
  regenerate: [messageId: string]
  copy: [content: string]
  delete: [messageId: string]
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 配置 marked 选项
marked.setOptions({
  breaks: true,
  gfm: true,
  sanitize: false,
  smartypants: true
})

// Markdown预处理函数
const preprocessMarkdown = (content: string): string => {
  return content
    // 将中文破折号转换为标准连字符（用于列表）
    .replace(/^([\s]*)—(\s+)/gm, '$1- $2')
    // 将行中的中文破折号也转换（如果前面有空格的话）
    .replace(/(\n[\s]*)—(\s+)/g, '$1- $2')
    // 处理其他可能的列表符号
    .replace(/^([\s]*)•(\s+)/gm, '$1- $2')
    .replace(/(\n[\s]*)•(\s+)/g, '$1- $2')
}

// 渲染消息内容
const renderedContent = computed(() => {
  let content = props.message.content
  
  // 如果是用户消息，只进行简单的HTML转义
  if (props.message.isUser) {
    return content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;')
      .replace(/\n/g, '<br>')
  }
  
  // 如果是AI消息，使用Markdown渲染
  try {
    // 预处理Markdown内容
    content = preprocessMarkdown(content)
    
    const htmlContent = marked.parse(content)
    
    // 基本的XSS防护
    return htmlContent
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
      .replace(/javascript:/gi, '')
      .replace(/on\w+\s*=/gi, '')
  } catch (error) {
    console.error('[KimiMessage] Markdown解析失败:', error)
    return content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;')
      .replace(/\n/g, '<br>')
  }
})

// 消息操作菜单
const messageActions = [
  {
    label: '重新生成',
    key: 'regenerate',
    icon: () => h('svg', { viewBox: '0 0 24 24', width: 16, height: 16 }, [
      h('path', { d: 'M12,6V9L16,5L12,1V4A8,8 0 0,0 4,12C4,13.57 4.46,15.03 5.24,16.26L6.7,14.8C6.25,13.97 6,13 6,12A6,6 0 0,1 12,6M18.76,7.74L17.3,9.2C17.74,10.04 18,11 18,12A6,6 0 0,1 12,18V15L8,19L12,23V20A8,8 0 0,0 20,12C20,10.43 19.54,8.97 18.76,7.74Z', fill: 'currentColor' })
    ])
  },
  {
    label: '复制',
    key: 'copy',
    icon: () => h('svg', { viewBox: '0 0 24 24', width: 16, height: 16 }, [
      h('path', { d: 'M19,21H8V7H19M19,5H8A2,2 0 0,0 6,7V21A2,2 0 0,0 8,23H19A2,2 0 0,0 21,21V7A2,2 0 0,0 19,5M16,1H4A2,2 0 0,0 2,3V17H4V3H16V1Z', fill: 'currentColor' })
    ])
  },
  {
    label: '删除',
    key: 'delete',
    icon: () => h('svg', { viewBox: '0 0 24 24', width: 16, height: 16 }, [
      h('path', { d: 'M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z', fill: 'currentColor' })
    ])
  }
]

// 处理操作选择
const handleActionSelect = (key: string) => {
  switch (key) {
    case 'regenerate':
      emit('regenerate', props.message.id)
      break
    case 'copy':
      emit('copy', props.message.content)
      break
    case 'delete':
      emit('delete', props.message.id)
      break
  }
}

</script>

<style scoped>
.kimi-message {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  animation: messageSlideIn 0.3s ease-out;
}

.kimi-message.user-message {
  align-items: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
  word-wrap: break-word;
  transition: all 0.3s ease;
}

/* 用户消息样式 */
.user-message .message-bubble {
  background: #1677ff;
  color: white;
  border-bottom-right-radius: 6px;
  box-shadow: 0 1px 3px rgba(22, 119, 255, 0.3);
}

/* AI消息样式 */
.kimi-message:not(.user-message) .message-bubble {
  background: white;
  color: #1f2937;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

:global(.dark) .kimi-message:not(.user-message) .message-bubble {
  background: #374151;
  color: #f9fafb;
  border-color: #4b5563;
}

/* 流式输入状态 */
.message-bubble.streaming {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
}

/* 消息操作 */
.message-actions {
  position: absolute;
  top: -8px;
  right: -8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.kimi-message:hover .message-actions {
  opacity: 1;
}

.action-trigger {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  padding: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  color: #6b7280;
}

.action-trigger:hover {
  color: #1677ff;
  border-color: #1677ff;
}

:global(.dark) .action-trigger {
  background: #4b5563;
  border-color: #6b7280;
  color: #d1d5db;
}


/* Markdown 样式 */
.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4),
.message-content :deep(h5),
.message-content :deep(h6) {
  margin: 12px 0 6px 0;
  font-weight: 600;
  line-height: 1.4;
}

.message-content :deep(h1) { font-size: 1.3em; }
.message-content :deep(h2) { font-size: 1.2em; }
.message-content :deep(h3) { font-size: 1.1em; }

.message-content :deep(p) {
  margin: 6px 0;
  line-height: 1.6;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 6px 0;
  padding-left: 16px;
}

.message-content :deep(li) {
  margin: 2px 0;
  line-height: 1.5;
}

.message-content :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 12px;
  border-left: 3px solid #e5e7eb;
  background: #f9fafb;
  border-radius: 0 4px 4px 0;
  font-style: italic;
}

.user-message .message-content :deep(blockquote) {
  border-left-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.1);
}

:global(.dark) .message-content :deep(blockquote) {
  border-left-color: #4b5563;
  background: #2d2d2d;
}

.message-content :deep(code) {
  background: #f3f4f6;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
}

.user-message .message-content :deep(code) {
  background: rgba(255, 255, 255, 0.2);
}

:global(.dark) .message-content :deep(code) {
  background: #2d2d2d;
}

.message-content :deep(pre) {
  margin: 8px 0;
  padding: 8px;
  background: #1f2937;
  color: #f9fafb;
  border-radius: 6px;
  overflow-x: auto;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.85em;
  line-height: 1.4;
}

.message-content :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.message-content :deep(strong) {
  font-weight: 600;
}

.message-content :deep(em) {
  font-style: italic;
}

.message-content :deep(a) {
  color: #1677ff;
  text-decoration: underline;
}

.user-message .message-content :deep(a) {
  color: rgba(255, 255, 255, 0.9);
}

/* 动画 */
@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message-bubble {
    max-width: 85%;
  }
  
  .message-content {
    font-size: 13px;
  }
}
</style>
