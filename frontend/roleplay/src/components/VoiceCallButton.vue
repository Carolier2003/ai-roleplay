<template>
  <div class="voice-call-container">
    <!-- 按钮区域 - 固定布局 -->
    <div class="button-area">
      <button
        ref="voiceButton"
        class="voice-call-button"
        :class="{ 
          'recording': isRecording,
          'processing': isProcessing,
          'disabled': isDisabled
        }"
        @mousedown="startRecording"
        @mouseup="stopRecording"
        @mouseleave="stopRecording"
        @touchstart="startRecording"
        @touchend="stopRecording"
        :disabled="isDisabled"
      >
        <div class="button-content">
          <div class="icon">
            <svg v-if="!isRecording && !isProcessing" viewBox="0 0 24 24" width="24" height="24">
              <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" fill="currentColor"/>
              <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" fill="currentColor"/>
            </svg>
            <div v-else-if="isRecording" class="recording-indicator">
              <div class="pulse"></div>
            </div>
            <div v-else class="processing-indicator">
              <div class="spinner"></div>
            </div>
          </div>
          <span class="button-text">
            {{ buttonText }}
          </span>
        </div>
      </button>
    </div>

    <!-- 信息显示区域 - 独立布局，不影响按钮位置 -->
    <div class="info-area">
      <!-- 状态显示 -->
      <div v-if="statusText" class="status-text">
        {{ statusText }}
      </div>

      <!-- 识别文本显示 -->
      <div v-if="recognizedText" class="recognized-text">
        <div class="text-label">识别结果：</div>
        <div class="text-content">{{ recognizedText }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRecorder } from '../composables/useRecorder'
import { useASRStream } from '../composables/useASRStream'
import { useChatStream } from '../composables/useChatStream'
import { useTTSPlayer } from '../composables/useTTSPlayer'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'

// Props
interface Props {
  characterId?: number
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  characterId: 1,
  disabled: false
})

// Refs
const voiceButton = ref<HTMLButtonElement>()

// 状态管理
const isRecording = ref(false)
const isProcessing = ref(false)
const recognizedText = ref('')
const statusText = ref('')

// Composables
const recorder = useRecorder()
const asrStream = useASRStream()
const chatStream = useChatStream()
const ttsPlayer = useTTSPlayer()
const chatStore = useChatStore()
const authStore = useAuthStore()

// 计算属性
const isDisabled = computed(() => props.disabled || isProcessing.value)

const buttonText = computed(() => {
  if (isRecording.value) return '松手结束'
  if (isProcessing.value) return '处理中...'
  return '按住说话'
})

// 空格键监听
const handleKeyDown = (event: KeyboardEvent) => {
  if (event.code === 'Space' && !event.repeat && !isDisabled.value) {
    event.preventDefault()
    startRecording()
  }
}

const handleKeyUp = (event: KeyboardEvent) => {
  if (event.code === 'Space') {
    event.preventDefault()
    stopRecording()
  }
}

// 开始录音
const startRecording = async () => {
  if (isDisabled.value || isRecording.value) return

  console.log('[VoiceCallButton] 开始录音')
  
  try {
    isRecording.value = true
    statusText.value = '正在录音...'
    recognizedText.value = ''

    // 1. 开始录音
    await recorder.startRecording()
    
    // 2. 开始流式ASR
    await asrStream.startStreaming({
      onPartialResult: (text: string) => {
        console.log('[VoiceCallButton] ASR部分结果:', text)
        recognizedText.value = text
      },
      onFinalResult: (text: string) => {
        console.log('[VoiceCallButton] ASR最终结果:', text)
        recognizedText.value = text
      }
    })

    // 3. 开始发送音频数据到ASR
    recorder.onAudioData = (audioData: ArrayBuffer) => {
      asrStream.sendAudioData(audioData)
    }

  } catch (error) {
    console.error('[VoiceCallButton] 开始录音失败:', error)
    statusText.value = '录音失败'
    isRecording.value = false
  }
}

// 停止录音
const stopRecording = async () => {
  if (!isRecording.value) return

  console.log('[VoiceCallButton] 停止录音')
  
  try {
    isRecording.value = false
    isProcessing.value = true
    statusText.value = '处理中...'

    // 1. 停止录音
    await recorder.stopRecording()
    
    // 2. 停止ASR流
    const finalText = await asrStream.stopStreaming()
    
    if (!finalText || finalText.trim().length === 0) {
      statusText.value = '未识别到语音'
      isProcessing.value = false
      return
    }

    console.log('[VoiceCallButton] 最终识别文本:', finalText)
    recognizedText.value = finalText

    // 3. 立即将用户消息添加到聊天界面
    const actualCharacterId = chatStore.currentCharacterId || props.characterId
    console.log('[VoiceCallButton] 添加用户消息到聊天界面', {
      propsCharacterId: props.characterId,
      currentCharacterId: chatStore.currentCharacterId,
      actualCharacterId: actualCharacterId,
      content: finalText,
      isUser: true
    })
    const userMessage = chatStore.addMessage({
      characterId: actualCharacterId,
      content: finalText,
      isUser: true
    })

    // 4. 发送到聊天流并显示AI回复
    statusText.value = 'AI思考中...'
    await startChatAndTTS(finalText)

  } catch (error) {
    console.error('[VoiceCallButton] 停止录音失败:', error)
    statusText.value = '处理失败'
  } finally {
    isProcessing.value = false
  }
}

// 开始聊天和TTS播放
const startChatAndTTS = async (userMessage: string) => {
  try {
    let currentSentence = ''
    let isFirstSentence = true
    let isCompleted = false // 防止重复完成处理
    const processedSentences = new Set<string>() // 去重集合
    
    // 文字缓冲区
    let fullTextBuffer = '' // 完整的文字缓冲区

    // 创建AI回复消息（流式）
    const actualCharacterId = chatStore.currentCharacterId || props.characterId
    console.log('[VoiceCallButton] 创建AI回复消息', {
      propsCharacterId: props.characterId,
      currentCharacterId: chatStore.currentCharacterId,
      actualCharacterId: actualCharacterId,
      content: '',
      isUser: false,
      streaming: true
    })
    const aiMessage = chatStore.addMessage({
      characterId: actualCharacterId,
      content: '',
      isUser: false,
      streaming: true
    })


    // 开始流式聊天
    await chatStream.startStreaming({
      characterId: actualCharacterId,
      message: userMessage,
      onMessage: (content: string) => {
        console.log('[VoiceCallButton] 收到聊天内容:', content)
        
        // 如果已经完成，忽略后续消息
        if (isCompleted) {
          console.log('[VoiceCallButton] 流已完成，忽略后续消息:', content)
          return
        }

        // 将新内容添加到缓冲区并立即显示
        fullTextBuffer += content
        chatStore.updateMessage(aiMessage.id, {
          content: fullTextBuffer
        })
        
        currentSentence += content
        
        // SSE 断句阈值 - 检查是否包含句子结束符
        const sentenceEnders = ['。', '！', '？', '.', '!', '?']
        const hasSentenceEnd = sentenceEnders.some(ender => content.includes(ender))
        
        if (hasSentenceEnd) {
          // 提取完整句子 - 改进的断句逻辑
          const parts = currentSentence.split(/([。！？.!?])/)
          let completeSentences: string[] = []
          
          // 重新组合句子和标点
          for (let i = 0; i < parts.length - 1; i += 2) {
            if (parts[i] && parts[i + 1]) {
              const sentence = (parts[i] + parts[i + 1]).trim()
              if (sentence && !processedSentences.has(sentence)) {
                completeSentences.push(sentence)
                processedSentences.add(sentence) // 添加到去重集合
              }
            }
          }
          
          // 处理完整句子
          for (const sentence of completeSentences) {
            console.log('[VoiceCallButton] 检测到完整句子:', sentence)
            
            // 立即开始TTS
            ttsPlayer.addToQueue(sentence, props.characterId, isFirstSentence)
            
            if (isFirstSentence) {
              statusText.value = '开始播放...'
              isFirstSentence = false
            }
          }
          
          // 保留未完成的部分
          const lastPart = parts[parts.length - 1]
          currentSentence = lastPart ? lastPart.trim() : ''
        }
      },
      onComplete: () => {
        console.log('[VoiceCallButton] 聊天流结束')
        
        // 防止重复完成处理
        if (isCompleted) {
          console.log('[VoiceCallButton] 已经处理过完成事件，跳过')
          return
        }
        isCompleted = true
        
        // 确保显示完整内容
        chatStore.updateMessage(aiMessage.id, {
          content: fullTextBuffer
        })
        
        // 停止AI消息的流式状态
        chatStore.updateMessage(aiMessage.id, {
          streaming: false
        })
        
        // 处理剩余文本
        if (currentSentence.trim() && !processedSentences.has(currentSentence.trim())) {
          console.log('[VoiceCallButton] 处理剩余文本:', currentSentence.trim())
          processedSentences.add(currentSentence.trim())
          ttsPlayer.addToQueue(currentSentence.trim(), actualCharacterId, isFirstSentence)
        }
        
        statusText.value = '播放中...'
      },
      onError: (error: string) => {
        console.error('[VoiceCallButton] 聊天流错误:', error)
        statusText.value = '聊天失败'
        isCompleted = true // 错误时也标记为完成
        
        
        // 移除失败的AI消息
        chatStore.removeMessage(aiMessage.id)
      }
    })

  } catch (error) {
    console.error('[VoiceCallButton] 聊天和TTS失败:', error)
    statusText.value = '处理失败'
  }
}

// 生命周期
onMounted(() => {
  // 添加键盘监听
  document.addEventListener('keydown', handleKeyDown)
  document.addEventListener('keyup', handleKeyUp)
  
  // TTS播放完成回调
  ttsPlayer.onQueueEmpty = async () => {
    console.log('[VoiceCallButton] TTS播放队列清空')
    statusText.value = ''
    recognizedText.value = ''
    
    // 语音通话完成后刷新聊天内容
    console.log('[VoiceCallButton] 语音通话完成，开始刷新聊天内容')
    setTimeout(async () => {
      try {
        if (chatStore.currentCharacterId && authStore.isLoggedIn) {
          console.log('[VoiceCallButton] 重新加载聊天历史记录')
          await chatStore.loadMessages(chatStore.currentCharacterId)
          
          // 滚动到底部显示最新消息
          nextTick(() => {
            const chatContainer = document.querySelector('.messages-container')
            if (chatContainer) {
              chatContainer.scrollTop = chatContainer.scrollHeight
            }
          })
        }
      } catch (error) {
        console.error('[VoiceCallButton] 刷新聊天内容失败:', error)
      }
    }, 500) // 延迟0.5秒确保TTS播放完全结束
  }
})

onUnmounted(() => {
  // 清理资源
  document.removeEventListener('keydown', handleKeyDown)
  document.removeEventListener('keyup', handleKeyUp)
  
  // 停止所有流
  recorder.cleanup()
  asrStream.cleanup()
  chatStream.cleanup()
  ttsPlayer.cleanup()
})
</script>

<style scoped>
.voice-call-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  position: relative; /* 为绝对定位的子元素提供上下文 */
  min-height: 120px; /* 确保有足够空间显示信息区域 */
}

/* 按钮区域 - 绝对固定位置，防止布局变化 */
.button-area {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 80px; /* 固定高度，不使用min-height */
  margin-bottom: 12px;
  position: relative; /* 为按钮提供定位上下文 */
}

/* 信息显示区域 - 绝对定位，不影响按钮位置 */
.info-area {
  position: absolute;
  top: 100%; /* 位于按钮区域下方 */
  left: 50%;
  transform: translateX(-50%); /* 水平居中 */
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 100%; /* 确保有足够宽度显示文本 */
  z-index: 1; /* 确保在其他内容之上 */
}

.voice-call-button {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  user-select: none;
  -webkit-user-select: none;
  outline: none;
}

.voice-call-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.voice-call-button:active:not(:disabled) {
  transform: translateY(0);
}

.voice-call-button.recording {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
  animation: pulse 1.5s infinite;
}

.voice-call-button.processing {
  background: linear-gradient(135deg, #feca57 0%, #ff9ff3 100%);
  cursor: not-allowed;
}

.voice-call-button.disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.button-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.button-text {
  font-size: 10px;
  font-weight: 500;
  text-align: center;
  line-height: 1.2;
}

.recording-indicator {
  width: 24px;
  height: 24px;
  position: relative;
}

.pulse {
  width: 12px;
  height: 12px;
  background: white;
  border-radius: 50%;
  margin: 6px auto;
  animation: recording-pulse 1s infinite;
}

.processing-indicator {
  width: 24px;
  height: 24px;
  position: relative;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  margin: 4px auto;
  animation: spin 1s linear infinite;
}

.status-text {
  font-size: 12px;
  color: #666;
  text-align: center;
  min-height: 16px;
}

.recognized-text {
  max-width: 300px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 12px;
}

.text-label {
  color: #666;
  margin-bottom: 4px;
}

.text-content {
  color: #333;
  line-height: 1.4;
}

/* 动画 */
@keyframes pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 6px 20px rgba(255, 107, 107, 0.6);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
  }
}

@keyframes recording-pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 768px) {
  .voice-call-button {
    width: 70px;
    height: 70px;
  }
  
  .recognized-text {
    max-width: 250px;
  }
}
</style>
