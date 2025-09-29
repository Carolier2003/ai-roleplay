<template>
  <div 
    class="voice-waveform" 
    :class="{ 
      'playing': isPlaying, 
      'user': isUser 
    }"
    @click="handleClick"
    :title="isPlaying ? '暂停音频' : '播放音频'"
  >
    <!-- 播放图标 -->
    <div class="play-icon" v-if="!isPlaying">
      <svg viewBox="0 0 24 24" width="16" height="16">
        <path d="M8 5v14l11-7z" fill="currentColor"/>
      </svg>
    </div>
    <!-- 暂停图标 -->
    <div class="pause-icon" v-else>
      <svg viewBox="0 0 24 24" width="16" height="16">
        <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" fill="currentColor"/>
      </svg>
    </div>
    
    <div class="waveform-container">
      <div 
        v-for="i in waveformBars" 
        :key="i" 
        class="wave-bar"
        :style="{ 
          height: `${getBarHeight(i)}%`,
          animationDelay: `${i * 0.1}s`
        }"
      ></div>
    </div>
    
    <div class="duration-text">{{ duration }}"</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

interface Props {
  duration: number
  isPlaying?: boolean
  isUser?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isPlaying: false,
  isUser: false
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

// 处理点击事件
const handleClick = (event: MouseEvent) => {
  emit('click', event)
}

// 波形条数量
const waveformBars = 20

// 生成波形高度
const getBarHeight = (index: number): number => {
  // 基于时长和索引生成不同的波形高度
  const baseHeight = 20 + (props.duration * 5) % 40
  const variation = Math.sin((index + props.duration) * 0.5) * 20
  return Math.max(10, Math.min(80, baseHeight + variation))
}
</script>

<style scoped>
.voice-waveform {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--gray-100);
  border-radius: 18px;
  min-width: 120px;
  transition: all 0.2s ease;
  cursor: pointer;
  user-select: none;
}

.voice-waveform:hover {
  background: var(--gray-200);
}

.voice-waveform:active {
  transform: scale(0.98);
}

.voice-waveform.playing {
  background: var(--primary-color-light);
}

.waveform-container {
  display: flex;
  align-items: center;
  gap: 2px;
  height: 24px;
  flex: 1;
}

.wave-bar {
  width: 3px;
  background: var(--primary-color);
  border-radius: 1.5px;
  transition: height 0.3s ease;
  opacity: 0.7;
}

.voice-waveform.playing .wave-bar {
  animation: wave-pulse 1.5s ease-in-out infinite;
  opacity: 1;
}

.duration-text {
  font-size: 12px;
  color: var(--text-color-secondary);
  font-weight: 500;
  min-width: 20px;
  text-align: right;
}

.voice-waveform.playing .duration-text {
  color: var(--primary-color);
}

@keyframes wave-pulse {
  0%, 100% {
    transform: scaleY(1);
  }
  50% {
    transform: scaleY(1.5);
  }
}

.play-icon,
.pause-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: var(--primary-color);
  border-radius: 50%;
  color: white;
  flex-shrink: 0;
  transition: background-color 0.2s ease;
}

.pause-icon:hover {
  background: var(--primary-color-hover);
}

/* 用户消息样式 */
.voice-waveform.user {
  background: var(--primary-color);
}

.voice-waveform.user:hover {
  background: var(--primary-color-hover);
}

.voice-waveform.user .wave-bar {
  background: white;
}

.voice-waveform.user .duration-text {
  color: white;
}

.voice-waveform.user .play-icon,
.voice-waveform.user .pause-icon {
  background: white;
  color: var(--primary-color);
}
</style>
