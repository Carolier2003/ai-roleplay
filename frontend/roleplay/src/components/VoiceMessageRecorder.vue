<template>
  <div class="flex flex-col gap-3">
    <!-- 语音录制按钮 -->
    <div class="flex justify-center">
      <button
        class="flex flex-col items-center gap-2 px-5 py-4 text-white border-none rounded-xl cursor-pointer transition-all duration-300 select-none min-w-[120px] shadow-lg"
        :class="[
          isProcessing ? 'bg-gray-400 cursor-not-allowed shadow-none' : 
          isRecording ? 'bg-gradient-to-br from-red-500 to-red-700 animate-pulse shadow-red-500/40' : 
          'bg-gradient-to-br from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 hover:-translate-y-0.5 shadow-green-500/30 hover:shadow-green-500/40'
        ]"
        @mousedown="startRecording"
        @mouseup="stopRecording"
        @mouseleave="cancelRecording"
        @touchstart="startRecording"
        @touchend="stopRecording"
        :disabled="isProcessing"
      >
        <div class="flex items-center justify-center">
          <svg v-if="!isRecording" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
            <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
          </svg>
          <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M6 6h12v12H6z"/>
          </svg>
        </div>
        <span class="text-sm font-medium">
          {{ getButtonText() }}
        </span>
      </button>
    </div>

    <!-- 录制状态提示 -->
    <div v-if="isRecording" class="flex flex-col items-center gap-3 p-4 bg-red-50 rounded-xl border-2 border-red-100">
      <div class="flex gap-1 items-center h-5">
        <div class="w-1 h-5 bg-red-500 rounded-sm animate-wave delay-0"></div>
        <div class="w-1 h-5 bg-red-500 rounded-sm animate-wave delay-100"></div>
        <div class="w-1 h-5 bg-red-500 rounded-sm animate-wave delay-200"></div>
      </div>
      <div class="text-center">
        <div class="text-lg font-bold text-red-500 mb-1">{{ formatTime(recordingDuration) }}</div>
        <div class="text-xs text-gray-500">松开发送，上滑取消</div>
      </div>
    </div>

    <!-- 语音消息预览 -->
    <div v-if="audioBlob && !isRecording" class="bg-green-50 rounded-xl border-2 border-green-100 overflow-hidden">
      <div class="flex flex-col sm:flex-row justify-between items-center p-3 sm:px-4 bg-green-500/5 border-b border-green-500/10 gap-2 sm:gap-0">
        <span class="text-sm font-medium text-green-600">录制完成 ({{ formatTime(recordingDuration) }})</span>
        <div class="flex gap-2">
          <button 
            class="px-3 py-1 text-xs font-medium text-gray-700 bg-white border border-gray-300 rounded hover:bg-gray-50 focus:outline-none"
            @click="playPreview" 
            :disabled="isPlaying"
          >
            {{ isPlaying ? '播放中' : '试听' }}
          </button>
          <button 
            class="px-3 py-1 text-xs font-medium text-white bg-green-600 border border-green-600 rounded hover:bg-green-700 focus:outline-none flex items-center gap-1"
            @click="sendVoiceMessage" 
            :disabled="isSending"
          >
            <svg v-if="isSending" class="animate-spin h-3 w-3 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            发送
          </button>
          <button 
            class="px-3 py-1 text-xs font-medium text-gray-700 bg-white border border-gray-300 rounded hover:bg-gray-50 focus:outline-none"
            @click="cancelVoiceMessage"
          >
            取消
          </button>
        </div>
      </div>
      <div class="p-4 flex justify-center">
        <canvas ref="waveformCanvas" width="300" height="60" class="rounded bg-white/50"></canvas>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'

// Props
interface Props {
  maxDuration?: number // 最大录制时长（秒）
  minDuration?: number // 最小录制时长（秒）
}

const props = withDefaults(defineProps<Props>(), {
  maxDuration: 60,
  minDuration: 1
})

// Emits
const emit = defineEmits<{
  'voice-send': [audioBlob: Blob, duration: number]
  'recording-start': []
  'recording-stop': []
  'recording-cancel': []
}>()

// 状态管理
import { useToast } from '@/composables/useToast'

const authStore = useAuthStore()
const toast = useToast()

// Simple replacement for useMessage
const message = {
  warning: (msg: string) => {
    console.warn('Warning:', msg)
    toast.warning(msg)
  },
  error: (msg: string) => {
    console.error('Error:', msg)
    toast.error(msg)
  },
  success: (msg: string) => {
    console.log('Success:', msg)
    toast.success(msg)
  }
}

const isRecording = ref(false)
const isProcessing = ref(false)
const isSending = ref(false)
const isPlaying = ref(false)
const recordingDuration = ref(0)
const audioBlob = ref<Blob | null>(null)

// 录音相关
let mediaRecorder: MediaRecorder | null = null
let audioStream: MediaStream | null = null
let recordingTimer: number | null = null
let recordingStartTime = 0
let audioChunks: Blob[] = []

// 音频分析
let audioContext: AudioContext | null = null
let analyser: AnalyserNode | null = null
let dataArray: Uint8Array | null = null
let animationFrame: number | null = null

// DOM引用
const waveformCanvas = ref<HTMLCanvasElement>()

// 计算属性
const getButtonText = () => {
  if (isProcessing.value) return '处理中...'
  if (isRecording.value) return '录制中'
  if (audioBlob.value) return '重新录制'
  return '按住说话'
}

// 格式化时间
const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 开始录制
const startRecording = async () => {
  if (isProcessing.value || isRecording.value) return

  try {
    console.log('[VoiceRecorder] 开始录制语音')
    
    // 检查登录状态
    if (!authStore.isLoggedIn) {
      message.warning('请先登录后再使用语音功能')
      return
    }

    // 检查浏览器支持
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      message.error('您的浏览器不支持语音录制功能')
      return
    }

    isProcessing.value = true

    // 获取麦克风权限
    audioStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        sampleRate: 16000,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })

    // 创建录音器
    mediaRecorder = new MediaRecorder(audioStream, {
      mimeType: 'audio/webm;codecs=opus'
    })

    // 重置数据
    audioChunks = []
    recordingDuration.value = 0
    recordingStartTime = Date.now()

    // 设置录音事件
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = () => {
      console.log('[VoiceRecorder] 录音停止，处理音频数据')
      processRecordedAudio()
    }

    // 开始录音
    mediaRecorder.start(100) // 每100ms收集一次数据
    isRecording.value = true
    isProcessing.value = false

    // 开始计时
    startTimer()

    // 开始音频可视化
    setupAudioVisualization()

    emit('recording-start')
    console.log('[VoiceRecorder] 录音已开始')

  } catch (error) {
    console.error('[VoiceRecorder] 开始录音失败:', error)
    isProcessing.value = false
    message.error('无法访问麦克风，请检查权限设置')
  }
}

// 停止录制
const stopRecording = () => {
  if (!isRecording.value || !mediaRecorder) return

  console.log('[VoiceRecorder] 停止录制')

  const duration = (Date.now() - recordingStartTime) / 1000

  // 检查最小录制时长
  if (duration < props.minDuration) {
    message.warning(`录音时长不能少于${props.minDuration}秒`)
    cancelRecording()
    return
  }

  isRecording.value = false
  stopTimer()
  stopAudioVisualization()

  if (mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }

  // 关闭音频流
  if (audioStream) {
    audioStream.getTracks().forEach(track => track.stop())
  }

  emit('recording-stop')
}

// 取消录制
const cancelRecording = () => {
  if (!isRecording.value) return

  console.log('[VoiceRecorder] 取消录制')

  isRecording.value = false
  stopTimer()
  stopAudioVisualization()

  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }

  if (audioStream) {
    audioStream.getTracks().forEach(track => track.stop())
    audioStream = null
  }

  // 清理数据
  audioChunks = []
  audioBlob.value = null
  recordingDuration.value = 0

  emit('recording-cancel')
}

// 处理录制的音频
const processRecordedAudio = () => {
  if (audioChunks.length === 0) return

  console.log('[VoiceRecorder] 处理录制的音频数据')

  // 合并音频数据
  const blob = new Blob(audioChunks, { type: 'audio/webm;codecs=opus' })
  audioBlob.value = blob

  // 更新录制时长
  recordingDuration.value = (Date.now() - recordingStartTime) / 1000

  console.log('[VoiceRecorder] 音频处理完成:', {
    size: blob.size,
    duration: recordingDuration.value,
    type: blob.type
  })

  // 生成波形图
  nextTick(() => {
    generateWaveform()
  })
}

// 开始计时
const startTimer = () => {
  recordingTimer = window.setInterval(() => {
    recordingDuration.value = (Date.now() - recordingStartTime) / 1000

    // 检查最大录制时长
    if (recordingDuration.value >= props.maxDuration) {
      message.warning(`录音时长不能超过${props.maxDuration}秒`)
      stopRecording()
    }
  }, 100)
}

// 停止计时
const stopTimer = () => {
  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
}

// 设置音频可视化
const setupAudioVisualization = () => {
  if (!audioStream) return

  try {
    audioContext = new AudioContext()
    analyser = audioContext.createAnalyser()
    const source = audioContext.createMediaStreamSource(audioStream)
    
    analyser.fftSize = 256
    source.connect(analyser)
    
    dataArray = new Uint8Array(analyser.frequencyBinCount)
    
    // 开始动画
    animateWaveform()
  } catch (error) {
    console.warn('[VoiceRecorder] 音频可视化设置失败:', error)
  }
}

// 停止音频可视化
const stopAudioVisualization = () => {
  if (animationFrame) {
    cancelAnimationFrame(animationFrame)
    animationFrame = null
  }
  
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
  
  analyser = null
  dataArray = null
}

// 动画波形
const animateWaveform = () => {
  if (!isRecording.value || !analyser || !dataArray) return

  analyser.getByteFrequencyData(dataArray)
  
  // 这里可以添加实时波形绘制逻辑
  // 目前使用CSS动画代替
  
  animationFrame = requestAnimationFrame(animateWaveform)
}

// 生成波形图
const generateWaveform = () => {
  const canvas = waveformCanvas.value
  if (!canvas || !audioBlob.value) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 简单的波形可视化
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = '#16a34a' // green-600
  
  const barWidth = 4
  const barGap = 2
  const barCount = Math.floor(canvas.width / (barWidth + barGap))
  
  for (let i = 0; i < barCount; i++) {
    const height = Math.random() * canvas.height * 0.8 + canvas.height * 0.1
    const x = i * (barWidth + barGap)
    const y = (canvas.height - height) / 2
    
    ctx.fillRect(x, y, barWidth, height)
  }
}

// 试听录音
const playPreview = async () => {
  if (!audioBlob.value || isPlaying.value) return

  try {
    isPlaying.value = true
    
    const audio = new Audio(URL.createObjectURL(audioBlob.value))
    
    audio.onended = () => {
      isPlaying.value = false
      URL.revokeObjectURL(audio.src)
    }
    
    audio.onerror = () => {
      isPlaying.value = false
      URL.revokeObjectURL(audio.src)
      message.error('音频播放失败')
    }
    
    await audio.play()
    
  } catch (error) {
    console.error('[VoiceRecorder] 播放预览失败:', error)
    isPlaying.value = false
    message.error('音频播放失败')
  }
}

// 发送语音消息
const sendVoiceMessage = async () => {
  if (!audioBlob.value || isSending.value) return

  try {
    isSending.value = true
    
    console.log('[VoiceRecorder] 发送语音消息:', {
      size: audioBlob.value.size,
      duration: recordingDuration.value,
      type: audioBlob.value.type
    })

    // 发送语音消息
    emit('voice-send', audioBlob.value, recordingDuration.value)
    
    // 清理状态
    cancelVoiceMessage()
    
    message.success('语音发送成功')
    
  } catch (error) {
    console.error('[VoiceRecorder] 发送语音消息失败:', error)
    message.error('语音发送失败')
  } finally {
    isSending.value = false
  }
}

// 取消语音消息
const cancelVoiceMessage = () => {
  audioBlob.value = null
  recordingDuration.value = 0
  audioChunks = []
}

// 组件卸载时清理
onUnmounted(() => {
  cancelRecording()
  stopAudioVisualization()
})
</script>

<style scoped>
@keyframes wave {
  0%, 40%, 100% {
    transform: scaleY(0.4);
  }
  20% {
    transform: scaleY(1);
  }
}

.animate-wave {
  animation: wave 1s infinite ease-in-out;
}
</style>
