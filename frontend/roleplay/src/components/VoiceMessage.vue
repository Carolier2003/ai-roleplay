<template>
  <div class="voice-message" :class="{ 'is-user': isUser, 'is-playing': isPlaying }">
    <div class="voice-content" @click="togglePlay">
      <!-- 语音图标 -->
      <div class="voice-icon">
        <svg v-if="!isPlaying" viewBox="0 0 24 24" width="16" height="16">
          <path d="M8 5v14l11-7z" fill="currentColor"/>
        </svg>
        <div v-else class="voice-waves">
          <div class="wave" v-for="i in 3" :key="i" :style="{ animationDelay: `${i * 0.1}s` }"></div>
        </div>
      </div>
      
      <!-- 语音时长 -->
      <div class="voice-duration">
        {{ formatDuration(duration) }}
      </div>
      
      <!-- 语音波形动画 -->
      <div class="voice-waveform">
        <div 
          v-for="i in 20" 
          :key="i" 
          class="waveform-bar"
          :class="{ active: isPlaying }"
          :style="{ 
            animationDelay: `${i * 0.05}s`,
            height: `${Math.random() * 60 + 20}%`
          }"
        ></div>
      </div>
    </div>
    
    <!-- 播放进度条 -->
    <div v-if="isPlaying" class="progress-bar">
      <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
    </div>
    
    <!-- 语音状态提示 -->
    <div v-if="showStatus" class="voice-status">
      <span v-if="isLoading">加载中...</span>
      <span v-else-if="hasError">播放失败</span>
      <span v-else-if="isPlaying">正在播放</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'

interface Props {
  audioUrl: string
  duration?: number
  isUser?: boolean
  voice?: string
  showStatus?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  duration: 0,
  isUser: false,
  showStatus: false
})

// 音频播放状态
const audio = ref<HTMLAudioElement | null>(null)
const isPlaying = ref(false)
const isLoading = ref(false)
const hasError = ref(false)
const currentTime = ref(0)
const totalDuration = ref(props.duration || 0)

// 计算播放进度
const progress = computed(() => {
  if (totalDuration.value === 0) return 0
  return (currentTime.value / totalDuration.value) * 100
})

// 格式化时长显示
const formatDuration = (seconds: number) => {
  if (seconds === 0) return "0''"
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  if (mins > 0) {
    return `${mins}'${secs.toString().padStart(2, '0')}""`
  }
  return `${secs}''`
}

// 初始化音频
const initAudio = () => {
  if (audio.value) {
    audio.value.pause()
    audio.value = null
  }
  
  if (!props.audioUrl) return
  
  audio.value = new Audio(props.audioUrl)
  
  // 音频事件监听
  audio.value.addEventListener('loadstart', () => {
    isLoading.value = true
    hasError.value = false
  })
  
  audio.value.addEventListener('canplay', () => {
    isLoading.value = false
    if (audio.value && totalDuration.value === 0) {
      totalDuration.value = audio.value.duration || props.duration || 0
    }
  })
  
  audio.value.addEventListener('timeupdate', () => {
    if (audio.value) {
      currentTime.value = audio.value.currentTime
    }
  })
  
  audio.value.addEventListener('ended', () => {
    isPlaying.value = false
    currentTime.value = 0
  })
  
  audio.value.addEventListener('error', (e) => {
    console.error('音频播放错误:', e)
    isLoading.value = false
    hasError.value = true
    isPlaying.value = false
  })
  
  audio.value.addEventListener('pause', () => {
    isPlaying.value = false
  })
  
  audio.value.addEventListener('play', () => {
    isPlaying.value = true
  })
}

// 切换播放状态
const togglePlay = async () => {
  if (!audio.value) {
    initAudio()
    await new Promise(resolve => setTimeout(resolve, 100)) // 等待音频初始化
  }
  
  if (!audio.value || hasError.value) return
  
  try {
    if (isPlaying.value) {
      audio.value.pause()
    } else {
      // 暂停其他正在播放的语音消息
      document.querySelectorAll('audio').forEach(otherAudio => {
        if (otherAudio !== audio.value && !otherAudio.paused) {
          otherAudio.pause()
        }
      })
      
      await audio.value.play()
    }
  } catch (error) {
    console.error('播放失败:', error)
    hasError.value = true
  }
}

// 监听audioUrl变化
watch(() => props.audioUrl, () => {
  initAudio()
}, { immediate: true })

// 组件卸载时清理
onUnmounted(() => {
  if (audio.value) {
    audio.value.pause()
    audio.value = null
  }
})
</script>

<style scoped>
.voice-message {
  display: inline-flex;
  flex-direction: column;
  min-width: 120px;
  max-width: 200px;
  cursor: pointer;
  user-select: none;
}

.voice-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f0f0f0;
  border-radius: 18px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.voice-message.is-user .voice-content {
  background: linear-gradient(135deg, #1677ff, #1366d9);
  color: white;
}

.voice-message.is-playing .voice-content {
  background: #e6f7ff;
  border: 1px solid #1677ff;
}

.voice-message.is-user.is-playing .voice-content {
  background: linear-gradient(135deg, #4096ff, #1677ff);
}

.voice-content:hover {
  transform: scale(1.02);
}

.voice-content:active {
  transform: scale(0.98);
}

.voice-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.voice-icon svg {
  color: #666;
}

.voice-message.is-user .voice-icon svg {
  color: white;
}

.voice-waves {
  display: flex;
  align-items: center;
  gap: 2px;
}

.wave {
  width: 2px;
  height: 12px;
  background: #1677ff;
  border-radius: 1px;
  animation: waveAnimation 1.2s ease-in-out infinite;
}

.voice-message.is-user .wave {
  background: white;
}

@keyframes waveAnimation {
  0%, 100% {
    transform: scaleY(0.3);
  }
  50% {
    transform: scaleY(1);
  }
}

.voice-duration {
  font-size: 12px;
  color: #666;
  font-weight: 500;
  min-width: 24px;
  text-align: center;
}

.voice-message.is-user .voice-duration {
  color: rgba(255, 255, 255, 0.9);
}

.voice-waveform {
  display: flex;
  align-items: center;
  gap: 1px;
  flex: 1;
  height: 16px;
  margin-left: 4px;
}

.waveform-bar {
  width: 2px;
  background: #d9d9d9;
  border-radius: 1px;
  transition: all 0.3s ease;
}

.voice-message.is-user .waveform-bar {
  background: rgba(255, 255, 255, 0.4);
}

.waveform-bar.active {
  background: #1677ff;
  animation: waveformAnimation 1.5s ease-in-out infinite;
}

.voice-message.is-user .waveform-bar.active {
  background: white;
}

@keyframes waveformAnimation {
  0%, 100% {
    transform: scaleY(0.5);
    opacity: 0.6;
  }
  50% {
    transform: scaleY(1);
    opacity: 1;
  }
}

.progress-bar {
  height: 2px;
  background: #f0f0f0;
  border-radius: 1px;
  margin-top: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #1677ff;
  border-radius: 1px;
  transition: width 0.1s ease;
}

.voice-message.is-user .progress-bar {
  background: rgba(255, 255, 255, 0.3);
}

.voice-message.is-user .progress-fill {
  background: white;
}

.voice-status {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  text-align: center;
}

.voice-message.is-user .voice-status {
  color: rgba(255, 255, 255, 0.7);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .voice-message {
    min-width: 100px;
    max-width: 160px;
  }
  
  .voice-content {
    padding: 6px 10px;
  }
  
  .voice-duration {
    font-size: 11px;
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .voice-content {
    background: #2f2f2f;
    color: #fff;
  }
  
  .voice-icon svg {
    color: #ccc;
  }
  
  .voice-duration {
    color: #ccc;
  }
  
  .waveform-bar {
    background: #555;
  }
  
  .progress-bar {
    background: #444;
  }
}
</style>
