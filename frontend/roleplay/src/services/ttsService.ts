/**
 * TTS语音合成服务
 * 提供文本转语音功能和音频播放管理
 */

import { ref, reactive } from 'vue'

// TTS请求接口
export interface TtsSynthesisRequest {
  text: string
  voice?: string
  languageType?: string
  model?: string
  characterId?: number
  stream?: boolean
  saveToLocal?: boolean
  fileName?: string
}

// TTS响应接口
export interface TtsSynthesisResponse {
  success: boolean
  audioUrl?: string
  duration?: number
  voice?: string
  languageType?: string
  characterCount?: number
  estimatedCost?: number
  processingTime?: number
  errorMessage?: string
}

// 音频信息接口
export interface AudioInfo {
  audioUrl?: string
  duration?: number
  voice?: string
  languageType?: string
  success?: boolean
  errorMessage?: string
  characterCount?: number
  estimatedCost?: number
  processingTime?: number
}

// 流式TTS响应接口
export interface StreamingTtsResponse {
  type: 'chunk' | 'final' | 'error'
  audioData?: string
  isFinished?: boolean
  sequenceNumber?: number
  completeAudioUrl?: string
  errorMessage?: string
  sessionId?: string
}

// 音色信息接口
export interface VoiceInfo {
  name: string
  description: string
  languages: string[]
}

// TTS服务状态
export const ttsState = reactive({
  isPlaying: false,
  currentAudio: null as HTMLAudioElement | null,
  playbackProgress: 0,
  volume: 1.0,
  isLoading: false,
  error: null as string | null,
  supportedVoices: {} as Record<string, VoiceInfo>,
  activeStreamingSessions: new Map<string, EventSource>()
})

export class TtsService {
  private baseUrl = '/api/tts'
  private audioElement: HTMLAudioElement | null = null

  /**
   * 标准语音合成
   */
  async synthesizeText(request: TtsSynthesisRequest): Promise<TtsSynthesisResponse> {
    try {
      ttsState.isLoading = true
      ttsState.error = null

      const response = await fetch(`${this.baseUrl}/synthesize`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(request)
      })

      if (!response.ok) {
        throw new Error(`TTS请求失败: ${response.status}`)
      }

      const result = await response.json()
      
      if (result.code !== 200) {
        throw new Error(result.message || 'TTS合成失败')
      }

      return result.data as TtsSynthesisResponse

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '未知错误'
      ttsState.error = errorMessage
      console.error('TTS合成失败:', error)
      
      return {
        success: false,
        errorMessage: errorMessage
      }
    } finally {
      ttsState.isLoading = false
    }
  }

  /**
   * 角色语音合成
   */
  async synthesizeForCharacter(characterId: number, text: string, languageType = 'Chinese'): Promise<TtsSynthesisResponse> {
    try {
      ttsState.isLoading = true
      ttsState.error = null

      const params = new URLSearchParams({
        text: text,
        languageType: languageType
      })

      const response = await fetch(`${this.baseUrl}/synthesize/character/${characterId}?${params}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      })

      if (!response.ok) {
        throw new Error(`角色TTS请求失败: ${response.status}`)
      }

      const result = await response.json()
      return result.data as TtsSynthesisResponse

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '角色语音合成失败'
      ttsState.error = errorMessage
      console.error('角色TTS合成失败:', error)
      
      return {
        success: false,
        errorMessage: errorMessage
      }
    } finally {
      ttsState.isLoading = false
    }
  }

  /**
   * 流式语音合成
   */
  async streamingSynthesize(request: TtsSynthesisRequest, 
                           onChunk?: (chunk: StreamingTtsResponse) => void,
                           onComplete?: (final: StreamingTtsResponse) => void,
                           onError?: (error: string) => void): Promise<string> {
    return new Promise((resolve, reject) => {
      try {
        // 创建SSE连接
        const eventSource = new EventSource(`${this.baseUrl}/synthesize/stream`)
        const sessionId = `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
        
        // 存储会话
        ttsState.activeStreamingSessions.set(sessionId, eventSource)

        // 发送合成请求
        fetch(`${this.baseUrl}/synthesize/stream`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.getAuthToken()}`
          },
          body: JSON.stringify({ ...request, stream: true })
        }).catch(error => {
          console.error('发送流式TTS请求失败:', error)
          onError?.(error.message)
          reject(error)
        })

        // 监听音频数据块
        eventSource.addEventListener('audio-chunk', (event) => {
          try {
            const chunk: StreamingTtsResponse = JSON.parse(event.data)
            onChunk?.(chunk)
            
            // 播放音频数据块
            if (chunk.audioData) {
              this.playAudioChunk(chunk.audioData)
            }
          } catch (error) {
            console.error('处理音频数据块失败:', error)
          }
        })

        // 监听合成完成
        eventSource.addEventListener('synthesis-complete', (event) => {
          try {
            const final: StreamingTtsResponse = JSON.parse(event.data)
            onComplete?.(final)
            
            // 清理会话
            ttsState.activeStreamingSessions.delete(sessionId)
            eventSource.close()
            
            resolve(final.completeAudioUrl || sessionId)
          } catch (error) {
            console.error('处理合成完成事件失败:', error)
            reject(error)
          }
        })

        // 监听错误
        eventSource.addEventListener('error', (event) => {
          try {
            const errorData = JSON.parse((event as any).data)
            const errorMessage = errorData.errorMessage || '流式TTS合成失败'
            
            onError?.(errorMessage)
            ttsState.error = errorMessage
            
            // 清理会话
            ttsState.activeStreamingSessions.delete(sessionId)
            eventSource.close()
            
            reject(new Error(errorMessage))
          } catch (error) {
            const errorMessage = '流式TTS连接错误'
            onError?.(errorMessage)
            ttsState.error = errorMessage
            
            ttsState.activeStreamingSessions.delete(sessionId)
            eventSource.close()
            
            reject(new Error(errorMessage))
          }
        })

        // 设置超时
        setTimeout(() => {
          if (ttsState.activeStreamingSessions.has(sessionId)) {
            ttsState.activeStreamingSessions.delete(sessionId)
            eventSource.close()
            reject(new Error('流式TTS合成超时'))
          }
        }, 60000) // 60秒超时

      } catch (error) {
        console.error('创建流式TTS连接失败:', error)
        reject(error)
      }
    })
  }

  /**
   * 播放音频
   */
  async playAudio(audioUrl: string): Promise<void> {
    try {
      // 停止当前播放
      this.stopAudio()

      // 创建新的音频元素
      this.audioElement = new Audio(audioUrl)
      ttsState.currentAudio = this.audioElement

      // 设置音频属性
      this.audioElement.volume = ttsState.volume
      this.audioElement.preload = 'auto'

      // 添加事件监听器
      this.setupAudioEventListeners()

      // 开始播放
      await this.audioElement.play()
      ttsState.isPlaying = true

    } catch (error) {
      console.error('音频播放失败:', error)
      ttsState.error = error instanceof Error ? error.message : '音频播放失败'
      throw error
    }
  }

  /**
   * 播放音频数据块（流式）
   */
  private playAudioChunk(base64AudioData: string): void {
    try {
      // 将Base64转换为Blob
      const binaryString = window.atob(base64AudioData)
      const bytes = new Uint8Array(binaryString.length)
      
      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i)
      }
      
      const audioBlob = new Blob([bytes], { type: 'audio/wav' })
      const audioUrl = URL.createObjectURL(audioBlob)
      
      // 播放音频块
      const chunkAudio = new Audio(audioUrl)
      chunkAudio.volume = ttsState.volume
      
      chunkAudio.play().catch(error => {
        console.warn('播放音频块失败:', error)
      })
      
      // 清理URL对象
      chunkAudio.addEventListener('ended', () => {
        URL.revokeObjectURL(audioUrl)
      })
      
    } catch (error) {
      console.error('解析音频数据块失败:', error)
    }
  }

  /**
   * 停止音频播放
   */
  stopAudio(): void {
    if (this.audioElement) {
      this.audioElement.pause()
      this.audioElement.currentTime = 0
      this.audioElement = null
    }
    
    ttsState.currentAudio = null
    ttsState.isPlaying = false
    ttsState.playbackProgress = 0
  }

  /**
   * 暂停/恢复播放
   */
  togglePlayback(): void {
    if (!this.audioElement) return

    if (ttsState.isPlaying) {
      this.audioElement.pause()
      ttsState.isPlaying = false
    } else {
      this.audioElement.play()
      ttsState.isPlaying = true
    }
  }

  /**
   * 设置音量
   */
  setVolume(volume: number): void {
    ttsState.volume = Math.max(0, Math.min(1, volume))
    
    if (this.audioElement) {
      this.audioElement.volume = ttsState.volume
    }
  }

  /**
   * 获取支持的音色列表
   */
  async getSupportedVoices(): Promise<Record<string, Record<string, VoiceInfo>>> {
    try {
      const response = await fetch(`${this.baseUrl}/voices`, {
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      })

      if (!response.ok) {
        throw new Error(`获取音色列表失败: ${response.status}`)
      }

      const result = await response.json()
      ttsState.supportedVoices = result.data || {}
      
      return result.data

    } catch (error) {
      console.error('获取音色列表失败:', error)
      return {}
    }
  }

  /**
   * 获取角色推荐音色
   */
  async getRecommendedVoice(characterId: number): Promise<string> {
    try {
      const response = await fetch(`${this.baseUrl}/character/${characterId}/recommended-voice`, {
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      })

      if (!response.ok) {
        throw new Error(`获取推荐音色失败: ${response.status}`)
      }

      const result = await response.json()
      return result.data || 'Cherry'

    } catch (error) {
      console.error('获取推荐音色失败:', error)
      return 'Cherry'
    }
  }

  /**
   * 停止流式合成
   */
  stopStreamingSynthesis(sessionId: string): void {
    const eventSource = ttsState.activeStreamingSessions.get(sessionId)
    if (eventSource) {
      eventSource.close()
      ttsState.activeStreamingSessions.delete(sessionId)
    }
  }

  /**
   * 清理所有活动会话
   */
  cleanup(): void {
    // 停止音频播放
    this.stopAudio()
    
    // 关闭所有流式会话
    ttsState.activeStreamingSessions.forEach((eventSource, sessionId) => {
      eventSource.close()
    })
    ttsState.activeStreamingSessions.clear()
    
    // 重置状态
    ttsState.error = null
    ttsState.isLoading = false
  }

  /**
   * 设置音频事件监听器
   */
  private setupAudioEventListeners(): void {
    if (!this.audioElement) return

    this.audioElement.addEventListener('timeupdate', () => {
      if (this.audioElement) {
        const progress = (this.audioElement.currentTime / this.audioElement.duration) * 100
        ttsState.playbackProgress = isNaN(progress) ? 0 : progress
      }
    })

    this.audioElement.addEventListener('ended', () => {
      ttsState.isPlaying = false
      ttsState.playbackProgress = 100
    })

    this.audioElement.addEventListener('error', (error) => {
      console.error('音频播放错误:', error)
      ttsState.error = '音频播放失败'
      ttsState.isPlaying = false
    })

    this.audioElement.addEventListener('loadstart', () => {
      ttsState.isLoading = true
    })

    this.audioElement.addEventListener('canplay', () => {
      ttsState.isLoading = false
    })
  }

  /**
   * 获取认证令牌
   */
  private getAuthToken(): string {
    return localStorage.getItem('authToken') || ''
  }
}

// 创建TTS服务实例
export const ttsService = new TtsService()

// 导出组合式API
export function useTts() {
  return {
    ttsState: readonly(ttsState),
    synthesizeText: ttsService.synthesizeText.bind(ttsService),
    synthesizeForCharacter: ttsService.synthesizeForCharacter.bind(ttsService),
    streamingSynthesize: ttsService.streamingSynthesize.bind(ttsService),
    playAudio: ttsService.playAudio.bind(ttsService),
    stopAudio: ttsService.stopAudio.bind(ttsService),
    togglePlayback: ttsService.togglePlayback.bind(ttsService),
    setVolume: ttsService.setVolume.bind(ttsService),
    getSupportedVoices: ttsService.getSupportedVoices.bind(ttsService),
    getRecommendedVoice: ttsService.getRecommendedVoice.bind(ttsService),
    cleanup: ttsService.cleanup.bind(ttsService)
  }
}
