<template>
  <div 
    class="flex items-center gap-2 px-3 py-2 rounded-[18px] min-w-[120px] transition-all duration-200 cursor-pointer select-none group"
    :class="[
      isUser 
        ? 'bg-indigo-600 hover:bg-indigo-700 text-white' 
        : 'bg-gray-100 hover:bg-gray-200 text-gray-800',
      isPlaying && !isUser ? 'bg-indigo-100 hover:bg-indigo-200' : ''
    ]"
    @click="handleClick"
    :title="isPlaying ? '暂停音频' : '播放音频'"
  >
    <!-- 播放图标 -->
    <div 
      class="flex items-center justify-center w-5 h-5 rounded-full flex-shrink-0 transition-colors duration-200"
      :class="isUser ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white group-hover:bg-indigo-700'"
      v-if="!isPlaying"
    >
      <svg viewBox="0 0 24 24" width="14" height="14">
        <path d="M8 5v14l11-7z" fill="currentColor"/>
      </svg>
    </div>
    <!-- 暂停图标 -->
    <div 
      class="flex items-center justify-center w-5 h-5 rounded-full flex-shrink-0 transition-colors duration-200"
      :class="isUser ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white group-hover:bg-indigo-700'"
      v-else
    >
      <svg viewBox="0 0 24 24" width="14" height="14">
        <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" fill="currentColor"/>
      </svg>
    </div>
    
    <div class="flex items-center gap-[2px] h-6 flex-1">
      <div 
        v-for="i in waveformBars" 
        :key="i" 
        class="w-[3px] rounded-[1.5px] transition-[height] duration-300 ease-out opacity-70"
        :class="[
          isUser ? 'bg-white' : 'bg-indigo-600',
          isPlaying ? 'animate-wave-pulse opacity-100' : ''
        ]"
        :style="{ 
          height: `${getBarHeight(i)}%`,
          animationDelay: `${i * 0.1}s`
        }"
      ></div>
    </div>
    
    <div 
      class="text-xs font-medium min-w-[20px] text-right"
      :class="isUser ? 'text-white' : (isPlaying ? 'text-indigo-600' : 'text-gray-500')"
    >
      {{ duration }}"
    </div>
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
@keyframes wave-pulse {
  0%, 100% {
    transform: scaleY(1);
  }
  50% {
    transform: scaleY(1.5);
  }
}

.animate-wave-pulse {
  animation: wave-pulse 1.5s ease-in-out infinite;
}
</style>
