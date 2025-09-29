import axios from './axios'

/**
 * 语音通话相关接口
 */

export interface VoiceCallSession {
  sessionId: string
  status: 'active' | 'inactive'
  characterId: number
  userId: number
  createdAt: string
}

export interface SpeechRecognitionConfig {
  model?: string
  format?: string
  sampleRate?: number
  semanticPunctuationEnabled?: boolean
  punctuationPredictionEnabled?: boolean
  maxSentenceSilence?: number
  languageHints?: string[]
}

export interface TtsConfig {
  voice?: string
  speed?: number
  pitch?: number
  volume?: number
}

/**
 * 创建语音通话会话
 */
export const createVoiceCallSession = async (characterId: number): Promise<VoiceCallSession> => {
  console.log('[voiceApi] 创建语音通话会话, characterId:', characterId)
  
  try {
    const response = await axios.post('/api/voice/session/create', {
      characterId
    })
    
    if (response.data.code === 200) {
      console.log('[voiceApi] 语音通话会话创建成功:', response.data.data)
      return response.data.data
    } else {
      throw new Error(response.data.message || '创建会话失败')
    }
  } catch (error) {
    console.error('[voiceApi] 创建语音通话会话失败:', error)
    throw error
  }
}

/**
 * 结束语音通话会话
 */
export const endVoiceCallSession = async (sessionId: string): Promise<void> => {
  console.log('[voiceApi] 结束语音通话会话, sessionId:', sessionId)
  
  try {
    const response = await axios.post(`/api/voice/session/${sessionId}/end`)
    
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '结束会话失败')
    }
    
    console.log('[voiceApi] 语音通话会话结束成功')
  } catch (error) {
    console.error('[voiceApi] 结束语音通话会话失败:', error)
    throw error
  }
}

/**
 * 创建流式语音识别会话
 */
export const createSpeechRecognitionSession = async (config: SpeechRecognitionConfig = {}): Promise<EventSource> => {
  console.log('[voiceApi] 创建语音识别会话, config:', config)
  
  try {
    const response = await axios.post('/api/speech/streaming/create', {
      model: config.model || 'fun-asr-realtime',
      format: config.format || 'wav',
      sampleRate: config.sampleRate || 16000,
      semanticPunctuationEnabled: config.semanticPunctuationEnabled || false,
      punctuationPredictionEnabled: config.punctuationPredictionEnabled || true,
      maxSentenceSilence: config.maxSentenceSilence || 1300,
      languageHints: config.languageHints || []
    })
    
    // 返回SSE连接
    const eventSource = new EventSource('/api/speech/streaming/events')
    console.log('[voiceApi] 语音识别SSE连接已建立')
    
    return eventSource
    
  } catch (error) {
    console.error('[voiceApi] 创建语音识别会话失败:', error)
    throw error
  }
}

/**
 * 发送音频数据到语音识别
 */
export const sendAudioData = async (sessionId: string, audioData: ArrayBuffer): Promise<void> => {
  try {
    const response = await axios.post(`/api/speech/streaming/${sessionId}/audio`, audioData, {
      headers: {
        'Content-Type': 'application/octet-stream'
      }
    })
    
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '发送音频数据失败')
    }
    
  } catch (error) {
    console.error('[voiceApi] 发送音频数据失败:', error)
    throw error
  }
}

/**
 * 停止语音识别
 */
export const stopSpeechRecognition = async (sessionId: string): Promise<void> => {
  console.log('[voiceApi] 停止语音识别, sessionId:', sessionId)
  
  try {
    const response = await axios.post(`/api/speech/streaming/${sessionId}/stop`)
    
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '停止识别失败')
    }
    
    console.log('[voiceApi] 语音识别已停止')
  } catch (error) {
    console.error('[voiceApi] 停止语音识别失败:', error)
    throw error
  }
}

/**
 * 关闭语音识别会话
 */
export const closeSpeechRecognitionSession = async (sessionId: string): Promise<void> => {
  console.log('[voiceApi] 关闭语音识别会话, sessionId:', sessionId)
  
  try {
    const response = await axios.delete(`/api/speech/streaming/${sessionId}`)
    
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '关闭会话失败')
    }
    
    console.log('[voiceApi] 语音识别会话已关闭')
  } catch (error) {
    console.error('[voiceApi] 关闭语音识别会话失败:', error)
    throw error
  }
}

/**
 * 流式语音合成
 */
export const createStreamingTts = async (text: string, config: TtsConfig = {}): Promise<EventSource> => {
  console.log('[voiceApi] 创建流式TTS, text:', text.substring(0, 50) + '...')
  
  try {
    const response = await axios.post('/api/tts/synthesize/stream', {
      text,
      voice: config.voice || 'zh-CN-XiaoxiaoNeural',
      speed: config.speed || 1.0,
      pitch: config.pitch || 0,
      volume: config.volume || 1.0,
      format: 'wav'
    })
    
    // 返回SSE连接用于接收音频流
    const eventSource = new EventSource('/api/tts/streaming/events')
    console.log('[voiceApi] TTS SSE连接已建立')
    
    return eventSource
    
  } catch (error) {
    console.error('[voiceApi] 创建流式TTS失败:', error)
    throw error
  }
}

/**
 * 角色语音合成
 */
export const synthesizeCharacterVoice = async (characterId: number, text: string): Promise<EventSource> => {
  console.log('[voiceApi] 角色语音合成, characterId:', characterId, 'text:', text.substring(0, 50) + '...')
  
  try {
    const response = await axios.post(`/api/tts/synthesize/character/${characterId}`, {
      text
    })
    
    // 返回SSE连接用于接收音频流
    const eventSource = new EventSource(`/api/tts/character/${characterId}/events`)
    console.log('[voiceApi] 角色TTS SSE连接已建立')
    
    return eventSource
    
  } catch (error) {
    console.error('[voiceApi] 角色语音合成失败:', error)
    throw error
  }
}

/**
 * WebSocket连接工具函数
 */
export class VoiceWebSocket {
  private ws: WebSocket | null = null
  private url: string
  private onMessage?: (data: any) => void
  private onError?: (error: Event) => void
  private onClose?: () => void
  
  constructor(url: string) {
    this.url = url
  }
  
  connect(
    onMessage?: (data: any) => void,
    onError?: (error: Event) => void,
    onClose?: () => void
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.onMessage = onMessage
        this.onError = onError
        this.onClose = onClose
        
        this.ws = new WebSocket(this.url)
        
        this.ws.onopen = () => {
          console.log('[VoiceWebSocket] 连接已建立:', this.url)
          resolve()
        }
        
        this.ws.onmessage = (event) => {
          if (this.onMessage) {
            try {
              const data = JSON.parse(event.data)
              this.onMessage(data)
            } catch (error) {
              // 如果不是JSON，直接传递原始数据
              this.onMessage(event.data)
            }
          }
        }
        
        this.ws.onerror = (error) => {
          console.error('[VoiceWebSocket] 连接错误:', error)
          if (this.onError) {
            this.onError(error)
          }
          reject(new Error('WebSocket连接失败'))
        }
        
        this.ws.onclose = () => {
          console.log('[VoiceWebSocket] 连接已关闭')
          if (this.onClose) {
            this.onClose()
          }
        }
        
      } catch (error) {
        reject(error)
      }
    })
  }
  
  send(data: string | ArrayBuffer | Blob): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(data)
    } else {
      console.warn('[VoiceWebSocket] 连接未就绪，无法发送数据')
    }
  }
  
  close(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }
  
  get readyState(): number {
    return this.ws ? this.ws.readyState : WebSocket.CLOSED
  }
}

/**
 * 音频工具函数
 */
export class AudioUtils {
  /**
   * 将音频数据转换为WAV格式
   */
  static encodeWAV(samples: Float32Array, sampleRate: number = 16000): ArrayBuffer {
    const buffer = new ArrayBuffer(44 + samples.length * 2)
    const view = new DataView(buffer)
    
    // WAV文件头
    const writeString = (offset: number, string: string) => {
      for (let i = 0; i < string.length; i++) {
        view.setUint8(offset + i, string.charCodeAt(i))
      }
    }
    
    writeString(0, 'RIFF')
    view.setUint32(4, 36 + samples.length * 2, true)
    writeString(8, 'WAVE')
    writeString(12, 'fmt ')
    view.setUint32(16, 16, true)
    view.setUint16(20, 1, true)
    view.setUint16(22, 1, true)
    view.setUint32(24, sampleRate, true)
    view.setUint32(28, sampleRate * 2, true)
    view.setUint16(32, 2, true)
    view.setUint16(34, 16, true)
    writeString(36, 'data')
    view.setUint32(40, samples.length * 2, true)
    
    // 音频数据
    let offset = 44
    for (let i = 0; i < samples.length; i++) {
      const sample = Math.max(-1, Math.min(1, samples[i]))
      view.setInt16(offset, sample < 0 ? sample * 0x8000 : sample * 0x7FFF, true)
      offset += 2
    }
    
    return buffer
  }
  
  /**
   * 播放音频数据
   */
  static async playAudio(audioData: ArrayBuffer, audioContext?: AudioContext): Promise<void> {
    try {
      const context = audioContext || new AudioContext()
      const audioBuffer = await context.decodeAudioData(audioData)
      const source = context.createBufferSource()
      
      source.buffer = audioBuffer
      source.connect(context.destination)
      source.start()
      
      console.log('[AudioUtils] 音频播放开始')
      
    } catch (error) {
      console.error('[AudioUtils] 音频播放失败:', error)
      throw error
    }
  }
}
