<template>
  <div class="flex flex-col gap-4 p-5 bg-gradient-to-br from-gray-50 to-gray-200 rounded-xl shadow-sm">
    <!-- 实时字幕显示区域 -->
    <div 
      class="min-h-[200px] max-h-[400px] overflow-y-auto p-4 bg-white/90 rounded-lg border-2 border-transparent transition-all duration-300"
      :class="{ 'border-green-500 shadow-[0_0_0_3px_rgba(24,160,88,0.1)]': isListening }"
    >
      <!-- 当前实时字幕（临时结果） -->
      <div v-if="currentPartialText" class="flex items-center gap-1 mb-3 p-2 px-3 bg-green-500/10 rounded-md border-l-[3px] border-green-500">
        <span class="text-base font-medium text-green-600 leading-relaxed">{{ currentPartialText }}</span>
        <span class="inline-block w-0.5 h-5 bg-green-500 animate-pulse" v-if="isListening"></span>
      </div>
      
      <!-- 历史确认字幕 -->
      <div class="flex flex-col gap-2">
        <div 
          v-for="segment in confirmedSegments" 
          :key="segment.id"
          class="flex gap-3 p-2 px-3 bg-black/2 rounded-md transition-all duration-300"
          :class="{ 'bg-green-500/5 border-l-[3px] border-green-500': isRecentSegment(segment.timestamp) }"
        >
          <span class="text-xs text-gray-500 whitespace-nowrap min-w-[70px]">{{ formatTime(segment.timestamp) }}</span>
          <span class="text-sm text-gray-800 leading-relaxed flex-1">{{ segment.text }}</span>
        </div>
      </div>
      
      <!-- 状态提示 -->
      <div v-if="!isListening && confirmedSegments.length === 0" class="flex items-center justify-center h-[100px] text-gray-400 text-sm">
        点击开始语音识别...
      </div>
      
      <!-- 错误提示 -->
      <div v-if="errorMessage" class="mt-2 p-3 bg-red-50 text-red-600 text-sm rounded border border-red-100">
        {{ errorMessage }}
      </div>
    </div>
    
    <!-- 控制按钮 -->
    <div class="flex gap-3 justify-center items-center">
      <button 
        v-if="!isListening" 
        class="px-6 py-2.5 bg-green-600 text-white rounded-lg hover:bg-green-700 active:bg-green-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2 font-medium shadow-sm"
        @click="startListening"
        :disabled="isConnecting"
      >
        <svg v-if="isConnecting" class="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
          <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
        </svg>
        {{ isConnecting ? '连接中...' : '开始语音识别' }}
      </button>
      
      <button 
        v-else 
        class="px-6 py-2.5 bg-red-500 text-white rounded-lg hover:bg-red-600 active:bg-red-700 transition-colors flex items-center gap-2 font-medium shadow-sm"
        @click="stopListening"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M6 6h12v12H6z"/>
        </svg>
        停止识别
      </button>
      
      <button 
        v-if="confirmedSegments.length > 0" 
        class="px-6 py-2.5 bg-white text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 active:bg-gray-100 transition-colors font-medium shadow-sm"
        @click="clearHistory"
      >
        清空历史
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'

// Props
interface Props {
  autoStart?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  autoStart: false
})

// Emits
const emit = defineEmits<{
  'subtitle-update': [text: string, isFinal: boolean]
  'listening-change': [isListening: boolean]
  'error': [error: string]
}>()

// 状态管理
const authStore = useAuthStore()

// Simple replacement for useMessage
const message = {
  warning: (msg: string) => {
    console.warn('Warning:', msg)
    alert(msg)
  },
  error: (msg: string) => {
    console.error('Error:', msg)
    alert(msg)
  }
}

const isListening = ref(false)
const isConnecting = ref(false)
const currentPartialText = ref('')
const confirmedSegments = ref<Array<{
  id: string
  text: string
  timestamp: number
  confidence: number
}>>([])
const errorMessage = ref('')

// SSE 和录音相关
let sseEventSource: EventSource | null = null
let mediaRecorder: MediaRecorder | null = null
let audioStream: MediaStream | null = null
let currentSessionId = ref('')
let audioChunkIndex = 0

// 工具函数
const generateSessionId = () => {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric',
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit', 
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

const isRecentSegment = (timestamp: number) => {
  return Date.now() - timestamp < 5000 // 5秒内的为最近
}

// 清空历史
const clearHistory = () => {
  confirmedSegments.value = []
  currentPartialText.value = ''
}

// 建立SSE连接
const connectSSE = async (): Promise<void> => {
  return new Promise((resolve, reject) => {
    try {
      console.log('[RealtimeSubtitle] 开始建立SSE连接')
      
      // 生成会话ID
      currentSessionId.value = generateSessionId()
      
      // 使用fetch建立SSE连接
      fetch('http://localhost:18080/api/speech/streaming/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authStore.token || ''}`
        },
        body: JSON.stringify({
          model: 'fun-asr-realtime',
          format: 'wav',
          sampleRate: 16000,
          enableRealTimeResults: true,
          enablePartialResults: true,
          maxSilenceMs: 800,
          semanticPunctuationEnabled: false,
          punctuationPredictionEnabled: true,
          maxSentenceSilence: 1300,
          languageHints: []
        })
      }).then(async (response) => {
        if (!response.ok) {
          throw new Error(`创建语音识别会话失败: ${response.status}`)
        }
        
        // 处理SSE流响应
        const reader = response.body?.getReader()
        if (!reader) {
          throw new Error('无法获取响应流')
        }
        
        console.log('[RealtimeSubtitle] SSE连接建立成功，开始处理流数据')
        
        // 处理SSE流数据
        let buffer = ''
        const processStream = async () => {
          try {
            while (true) {
              const { done, value } = await reader.read()
              if (done) break
              
              // 解析SSE数据
              const chunk = new TextDecoder().decode(value)
              buffer += chunk
              
              // 按双换行符分割SSE事件
              const events = buffer.split('\n\n')
              buffer = events.pop() || '' // 保留最后一个不完整的事件
              
              for (const event of events) {
                if (!event.trim()) continue
                
                const lines = event.split('\n')
                let eventType = ''
                let eventData = ''
                
                for (const line of lines) {
                  if (line.startsWith('event:')) {
                    eventType = line.substring(6).trim()
                  } else if (line.startsWith('data:')) {
                    eventData = line.substring(5).trim()
                  }
                }
                
                console.log('[RealtimeSubtitle] 收到SSE事件:', { eventType, eventData })
                
                if (eventData && eventData !== '[DONE]') {
                  try {
                    const result = JSON.parse(eventData)
                    handleSSEEvent(result)
                  } catch (e) {
                    console.warn('[RealtimeSubtitle] 解析SSE数据失败:', e, '原始数据:', eventData)
                  }
                }
              }
            }
          } catch (error: any) {
            console.error('[RealtimeSubtitle] 处理SSE流失败:', error)
            handleError(`处理语音识别流失败: ${error.message}`)
            reject(error)
          }
        }
        
        // 异步处理流数据
        processStream()
        resolve()
        
      }).catch(error => {
        console.error('[RealtimeSubtitle] 建立SSE连接失败:', error)
        reject(error)
      })
      
    } catch (error) {
      console.error('[RealtimeSubtitle] SSE连接异常:', error)
      reject(error)
    }
  })
}

// 处理SSE事件
const handleSSEEvent = (eventData: any) => {
  console.log('[RealtimeSubtitle] 收到SSE事件:', eventData)
  
  if (eventData.type === 'CONNECTED') {
    // 连接建立，获取sessionId
    if (eventData.requestId) {
      currentSessionId.value = eventData.requestId
      console.log('[RealtimeSubtitle] 获取到会话ID:', currentSessionId.value)
      
      // 连接建立成功，开始录音
      startRecording()
    }
  } else if (eventData.type === 'ERROR') {
    // 处理错误
    console.error('[RealtimeSubtitle] SSE错误事件:', eventData.error)
    handleError(`语音识别错误: ${eventData.error}`)
  } else if (eventData.output && eventData.output.sentence) {
    // 处理识别结果
    handleRecognitionResult(eventData)
  }
}

// 处理语音识别结果
const handleRecognitionResult = (result: any) => {
  console.log('[RealtimeSubtitle] 处理识别结果:', result)
  
  const sentence = result.output.sentence
  if (!sentence) return
  
  const text = sentence.text || ''
  const confidence = sentence.confidence || 0
  const isFinal = sentence.end_time > 0 // 有结束时间表示是最终结果
  
  if (isFinal) {
    // 最终结果：添加到确认段落
    if (text.trim()) {
      const segment = {
        id: `seg_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`,
        text: text.trim(),
        timestamp: Date.now(),
        confidence: confidence
      }
      
      confirmedSegments.value.push(segment)
      console.log('[RealtimeSubtitle] 添加确认字幕:', segment)
      
      // 清空临时文本
      currentPartialText.value = ''
      
      // 发送事件
      emit('subtitle-update', text, true)
    }
  } else {
    // 临时结果：更新当前显示
    currentPartialText.value = text
    console.log('[RealtimeSubtitle] 更新临时字幕:', text)
    
    // 发送事件
    emit('subtitle-update', text, false)
  }
}

// 开始录音
const startRecording = async () => {
  try {
    console.log('[RealtimeSubtitle] 开始录音')
    
    // 获取麦克风权限
    audioStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        sampleRate: 16000,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true
      }
    })
    
    // 创建录音器
    mediaRecorder = new MediaRecorder(audioStream, {
      mimeType: 'audio/webm;codecs=opus'
    })
    
    // 设置录音数据处理
    mediaRecorder.ondataavailable = async (event) => {
      if (event.data.size > 0 && currentSessionId.value && isListening.value) {
        try {
          // 发送音频数据到后端
          const audioBuffer = await event.data.arrayBuffer()
          const audioBytes = new Uint8Array(audioBuffer)
          
          console.log(`[RealtimeSubtitle] 发送音频数据: ${audioBytes.length} bytes`)
          
          const response = await fetch(`http://localhost:18080/api/speech/streaming/${currentSessionId.value}/audio`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/octet-stream',
              'Authorization': `Bearer ${authStore.token || ''}`,
              'X-Audio-Format': 'webm',
              'X-Chunk-Index': audioChunkIndex.toString()
            },
            body: audioBytes
          })
          
          if (!response.ok) {
            console.error(`[RealtimeSubtitle] 音频数据发送失败: ${response.status}`)
          } else {
            audioChunkIndex++
          }
          
        } catch (error) {
          console.error('[RealtimeSubtitle] 发送音频数据失败:', error)
        }
      }
    }
    
    // 开始录音（每500ms一个chunk）
    mediaRecorder.start(500)
    console.log('[RealtimeSubtitle] 录音已开始')
    
  } catch (error: any) {
    console.error('[RealtimeSubtitle] 开始录音失败:', error)
    handleError(`无法访问麦克风: ${error.message}`)
  }
}

// 开始监听
const startListening = async () => {
  try {
    isConnecting.value = true
    errorMessage.value = ''
    audioChunkIndex = 0
    
    console.log('[RealtimeSubtitle] 开始语音识别')
    
    // 检查登录状态
    if (!authStore.isLoggedIn) {
      message.warning('请先登录后再使用语音识别功能')
      // authStore.showLoginModal() // Assuming this method exists or handled elsewhere
      return
    }
    
    // 检查浏览器支持
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      handleError('您的浏览器不支持语音识别功能')
      return
    }
    
    // 建立SSE连接
    await connectSSE()
    
    isListening.value = true
    isConnecting.value = false
    
    emit('listening-change', true)
    // 移除成功提示，静默开始识别
    
  } catch (error: any) {
    console.error('[RealtimeSubtitle] 开始监听失败:', error)
    isConnecting.value = false
    handleError(`启动语音识别失败: ${error.message}`)
  }
}

// 停止监听
const stopListening = async () => {
  try {
    console.log('[RealtimeSubtitle] 停止语音识别')
    
    isListening.value = false
    
    // 停止录音
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
    }
    
    // 关闭音频流
    if (audioStream) {
      audioStream.getTracks().forEach(track => track.stop())
      audioStream = null
    }
    
    // 通知后端停止识别
    if (currentSessionId.value) {
      try {
        await fetch(`http://localhost:18080/api/speech/streaming/${currentSessionId.value}/stop`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${authStore.token || ''}`
          }
        })
        
        // 删除会话
        await fetch(`http://localhost:18080/api/speech/streaming/${currentSessionId.value}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${authStore.token || ''}`
          }
        })
      } catch (error) {
        console.error('[RealtimeSubtitle] 停止会话失败:', error)
      }
    }
    
    // 关闭SSE连接
    if (sseEventSource) {
      sseEventSource.close()
      sseEventSource = null
    }
    
    currentSessionId.value = ''
    mediaRecorder = null
    
    emit('listening-change', false)
    // 移除停止提示，静默停止识别
    
  } catch (error: any) {
    console.error('[RealtimeSubtitle] 停止监听失败:', error)
    handleError(`停止语音识别失败: ${error.message}`)
  }
}

// 错误处理
const handleError = (error: string) => {
  errorMessage.value = error
  isListening.value = false
  isConnecting.value = false
  
  // 统一错误提示为"转化失败"
  const userFriendlyError = '转化失败'
  
  emit('error', userFriendlyError)
  message.error(userFriendlyError)
  
  // 清理资源
  stopListening()
}

// 组件卸载时清理
onUnmounted(() => {
  stopListening()
})

// 自动开始
if (props.autoStart) {
  nextTick(() => {
    startListening()
  })
}
</script>
