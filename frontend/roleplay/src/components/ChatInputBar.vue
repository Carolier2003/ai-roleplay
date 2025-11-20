<template>
  <div class="chat-input-bar">
    <!-- RAG功能开关已移除，默认开启 -->

    <div class="input-container">
      <!-- 语音转文字按钮 (左边) -->
      <n-button
        circle
        size="large"
        :type="voiceMode === 'voiceRecording' ? 'primary' : 'default'"
        class="voice-to-text-btn"
        @click="toggleRecording"
        :title="voiceMode === 'voiceRecording' ? '点击停止录音' : '点击开始录音'"
      >
        <template #icon>
          <span v-if="voiceMode === 'text'" class="text-icon">T</span>
          <svg v-else-if="voiceMode === 'voiceRecording'" viewBox="0 0 24 24" width="20" height="20">
            <!-- 录音中显示停止图标 -->
            <rect x="6" y="6" width="12" height="12" rx="2" fill="currentColor"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" width="20" height="20">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="currentColor"/>
          </svg>
        </template>
      </n-button>

      <!-- 麦克风按钮 (切换语音消息模式) -->
      <n-button
        circle
        size="large"
        :type="isVoiceMessageMode ? 'primary' : 'default'"
        class="microphone-btn"
        @click="toggleVoiceMessageMode"
        :title="isVoiceMessageMode ? '切换到文字输入' : '切换到语音消息'"
      >
        <template #icon>
          <svg v-if="!isVoiceMessageMode" viewBox="0 0 24 24" width="20" height="20">
            <path d="M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z" fill="currentColor"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" width="20" height="20">
            <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z" fill="currentColor"/>
          </svg>
        </template>
      </n-button>

      <!-- 统一的输入区域 -->
      <div class="unified-input-area">
        <!-- 语音转文字状态提示 -->
        <div v-if="voiceMode === 'voiceTranscribing'" class="status-display">
          <n-spin size="small" />
          <span>正在转换语音...</span>
        </div>

        <!-- 语音波形 -->
        <div v-else-if="voiceMode === 'voiceRecording'" class="status-display">
          <VoiceWave :recording="voiceMode === 'voiceRecording'" />
        </div>

        <!-- 语音通话状态 -->
        <div v-else-if="isVoiceCallActive" class="status-display voice-call-status">
          <div class="voice-call-indicator">
            <div class="pulse-dot"></div>
            <span>语音通话中...</span>
          </div>
        </div>

        <!-- 语音消息模式：长按录音按钮 -->
        <div v-else-if="isVoiceMessageMode" class="voice-message-container">
          <n-button
            :type="isRecordingVoiceMessage ? 'primary' : 'default'"
            class="voice-message-btn"
            @mousedown="startVoiceMessageRecording"
            @mouseup="stopVoiceMessageRecording"
            @mouseleave="stopVoiceMessageRecording"
            @touchstart="startVoiceMessageRecording"
            @touchend="stopVoiceMessageRecording"
          >
            <template v-if="isRecordingVoiceMessage">
              <div class="recording-indicator">
                <div class="pulse-dot"></div>
                <span>松开发送</span>
              </div>
            </template>
            <template v-else>
              <span>按住说话</span>
            </template>
          </n-button>
        </div>

        <!-- 文本输入框 -->
        <n-input
          v-else
          ref="inputRef"
          v-model:value="inputText"
          type="textarea"
          :placeholder="getPlaceholder()"
          :autosize="{ minRows: 1, maxRows: 3 }"
          :disabled="voiceMode === 'voiceTranscribing' || isVoiceCallActive"
          @keydown="handleKeydown"
          class="message-input"
        />
      </div>


      <!-- 发送/终止按钮 -->
      <n-button
        circle
        size="large"
        :type="sendButtonState.type === 'send' ? 'primary' : 'error'"
        :disabled="sendButtonState.disabled"
        :loading="sendButtonState.loading"
        @click="handleButtonClick"
        class="send-btn"
      >
        <template #icon>
          <!-- 发送图标 -->
          <svg v-if="sendButtonState.icon === 'send'" viewBox="0 0 24 24" width="20" height="20">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" fill="currentColor"/>
          </svg>

          <!-- 终止图标 -->
          <svg v-else-if="sendButtonState.icon === 'stop'" viewBox="0 0 24 24" width="20" height="20">
            <path d="M6 6h12v12H6z" fill="currentColor"/>
          </svg>
        </template>
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { NButton, NInput, NSpin, NTooltip, NSwitch, useMessage } from 'naive-ui'
import VoiceWave from './VoiceWave.vue'
import VoiceCallButton from './VoiceCallButton.vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { sendStreamMessage, updateVoiceDuration } from '@/api/chat'
import { SpeechRecognitionAPI, AudioConverter, SpeechRecognitionError, SpeechRecognitionUtils } from '@/api/speech'
import { speechConfig, getRecommendedRecordingConfig, isSpeechRecordingSupported } from '@/config/speech'
import type { SendMessageRequest, StreamResponse, UpdateVoiceDurationRequest } from '@/api/chat'
import { useTTSPlayer } from '@/composables/useTTSPlayer'
import { ttsState } from '@/services/ttsService'

interface Props {
  currentCharacterId?: number
}

interface Emits {
  send: [content: string]
}

const props = withDefaults(defineProps<Props>(), {
  currentCharacterId: undefined
})

const emit = defineEmits<Emits>()
const chatStore = useChatStore()
const authStore = useAuthStore()
const message = useMessage()

// RAG开关状态已移除，直接使用store中的状态（默认开启）
// const { enableRag } = storeToRefs(chatStore)

// TTS播放器
const { addToQueue, stopPlaying, clearQueue } = useTTSPlayer()

const inputRef = ref<InstanceType<typeof NInput>>()
const inputText = ref('')
const mediaRecorder = ref<MediaRecorder | null>(null)
const audioChunks = ref<Blob[]>([])
const isSending = ref(false)
const isTerminated = ref(false)  // 是否终止了消息接收
const currentAIMessageId = ref<string | null>(null)  // 当前AI消息ID
const isVoiceCallActive = ref(false)
const isVoiceMessageMode = ref(false)  // 语音消息模式
const isRecordingVoiceMessage = ref(false)  // 是否正在录制语音消息
const voiceMessageStartTime = ref(0)  // 语音消息开始录制时间
const voiceMessageDuration = ref(0)  // 语音消息时长
const pendingTextContent = ref('')  // TTS模式下暂存的完整文字内容

// 全局音频播放控制 - 确保同时只有一条语音播放
const currentPlayingAudio = ref<HTMLAudioElement | null>(null)  // 当前播放的音频实例


const voiceMode = computed(() => chatStore.voiceMode)
const canSend = computed(() => {
  return inputText.value.trim().length > 0 && voiceMode.value === 'text' && !isSending.value
})

// 发送按钮状态计算
const sendButtonState = computed(() => {
  if (!isSending.value) {
    return {
      type: 'send',
      icon: 'send',
      disabled: !canSend.value,
      loading: voiceMode.value === 'voiceTranscribing'
    }
  }

  return {
    type: 'stop',
    icon: 'stop',
    disabled: false,
    loading: false
  }
})

const getPlaceholder = () => {
  switch (voiceMode.value) {
    case 'voiceRecording':
      return '正在录音...'
    case 'voiceTranscribing':
      return '正在转换语音...'
    default:
      return '输入消息...'
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleButtonClick()
  }
}

// 按钮点击处理
const handleButtonClick = () => {
  switch (sendButtonState.value.type) {
    case 'send':
      handleSend()
      break
    case 'stop':
      handleStop()
      break
  }
}

// 终止消息接收和播放
const handleStop = () => {
  console.log('[ChatInputBar] 终止消息接收和播放')
  isTerminated.value = true

  // 停止TTS播放
  stopTTSPlayback()

  // 停止AI消息的流式状态（停止光标闪烁）
  if (currentAIMessageId.value) {
    chatStore.updateMessage(currentAIMessageId.value, {
      streaming: false
    })
    console.log('[ChatInputBar] 停止AI消息流式状态:', currentAIMessageId.value)
  }

  // 重置状态
  isSending.value = false
  currentAIMessageId.value = null

  console.log('[ChatInputBar] 消息终止完成')
}

// 停止当前正在播放的音频（确保同时只有一条语音播放）
const stopCurrentAudio = () => {
  if (currentPlayingAudio.value) {
    console.log('[ChatInputBar] 停止当前正在播放的音频')
    currentPlayingAudio.value.pause()
    currentPlayingAudio.value.currentTime = 0
    currentPlayingAudio.value = null
  }
}

// 停止TTS播放
const stopTTSPlayback = () => {
  // 停止当前正在播放的音频
  stopCurrentAudio()

  // 获取当前播放的音频元素并停止
  const audioElements = document.querySelectorAll('audio')
  audioElements.forEach(audio => {
    if (!audio.paused) {
      audio.pause()
      audio.currentTime = 0  // 重置播放位置
      console.log('[ChatInputBar] 停止音频播放')
    }
  })
}


// 停止所有音频播放
const stopAllAudio = () => {
  console.log('[ChatInputBar] 停止所有正在播放的音频')

  // 停止所有audio元素
  const audioElements = document.querySelectorAll('audio')
  audioElements.forEach(audio => {
    if (!audio.paused) {
      audio.pause()
      console.log('[ChatInputBar] 停止音频元素播放')
    }
  })

  // 停止TTS播放器
  try {
    if (stopPlaying) {
      stopPlaying()
      console.log('[ChatInputBar] 停止TTS播放')
    }
    if (clearQueue) {
      clearQueue()
      console.log('[ChatInputBar] 清空TTS队列')
    }
  } catch (error) {
    console.warn('[ChatInputBar] 停止TTS播放器失败:', error)
  }
}

const handleSend = async () => {
  if (!canSend.value) return
  
  const content = inputText.value.trim()
  if (!content) return
  
  // 游客模式可以直接聊天，不需要强制登录
  // 注释掉登录检查，允许游客聊天
  
  // 检查是否选择了角色
  if (!chatStore.currentCharacterId) {
    message.warning('请先选择一个角色')
    return
  }
  
  try {
    // 发送新消息前，先停止上一条正在播放的语音
    stopAllAudio()

    isSending.value = true
    
    // 添加用户消息到聊天记录
    const userMessage = chatStore.addMessage({
      characterId: chatStore.currentCharacterId,
      content: content,
      isUser: true
    })
    
    // 清空输入框
    inputText.value = ''
    
    // 重新聚焦输入框
    nextTick(() => {
      inputRef.value?.focus()
    })
    
    // 创建AI回复消息（流式）
    const aiMessage = chatStore.addMessage({
      characterId: chatStore.currentCharacterId,
      content: '',
      isUser: false,
      streaming: true
    })
    
    // 保存当前AI消息ID，用于终止控制
    currentAIMessageId.value = aiMessage.id
    isTerminated.value = false  // 重置终止状态
    pendingTextContent.value = ''  // 重置暂存的文字内容

    // 准备发送请求 - 匹配后端 ChatRequest 字段
    // 根据当前输入模式决定是否启用TTS：文字模式不启用，语音消息模式启用
    const enableTtsForThisMessage = isVoiceMessageMode.value

    const requestData: SendMessageRequest = {
      characterId: chatStore.currentCharacterId,
      message: content,  // ✅ 使用 message 字段匹配后端
      enableTts: enableTtsForThisMessage,   // ✅ 根据输入模式决定是否启用TTS
      enableRag: chatStore.enableRag,  // ✅ 传递RAG开关状态
      languageType: "Chinese"  // ✅ 设置语言类型
      // ✅ 不需要传递 userId，后端从 JWT token 中自动获取
    }
    
    console.log('[ChatInputBar] 发送消息到后端:', requestData)
    
    // 发送流式请求
    await sendStreamMessage(
      requestData,
      // onMessage - 处理流式数据
      (chunk: StreamResponse) => {
        console.log('[ChatInputBar] 收到流式数据:', chunk)
        
        // 如果终止了，忽略消息和TTS事件
        if (isTerminated.value && (chunk.type === 'message' || chunk.type === 'tts')) {
          console.log('[ChatInputBar] 已终止，忽略事件:', chunk.type)
          return
        }

        if (chunk.type === 'message' && chunk.content) {
          // 根据是否启用TTS来决定处理方式
          if (enableTtsForThisMessage) {
            // TTS模式：暂存文字内容，等语音下载完成后再显示
            pendingTextContent.value += chunk.content

            // 显示"正在生成语音..."的提示
            chatStore.updateMessage(aiMessage.id, {
              content: '🎵 正在生成语音...',
              isVoiceMessage: true
            })
            
            console.log('[ChatInputBar] TTS模式：暂存文字内容，等待语音生成:', {
              messageId: aiMessage.id,
              chunkContent: chunk.content,
              totalPendingLength: pendingTextContent.value.length
            })
          } else {
            // ✅优化 自动滚动 - 使用appendToStream方法，每段内容都会触发滚动
            chatStore.appendToStream(aiMessage.id, chunk.content)
            
            console.log('[ChatInputBar] 文字模式：流式更新消息内容:', {
              messageId: aiMessage.id,
              newChunk: chunk.content,
              totalLength: (chatStore.messageList.find(m => m.id === aiMessage.id)?.content || '').length
            })
          }
        } else if (chunk.type === 'error') {
          console.error('[ChatInputBar] 流式响应错误:', chunk.error)
          
          // 检查是否为游客聊天限制错误
          if (chunk.error && chunk.error.includes('游客模式每日最多可聊天5次')) {
            console.log('[ChatInputBar] 游客聊天次数已达上限，直接弹出登录弹窗')
            // 直接弹出登录弹窗，不显示任何提示信息
            authStore.showLoginModal()
          } else {
            message.error(chunk.error || '消息发送失败')
          }
          
          // 移除失败的AI消息
          chatStore.removeMessage(aiMessage.id)
        } else if (chunk.type === 'tts') {
          console.log('[ChatInputBar] 收到TTS事件:', chunk)
          
          // 处理TTS音频播放
          if (chunk.success && chunk.audioUrl) {
            console.log('[ChatInputBar] 收到TTS音频URL:', chunk.audioUrl)
            
            // 保存音频URL到消息中，供后续点击播放
            chatStore.updateMessage(aiMessage.id, {
              audioUrl: chunk.audioUrl,
              voice: chunk.voice,
              languageType: chunk.languageType,
              isVoiceMessage: isVoiceMessageMode.value
            })

            // 下载音频并播放
            fetch(chunk.audioUrl)
              .then(response => response.blob())
              .then(audioBlob => {
                console.log('[ChatInputBar] TTS音频下载成功，现在同时显示文字和播放语音')

                // 🎯 关键改进：TTS下载完成后，同时显示完整文字内容
                if (enableTtsForThisMessage && pendingTextContent.value) {
                  console.log('[ChatInputBar] 显示暂存的完整文字内容:', {
                    messageId: aiMessage.id,
                    textLength: pendingTextContent.value.length,
                    textPreview: pendingTextContent.value.substring(0, 50) + '...'
                  })

                  // 更新消息：显示完整文字内容，并标记为语音消息
                  chatStore.updateMessage(aiMessage.id, {
                    content: pendingTextContent.value,
                    isVoiceMessage: true,
                    streaming: false  // 确保停止流式状态，光标位于文字末尾
                  })

                  // 清空暂存的文字内容
                  pendingTextContent.value = ''
                }

                // 🎯 关键改进：播放新音频前先停止当前正在播放的音频
                stopCurrentAudio()
                
                // 创建一个简单的播放器来播放音频
                const audioUrl = URL.createObjectURL(audioBlob)
                const audio = new Audio(audioUrl)
                audio.volume = 1.0
                
                // 设置为当前播放的音频
                currentPlayingAudio.value = audio

                // 监听音频元数据获取时长
                audio.addEventListener('loadedmetadata', () => {
                  const duration = Math.round(audio.duration) || 1
                  console.log('[ChatInputBar] TTS语音时长:', duration, '秒')

                  // 更新语音时长信息
                  chatStore.updateMessage(aiMessage.id, {
                    voiceDuration: duration
                  })
                })
                
                // 监听音频播放结束，清理状态
                audio.addEventListener('ended', () => {
                  console.log('[ChatInputBar] TTS音频播放完成')
                  if (currentPlayingAudio.value === audio) {
                    currentPlayingAudio.value = null
                  }
                  URL.revokeObjectURL(audioUrl)
                })

                // 监听音频播放错误，清理状态
                audio.addEventListener('error', () => {
                  console.error('[ChatInputBar] TTS音频播放出错')
                  if (currentPlayingAudio.value === audio) {
                    currentPlayingAudio.value = null
                  }
                  URL.revokeObjectURL(audioUrl)
                })

                return audio.play().then(() => {
                  console.log('[ChatInputBar] TTS音频开始播放')
                })
              })
              .catch(error => {
                console.error('[ChatInputBar] TTS音频播放失败:', error)
                message.warning('语音播放失败')

                // 如果音频播放失败，仍然要显示文字内容
                if (enableTtsForThisMessage && pendingTextContent.value) {
                  console.log('[ChatInputBar] 音频播放失败，但仍显示文字内容')
                  chatStore.updateMessage(aiMessage.id, {
                    content: pendingTextContent.value,
                    streaming: false
                  })
                  pendingTextContent.value = ''
                }
              })
          } else {
            console.warn('[ChatInputBar] TTS合成失败:', chunk.error)
            if (chunk.error) {
              message.warning(`语音合成失败: ${chunk.error}`)
            }

            // TTS合成失败时，显示文字内容作为备选方案
            if (enableTtsForThisMessage && pendingTextContent.value) {
              console.log('[ChatInputBar] TTS合成失败，显示文字内容作为备选方案')
              chatStore.updateMessage(aiMessage.id, {
                content: pendingTextContent.value,
                streaming: false
              })
              pendingTextContent.value = ''
            }
          }
        } else if (chunk.type === 'end') {
          console.log('[ChatInputBar] 收到结束信号')
          // 停止流式状态
          chatStore.updateMessage(aiMessage.id, {
            streaming: false
          })
        }
      },
      // onError - 处理错误
      (error: Error) => {
        console.error('[ChatInputBar] 消息发送失败:', error)
        message.error('消息发送失败: ' + error.message)
        
        // 移除失败的AI消息
        chatStore.removeMessage(aiMessage.id)
      },
      // onComplete - 完成回调
      () => {
        console.log('[ChatInputBar] 消息发送完成')
        
        // 如果还有暂存的文字内容未显示（比如没有TTS的情况），确保显示出来
        if (pendingTextContent.value) {
          console.log('[ChatInputBar] 完成时发现未显示的文字内容，立即显示')
          chatStore.updateMessage(aiMessage.id, {
            content: pendingTextContent.value,
            streaming: false
          })
          pendingTextContent.value = ''
        }

        // 确保停止流式状态
        chatStore.updateMessage(aiMessage.id, {
          streaming: false
        })
        
        // 重置终止状态
        isTerminated.value = false
        currentAIMessageId.value = null

        // 滚动到底部
        nextTick(() => {
          const chatContainer = document.querySelector('.chat-messages')
          if (chatContainer) {
            chatContainer.scrollTop = chatContainer.scrollHeight
          }
        })
      }
    )
    
  } catch (error) {
    console.error('[ChatInputBar] 发送消息异常:', error)
    message.error('消息发送失败')
  } finally {
    isSending.value = false
    // 确保在异常情况下也重置终止状态和暂存内容
    if (currentAIMessageId.value) {
      isTerminated.value = false
      currentAIMessageId.value = null
    }
    pendingTextContent.value = ''  // 清理暂存的文字内容
  }
}

// === 语音转文字功能（点击切换录音） ===
const toggleRecording = async () => {
  if (voiceMode.value === 'voiceRecording') {
    // 如果正在录音，停止录音
    await stopRecording()
  } else {
    // 如果没有录音，开始录音
    await startRecording()
  }
}

const startRecording = async () => {
  try {
    // 检查浏览器支持
    if (!isSpeechRecordingSupported()) {
      message.error('您的浏览器不支持语音录制功能')
      return
    }
    
    // 获取推荐的录音配置
    const recordingConfig = getRecommendedRecordingConfig()
    
    console.log('[ChatInputBar] 开始录音，配置:', recordingConfig)
    
    const stream = await navigator.mediaDevices.getUserMedia({ 
      audio: {
        sampleRate: speechConfig.recognition.sampleRate,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })
    
    mediaRecorder.value = new MediaRecorder(stream, {
      mimeType: recordingConfig.mimeType,
      audioBitsPerSecond: recordingConfig.audioBitsPerSecond
    })
    
    audioChunks.value = []
    
    mediaRecorder.value.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.value.push(event.data)
      }
    }
    
    mediaRecorder.value.onstop = async () => {
      const audioBlob = new Blob(audioChunks.value, { type: recordingConfig.mimeType })
      
      // 检查录音时长
      if (audioBlob.size < 1000) { // 小于1KB认为录音太短
        message.warning('录音时间太短，请重新录制')
        chatStore.setVoiceMode('text')
        return
      }
      
      console.log('[ChatInputBar] 录音完成，格式:', recordingConfig.format, '大小:', audioBlob.size, 'bytes')
      
      await processAudioBlob(audioBlob)
      
      // 停止所有音频轨道
      stream.getTracks().forEach(track => track.stop())
    }
    
    mediaRecorder.value.start()
    chatStore.setVoiceMode('voiceRecording')
    
    // 震动反馈（如果支持）
    if (speechConfig.ui.vibrationOnStart && navigator.vibrate) {
      navigator.vibrate(50)
    }
    
  } catch (error) {
    console.error('[ChatInputBar] 录音启动失败:', error)
    
    if (error instanceof Error) {
      if (error.name === 'NotAllowedError') {
        message.error('请允许访问麦克风权限')
      } else if (error.name === 'NotFoundError') {
        message.error('未找到可用的麦克风设备')
      } else {
        message.error('录音启动失败: ' + error.message)
      }
    } else {
      message.error('录音启动失败，请重试')
    }
    
    chatStore.setVoiceMode('text')
  }
}

const stopRecording = async () => {
  if (mediaRecorder.value && mediaRecorder.value.state === 'recording') {
    console.log('[ChatInputBar] 停止录音')
    mediaRecorder.value.stop()
    chatStore.setVoiceMode('voiceTranscribing')
    
    // 震动反馈（如果支持）
    if (speechConfig.ui.vibrationOnStop && navigator.vibrate) {
      navigator.vibrate(30)
    }
    
    console.log('[ChatInputBar] 录音已停止，开始处理音频')
  }
}

const processAudioBlob = async (audioBlob: Blob) => {
  try {
    console.log('[ChatInputBar] 开始处理音频文件，原始格式:', audioBlob.type, '大小:', audioBlob.size, 'bytes')
    
    // 将音频转换为WAV格式
    console.log('[ChatInputBar] 开始转换音频为WAV格式...')
    const wavBlob = await AudioConverter.toWav(audioBlob)
    console.log('[ChatInputBar] 音频转换完成，WAV大小:', wavBlob.size, 'bytes')
    
    // 调用真实的语音识别API，使用转换后的WAV文件
    const response = await SpeechRecognitionAPI.recognizeAudio(wavBlob, {
      model: speechConfig.recognition.model,
      format: speechConfig.recognition.format, // 'wav'
      sampleRate: speechConfig.recognition.sampleRate,
      punctuationPredictionEnabled: speechConfig.recognition.punctuationPredictionEnabled,
      semanticPunctuationEnabled: speechConfig.recognition.semanticPunctuationEnabled,
      maxSentenceSilence: speechConfig.recognition.maxSentenceSilence,
      languageHints: speechConfig.recognition.languageHints
    })
    
    console.log('[ChatInputBar] 语音识别成功:', response)
    
    // 使用工具类提取文本
    const recognizedText = SpeechRecognitionUtils.extractTextFromResponse(response)
    console.log('[ChatInputBar] 提取的文本:', recognizedText)
    
    // 验证识别结果是否有效
    if (!SpeechRecognitionUtils.isValidRecognitionResult(recognizedText)) {
      console.warn('[ChatInputBar] 识别结果无效或为空:', recognizedText)
      message.warning('未识别到有效语音内容，请重新录制')
      chatStore.setVoiceMode('text')
      return
    }
    
    // 将转换的文字填入输入框
    inputText.value = recognizedText
    chatStore.setVoiceMode('text')
    
    // 显示成功提示（如果配置启用）
    if (speechConfig.ui.showConfidence) {
      if (response.confidence !== undefined && response.confidence !== null && !isNaN(response.confidence)) {
        message.success(`语音识别成功，置信度: ${Math.round(response.confidence * 100)}%`)
      } else {
        message.success('语音识别成功')
      }
    }
    
    // T按钮的语音转文字功能：不自动发送，让用户编辑后再发送
    // 聚焦输入框，方便用户编辑
    await nextTick()
    inputRef.value?.focus()
    
    // 显示提示信息
    message.success('语音识别完成，请检查文字后发送')
    
  } catch (error) {
    console.error('[ChatInputBar] 语音转文字失败:', error)
    
    // 处理不同类型的错误
    if (error instanceof SpeechRecognitionError) {
      message.error(`语音识别失败: ${error.message}`)
    } else if (error instanceof Error) {
      // 检查是否是音频转换错误
      if (error.message.includes('decodeAudioData') || error.message.includes('AudioContext')) {
        message.error('音频格式转换失败，请重新录制')
      } else {
        message.error(`语音识别失败: ${error.message}`)
      }
    } else {
      message.error('语音识别失败，请重试')
    }
    
    chatStore.setVoiceMode('text')
  }
}

// === 语音消息功能 ===
const toggleVoiceMessageMode = () => {
  console.log('[ChatInputBar] 切换语音消息模式:', !isVoiceMessageMode.value)
  isVoiceMessageMode.value = !isVoiceMessageMode.value
  
  // 如果切换到文字模式，清理语音消息状态
  if (!isVoiceMessageMode.value) {
    isRecordingVoiceMessage.value = false
  }
}

const startVoiceMessageRecording = async () => {
  if (!props.currentCharacterId) {
    message.warning('请先选择角色')
    return
  }
  
  try {
    console.log('[ChatInputBar] 开始录制语音消息')
    
    // 检查浏览器支持
    if (!isSpeechRecordingSupported()) {
      message.error('您的浏览器不支持语音录制功能')
      return
    }
    
    // 获取推荐的录音配置
    const recordingConfig = getRecommendedRecordingConfig()
    
    const stream = await navigator.mediaDevices.getUserMedia({ 
      audio: {
        sampleRate: speechConfig.recognition.sampleRate,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })
    
    mediaRecorder.value = new MediaRecorder(stream, {
      mimeType: recordingConfig.mimeType,
      audioBitsPerSecond: recordingConfig.audioBitsPerSecond
    })
    
    audioChunks.value = []
    
    mediaRecorder.value.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.value.push(event.data)
      }
    }
    
    mediaRecorder.value.onstop = async () => {
      const audioBlob = new Blob(audioChunks.value, { type: recordingConfig.mimeType })
      
      // 检查录音时长
      if (audioBlob.size < 1000) { // 小于1KB认为录音太短
        message.warning('录音时间太短，请重新录制')
        return
      }
      
      console.log('[ChatInputBar] 语音消息录制完成，开始发送')
      await processVoiceMessage(audioBlob)
      
      // 停止所有音频轨道
      stream.getTracks().forEach(track => track.stop())
    }
    
    mediaRecorder.value.start()
    isRecordingVoiceMessage.value = true
    voiceMessageStartTime.value = Date.now()  // 记录开始时间
    
    // 震动反馈（如果支持）
    if (speechConfig.ui.vibrationOnStart && navigator.vibrate) {
      navigator.vibrate(50)
    }
    
  } catch (error) {
    console.error('[ChatInputBar] 语音消息录制启动失败:', error)
    
    if (error instanceof Error) {
      if (error.name === 'NotAllowedError') {
        message.error('请允许访问麦克风权限')
      } else if (error.name === 'NotFoundError') {
        message.error('未找到可用的麦克风设备')
      } else {
        message.error('录音启动失败: ' + error.message)
      }
    } else {
      message.error('录音启动失败，请重试')
    }
    
    isRecordingVoiceMessage.value = false
  }
}

const stopVoiceMessageRecording = () => {
  if (mediaRecorder.value && mediaRecorder.value.state === 'recording') {
    console.log('[ChatInputBar] 停止录制语音消息')
    
    // 计算录音时长
    const endTime = Date.now()
    const duration = Math.round((endTime - voiceMessageStartTime.value) / 1000)
    voiceMessageDuration.value = Math.max(1, duration)  // 最少1秒
    
    console.log('[ChatInputBar] 语音消息录制时长:', voiceMessageDuration.value, '秒')
    
    mediaRecorder.value.stop()
    isRecordingVoiceMessage.value = false
    
    // 震动反馈（如果支持）
    if (speechConfig.ui.vibrationOnStop && navigator.vibrate) {
      navigator.vibrate(30)
    }
  }
}

const processVoiceMessage = async (audioBlob: Blob) => {
  try {
    console.log('[ChatInputBar] 开始处理语音消息')
    
    // 发送新语音消息前，先停止上一条正在播放的语音
    stopAllAudio()

    // 将音频转换为WAV格式
    const wavBlob = await AudioConverter.toWav(audioBlob)
    console.log('[ChatInputBar] 语音消息转换完成，WAV大小:', wavBlob.size, 'bytes')
    
    // 先进行语音识别，获取文本内容
    const response = await SpeechRecognitionAPI.recognizeAudio(wavBlob, {
      model: speechConfig.recognition.model,
      format: speechConfig.recognition.format,
      sampleRate: speechConfig.recognition.sampleRate,
      punctuationPredictionEnabled: speechConfig.recognition.punctuationPredictionEnabled,
      semanticPunctuationEnabled: speechConfig.recognition.semanticPunctuationEnabled,
      maxSentenceSilence: speechConfig.recognition.maxSentenceSilence,
      languageHints: speechConfig.recognition.languageHints
    })
    
    console.log('[ChatInputBar] 语音消息识别成功:', response)
    
    // 使用工具类提取文本
    const recognizedText = SpeechRecognitionUtils.extractTextFromResponse(response)
    console.log('[ChatInputBar] 语音消息提取的文本:', recognizedText)
    
    // 验证识别结果是否有效
    if (!SpeechRecognitionUtils.isValidRecognitionResult(recognizedText)) {
      console.warn('[ChatInputBar] 语音消息识别结果无效或为空:', recognizedText)
      message.warning('未识别到有效语音内容，请重新录制')
      return
    }
    
    // 发送语音消息：显示为语音信息标识，包含时长
    const voiceMessageContent = `🎵 ${voiceMessageDuration.value}"`
    inputText.value = voiceMessageContent
    
    // 添加用户语音消息到聊天记录
    const userMessage = chatStore.addMessage({
      characterId: chatStore.currentCharacterId,
      content: voiceMessageContent,
      isUser: true,
      isVoiceMessage: true,
      voiceDuration: voiceMessageDuration.value
    })
    
    // 调用后端API更新语音时长
    try {
      const updateRequest: UpdateVoiceDurationRequest = {
        messageContent: voiceMessageContent,
        voiceDuration: voiceMessageDuration.value,
        characterId: chatStore.currentCharacterId
      }
      
      console.log('[ChatInputBar] 更新语音时长到后端:', updateRequest)
      const updateResponse = await updateVoiceDuration(updateRequest)
      
      if (updateResponse.success) {
        console.log('[ChatInputBar] 语音时长更新成功:', updateResponse)
      } else {
        console.warn('[ChatInputBar] 语音时长更新失败:', updateResponse.message)
      }
    } catch (error) {
      console.error('[ChatInputBar] 更新语音时长失败:', error)
      // 不影响主流程，只记录错误
    }
    
    // 清空输入框
    inputText.value = ''
    
    // 实际发送识别的文本给AI（但不显示在界面上）
    const requestData: SendMessageRequest = {
      characterId: chatStore.currentCharacterId,
      message: recognizedText,  // 发送实际的文字内容给AI
      enableTts: true,  // 语音消息模式始终启用TTS
      enableRag: chatStore.enableRag,  // ✅ 传递RAG开关状态
      languageType: "Chinese"
    }
    
    console.log('[ChatInputBar] 发送语音消息到后端:', requestData)
    
    // 创建AI回复消息（流式）
    const aiMessage = chatStore.addMessage({
      characterId: chatStore.currentCharacterId,
      content: '',
      isUser: false,
      streaming: true
    })
    
    // 保存当前AI消息ID，用于终止控制
    currentAIMessageId.value = aiMessage.id
    isTerminated.value = false  // 重置终止状态
    pendingTextContent.value = ''  // 重置暂存的文字内容

    // 发送流式请求
    await sendStreamMessage(
      requestData,
      // onMessage - 处理流式数据
      (chunk: StreamResponse) => {
        console.log('[ChatInputBar] 收到语音消息流式数据:', chunk)
        
        // 如果终止了，忽略消息和TTS事件
        if (isTerminated.value && (chunk.type === 'message' || chunk.type === 'tts')) {
          console.log('[ChatInputBar] 语音消息已终止，忽略事件:', chunk.type)
          return
        }

        if (chunk.type === 'message' && chunk.content) {
          // 语音消息模式：暂存文字内容，等语音下载完成后再显示
          pendingTextContent.value += chunk.content

          // 显示"正在生成语音..."的提示
          chatStore.updateMessage(aiMessage.id, {
            content: '🎵 正在生成语音...',
            isVoiceMessage: true
          })

          console.log('[ChatInputBar] 语音消息模式：暂存文字内容:', {
            messageId: aiMessage.id,
            chunkContent: chunk.content,
            totalPendingLength: pendingTextContent.value.length
          })
        } else if (chunk.type === 'tts') {
          console.log('[ChatInputBar] 收到语音消息TTS事件:', chunk)
          
          // 处理TTS音频播放
          if (chunk.success && chunk.audioUrl) {
            console.log('[ChatInputBar] 收到语音消息TTS音频URL:', chunk.audioUrl)
            
            // 保存音频URL到消息中，供后续点击播放
            chatStore.updateMessage(aiMessage.id, {
              audioUrl: chunk.audioUrl,
              voice: chunk.voice,
              languageType: chunk.languageType,
              isVoiceMessage: true
            })

            // 下载并播放音频
            fetch(chunk.audioUrl)
              .then(response => response.blob())
              .then(audioBlob => {
                console.log('[ChatInputBar] 语音消息TTS音频下载成功，现在同时显示文字和播放语音')

                // 🎯 关键改进：TTS下载完成后，同时显示完整文字内容
                if (pendingTextContent.value) {
                  console.log('[ChatInputBar] 语音消息：显示暂存的完整文字内容:', {
                    messageId: aiMessage.id,
                    textLength: pendingTextContent.value.length,
                    textPreview: pendingTextContent.value.substring(0, 50) + '...'
                  })

                  // 更新消息：显示完整文字内容，并标记为语音消息
                  chatStore.updateMessage(aiMessage.id, {
                    content: pendingTextContent.value,
                    isVoiceMessage: true,
                    streaming: false  // 确保停止流式状态，光标位于文字末尾
                  })

                  // 清空暂存的文字内容
                  pendingTextContent.value = ''
                }

                // 🎯 关键改进：播放新音频前先停止当前正在播放的音频
                stopCurrentAudio()
                
                const audioUrl = URL.createObjectURL(audioBlob)
                const audio = new Audio(audioUrl)
                audio.volume = 1.0
                
                // 设置为当前播放的音频
                currentPlayingAudio.value = audio

                // 监听音频元数据加载，获取时长
                audio.addEventListener('loadedmetadata', () => {
                  const duration = Math.round(audio.duration) || 1
                  console.log('[ChatInputBar] AI语音消息时长:', duration, '秒')
                  
                  // 更新语音时长信息
                  chatStore.updateMessage(aiMessage.id, {
                    voiceDuration: duration
                  })
                })
                
                // 监听音频播放结束，清理状态
                audio.addEventListener('ended', () => {
                  console.log('[ChatInputBar] 语音消息TTS音频播放完成')
                  if (currentPlayingAudio.value === audio) {
                    currentPlayingAudio.value = null
                  }
                  URL.revokeObjectURL(audioUrl)
                })

                // 监听音频播放错误，清理状态
                audio.addEventListener('error', () => {
                  console.error('[ChatInputBar] 语音消息TTS音频播放出错')
                  if (currentPlayingAudio.value === audio) {
                    currentPlayingAudio.value = null
                  }
                  URL.revokeObjectURL(audioUrl)
                })

                return audio.play().then(() => {
                  console.log('[ChatInputBar] 语音消息TTS音频开始播放')
                })
              })
              .catch(error => {
                console.error('[ChatInputBar] 语音消息TTS音频播放失败:', error)
                message.warning('语音播放失败')
              })
          }
        } else if (chunk.type === 'end') {
          console.log('[ChatInputBar] 语音消息流式响应结束')
          chatStore.updateMessage(aiMessage.id, {
            streaming: false
          })
        }
      },
      // onError
      (error: Error) => {
        console.error('[ChatInputBar] 语音消息发送失败:', error)
        message.error('语音消息发送失败: ' + error.message)
        chatStore.removeMessage(aiMessage.id)
      },
      // onComplete
      () => {
        console.log('[ChatInputBar] 语音消息发送完成')
        chatStore.updateMessage(aiMessage.id, {
          streaming: false
        })

        // 重置终止状态
        isTerminated.value = false
        currentAIMessageId.value = null
      }
    )
    
    message.success('语音消息发送成功')
    
  } catch (error) {
    console.error('[ChatInputBar] 语音消息处理失败:', error)
    
    // 处理不同类型的错误
    if (error instanceof SpeechRecognitionError) {
      message.error(`语音识别失败: ${error.message}`)
    } else if (error instanceof Error) {
      if (error.message.includes('decodeAudioData') || error.message.includes('AudioContext')) {
        message.error('音频格式转换失败，请重新录制')
      } else {
        message.error(`语音消息发送失败: ${error.message}`)
      }
    } else {
      message.error('语音消息发送失败，请重试')
    }
  }
}

</script>

<style scoped>
.chat-input-bar {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.input-container {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border-radius: 24px;
  border: 1px solid var(--gray-200);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
  min-height: 56px;
}

.input-container:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  border-color: var(--primary-300);
}

.input-container:focus-within {
  box-shadow: 0 6px 20px rgba(22, 119, 255, 0.15);
  border-color: var(--primary-500);
}

.voice-to-text-btn {
  flex-shrink: 0;
  width: 40px !important;
  height: 40px !important;
  transition: all 0.2s ease;
}

.voice-to-text-btn:active {
  transform: scale(0.95);
}

.text-icon {
  font-size: 18px;
  font-weight: bold;
  color: currentColor;
}

.microphone-btn {
  flex-shrink: 0;
  width: 40px !important;
  height: 40px !important;
  transition: all 0.2s ease;
}

.microphone-btn:active {
  transform: scale(0.95);
}

.microphone-btn.n-button--primary {
  animation: microphone-pulse 1.5s ease-in-out infinite;
}

.unified-input-area {
  flex: 1;
  min-height: 40px;
  display: flex;
  align-items: center;
  background: var(--gray-50);
  border-radius: 20px;
  padding: 8px 16px;
  margin: 0 4px;
}

.status-display {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: var(--primary-color);
}

.voice-call-status {
  color: var(--success-color);
}

.voice-call-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: var(--success-color);
  border-radius: 50%;
  animation: pulse-dot 1.5s ease-in-out infinite;
}

.voice-message-container {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voice-message-btn {
  width: 100% !important;
  height: 40px !important;
  border-radius: 20px !important;
  font-size: 14px !important;
  transition: all 0.2s ease !important;
}

.voice-message-btn.n-button--primary {
  animation: voice-message-pulse 1.5s ease-in-out infinite;
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-input {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  width: 100%;
}

.message-input :deep(.n-input__input-el) {
  font-size: 16px !important;
  line-height: 1.5 !important;
  padding: 8px 0 !important;
  min-height: 24px !important;
}

.message-input :deep(.n-input__textarea-el) {
  font-size: 16px !important;
  line-height: 1.5 !important;
  padding: 8px 0 !important;
  min-height: 24px !important;
  resize: none !important;
}

.send-btn {
  flex-shrink: 0;
  width: 40px !important;
  height: 40px !important;
  transition: all 0.2s ease;
}

.send-btn:not(:disabled):hover {
  transform: scale(1.05);
}

.send-btn:not(:disabled):active {
  transform: scale(0.95);
}


/* 移动端适配 */
@media (max-width: 768px) {
  .chat-input-bar {
    max-width: 100%;
    margin: 0;
  }
  
  .input-container {
    padding: 12px 16px;
    gap: 12px;
    border-radius: 20px;
    min-height: 48px;
  }
  
  .voice-btn,
  .send-btn {
    width: 36px !important;
    height: 36px !important;
  }
  
  .voice-wave-container {
    min-height: 36px;
    margin: 0 2px;
    padding: 6px 12px;
  }
  
  .text-input-container {
    margin: 0 2px;
  }
  
  .message-input :deep(.n-input__textarea-el) {
    font-size: 16px !important;
    padding: 6px 0 !important;
  }
}

/* 录音状态动画 */
.voice-to-text-btn.n-button--primary {
  animation: recording-pulse 1.5s ease-in-out infinite;
}


@keyframes recording-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(22, 119, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(22, 119, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(22, 119, 255, 0);
  }
}

@keyframes microphone-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(22, 119, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(22, 119, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(22, 119, 255, 0);
  }
}

@keyframes pulse-dot {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

@keyframes voice-message-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(22, 119, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(22, 119, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(22, 119, 255, 0);
  }
}

/* RAG开关样式 */
.rag-toggle-container {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 8px;
  border-radius: 4px;
}

</style>

