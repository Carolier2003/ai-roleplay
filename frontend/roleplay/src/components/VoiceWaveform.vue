<template>
  <div 
    class="relative overflow-hidden flex items-center gap-3 px-4 py-3 rounded-[20px] transition-all duration-300 cursor-pointer select-none group border"
    :class="[
      isUser 
        ? 'bg-gradient-to-br from-indigo-500/90 to-violet-600/90 border-white/20 text-white shadow-lg shadow-indigo-500/20' 
        : 'bg-white/60 backdrop-blur-md border-white/40 text-gray-800 shadow-sm hover:shadow-md hover:bg-white/80',
      isPlaying && !isUser ? 'ring-2 ring-indigo-200 bg-white/80' : ''
    ]"
    :style="{ width: `${playerWidth}px` }"
    @click="handleClick"
    :title="isPlaying ? '暂停音频' : '播放音频'"
  >
    <!-- 播放/暂停按钮容器 -->
    <div 
      class="relative flex items-center justify-center w-8 h-8 rounded-full flex-shrink-0 transition-all duration-300 shadow-sm"
      :class="[
        isUser 
          ? 'bg-white/20 text-white hover:bg-white/30 hover:scale-105' 
          : 'bg-indigo-50 text-indigo-600 group-hover:bg-indigo-100 group-hover:scale-105 group-hover:text-indigo-700'
      ]"
    >
      <!-- 播放图标 -->
      <svg 
        v-if="!isPlaying" 
        viewBox="0 0 24 24" 
        width="16" 
        height="16" 
        class="ml-0.5 transition-transform duration-300 group-hover:scale-110"
      >
        <path d="M8 5v14l11-7z" fill="currentColor"/>
      </svg>
      
      <!-- 暂停图标 -->
      <svg 
        v-else 
        viewBox="0 0 24 24" 
        width="16" 
        height="16"
        class="transition-transform duration-300 group-hover:scale-110"
      >
        <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" fill="currentColor"/>
      </svg>

      <!-- 播放时的光晕效果 -->
      <div 
        v-if="isPlaying"
        class="absolute inset-0 rounded-full animate-ping opacity-20"
        :class="isUser ? 'bg-white' : 'bg-indigo-500'"
      ></div>
    </div>
    
    <!-- 波形区域 -->
    <div class="flex items-center gap-[3px] h-8 flex-1 mx-1 justify-center overflow-hidden">
      <div 
        v-for="i in barCount" 
        :key="i" 
        class="w-[3px] rounded-full transition-all duration-300 ease-out flex-shrink-0"
        :class="[
          isUser ? 'bg-white/80' : 'bg-indigo-500/80',
          isPlaying ? 'animate-wave-pulse' : 'opacity-60'
        ]"
        :style="{ 
          height: isPlaying ? undefined : `${getStaticHeight(i)}%`,
          animationDelay: `-${Math.random() * 1.5}s`,
          animationDuration: `${0.8 + Math.random() * 0.8}s`
        }"
      ></div>
    </div>
    
    <!-- 时长显示 -->
    <div 
      class="text-xs font-semibold min-w-[24px] text-right tracking-wide flex-shrink-0"
      :class="isUser ? 'text-white/90' : 'text-gray-500 group-hover:text-gray-700'"
    >
      {{ displayDuration }}"
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

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

// 计算播放器宽度
const playerWidth = computed(() => {
  // 基础宽度 160px，每秒增加 5px，最大 320px
  const width = 160 + (props.duration * 5)
  return Math.min(320, Math.max(160, width))
})

// 计算波形条数量
const barCount = computed(() => {
  // 除去按钮和时长显示的宽度，剩余空间用于波形
  // 按钮(32+12) + 时长(24+12) + padding(32) ≈ 112px
  const availableWidth = playerWidth.value - 112
  // 每个条宽度3px + 间距3px = 6px
  return Math.floor(availableWidth / 6)
})

// 格式化时长显示
const displayDuration = computed(() => {
  if (props.duration < 60) {
    return `${props.duration}"`
  }
  const minutes = Math.floor(props.duration / 60)
  const seconds = props.duration % 60
  return `${minutes}'${seconds}"`
})

// 生成静态波形高度（非播放状态）
const getStaticHeight = (index: number): number => {
  // 使用正弦波组合生成自然的静态波形
  const x = index / barCount.value * Math.PI * 2
  const wave1 = Math.sin(x) * 30
  const wave2 = Math.sin(x * 2 + 1) * 15
  const wave3 = Math.sin(x * 4 + 2) * 10
  
  // 归一化并映射到 20-90% 范围
  const height = 45 + (wave1 + wave2 + wave3)
  return Math.max(20, Math.min(90, height))
}
</script>

<style scoped>
@keyframes wave-pulse {
  0%, 100% {
    height: 30%;
    opacity: 0.6;
  }
  50% {
    height: 90%;
    opacity: 1;
  }
}

.animate-wave-pulse {
  animation-name: wave-pulse;
  animation-iteration-count: infinite;
  animation-timing-function: ease-in-out;
}
</style>
