<template>
  <n-collapse-transition :show="true">
    <div class="message" :class="{ self: message.isUser }" :data-is-user="message.isUser" :data-message-id="message.id">
      <!-- 调试信息 -->
      <div v-if="showDebug" class="debug-info">
        ID: {{ message.id }}, 
        isUser: {{ message.isUser }}, 
        CSS类: {{ message.isUser ? 'message self' : 'message' }}, 
        预期位置: {{ message.isUser ? '右侧' : '左侧' }},
        内容: {{ message.content.substring(0, 30) }}...
      </div>
      <div class="message-content">
        <n-avatar
          v-if="!message.isUser"
          :size="32"
          :src="characterAvatar"
          :fallback-src="'/src/assets/characters/default.svg'"
          class="message-avatar"
        />
        
        <div class="bubble-container">
          <div class="bubble" :class="{ streaming: message.streaming }">
            <div v-if="!message.isUser && characterName" class="character-name">
              {{ characterName }}
            </div>
            
            <!-- 语音消息显示波形（如果有语音） -->
            <VoiceWaveform 
              v-if="hasAudioUrl"
              :duration="voiceDuration"
              :is-playing="isPlaying"
              :is-user="message.isUser"
              @click="playVoiceMessage"
            />
            
            <!-- 文字消息内容（始终显示，除非是纯语音消息且没有文字内容） -->
            <div 
              v-if="shouldShowTextContent" 
              class="content" 
              :class="{ 'with-voice': hasAudioUrl }"
              :key="`content-${message.id}-${message.streaming}-${message.content.length}`"
            >
              <div v-html="safeContent"></div>
              <span v-if="message.streaming" class="typing-cursor-inline">▍</span>
            </div>
            
          </div>
        </div>
        
        <!-- 用户头像已移除，只保留角色头像 -->
      </div>
    </div>
  </n-collapse-transition>
</template>

<script setup lang="ts">
import { computed, ref, watch, watchEffect, onUnmounted, nextTick } from 'vue'
import { NAvatar, NButton, NCollapseTransition } from 'naive-ui'
import { useChatStore, type ChatMessage } from '@/stores/chat'
import { marked } from 'marked'
import VoiceWaveform from './VoiceWaveform.vue'

interface Props {
  message: ChatMessage
}

const props = defineProps<Props>()
const chatStore = useChatStore()
const isPlaying = ref(false)

// 语音消息相关计算属性（在文件末尾重新定义）

const voiceDuration = computed(() => {
  if (props.message.voiceDuration) {
    return props.message.voiceDuration
  }
  
  // 从内容中提取时长（格式：🎵 5"）
  const match = props.message.content.match(/🎵\s*(\d+)"/)
  return match ? parseInt(match[1]) : 3
})

// ✅优化 调试开关热更新 - 使用 watchEffect 监听 localStorage 变化
const showDebug = ref(false)
watchEffect(() => {
  showDebug.value = localStorage.getItem('CHAT_DEBUG') === 'true'
})

// 🔍 监控 message.isUser 的变化
watch(() => props.message.isUser, (newValue, oldValue) => {
  if (oldValue !== undefined && newValue !== oldValue) {
    console.warn('🚨 [ChatMessage] 消息的 isUser 字段发生了变化！', {
      messageId: props.message.id,
      from: oldValue,
      to: newValue,
      content: props.message.content.substring(0, 30) + '...'
    })
  }
  
  console.log('[ChatMessage] 消息渲染:', {
    messageId: props.message.id,
    isUser: props.message.isUser,
    content: props.message.content.substring(0, 30) + '...',
    expectedPosition: props.message.isUser ? '右侧' : '左侧'
  })
}, { immediate: true })

// ✅优化 角色名/头像缓存 - 使用 Map 缓存 characters
const charactersMap = computed(() => {
  const map = new Map()
  chatStore.characters.forEach(char => {
    map.set(char.id, char)
  })
  return map
})

const characterName = computed(() => {
  const character = charactersMap.value.get(props.message.characterId)
  return character?.name || ''
})

const characterAvatar = computed(() => {
  const character = charactersMap.value.get(props.message.characterId)
  return character?.avatar || '/src/assets/characters/default.webp'
})

// 配置 marked 选项
marked.setOptions({
  breaks: true,        // 支持换行符转换为 <br>
  gfm: true,          // 启用 GitHub Flavored Markdown
  sanitize: false,    // 不自动清理HTML（我们会手动处理）
  smartypants: true   // 启用智能标点符号
})

// Markdown预处理函数
const preprocessMarkdown = (content: string): string => {
  let processed = content
    // 将中文破折号转换为标准连字符（用于列表）
    .replace(/^([\s]*)—(\s+)/gm, '$1- $2')
    // 将行中的中文破折号也转换（如果前面有空格的话）
    .replace(/(\n[\s]*)—(\s+)/g, '$1- $2')
    // 处理其他可能的列表符号
    .replace(/^([\s]*)•(\s+)/gm, '$1- $2')
    .replace(/(\n[\s]*)•(\s+)/g, '$1- $2')
  
  // ⚠️ 核心修复1: 处理列表项之间缺少换行的问题
  
  // 策略A: 在冒号后面紧跟的"- 项目"前添加换行
  // 例如: "材料：- 泰拉刃" -> "材料：\n- 泰拉刃"
  processed = processed.replace(/([:：])\s*(-\s+)/g, '$1\n$2')
  
  // 策略B: 处理连续的列表项 "项目1- 项目2"（有空格）
  // 例如: "泰拉刃- 彩虹猫之刃" -> "泰拉刃\n- 彩虹猫之刃"
  processed = processed.replace(/([^-\n])-(\s+[\u4e00-\u9fa5A-Za-z])/g, '$1\n-$2')
  
  // 策略C: 处理没有空格的连续列表项 "项目1-项目2"（中文/星号后紧跟破折号）
  // 例如: "无头骑士剑-种子弯刀" -> "无头骑士剑\n- 种子弯刀"
  processed = processed.replace(/([\u4e00-\u9fa5*])-(?=[\u4e00-\u9fa5A-Za-z])/g, '$1\n- ')
  
  // ⚠️ 核心修复2: 在明显的语义断点处插入双换行
  // 策略1: 在句号/感叹号后面紧跟中文或大写字母时,插入双换行(更保守的段落分隔)
  // 排除: a) 感叹号后面紧跟另一个标点(如"！它"这种情况视为同一段落)
  //       b) 中间有括号、引号等(如"。（"不分段）
  processed = processed.replace(/([。！])(?=[\u4e00-\u9fa5A-Z])/g, '$1\n\n')
  
  // 策略2: 将已有的单换行转为双换行(如果AI确实输出了换行的话)
  processed = processed.replace(/(\S)\n(?!\n)(?![\->*#])/g, '$1\n\n')
  
  return processed
}

// Markdown渲染逻辑,直接根据streaming状态切换
const safeContent = computed(() => {
  // 直接依赖props.message的streaming和content,确保响应式更新
  const isStreaming = props.message.streaming
  let content = props.message.content
  
  console.log(`[ChatMessage] safeContent计算 - ID: ${props.message.id}, 流式: ${isStreaming}, 内容长度: ${content.length}`)
  
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
  
  // AI消息: 根据streaming状态选择渲染方式
  try {
    // 预处理Markdown内容
    content = preprocessMarkdown(content)
    
    let htmlContent: string
    
    if (isStreaming) {
      // 流式输出时: 使用简化渲染,减少计算开销
      htmlContent = content
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
        .replace(/^### (.*$)/gm, '<h3>$1</h3>')
        .replace(/^## (.*$)/gm, '<h2>$1</h2>')
        .replace(/^# (.*$)/gm, '<h1>$1</h1>')
    } else {
      // 非流式时: 使用完整的Markdown渲染
      console.log(`[ChatMessage] 执行marked.parse()完整渲染 - ID: ${props.message.id}`)
      htmlContent = marked.parse(content) as string
    }
    
    // 基本的XSS防护
    const result = htmlContent
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
      .replace(/javascript:/gi, '')
      .replace(/on\w+\s*=/gi, '')
    
    return result
  } catch (error) {
    console.error('[ChatMessage] Markdown解析失败:', error)
    // 如果Markdown解析失败，回退到简单的HTML转义
    return content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;')
      .replace(/\n/g, '<br>')
  }
})

// ✅优化 音频互斥播放 - currentPlayingId 已在 store 中定义，无需重新创建

// 音频实例引用，用于控制播放和暂停
const currentAudio = ref<HTMLAudioElement | null>(null)

const playAudio = async () => {
  if (!props.message.audioUrl) return
  
  // 如果正在播放，则暂停
  if (isPlaying.value && currentAudio.value) {
    console.log('[ChatMessage] [playAudio] 暂停音频播放')
    currentAudio.value.pause()
    isPlaying.value = false
    if (chatStore.currentPlayingId && chatStore.currentPlayingId.value === props.message.id) {
      chatStore.currentPlayingId.value = null
    }
    return
  }
  
  try {
    // ✅优化 音频互斥播放 - 停止其他正在播放的音频
    if (chatStore.currentPlayingId && chatStore.currentPlayingId.value && chatStore.currentPlayingId.value !== props.message.id) {
      // 通知其他组件停止播放
      const event = new CustomEvent('stopAudio', { detail: { excludeId: props.message.id } })
      window.dispatchEvent(event)
    }
    
    console.log('[ChatMessage] [playAudio] 开始播放音频')
    isPlaying.value = true
    if (chatStore.currentPlayingId) {
      chatStore.currentPlayingId.value = props.message.id
    }
    
    const audio = new Audio(props.message.audioUrl)
    currentAudio.value = audio
    
    audio.onended = () => {
      console.log('[ChatMessage] [playAudio] 音频播放结束')
      isPlaying.value = false
      currentAudio.value = null
      if (chatStore.currentPlayingId && chatStore.currentPlayingId.value === props.message.id) {
        chatStore.currentPlayingId.value = null
      }
    }
    
    audio.onerror = () => {
      console.error('[ChatMessage] [playAudio] 音频播放失败')
      isPlaying.value = false
      currentAudio.value = null
      if (chatStore.currentPlayingId && chatStore.currentPlayingId.value === props.message.id) {
        chatStore.currentPlayingId.value = null
      }
    }
    
    // 添加暂停事件监听
    audio.onpause = () => {
      console.log('[ChatMessage] [playAudio] 音频已暂停')
      isPlaying.value = false
      if (chatStore.currentPlayingId && chatStore.currentPlayingId.value === props.message.id) {
        chatStore.currentPlayingId.value = null
      }
    }
    
    await audio.play()
  } catch (error) {
    console.error('[ChatMessage] [playAudio] 音频播放失败:', error)
    isPlaying.value = false
    currentAudio.value = null
    if (chatStore.currentPlayingId && chatStore.currentPlayingId.value === props.message.id) {
      chatStore.currentPlayingId.value = null
    }
  }
}

// 播放语音消息
const playVoiceMessage = async (event?: MouseEvent) => {
  // 阻止事件冒泡（如果有事件对象）
  if (event) {
    event.stopPropagation()
  }
  
  // 如果有音频URL，播放或暂停音频
  if (props.message.audioUrl) {
    await playAudio()
    return
  }
  
  // 如果没有音频URL，可能需要重新生成TTS
  console.log('[ChatMessage] [playVoiceMessage] 语音消息没有音频URL，需要重新生成TTS')
  // 这里可以添加重新生成TTS的逻辑
}

// ✅优化 音频互斥播放 - 监听全局停止音频事件
const handleStopAudio = (event: CustomEvent) => {
  if (event.detail.excludeId !== props.message.id && isPlaying.value) {
    console.log('[ChatMessage] [handleStopAudio] 收到全局停止音频事件，停止当前音频')
    // 如果有正在播放的音频，暂停它
    if (currentAudio.value) {
      currentAudio.value.pause()
      currentAudio.value = null
    }
    isPlaying.value = false
  }
}

// 添加事件监听器
window.addEventListener('stopAudio', handleStopAudio as EventListener)

// ✅优化 音频互斥播放 - 组件卸载时清理
onUnmounted(() => {
  console.log('[ChatMessage] [onUnmounted] 组件卸载，清理音频资源')
  window.removeEventListener('stopAudio', handleStopAudio as EventListener)
  
  // 停止并清理音频实例
  if (currentAudio.value) {
    currentAudio.value.pause()
    currentAudio.value = null
  }
  
  if (isPlaying.value && chatStore.currentPlayingId && chatStore.currentPlayingId.value === props.message.id) {
    isPlaying.value = false
    chatStore.currentPlayingId.value = null
  }
})

// 检查是否有音频URL
const hasAudioUrl = computed(() => {
  return !!(props.message.audioUrl && !props.message.isUser)
})

// 检查是否为语音消息（增强判断逻辑）
const isVoiceMessage = computed(() => {
  return props.message.isVoiceMessage || 
         props.message.content.includes('🎵') ||
         hasAudioUrl.value
})

// 检查是否应该显示文字内容
const shouldShowTextContent = computed(() => {
  // 如果是用户消息，始终显示文字
  if (props.message.isUser) {
    return true
  }
  
  // 如果是AI消息，检查是否有实际的文字内容
  const content = props.message.content.trim()
  
  // 如果内容为空，不显示
  if (!content) {
    return false
  }
  
  // 如果只包含语音标识符（如 🎵 3"），不显示文字
  const voiceOnlyPattern = /^🎵\s*\d+"?\s*$/
  if (voiceOnlyPattern.test(content)) {
    return false
  }
  
  // 其他情况都显示文字内容
  return true
})

</script>

<style scoped>
.message {
  margin-bottom: var(--spacing-lg);
  animation: messageIn 0.3s ease-out;
}

/* 调试信息样式 */
.debug-info {
  font-size: 10px;
  background: #1f2937;
  color: #f9fafb;
  padding: 4px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
  font-family: monospace;
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-content {
  display: flex;
  align-items: flex-end;
  gap: var(--spacing-sm);
  max-width: 80%;
}

.message.self .message-content {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-avatar {
  border-radius: var(--radius-sm) !important;
  flex-shrink: 0;
}

.user-avatar {
  background: var(--primary-500) !important;
}

.bubble-container {
  position: relative;
  /* ✅优化 移动端样式微调 - 使用 min() 函数 */
  max-width: min(80vw, 480px);
}

.bubble {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--spacing-md) var(--spacing-lg);
  box-shadow: var(--shadow-base);
  border: 1px solid var(--gray-200);
  position: relative;
}

.message.self .bubble {
  background: linear-gradient(135deg, var(--primary-500), var(--primary-600));
  color: white;
  border: none;
}

.bubble.streaming {
  border-color: var(--primary-300);
  box-shadow: 0 0 0 2px var(--primary-100);
}

.character-name {
  font-size: var(--font-sm);
  font-weight: 600;
  color: var(--primary-600);
  margin-bottom: var(--spacing-xs);
}

.content {
  font-size: var(--font-base);
  line-height: 1.6;
  color: var(--gray-800);
  word-wrap: break-word;
}

.content.with-voice {
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--gray-200);
}

.message.self .content {
  color: white;
}

/* Markdown 样式 */
.content :deep(h1),
.content :deep(h2),
.content :deep(h3),
.content :deep(h4),
.content :deep(h5),
.content :deep(h6) {
  margin: 16px 0 8px 0;
  font-weight: 600;
  line-height: 1.4;
}

.content :deep(h1) { font-size: 1.5em; }
.content :deep(h2) { font-size: 1.3em; }
.content :deep(h3) { font-size: 1.1em; }
.content :deep(h4) { font-size: 1em; }

.content :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
}

.content :deep(ul),
.content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.content :deep(li) {
  margin: 4px 0;
  line-height: 1.5;
}

.content :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 16px;
  border-left: 4px solid var(--primary-300);
  background: var(--gray-50);
  border-radius: 0 4px 4px 0;
  font-style: italic;
}

.message.self .content :deep(blockquote) {
  border-left-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.1);
}

.content :deep(code) {
  background: var(--gray-100);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
}

.message.self .content :deep(code) {
  background: rgba(255, 255, 255, 0.2);
}

.content :deep(pre) {
  margin: 12px 0;
  padding: 12px;
  background: var(--gray-900);
  color: var(--gray-100);
  border-radius: 6px;
  overflow-x: auto;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
  line-height: 1.4;
}

.content :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.content :deep(strong) {
  font-weight: 600;
}

.content :deep(em) {
  font-style: italic;
}

.content :deep(a) {
  color: var(--primary-600);
  text-decoration: underline;
}

.message.self .content :deep(a) {
  color: rgba(255, 255, 255, 0.9);
}

.content :deep(table) {
  margin: 12px 0;
  border-collapse: collapse;
  width: 100%;
  font-size: 0.9em;
}

.content :deep(th),
.content :deep(td) {
  border: 1px solid var(--gray-300);
  padding: 8px 12px;
  text-align: left;
}

.content :deep(th) {
  background: var(--gray-100);
  font-weight: 600;
}

.message.self .content :deep(th) {
  background: rgba(255, 255, 255, 0.2);
}

.message.self .content :deep(th),
.message.self .content :deep(td) {
  border-color: rgba(255, 255, 255, 0.3);
}

.content :deep(hr) {
  margin: 16px 0;
  border: none;
  border-top: 1px solid var(--gray-300);
}

.message.self .content :deep(hr) {
  border-top-color: rgba(255, 255, 255, 0.3);
}


.typing-cursor-inline {
  color: var(--primary-500);
  font-weight: bold;
  animation: blink 1s infinite;
  display: inline;
  vertical-align: baseline;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .message-content {
    max-width: 90%;
  }
  
  .bubble-container {
    /* ✅优化 移动端样式 - 使用 min() 函数已在上面统一处理 */
    max-width: min(90vw, 320px);
  }
  
  .bubble {
    padding: var(--spacing-sm) var(--spacing-md);
  }
}
</style>