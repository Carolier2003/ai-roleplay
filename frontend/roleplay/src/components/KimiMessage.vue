<template>
  <div class="flex flex-col mb-4 animate-slide-in" :class="{ 'items-end': message.isUser }">
    <div 
      class="relative max-w-[85%] md:max-w-[70%] px-4 py-3 rounded-[18px] break-words transition-all duration-300 group"
      :class="[
        message.isUser 
          ? 'bg-blue-600 text-white rounded-br-md shadow-md shadow-blue-600/30' 
          : 'bg-white dark:bg-gray-700 text-gray-800 dark:text-gray-100 border border-gray-200 dark:border-gray-600 rounded-bl-md shadow-sm',
        message.streaming ? 'border-blue-500 shadow-[0_0_0_2px_rgba(59,130,246,0.1)]' : ''
      ]"
    >
      <div class="text-sm leading-relaxed message-content" v-html="renderedContent"></div>
      
      <!-- 消息操作菜单 -->
      <div v-if="!message.isUser && !message.streaming" class="absolute -top-2 -right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
        <div class="relative">
          <button 
            @click="toggleMenu" 
            class="w-6 h-6 bg-white dark:bg-gray-600 border border-gray-200 dark:border-gray-500 rounded-full flex items-center justify-center shadow-sm text-gray-500 dark:text-gray-300 hover:text-blue-600 hover:border-blue-600 dark:hover:text-blue-400 dark:hover:border-blue-400 transition-colors"
          >
            <svg viewBox="0 0 24 24" width="14" height="14">
              <path d="M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z" fill="currentColor"/>
            </svg>
          </button>
          
          <!-- Dropdown Menu -->
          <div v-if="showMenu" class="absolute right-0 top-full mt-1 w-32 bg-white dark:bg-gray-800 rounded-md shadow-lg border border-gray-100 dark:border-gray-700 py-1 z-10 overflow-hidden">
            <button 
              v-for="action in messageActions" 
              :key="action.key"
              @click="handleActionSelect(action.key)"
              class="w-full px-4 py-2 text-left text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
            >
              <component :is="action.icon" class="w-4 h-4" />
              {{ action.label }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 点击外部关闭菜单遮罩 -->
    <div v-if="showMenu" class="fixed inset-0 z-0" @click="showMenu = false"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
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

const showMenu = ref(false)

const toggleMenu = () => {
  showMenu.value = !showMenu.value
}

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
    icon: () => h('svg', { viewBox: '0 0 24 24', width: 16, height: 16, fill: 'currentColor' }, [
      h('path', { d: 'M12,6V9L16,5L12,1V4A8,8 0 0,0 4,12C4,13.57 4.46,15.03 5.24,16.26L6.7,14.8C6.25,13.97 6,13 6,12A6,6 0 0,1 12,6M18.76,7.74L17.3,9.2C17.74,10.04 18,11 18,12A6,6 0 0,1 12,18V15L8,19L12,23V20A8,8 0 0,0 20,12C20,10.43 19.54,8.97 18.76,7.74Z' })
    ])
  },
  {
    label: '复制',
    key: 'copy',
    icon: () => h('svg', { viewBox: '0 0 24 24', width: 16, height: 16, fill: 'currentColor' }, [
      h('path', { d: 'M19,21H8V7H19M19,5H8A2,2 0 0,0 6,7V21A2,2 0 0,0 8,23H19A2,2 0 0,0 21,21V7A2,2 0 0,0 19,5M16,1H4A2,2 0 0,0 2,3V17H4V3H16V1Z' })
    ])
  },
  {
    label: '删除',
    key: 'delete',
    icon: () => h('svg', { viewBox: '0 0 24 24', width: 16, height: 16, fill: 'currentColor' }, [
      h('path', { d: 'M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z' })
    ])
  }
]

// 处理操作选择
const handleActionSelect = (key: string) => {
  showMenu.value = false
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
@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-slide-in {
  animation: slideIn 0.3s ease-out;
}

/* Markdown Styles */
.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4),
.message-content :deep(h5),
.message-content :deep(h6) {
  margin: 0.75em 0 0.375em 0;
  font-weight: 600;
  line-height: 1.4;
}

.message-content :deep(h1) { font-size: 1.3em; }
.message-content :deep(h2) { font-size: 1.2em; }
.message-content :deep(h3) { font-size: 1.1em; }

.message-content :deep(p) {
  margin: 0.375em 0;
  line-height: 1.6;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 0.375em 0;
  padding-left: 1em;
}

.message-content :deep(li) {
  margin: 0.125em 0;
  line-height: 1.5;
}

.message-content :deep(blockquote) {
  margin: 0.5em 0;
  padding: 0.375em 0.75em;
  border-left: 3px solid #e5e7eb;
  background: #f9fafb;
  border-radius: 0 4px 4px 0;
  font-style: italic;
}

:global(.dark) .message-content :deep(blockquote) {
  border-left-color: #4b5563;
  background: #2d2d2d;
}

.message-content :deep(code) {
  background: #f3f4f6;
  padding: 0.125em 0.25em;
  border-radius: 3px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
}

:global(.dark) .message-content :deep(code) {
  background: #2d2d2d;
}

.message-content :deep(pre) {
  margin: 0.5em 0;
  padding: 0.5em;
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

.message-content :deep(a) {
  color: #2563eb;
  text-decoration: underline;
}

:global(.dark) .message-content :deep(a) {
  color: #60a5fa;
}
</style>
