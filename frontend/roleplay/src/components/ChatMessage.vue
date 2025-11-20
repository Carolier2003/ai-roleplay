<template>
  <div class="mb-6 animate-fade-in-up group">
    <div 
      class="flex items-end gap-3 max-w-[85%] md:max-w-[75%]"
      :class="{ 'ml-auto flex-row-reverse': message.isUser }"
    >
      <!-- 角色头像 -->
      <img
        v-if="!message.isUser"
        :src="characterAvatar"
        class="w-8 h-8 rounded-lg object-cover flex-shrink-0 shadow-sm transition-transform duration-300 group-hover:scale-110"
        alt="Avatar"
      />
      
      <div class="relative max-w-full">
        <!-- 角色名字 -->
        <div 
          v-if="!message.isUser && characterName" 
          class="text-xs font-medium text-gray-500 mb-1 ml-1"
        >
          {{ characterName }}
        </div>

          <div 
            class="relative px-4 py-3 rounded-2xl shadow-sm border text-base leading-relaxed break-words overflow-hidden transition-all duration-300"
            :class="[
              message.isUser 
                ? 'bg-gradient-to-r from-violet-600 to-indigo-600 text-white border-transparent rounded-br-none shadow-md shadow-indigo-500/20' 
                : 'bg-white/80 backdrop-blur-sm text-gray-800 border-white/50 rounded-bl-none shadow-sm hover:bg-white/90 hover:shadow-md',
              message.streaming ? 'border-indigo-300 ring-2 ring-indigo-100' : ''
            ]"
          >
          <!-- 语音消息显示波形 -->
          <VoiceWaveform 
            v-if="hasAudioUrl"
            :duration="voiceDuration"
            :is-playing="isPlaying"
            :is-user="message.isUser"
            @click="playVoiceMessage"
            class="mb-2"
          />
          
          <!-- 文字消息内容 -->
          <div 
            v-if="shouldShowTextContent" 
            class="markdown-body"
            :class="{ 'pt-2 border-t border-white/20': hasAudioUrl && message.isUser, 'pt-2 border-t border-gray-100': hasAudioUrl && !message.isUser }"
          >
            <div v-html="safeContent"></div>
            <span v-if="message.streaming" class="inline-block w-2 h-4 bg-indigo-500 ml-1 align-middle animate-pulse"></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, watchEffect, onUnmounted } from 'vue'
import { useChatStore, type ChatMessage } from '@/stores/chat'
import { marked } from 'marked'
import VoiceWaveform from './VoiceWaveform.vue'

interface Props {
  message: ChatMessage
}

const props = defineProps<Props>()
const chatStore = useChatStore()
const isPlaying = ref(false)

// 语音消息相关计算属性
const voiceDuration = computed(() => {
  if (props.message.voiceDuration) {
    return props.message.voiceDuration
  }
  const match = props.message.content.match(/🎵\s*(\d+)"/)
  return match ? parseInt(match[1]) : 3
})

// 角色信息
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

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
})

// Markdown预处理
const preprocessMarkdown = (content: string): string => {
  let processed = content
    .replace(/^([\s]*)—(\s+)/gm, '$1- $2')
    .replace(/(\n[\s]*)—(\s+)/g, '$1- $2')
    .replace(/^([\s]*)•(\s+)/gm, '$1- $2')
    .replace(/(\n[\s]*)•(\s+)/g, '$1- $2')
    .replace(/([:：])\s*(-\s+)/g, '$1\n$2')
    .replace(/([^-\n])-(\s+[\u4e00-\u9fa5A-Za-z])/g, '$1\n-$2')
    .replace(/([\u4e00-\u9fa5*])-(?=[\u4e00-\u9fa5A-Za-z])/g, '$1\n- ')
    .replace(/([。！])(?=[\u4e00-\u9fa5A-Z])/g, '$1\n\n')
    .replace(/(\S)\n(?!\n)(?![\->*#])/g, '$1\n\n')
    .replace(/([^-\n])\s+-\s*([\u4e00-\u9fa5A-Za-z])/g, '$1\n- $2')
    .replace(/([^\n])\s*(#{1,6})(?=[^#])/g, '$1\n\n$2 ')
    .replace(/([^\n])\s*-\s*(?=[**\u4e00-\u9fa5])/g, '$1\n- ')
    .replace(/([^\n])\s*>\s*(?=[📌\u4e00-\u9fa5])/g, '$1\n\n> ')
    .replace(/([：:])\s*-\s*([\u4e00-\u9fa5A-Za-z])/g, '$1\n- $2')
    .replace(/\*\*\s*([^\n]*?)\s*\*\*/g, ' **$1** ')
  return processed
}

// Markdown渲染
const safeContent = computed(() => {
  let content = props.message.content
  
  if (props.message.isUser) {
    return content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;')
      .replace(/\n/g, '<br>')
  }
  
  try {
    content = preprocessMarkdown(content)
    let htmlContent = marked.parse(content) as string
    return htmlContent
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
      .replace(/javascript:/gi, '')
      .replace(/on\w+\s*=/gi, '')
  } catch (error) {
    console.error('[ChatMessage] Markdown解析失败:', error)
    return content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;')
      .replace(/\n/g, '<br>')
  }
})

// 音频播放逻辑
const currentAudio = ref<HTMLAudioElement | null>(null)

const playAudio = async () => {
  if (!props.message.audioUrl) return
  
  if (isPlaying.value && currentAudio.value) {
    currentAudio.value.pause()
    isPlaying.value = false
    if (chatStore.currentPlayingId === props.message.id) {
      chatStore.currentPlayingId = null
    }
    return
  }
  
  try {
    if (chatStore.currentPlayingId && chatStore.currentPlayingId !== props.message.id) {
      const event = new CustomEvent('stopAudio', { detail: { excludeId: props.message.id } })
      window.dispatchEvent(event)
    }
    
    isPlaying.value = true
    chatStore.currentPlayingId = props.message.id
    
    const audio = new Audio(props.message.audioUrl)
    currentAudio.value = audio
    
    audio.onended = () => {
      isPlaying.value = false
      currentAudio.value = null
      if (chatStore.currentPlayingId === props.message.id) {
        chatStore.currentPlayingId = null
      }
    }
    
    audio.onerror = () => {
      isPlaying.value = false
      currentAudio.value = null
      if (chatStore.currentPlayingId === props.message.id) {
        chatStore.currentPlayingId = null
      }
    }
    
    audio.onpause = () => {
      isPlaying.value = false
      if (chatStore.currentPlayingId === props.message.id) {
        chatStore.currentPlayingId = null
      }
    }
    
    await audio.play()
  } catch (error) {
    console.error('[ChatMessage] 音频播放失败:', error)
    isPlaying.value = false
    currentAudio.value = null
    if (chatStore.currentPlayingId === props.message.id) {
      chatStore.currentPlayingId = null
    }
  }
}

const playVoiceMessage = async (event?: MouseEvent) => {
  if (event) event.stopPropagation()
  if (props.message.audioUrl) {
    await playAudio()
  }
}

const handleStopAudio = (event: CustomEvent) => {
  if (event.detail.excludeId !== props.message.id && isPlaying.value) {
    if (currentAudio.value) {
      currentAudio.value.pause()
      currentAudio.value = null
    }
    isPlaying.value = false
  }
}

window.addEventListener('stopAudio', handleStopAudio as EventListener)

onUnmounted(() => {
  window.removeEventListener('stopAudio', handleStopAudio as EventListener)
  if (currentAudio.value) {
    currentAudio.value.pause()
    currentAudio.value = null
  }
  if (isPlaying.value && chatStore.currentPlayingId === props.message.id) {
    isPlaying.value = false
    chatStore.currentPlayingId = null
  }
})

const hasAudioUrl = computed(() => !!(props.message.audioUrl && !props.message.isUser))

const shouldShowTextContent = computed(() => {
  if (props.message.isUser) return true
  const content = props.message.content.trim()
  if (!content) return false
  const voiceOnlyPattern = /^🎵\s*\d+"?\s*$/
  if (voiceOnlyPattern.test(content)) return false
  return true
})
</script>

<style scoped>
.animate-fade-in-up {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Markdown Styles - Tailwind-like custom styles */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  @apply font-bold mt-4 mb-2;
}

.markdown-body :deep(h1) { @apply text-xl; }
.markdown-body :deep(h2) { @apply text-lg; }
.markdown-body :deep(h3) { @apply text-base; }

.markdown-body :deep(p) {
  @apply my-2 leading-relaxed;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  @apply my-2 pl-5 list-disc;
}

.markdown-body :deep(li) {
  @apply my-1;
}

.markdown-body :deep(blockquote) {
  @apply my-3 pl-4 border-l-4 border-indigo-300 bg-gray-50/50 italic py-1 rounded-r;
}

.markdown-body :deep(code) {
  @apply bg-gray-100 px-1.5 py-0.5 rounded text-sm font-mono text-pink-600;
}

.markdown-body :deep(pre) {
  @apply my-3 p-3 bg-gray-900 text-gray-100 rounded-lg overflow-x-auto font-mono text-sm;
}

.markdown-body :deep(pre code) {
  @apply bg-transparent p-0 text-inherit;
}

.markdown-body :deep(a) {
  @apply text-indigo-600 underline hover:text-indigo-800;
}

/* User message specific overrides */
.group:has(.flex-row-reverse) .markdown-body :deep(code) {
  @apply bg-white/20 text-white;
}

.group:has(.flex-row-reverse) .markdown-body :deep(pre) {
  @apply bg-black/30 text-white;
}

.group:has(.flex-row-reverse) .markdown-body :deep(a) {
  @apply text-white underline hover:text-white/80;
}

.group:has(.flex-row-reverse) .markdown-body :deep(blockquote) {
  @apply border-white/50 bg-white/10;
}
</style>