<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center overflow-hidden bg-black/60 backdrop-blur-sm" @click.self="$emit('update:visible', false)">
        <div class="relative w-full max-w-md transform overflow-hidden rounded-3xl bg-white/10 p-6 text-left shadow-2xl backdrop-blur-xl border border-white/20 transition-all sm:my-8 m-4">
          
          <!-- Background Glow Effects -->
          <div class="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none z-0">
            <div class="absolute top-[-50%] left-[-50%] w-[200%] h-[200%] bg-gradient-to-br from-purple-500/20 via-transparent to-blue-500/20 blur-3xl opacity-50 animate-pulse-slow"></div>
          </div>

          <!-- Content Container -->
          <div class="relative z-10 flex flex-col items-center">
            
            <!-- Header -->
            <div class="w-full flex justify-between items-center mb-8">
              <div class="flex items-center gap-3">
                <div class="w-2 h-2 rounded-full" :class="isConnected ? 'bg-green-400 shadow-[0_0_10px_rgba(74,222,128,0.5)]' : 'bg-gray-400'"></div>
                <span class="text-white/80 text-sm font-medium tracking-wide">{{ statusText }}</span>
              </div>
              <button @click="$emit('update:visible', false)" class="text-white/50 hover:text-white transition-colors p-2 rounded-full hover:bg-white/10">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Avatar / Visualizer -->
            <div class="relative mb-8 group">
              <div class="absolute inset-0 bg-gradient-to-r from-purple-500 to-blue-500 rounded-full blur-xl opacity-20 group-hover:opacity-30 transition-opacity duration-500"></div>
              <div class="relative w-24 h-24 rounded-full bg-gradient-to-br from-white/10 to-white/5 border border-white/10 flex items-center justify-center shadow-inner overflow-hidden">
                <img v-if="currentCharacter?.avatarUrl" :src="currentCharacter.avatarUrl" class="w-full h-full object-cover opacity-80" alt="Character Avatar" />
                <div v-else class="text-4xl">🎙️</div>
                
                <!-- Ripple Effect when talking -->
                <div v-if="isRecording && currentVolume > 0.01" class="absolute inset-0 rounded-full border-2 border-white/30 animate-ping opacity-50"></div>
              </div>
            </div>

            <!-- Character Name -->
            <h3 class="text-xl font-bold text-white mb-1 tracking-wide">{{ currentCharacter?.name || 'AI Assistant' }}</h3>
            <p class="text-white/40 text-xs mb-8 font-mono">{{ formatDuration(callDuration) }}</p>

            <!-- Waveform & Text Area -->
            <div class="w-full mb-8 min-h-[120px] flex flex-col justify-end">
              <!-- Recognized Text -->
              <div v-if="recognizedText" class="mb-4 text-center">
                <p class="text-white/90 text-lg font-medium leading-relaxed animate-fade-in-up">
                  "{{ recognizedText }}"
                </p>
              </div>
              
              <!-- Waveform Canvas -->
              <div class="relative w-full h-16 rounded-xl overflow-hidden bg-black/20 border border-white/5 backdrop-blur-sm">
                <canvas 
                  ref="waveformCanvas" 
                  class="block w-full h-full opacity-80"
                  :width="canvasWidth"
                  :height="canvasHeight"
                ></canvas>
              </div>
            </div>

            <!-- Controls -->
            <div class="flex items-center gap-6">
              <!-- Start Call -->
              <button 
                v-if="callStatus === 'idle'"
                @click="startCall"
                :disabled="isConnecting"
                class="w-16 h-16 rounded-full bg-gradient-to-r from-green-500 to-emerald-600 text-white flex items-center justify-center hover:scale-105 active:scale-95 transition-all shadow-lg shadow-green-500/30 disabled:opacity-50 disabled:cursor-not-allowed group"
              >
                <svg v-if="isConnecting" class="animate-spin h-6 w-6 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="28" height="28" fill="currentColor" class="group-hover:drop-shadow-md">
                  <path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/>
                </svg>
              </button>

              <template v-else-if="callStatus === 'connected'">
                <!-- Mute Button -->
                <button 
                  @click="toggleMute"
                  class="w-12 h-12 rounded-full flex items-center justify-center transition-all border border-white/10 hover:bg-white/10"
                  :class="isMuted ? 'bg-white/20 text-white' : 'bg-transparent text-white/70'"
                >
                  <svg v-if="isMuted" viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M19 11h-1.7c0 .74-.16 1.43-.43 2.05l1.23 1.23c.56-.98.9-2.09.9-3.28zm-4.02.17c0-.06.02-.11.02-.17V5c0-1.66-1.34-3-3-3S9 3.34 9 5v.18l5.98 5.99zM4.27 3L3 4.27l6.01 6.01V11c0 1.66 1.33 3 2.99 3 .22 0 .44-.03.65-.08l1.66 1.66c-.71.33-1.5.52-2.31.52-2.76 0-5.3-2.1-5.3-5.1H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c.91-.13 1.77-.45 2.54-.9L19.73 21 21 19.73 4.27 3z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z"/>
                  </svg>
                </button>

                <!-- End Call -->
                <button 
                  @click="endCall"
                  class="w-16 h-16 rounded-full bg-gradient-to-r from-red-500 to-rose-600 text-white flex items-center justify-center hover:scale-105 active:scale-95 transition-all shadow-lg shadow-red-500/30"
                >
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
                    <path d="M17.73 14.27L19.07 12.93C19.32 12.68 19.32 12.25 19.07 12L17.73 10.66C17.48 10.41 17.05 10.41 16.8 10.66L15.46 12C14.5 11.5 13.5 11.2 12.5 11.05V9C12.5 8.72 12.28 8.5 12 8.5S11.5 8.72 11.5 9V11.05C10.5 11.2 9.5 11.5 8.54 12L7.2 10.66C6.95 10.41 6.52 10.41 6.27 10.66L4.93 12C4.68 12.25 4.68 12.68 4.93 12.93L6.27 14.27C5.77 15.23 5.47 16.23 5.32 17.23H3.27C2.99 17.23 2.77 17.45 2.77 17.73S2.99 18.23 3.27 18.23H5.32C5.47 19.23 5.77 20.23 6.27 21.19L4.93 22.53C4.68 22.78 4.68 23.21 4.93 23.46L6.27 24.8C6.52 25.05 6.95 25.05 7.2 24.8L8.54 23.46C9.5 23.96 10.5 24.26 11.5 24.41V26.46C11.5 26.74 11.72 26.96 12 26.96S12.5 26.74 12.5 26.46V24.41C13.5 24.26 14.5 23.96 15.46 23.46L16.8 24.8C17.05 25.05 17.48 25.05 17.73 24.8L19.07 23.46C19.32 23.21 19.32 22.78 19.07 22.53L17.73 21.19C18.23 20.23 18.53 19.23 18.68 18.23H20.73C21.01 18.23 21.23 18.01 21.23 17.73S21.01 17.23 20.73 17.23H18.68C18.53 16.23 18.23 15.23 17.73 14.27Z"/>
                  </svg>
                </button>
              </template>

              <button 
                v-else-if="callStatus === 'connecting'"
                @click="endCall"
                class="w-16 h-16 rounded-full bg-red-500/80 text-white flex items-center justify-center hover:bg-red-600 transition-all shadow-lg"
              >
                <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
                  <path d="M17.73 14.27L19.07 12.93C19.32 12.68 19.32 12.25 19.07 12L17.73 10.66C17.48 10.41 17.05 10.41 16.8 10.66L15.46 12C14.5 11.5 13.5 11.2 12.5 11.05V9C12.5 8.72 12.28 8.5 12 8.5S11.5 8.72 11.5 9V11.05C10.5 11.2 9.5 11.5 8.54 12L7.2 10.66C6.95 10.41 6.52 10.41 6.27 10.66L4.93 12C4.68 12.25 4.68 12.68 4.93 12.93L6.27 14.27C5.77 15.23 5.47 16.23 5.32 17.23H3.27C2.99 17.23 2.77 17.45 2.77 17.73S2.99 18.23 3.27 18.23H5.32C5.47 19.23 5.77 20.23 6.27 21.19L4.93 22.53C4.68 22.78 4.68 23.21 4.93 23.46L6.27 24.8C6.52 25.05 6.95 25.05 7.2 24.8L8.54 23.46C9.5 23.96 10.5 24.26 11.5 24.41V26.46C11.5 26.74 11.72 26.96 12 26.96S12.5 26.74 12.5 26.46V24.41C13.5 24.26 14.5 23.96 15.46 23.46L16.8 24.8C17.05 25.05 17.48 25.05 17.73 24.8L19.07 23.46C19.32 23.21 19.32 22.78 19.07 22.53L17.73 21.19C18.23 20.23 18.53 19.23 18.68 18.23H20.73C21.01 18.23 21.23 18.01 21.23 17.73S21.01 17.23 20.73 17.23H18.68C18.53 16.23 18.23 15.23 17.73 14.27Z"/>
                </svg>
              </button>
            </div>

            <!-- Error Toast (Inline) -->
            <Transition name="fade">
              <div v-if="errorMessage" class="absolute bottom-4 left-4 right-4 bg-red-500/90 text-white px-4 py-2 rounded-lg text-sm text-center backdrop-blur-md shadow-lg">
                {{ errorMessage }}
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'

// Props
interface Props {
  visible: boolean
}

const props = withDefaults(defineProps<Props>(), {
  visible: false
})

// Emits
const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

// Stores & Composables
import { useToast } from '@/composables/useToast'

const chatStore = useChatStore()
const authStore = useAuthStore()
const errorHandler = useErrorHandler()
const toast = useToast()

// Simple replacement for useMessage
const message = {
  success: (msg: string) => {
    console.log('Success:', msg)
    toast.success(msg)
  },
  info: (msg: string) => {
    console.log('Info:', msg)
    toast.info(msg)
  },
  error: (msg: string) => {
    console.error('Error:', msg)
    toast.error(msg)
  },
  warning: (msg: string) => {
    console.warn('Warning:', msg)
    toast.warning(msg)
  }
}

// 响应式数据
const callStatus = ref<'idle' | 'connecting' | 'connected' | 'disconnected'>('idle')
const isRecording = ref(false)
const isMuted = ref(false)
const isConnecting = ref(false)
const currentVolume = ref(0)
const callDuration = ref(0)
const errorMessage = ref('')

// 音频相关
const mediaRecorder = ref<MediaRecorder | null>(null)
const audioContext = ref<AudioContext | null>(null)
const analyser = ref<AnalyserNode | null>(null)
const microphone = ref<MediaStreamAudioSourceNode | null>(null)
const audioStream = ref<MediaStream | null>(null)

// SSE连接
const speechSSE = ref<EventSource | null>(null)
const ttsSSE = ref<EventSource | null>(null)
const currentSessionId = ref<string>('')

// Canvas相关
const waveformCanvas = ref<HTMLCanvasElement>()
const canvasWidth = 460
const canvasHeight = 120
let animationId: number | null = null

// 定时器
let callTimer: NodeJS.Timeout | null = null

// 计算属性
const currentCharacter = computed(() => chatStore.currentCharacter)

const isConnected = computed(() => callStatus.value === 'connected')

const statusColor = computed(() => {
  switch (callStatus.value) {
    case 'idle': return '#909399'
    case 'connecting': return '#E6A23C'
    case 'connected': return '#67C23A'
    case 'disconnected': return '#F56C6C'
    default: return '#909399'
  }
})

const statusText = computed(() => {
  switch (callStatus.value) {
    case 'idle': return '点击开始通话'
    case 'connecting': return '正在连接...'
    case 'connected': return '通话中'
    case 'disconnected': return '通话已结束'
    default: return ''
  }
})

// 方法
const startCall = async () => {
  try {
    isConnecting.value = true
    callStatus.value = 'connecting'
    
    console.log('[VoiceCall] 开始语音通话')
    
    // 1. 请求麦克风权限
    await requestMicrophonePermission()
    
    // 2. 建立SSE连接
    await connectSSE()
    
    // 3. 开始录音
    await startRecording()
    
    // 4. 开始通话计时
    startCallTimer()
    
    callStatus.value = 'connected'
    message.success('语音通话已建立')
    
  } catch (error: any) {
    console.error('[VoiceCall] 启动通话失败:', error)
    errorMessage.value = `启动通话失败: ${error.message}`
    callStatus.value = 'idle'
  } finally {
    isConnecting.value = false
  }
}

const endCall = async () => {
  try {
    console.log('[VoiceCall] 结束语音通话')
    
    // 停止录音
    await stopRecording()
    
    // 关闭SSE连接
    closeSSE()
    
    // 停止计时器
    stopCallTimer()
    
    callStatus.value = 'disconnected'
    
    // 2秒后重置状态
    setTimeout(() => {
      callStatus.value = 'idle'
      callDuration.value = 0
      emit('update:visible', false)
    }, 2000)
    
    message.info('通话已结束')
    
  } catch (error: any) {
    console.error('[VoiceCall] 结束通话失败:', error)
    errorMessage.value = `结束通话失败: ${error.message}`
  }
}

const toggleMute = () => {
  isMuted.value = !isMuted.value
  
  if (audioStream.value) {
    audioStream.value.getAudioTracks().forEach(track => {
      track.enabled = !isMuted.value
    })
  }
  
  message.info(isMuted.value ? '已静音' : '已取消静音')
  console.log('[VoiceCall] 切换静音状态:', isMuted.value)
}

const requestMicrophonePermission = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ 
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        sampleRate: 16000
      } 
    })
    
    audioStream.value = stream
    console.log('[VoiceCall] 麦克风权限获取成功')
    
    // 设置音频上下文和分析器
    await setupAudioAnalysis(stream)
    
  } catch (error) {
    throw new Error('无法获取麦克风权限，请检查浏览器设置')
  }
}

const setupAudioAnalysis = async (stream: MediaStream) => {
  try {
    audioContext.value = new AudioContext({ sampleRate: 16000 })
    analyser.value = audioContext.value.createAnalyser()
    microphone.value = audioContext.value.createMediaStreamSource(stream)
    
    analyser.value.fftSize = 256
    microphone.value.connect(analyser.value)
    
    // 开始波形图绘制
    startWaveformAnimation()
    
    console.log('[VoiceCall] 音频分析设置完成')
    
  } catch (error) {
    console.error('[VoiceCall] 音频分析设置失败:', error)
    throw new Error('音频分析设置失败')
  }
}

const connectSSE = async () => {
  try {
    console.log('[VoiceCall] 开始建立SSE连接')
    
    // 生成会话ID
    currentSessionId.value = generateSessionId()
    
    // 1. 通过POST请求创建语音识别SSE会话
    // 后端的/api/speech/streaming/create接收POST请求并返回SseEmitter
    const response = await fetch('http://localhost:18080/api/speech/streaming/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token || ''}`
      },
      body: JSON.stringify({
        model: 'fun-asr-realtime',
        format: 'wav',
        sampleRate: 16000,
        semanticPunctuationEnabled: false,
        punctuationPredictionEnabled: true,
        maxSentenceSilence: 1300,
        languageHints: []
      })
    })
    
    if (!response.ok) {
      throw new Error(`创建语音识别会话失败: ${response.status}`)
    }
    
    // 2. 处理SSE流响应
    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法获取响应流')
    }
    
    console.log('[VoiceCall] SSE连接建立成功，开始处理流数据')
    
    // 3. 读取SSE流数据
    const processStream = async () => {
      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          
          // 解析SSE数据
          const chunk = new TextDecoder().decode(value)
          const lines = chunk.split('\n')
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6)
              if (data.trim() && data !== '[DONE]') {
                try {
                  const result = JSON.parse(data)
                  handleSpeechRecognitionResult(result)
                } catch (e) {
                  console.warn('[VoiceCall] 解析SSE数据失败:', e)
                }
              }
            } else if (line.startsWith('event: ')) {
              const eventType = line.substring(7)
              console.log('[VoiceCall] SSE事件类型:', eventType)
            }
          }
        }
      } catch (error: any) {
        console.error('[VoiceCall] 处理SSE流失败:', error)
        errorMessage.value = `处理语音识别流失败: ${error.message}`
      }
    }
    
    // 异步处理流数据
    processStream()
    
  } catch (error) {
    console.error('[VoiceCall] 建立SSE连接失败:', error)
    throw error
  }
}

const generateSessionId = (): string => {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

const closeSSE = () => {
  if (speechSSE.value) {
    speechSSE.value.close()
    speechSSE.value = null
  }
  
  if (ttsSSE.value) {
    ttsSSE.value.close()
    ttsSSE.value = null
  }
  
  // 如果有活动会话，通知后端关闭
  if (currentSessionId.value) {
    fetch(`http://localhost:18080/api/speech/streaming/${currentSessionId.value}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${authStore.token || ''}`
      }
    }).catch(error => {
      console.warn('[VoiceCall] 关闭后端会话失败:', error)
    })
    
    currentSessionId.value = ''
  }
  
  console.log('[VoiceCall] SSE连接已关闭')
}

const startRecording = async () => {
  if (!audioStream.value) return
  
  try {
    mediaRecorder.value = new MediaRecorder(audioStream.value, {
      mimeType: 'audio/webm;codecs=opus'
    })
    
    mediaRecorder.value.ondataavailable = async (event) => {
      if (event.data.size > 0 && currentSessionId.value) {
        try {
          // 将音频数据发送到SSE会话
          const audioBuffer = await event.data.arrayBuffer()
          const audioBytes = new Uint8Array(audioBuffer)
          
          await fetch(`http://localhost:18080/api/speech/streaming/${currentSessionId.value}/audio`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/octet-stream',
              'Authorization': `Bearer ${authStore.token || ''}`
            },
            body: audioBytes
          })
          
        } catch (error) {
          console.error('[VoiceCall] 发送音频数据失败:', error)
        }
      }
    }
    
    mediaRecorder.value.start(100) // 每100ms发送一次数据
    isRecording.value = true
    
    console.log('[VoiceCall] 开始录音')
    
  } catch (error) {
    console.error('[VoiceCall] 启动录音失败:', error)
    throw new Error('启动录音失败')
  }
}

const stopRecording = async () => {
  if (mediaRecorder.value && isRecording.value) {
    mediaRecorder.value.stop()
    isRecording.value = false
    console.log('[VoiceCall] 停止录音')
  }
  
  if (audioStream.value) {
    audioStream.value.getTracks().forEach(track => track.stop())
    audioStream.value = null
  }
  
  if (audioContext.value) {
    await audioContext.value.close()
    audioContext.value = null
  }
  
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
}

const startWaveformAnimation = () => {
  if (!analyser.value || !waveformCanvas.value) return
  
  const canvas = waveformCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  const bufferLength = analyser.value.frequencyBinCount
  const dataArray = new Uint8Array(bufferLength)
  
  const draw = () => {
    if (!analyser.value || callStatus.value !== 'connected') return
    
    animationId = requestAnimationFrame(draw)
    
    analyser.value.getByteFrequencyData(dataArray)
    
    // 清空画布
    ctx.fillStyle = '#1a1a1a'
    ctx.fillRect(0, 0, canvasWidth, canvasHeight)
    
    // 计算音量
    const sum = dataArray.reduce((a, b) => a + b, 0)
    currentVolume.value = sum / (bufferLength * 255)
    
    // 绘制波形
    const barWidth = canvasWidth / bufferLength
    let x = 0
    
    for (let i = 0; i < bufferLength; i++) {
      const barHeight = (dataArray[i] / 255) * canvasHeight
      
      // 创建渐变色
      const gradient = ctx.createLinearGradient(0, canvasHeight - barHeight, 0, canvasHeight)
      gradient.addColorStop(0, '#67C23A')
      gradient.addColorStop(0.5, '#E6A23C')
      gradient.addColorStop(1, '#F56C6C')
      
      ctx.fillStyle = gradient
      ctx.fillRect(x, canvasHeight - barHeight, barWidth, barHeight)
      
      x += barWidth + 1
    }
  }
  
  draw()
}

const recognizedText = ref('')

// ... (existing code)

const handleSpeechRecognitionResult = (result: any) => {
  try {
    console.log('[VoiceCall] 语音识别结果:', result)
    
    if (result.text && result.text.trim()) {
      recognizedText.value = result.text.trim()
      
      if (result.isFinal) {
        // 发送识别到的文本到聊天
        if (chatStore.currentCharacterId) {
          chatStore.addMessage({
            characterId: chatStore.currentCharacterId,
            content: result.text.trim(),
            isUser: true
          })
        }
        
        // 延迟清除显示的文本
        setTimeout(() => {
          recognizedText.value = ''
        }, 3000)
      }
    }
    
  } catch (error) {
    console.error('[VoiceCall] 解析语音识别结果失败:', error)
  }
}

const startCallTimer = () => {
  callTimer = setInterval(() => {
    callDuration.value++
  }, 1000)
}

const stopCallTimer = () => {
  if (callTimer) {
    clearInterval(callTimer)
    callTimer = null
  }
}

const formatDuration = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const clearError = () => {
  errorMessage.value = ''
}

// 监听弹窗关闭
watch(() => props.visible, (newVal) => {
  if (!newVal && callStatus.value !== 'idle') {
    endCall()
  }
})

// 组件卸载时清理资源
onUnmounted(() => {
  if (callStatus.value !== 'idle') {
    endCall()
  }
})
</script>

<style scoped>
@keyframes pulse-red {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.7);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(245, 108, 108, 0);
  }
}

@keyframes pulse-slow {
  0%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.05);
  }
}

@keyframes fade-in-up {
  0% {
    opacity: 0;
    transform: translateY(10px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-pulse-red {
  animation: pulse-red 1.5s infinite;
}

.animate-pulse-slow {
  animation: pulse-slow 4s ease-in-out infinite;
}

.animate-fade-in-up {
  animation: fade-in-up 0.5s ease-out forwards;
}
</style>
