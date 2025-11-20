<template>
  <div class="w-full h-full flex items-center justify-center" ref="waveContainer">
    <div class="flex items-center justify-center gap-[2px] h-full">
      <div
        v-for="i in 20"
        :key="i"
        class="w-[3px] min-h-[4px] rounded-[2px] transition-[height] duration-100 ease-linear bg-gradient-to-t from-indigo-600 to-indigo-400"
        :class="{ 'animate-wave-idle': !recording }"
        :style="{ 
          height: recording ? `${waveHeights[i - 1]}%` : '20%',
          animationDelay: `${i * 50}ms`,
          animationDirection: i % 2 === 0 ? 'normal' : 'reverse'
        }"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  recording: boolean
}

const props = defineProps<Props>()

const waveContainer = ref<HTMLElement>()
const waveHeights = ref<number[]>(Array(20).fill(20))
const animationFrame = ref<number>()
const audioContext = ref<AudioContext>()
const analyser = ref<AnalyserNode>()
const dataArray = ref<Uint8Array>()

// 监听录音状态变化
watch(() => props.recording, (isRecording) => {
  if (isRecording) {
    startWaveAnimation()
  } else {
    stopWaveAnimation()
  }
})

const startWaveAnimation = async () => {
  try {
    // 创建音频上下文
    audioContext.value = new AudioContext()
    analyser.value = audioContext.value.createAnalyser()
    analyser.value.fftSize = 64
    
    const bufferLength = analyser.value.frequencyBinCount
    dataArray.value = new Uint8Array(bufferLength)
    
    // 获取麦克风输入
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const source = audioContext.value.createMediaStreamSource(stream)
    source.connect(analyser.value)
    
    // 开始动画循环
    animateWave()
    
  } catch (error) {
    console.error('音频上下文创建失败:', error)
    // 降级到模拟动画
    simulateWaveAnimation()
  }
}

const animateWave = () => {
  if (!props.recording || !analyser.value || !dataArray.value) return
  
  analyser.value.getByteFrequencyData(dataArray.value)
  
  // 将音频数据转换为波形高度
  const step = Math.floor(dataArray.value.length / 20)
  for (let i = 0; i < 20; i++) {
    const dataIndex = Math.min(i * step, dataArray.value.length - 1)
    const amplitude = dataArray.value[dataIndex] / 255
    waveHeights.value[i] = Math.max(20, amplitude * 100)
  }
  
  animationFrame.value = requestAnimationFrame(animateWave)
}

const simulateWaveAnimation = () => {
  if (!props.recording) return
  
  // 模拟随机波形
  for (let i = 0; i < 20; i++) {
    waveHeights.value[i] = 20 + Math.random() * 60
  }
  
  animationFrame.value = requestAnimationFrame(() => {
    setTimeout(simulateWaveAnimation, 100)
  })
}

const stopWaveAnimation = () => {
  if (animationFrame.value) {
    cancelAnimationFrame(animationFrame.value)
  }
  
  if (audioContext.value) {
    audioContext.value.close()
    audioContext.value = undefined
  }
  
  // 重置波形高度
  waveHeights.value = Array(20).fill(20)
}

onMounted(() => {
  if (props.recording) {
    startWaveAnimation()
  }
})

onUnmounted(() => {
  stopWaveAnimation()
})
</script>

<style scoped>
@keyframes waveIdle {
  0%, 100% {
    opacity: 0.6;
    transform: scaleY(0.8);
  }
  50% {
    opacity: 1;
    transform: scaleY(1.2);
  }
}

.animate-wave-idle {
  animation: waveIdle 2s ease-in-out infinite;
}
</style>
