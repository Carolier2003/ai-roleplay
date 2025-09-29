<template>
  <div class="voice-wave" ref="waveContainer">
    <div class="wave-bars">
      <div
        v-for="i in 20"
        :key="i"
        class="wave-bar"
        :style="{ 
          height: recording ? `${waveHeights[i - 1]}%` : '20%',
          animationDelay: `${i * 50}ms`
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
.voice-wave {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wave-bars {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 100%;
}

.wave-bar {
  width: 3px;
  background: linear-gradient(to top, var(--primary-600), var(--primary-400));
  border-radius: 2px;
  transition: height 0.1s ease;
  min-height: 4px;
  animation: waveIdle 2s ease-in-out infinite;
}

.wave-bar:nth-child(odd) {
  animation-direction: reverse;
}

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

/* 录音时的活跃动画 */
.voice-wave:has(.wave-bar[style*="height"]) .wave-bar {
  animation: waveActive 0.3s ease-in-out infinite alternate;
}

@keyframes waveActive {
  from {
    opacity: 0.7;
  }
  to {
    opacity: 1;
  }
}
</style>
