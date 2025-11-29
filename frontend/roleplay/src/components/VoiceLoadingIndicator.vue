<template>
  <div 
    class="flex items-center gap-3 px-4 py-3 rounded-[20px] transition-all duration-300"
    :class="[
      isUser 
        ? 'bg-gradient-to-br from-indigo-500/90 to-violet-600/90 text-white' 
        : 'bg-white/60 backdrop-blur-md border border-white/40 text-gray-800'
    ]"
  >
    <!-- 音符图标 -->
    <div class="flex-shrink-0">
      <svg 
        class="w-6 h-6 animate-pulse" 
        :class="isUser ? 'text-white' : 'text-indigo-600'"
        fill="currentColor" 
        viewBox="0 0 24 24"
      >
        <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
      </svg>
    </div>

    <!-- 动画波形 -->
    <div class="flex items-center gap-1 flex-1">
      <div 
        v-for="i in 5" 
        :key="i"
        class="w-1 rounded-full transition-all duration-300 ease-in-out"
        :class="isUser ? 'bg-white/80' : 'bg-indigo-500/80'"
        :style="{ 
          height: `${getBarHeight(i)}px`,
          animationDelay: `${i * 0.1}s`
        }"
        style="animation: wave-pulse 1.2s ease-in-out infinite"
      ></div>
    </div>

    <!-- 文字提示 -->
    <div 
      class="text-sm font-medium whitespace-nowrap flex-shrink-0"
      :class="isUser ? 'text-white/90' : 'text-gray-600'"
    >
      {{ text }}
    </div>

    <!-- 脉冲动画装饰 -->
    <div class="flex gap-1 flex-shrink-0">
      <div 
        v-for="i in 3" 
        :key="i"
        class="w-1.5 h-1.5 rounded-full animate-ping"
        :class="isUser ? 'bg-white/60' : 'bg-indigo-400/60'"
        :style="{ animationDelay: `${i * 0.15}s` }"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  text?: string
  isUser?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  text: '正在生成语音...',
  isUser: false
})

// 生成波形高度（使用正弦波模式）
const getBarHeight = (index: number): number => {
  const base = 16
  const amplitude = 12
  const phase = (index - 1) * 0.6
  return base + Math.sin(phase) * amplitude
}
</script>

<style scoped>
@keyframes wave-pulse {
  0%, 100% {
    transform: scaleY(0.5);
    opacity: 0.6;
  }
  50% {
    transform: scaleY(1.5);
    opacity: 1;
  }
}
</style>
