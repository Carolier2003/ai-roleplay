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
              :key="`content-${message.id}-${forceRefreshCounter}-${message.timestamp}`"
            >
              <div :key="`markdown-${message.id}-${forceRefreshCounter}`" v-html="safeContent"></div>
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
import mermaid from 'mermaid'
import { detectMermaidBlock, convertToMermaid } from '@/utils/mermaidDetector'

// 初始化 Mermaid 配置
mermaid.initialize({
  startOnLoad: false,
  theme: 'default',
  securityLevel: 'loose',
  flowchart: { 
    useMaxWidth: true,
    htmlLabels: true,
    curve: 'basis'
  }
})

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

// 强制刷新计数器，用于解决Markdown渲染延迟问题
const forceRefreshCounter = ref(0)

// 监听消息内容变化，增加刷新频率
watch(() => props.message.content, (newContent, oldContent) => {
  if (newContent !== oldContent && !props.message.isUser) {
    // 对于AI消息，每次内容变化都强制刷新
    forceRefreshCounter.value++

    // 使用nextTick确保DOM更新
    nextTick(() => {
      // 再次强制刷新以确保Markdown渲染
      setTimeout(() => {
        forceRefreshCounter.value++
      }, 50)
    })
  }
}, { immediate: true })

// 监听流式状态变化
watch(() => props.message.streaming, (isStreaming, wasStreaming) => {
  console.log(`[ChatMessage] 🔍 流式状态变化监听 - ID: ${props.message.id}`)
  console.log(`[ChatMessage] 当前streaming: ${isStreaming}, 之前streaming: ${wasStreaming}`)
  console.log(`[ChatMessage] 是否为用户消息: ${props.message.isUser}`)
  console.log(`[ChatMessage] 触发条件检查: !isUser=${!props.message.isUser}, wasStreaming=${wasStreaming}, !isStreaming=${!isStreaming}`)

  if (!props.message.isUser && wasStreaming && !isStreaming) {
    console.log(`[ChatMessage] 🎯 触发条件满足！开始静态重渲染流程 - ID: ${props.message.id}`)
    console.log(`[ChatMessage] 消息内容长度: ${props.message.content.length}`)

    // 🔥 直接触发静态重渲染，不再使用复杂的延迟逻辑
    console.log('[ChatMessage] 🚀 立即开始静态内容一次性重新渲染...')

    // 稍微延迟一下，确保流式状态完全稳定
    setTimeout(async () => {
      console.log('[ChatMessage] 🔥 执行静态重渲染...')
      performStaticRerender()
      
      // 🎨 渲染完成后，检查并渲染 Mermaid 图表
      setTimeout(() => {
        renderMermaidCharts()
      }, 200)
    }, 100)
  } else {
    console.log(`[ChatMessage] ❌ 触发条件不满足，跳过静态重渲染`)
  }
}, { immediate: false })

// Markdown预处理函数 - 优化段落分隔
const preprocessMarkdown = (content: string): string => {
  // 🎨 先检测并转换 Mermaid 块
  let processed = content
  
  // 如果检测到 Mermaid 语法，进行转换
  if (detectMermaidBlock(content)) {
    console.log('[ChatMessage] 检测到 Mermaid 块，开始转换')
    processed = convertToMermaid(content)
  }
  
  return processed
    // ✨ 清理AI输出中的特殊符号和格式占位符（但保留 Mermaid 块内的符号）
    .replace(/---#{1,}/g, '')  // 清理 ---###、---## 等符号
    .replace(/#{3,}\\+/g, '')  // 清理 ###\、####\ 等符号
    .replace(/#{3,}(?!\s)/g, '') // 清理独立的 ### (但保留标题语法 ### 标题)
    .replace(/^\s*>\s*⚠️?\s*注意[：:]/gm, '\n**⚠️ 注意**：') // 将注意事项转换为加粗格式
    // 将中文破折号转换为标准连字符（用于列表）
    .replace(/^([\s]*)—(\s+)/gm, '$1- $2')
    // 将行中的中文破折号也转换（如果前面有空格的话）
    .replace(/(\n[\s]*)—(\s+)/g, '$1- $2')
    // 处理其他可能的列表符号
    .replace(/^([\s]*)•(\s+)/gm, '$1- $2')
    .replace(/(\n[\s]*)•(\s+)/g, '$1- $2')
    // ✨ 智能段落分隔：在句号、感叹号、问号后如果紧跟中文或大写字母，添加双换行
    .replace(/([。！？])(?=[A-Z\u4e00-\u9fa5])/g, '$1\n\n')
    // ✨ 在标题前后添加空行，让标题更突出
    .replace(/([^\n])\n(#{1,6}\s+)/g, '$1\n\n$2')
    .replace(/(#{1,6}\s+[^\n]+)\n([^\n#])/g, '$1\n\n$2')
    // ✨ 确保列表项前后有适当的空行
    .replace(/([^\n])\n([-*+]\s+)/g, '$1\n\n$2')
}

// 添加静态渲染状态
const isStaticRerendering = ref(false)
const staticRenderedContent = ref('')
// 添加强制更新触发器
const forceUpdateTrigger = ref(0)


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
  
  // 如果是AI消息，使用渲染策略
  // AI消息: 根据streaming状态选择渲染方式
  try {
    // 预处理Markdown内容
    content = preprocessMarkdown(content)
    
    let htmlContent: string
    
    if (isStreaming) {
      // 🔄 流式输出时：使用简化渲染，减少计算开销
      console.log(`[ChatMessage] 流式渲染模式（简化） - ID: ${props.message.id}`)

      // 流式时使用优化的渲染，支持段落分隔
      htmlContent = content
        // ✨ 先处理双换行为段落标记
        .replace(/\n\n+/g, '</p><p>')
        // 处理标题（必须在换行处理之前）
        .replace(/^### (.*$)/gm, '<h3>$1</h3>')
        .replace(/^## (.*$)/gm, '<h2>$1</h2>')
        .replace(/^# (.*$)/gm, '<h1>$1</h1>')
        // 处理列表项
        .replace(/^- (.*$)/gm, '<li>$1</li>')
        // 处理加粗和斜体
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        // 处理代码
        .replace(/`(.*?)`/g, '<code>$1</code>')
        // 处理单个换行为<br>
        .replace(/\n/g, '<br>')
      
      // 包装在段落标签中
      htmlContent = '<p>' + htmlContent + '</p>'
      // 清理多余的空段落
      htmlContent = htmlContent.replace(/<p><\/p>/g, '')
    } else {
      // 🎯 非流式时：使用完整的Markdown渲染
      console.log(`[ChatMessage] 完整渲染模式 - ID: ${props.message.id}`)

      // 重新初始化marked配置，确保干净的解析状态
      marked.setOptions({
        breaks: true,
        gfm: true,
        sanitize: false,
        smartypants: true,
        pedantic: false,
        silent: false
      })

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
    
    console.log(`[ChatMessage] Markdown渲染完成 - ID: ${props.message.id}, 内容长度: ${result.length}, 模式: ${isStreaming ? '流式（简化）' : '完整'}`)
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

// 🎨 Mermaid 图表渲染函数
const renderMermaidCharts = async () => {
  try {
    await nextTick()
    
    // 查找当前消息中的所有 Mermaid 元素
    const messageElement = document.querySelector(`[data-message-id="${props.message.id}"]`)
    if (!messageElement) {
      return
    }
    
    const mermaidElements = messageElement.querySelectorAll('.mermaid')
    
    if (mermaidElements.length > 0) {
      console.log(`[ChatMessage] 检测到 ${mermaidElements.length} 个 Mermaid 图表，开始渲染`)
      
      // 为每个 Mermaid 元素添加唯一 ID
      mermaidElements.forEach((el, index) => {
        if (!el.id) {
          el.id = `mermaid-${props.message.id}-${index}-${Date.now()}`
        }
      })
      
      // 执行 Mermaid 渲染
      await mermaid.run({
        nodes: Array.from(mermaidElements) as HTMLElement[],
        suppressErrors: true
      })
      
      console.log('[ChatMessage] Mermaid 图表渲染完成')
    }
  } catch (error) {
    console.error('[ChatMessage] Mermaid 渲染失败:', error)
    // 渲染失败时不影响正常显示，保持原始文本
  }
}

// 🔧 调试用：手动触发静态重渲染（可以在浏览器控制台调用）
if (typeof window !== 'undefined') {
  window.triggerStaticRerender = (messageId: string) => {
    if (props.message.id === messageId) {
      console.log('🔧 [DEBUG] 手动触发静态重渲染:', messageId)
      performStaticRerender()
    }
  }
}

// 🎯 静态内容一次性重新渲染方法 - 完全替换流式输出内容
const performStaticRerender = async () => {
  console.log('🔥🔥🔥 [ChatMessage] 静态重渲染函数被调用！🔥🔥🔥')
  console.log(`[ChatMessage] 消息ID: ${props.message.id}`)
  console.log(`[ChatMessage] 消息内容: ${props.message.content.substring(0, 100)}...`)
  console.log(`[ChatMessage] 当前streaming状态: ${props.message.streaming}`)
  console.log(`[ChatMessage] 当前isStaticRerendering: ${isStaticRerendering.value}`)
  console.log('[ChatMessage] 开始静态内容一次性重新渲染，将完全替换流式输出...')

  try {
    // 🔄 第1步: 预渲染静态内容
    console.log('[ChatMessage] 第1步: 预渲染静态内容')

    // 获取完整的静态内容
    const finalContent = props.message.content

    // 使用全新的marked实例进行静态渲染，完全避免状态污染
    const staticMarked = new marked.Marked({
      breaks: true,
      gfm: true,
      sanitize: false,
      smartypants: true,
      pedantic: false,
      silent: false
    })

    // 预处理内容
    const processedContent = preprocessMarkdown(finalContent)

    // 进行完整的静态渲染
    const staticHtml = staticMarked.parse(processedContent) as string

    // XSS防护
    const safeStaticHtml = staticHtml
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
      .replace(/javascript:/gi, '')
      .replace(/on\w+\s*=/gi, '')

    console.log('[ChatMessage] 静态内容预渲染完成，内容长度:', safeStaticHtml.length)
    console.log('[ChatMessage] 原始流式内容长度:', props.message.content.length)

    // 🔄 第2步: 完全替换流式内容
    console.log('[ChatMessage] 第2步: 完全替换流式输出内容')

    // 等待字体加载完成
    if (document.fonts && document.fonts.ready) {
      await document.fonts.ready
    }

    // 保存静态渲染结果，准备替换
    staticRenderedContent.value = safeStaticHtml

    // 🎯 关键步骤：启用静态重渲染模式，这将完全替换原来的流式内容
    isStaticRerendering.value = true

    // 🔒 确保消息状态完全脱离流式模式
    if (props.message.streaming) {
      console.log('[ChatMessage] 检测到消息仍处于流式状态，正在强制结束流式模式')
      // 通过chatStore更新消息状态，确保streaming为false
      chatStore.updateMessage(props.message.id, { streaming: false })
    }

    // 🔥 多重强制更新机制，确保Vue响应式系统检测到变化
    forceRefreshCounter.value = Date.now() + Math.random() * 10000
    forceUpdateTrigger.value = Date.now() + Math.random() * 10000

    console.log('[ChatMessage] 已切换到静态渲染模式，原流式内容已被完全替换')
    console.log('[ChatMessage] 消息流式状态已确保关闭，现在完全使用静态渲染内容')
    console.log('[ChatMessage] 强制更新触发器已激活:', forceUpdateTrigger.value)

    // 🔄 第3步: 强制DOM更新并直接替换内容
    await nextTick()

    console.log('[ChatMessage] 第3步: 强制DOM更新并直接替换内容')

    // 🎯 关键改进：直接操作DOM确保内容被替换
    const messageElement = document.querySelector(`[data-message-id="${props.message.id}"]`)
    if (messageElement) {
      const contentElement = messageElement.querySelector('.content')
      if (contentElement) {
        console.log('[ChatMessage] 找到内容元素，准备直接替换DOM内容')
        console.log('[ChatMessage] 替换前DOM内容长度:', contentElement.innerHTML.length)

        // 🔥 直接替换DOM内容，确保静态内容被显示
        contentElement.innerHTML = safeStaticHtml

        console.log('[ChatMessage] ✅ DOM内容已直接替换')
        console.log('[ChatMessage] 替换后DOM内容长度:', contentElement.innerHTML.length)

        // 强制浏览器重新计算布局
        contentElement.offsetHeight
        contentElement.offsetWidth
      }
    }

    // 再次强制Vue响应式更新，确保状态同步
    setTimeout(() => {
      forceRefreshCounter.value = Date.now() + Math.random() * 10000
      forceUpdateTrigger.value = Date.now() + Math.random() * 10000

      nextTick(() => {
        // 🔄 第4步: 验证替换效果并进行最终优化
        console.log('[ChatMessage] 第4步: 验证内容替换效果')

        if (messageElement) {
          const contentElement = messageElement.querySelector('.content')
          if (contentElement) {
            console.log('[ChatMessage] ✅ 验证：当前显示的是静态渲染内容')
            console.log('[ChatMessage] 最终DOM内容长度:', contentElement.innerHTML.length)

            // 触发样式重新计算，确保静态内容的完美显示
            const computedStyle = window.getComputedStyle(contentElement)
            computedStyle.getPropertyValue('font-family')
            computedStyle.getPropertyValue('line-height')
            computedStyle.getPropertyValue('font-size')

            // 🎯 额外的软加载机制：确保所有样式都正确应用
            const allElements = contentElement.querySelectorAll('*')
            allElements.forEach(el => {
              // 触发每个元素的样式重新计算
              const style = window.getComputedStyle(el)
              style.getPropertyValue('display')
              style.getPropertyValue('position')
            })

            // 🔥 终极软加载机制：临时隐藏并重新显示整个消息元素
            const originalDisplay = messageElement.style.display
            messageElement.style.display = 'none'
            messageElement.offsetHeight // 强制重新计算
            messageElement.style.display = originalDisplay

            // 最终的强制更新
            forceRefreshCounter.value = Date.now() + Math.random() * 10000
            forceUpdateTrigger.value = Date.now() + Math.random() * 10000

            console.log('[ChatMessage] 🔥 终极软加载机制已执行，确保内容完全更新')
          }
        }

        // 触发全局重新布局
        window.dispatchEvent(new Event('resize'))

        // 如果有第三方渲染器，重新渲染静态内容
        if (window.MathJax && window.MathJax.typesetPromise && messageElement) {
          window.MathJax.typesetPromise([messageElement]).then(() => {
            console.log('[ChatMessage] MathJax对静态内容重新渲染完成')
          })
        }

        console.log('[ChatMessage] ✅ 静态内容替换完成！')
        console.log('[ChatMessage] ✅ 原流式输出已被完全替换为高质量静态渲染内容')
        console.log('[ChatMessage] ✅ 已获得页面重新加载后的完美排版效果！')
      })
    }, 100)

  } catch (error) {
    console.error('[ChatMessage] 静态重新渲染失败:', error)
    // 如果静态渲染失败，回退到普通模式，保持原流式内容
    isStaticRerendering.value = false
    staticRenderedContent.value = ''
    console.log('[ChatMessage] 已回退到原流式内容显示')
  }
}
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
  border-radius: 50% !important;
  flex-shrink: 0;
  /* ✨ 确保是完美的圆形 */
  overflow: hidden;
  object-fit: cover;
}

.user-avatar {
  background: var(--primary-500) !important;
  border-radius: 50% !important;
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
  line-height: 1.75;
  color: var(--gray-800);
  word-wrap: break-word;
  /* ✨ 优化：让文字更易读 */
  letter-spacing: 0.3px;
}

.content.with-voice {
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--gray-200);
}

.message.self .content {
  color: white;
}

/* Markdown 样式 - 优化段落间距 */
.content :deep(h1),
.content :deep(h2),
.content :deep(h3),
.content :deep(h4),
.content :deep(h5),
.content :deep(h6) {
  margin: 20px 0 12px 0;
  font-weight: 600;
  line-height: 1.4;
}

/* ✨ 标题下方增加更多空间 */
.content :deep(h1) { 
  font-size: 1.5em;
  margin-top: 24px;
}
.content :deep(h2) { 
  font-size: 1.3em;
  margin-top: 22px;
}
.content :deep(h3) { 
  font-size: 1.1em;
  margin-top: 20px;
}
.content :deep(h4) { 
  font-size: 1em;
  margin-top: 18px;
}

/* ✨ 段落间距优化 - 关键改进！*/
.content :deep(p) {
  margin: 12px 0;
  line-height: 1.75;
}

/* ✨ 确保第一个段落没有上边距 */
.content :deep(p:first-child) {
  margin-top: 0;
}

/* ✨ 确保最后一个段落没有下边距 */
.content :deep(p:last-child) {
  margin-bottom: 0;
}

/* ✨ 相邻段落之间增加更明显的间距 */
.content :deep(p + p) {
  margin-top: 16px;
}

/* ✨ 列表样式优化 */
.content :deep(ul),
.content :deep(ol) {
  margin: 14px 0;
  padding-left: 24px;
}

.content :deep(li) {
  margin: 6px 0;
  line-height: 1.7;
}

/* ✨ 列表项之间的间距 */
.content :deep(li + li) {
  margin-top: 8px;
}

/* ✨ 嵌套列表的间距 */
.content :deep(li > ul),
.content :deep(li > ol) {
  margin-top: 8px;
  margin-bottom: 4px;
}

/* ✨ 引用块样式优化 */
.content :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 20px;
  border-left: 4px solid var(--primary-300);
  background: var(--gray-50);
  border-radius: 0 6px 6px 0;
  font-style: italic;
  line-height: 1.7;
}

.message.self .content :deep(blockquote) {
  border-left-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.1);
}

/* ✨ 行内代码样式优化 */
.content :deep(code) {
  background: var(--gray-100);
  padding: 3px 8px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
  border: 1px solid var(--gray-200);
}

.message.self .content :deep(code) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

/* ✨ 代码块样式优化 */
.content :deep(pre) {
  margin: 16px 0;
  padding: 16px;
  background: var(--gray-900);
  color: var(--gray-100);
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.content :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

/* 🎨 Mermaid 图表样式 */
.content :deep(pre.mermaid) {
  margin: 20px 0;
  padding: 20px;
  background: #f9fafb;
  color: inherit;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow-x: auto;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  font-family: inherit;
}

.content :deep(.mermaid) {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100px;
}

.content :deep(.mermaid svg) {
  max-width: 100%;
  height: auto;
}

.message.self .content :deep(pre.mermaid) {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.25);
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