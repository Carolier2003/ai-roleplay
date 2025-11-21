import apiClient from './axios'

/**
 * 语音识别请求参数
 */
export interface SpeechRecognitionRequest {
  model?: string
  format?: string
  sampleRate?: number
  semanticPunctuationEnabled?: boolean
  punctuationPredictionEnabled?: boolean
  maxSentenceSilence?: number
  languageHints?: string[]
}

/**
 * 语音识别响应
 */
export interface SpeechRecognitionResponse {
  requestId: string
  text: string
  confidence: number
  duration: number
  timestamp: string
  audioUrl?: string
  audioDuration?: number
}

/**
 * 流式语音识别事件
 */
export interface StreamingSpeechEvent {
  type: 'partial' | 'final' | 'error' | 'complete'
  data: {
    text?: string
    confidence?: number
    isFinal?: boolean
    error?: string
  }
}

/**
 * 语音识别API服务
 */
export class SpeechRecognitionAPI {

  /**
   * 同步语音识别 - 上传音频文件
   */
  static async recognizeAudio(
    audioFile: File | Blob,
    options: SpeechRecognitionRequest = {}
  ): Promise<SpeechRecognitionResponse> {
    const formData = new FormData()

    // 添加音频文件
    if (audioFile instanceof File) {
      formData.append('audio', audioFile)
    } else {
      // Blob转换为File，根据实际格式设置文件名和类型
      const format = options.format || 'wav'
      const mimeType = audioFile.type || `audio/${format}`
      const file = new File([audioFile], `recording.${format}`, { type: mimeType })
      formData.append('audio', file)
    }

    // 添加可选参数
    if (options.model) formData.append('model', options.model)
    if (options.format) formData.append('format', options.format)
    if (options.sampleRate) formData.append('sampleRate', options.sampleRate.toString())
    if (options.semanticPunctuationEnabled !== undefined) {
      formData.append('semanticPunctuationEnabled', options.semanticPunctuationEnabled.toString())
    }
    if (options.punctuationPredictionEnabled !== undefined) {
      formData.append('punctuationPredictionEnabled', options.punctuationPredictionEnabled.toString())
    }
    if (options.maxSentenceSilence) {
      formData.append('maxSentenceSilence', options.maxSentenceSilence.toString())
    }
    if (options.languageHints) {
      options.languageHints.forEach(hint => formData.append('languageHints', hint))
    }

    const response = await apiClient.post('/api/speech/recognize', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 30000 // 30秒超时
    })

    return response.data.data
  }

  /**
   * 异步语音识别 - 上传音频文件进行异步处理
   */
  static async recognizeAudioAsync(
    audioFile: File | Blob,
    options: SpeechRecognitionRequest = {}
  ): Promise<{ taskId: string }> {
    const formData = new FormData()

    // 添加音频文件
    if (audioFile instanceof File) {
      formData.append('audio', audioFile)
    } else {
      // Blob转换为File，根据实际格式设置文件名和类型
      const format = options.format || 'wav'
      const mimeType = audioFile.type || `audio/${format}`
      const file = new File([audioFile], `recording.${format}`, { type: mimeType })
      formData.append('audio', file)
    }

    // 添加参数
    Object.entries(options).forEach(([key, value]) => {
      if (value !== undefined) {
        if (Array.isArray(value)) {
          value.forEach(item => formData.append(key, item))
        } else {
          formData.append(key, value.toString())
        }
      }
    })

    const response = await apiClient.post('/api/speech/recognize/async', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    return response.data.data
  }

  /**
   * 创建流式语音识别会话
   */
  static createStreamingSession(
    options: SpeechRecognitionRequest = {}
  ): EventSource {
    const params = new URLSearchParams()

    // 添加参数
    Object.entries(options).forEach(([key, value]) => {
      if (value !== undefined) {
        if (Array.isArray(value)) {
          value.forEach(item => params.append(key, item))
        } else {
          params.append(key, value.toString())
        }
      }
    })

    // 创建SSE连接
    const eventSource = new EventSource(
      `${apiClient.defaults.baseURL}/api/speech/streaming/create?${params.toString()}`
    )

    return eventSource
  }

  /**
   * 发送音频数据到流式识别会话
   */
  static async sendAudioData(sessionId: string, audioData: ArrayBuffer): Promise<void> {
    await apiClient.post(`/api/speech/streaming/${sessionId}/audio`, audioData, {
      headers: {
        'Content-Type': 'application/octet-stream'
      }
    })
  }

  /**
   * 停止流式识别
   */
  static async stopStreaming(sessionId: string): Promise<void> {
    await apiClient.post(`/api/speech/streaming/${sessionId}/stop`)
  }

  /**
   * 关闭流式识别会话
   */
  static async closeSession(sessionId: string): Promise<void> {
    await apiClient.delete(`/api/speech/streaming/${sessionId}`)
  }

  /**
   * 获取流式识别状态
   */
  static async getStreamingStatus(): Promise<{ activeSessionCount: number }> {
    const response = await apiClient.get('/api/speech/streaming/status')
    return response.data.data
  }
}

/**
 * 音频格式转换工具
 */
export class AudioConverter {

  /**
   * 将任意音频格式转换为WAV格式
   */
  static async toWav(audioBlob: Blob): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const audioContext = new AudioContext()
      const fileReader = new FileReader()

      fileReader.onload = async (e) => {
        try {
          const arrayBuffer = e.target?.result as ArrayBuffer
          const audioBuffer = await audioContext.decodeAudioData(arrayBuffer)

          console.log('[AudioConverter] 音频解码成功:', {
            sampleRate: audioBuffer.sampleRate,
            channels: audioBuffer.numberOfChannels,
            duration: audioBuffer.duration,
            length: audioBuffer.length
          })

          // 转换为WAV
          const wavBlob = this.audioBufferToWav(audioBuffer)
          console.log('[AudioConverter] WAV转换完成，大小:', wavBlob.size, 'bytes')
          resolve(wavBlob)
        } catch (error) {
          console.error('[AudioConverter] 音频解码失败:', error)
          reject(error)
        }
      }

      fileReader.onerror = () => reject(new Error('文件读取失败'))
      fileReader.readAsArrayBuffer(audioBlob)
    })
  }

  /**
   * 将WebM格式转换为WAV格式（向后兼容）
   */
  static async webmToWav(webmBlob: Blob): Promise<Blob> {
    return this.toWav(webmBlob)
  }

  /**
   * AudioBuffer转换为WAV Blob (确保采样率为16000Hz)
   */
  private static audioBufferToWav(audioBuffer: AudioBuffer): Blob {
    const numberOfChannels = 1 // 强制单声道
    const targetSampleRate = 16000 // 目标采样率16kHz
    const format = 1 // PCM
    const bitDepth = 16

    console.log('[AudioConverter] 原始音频信息:', {
      sampleRate: audioBuffer.sampleRate,
      channels: audioBuffer.numberOfChannels,
      length: audioBuffer.length,
      duration: audioBuffer.duration
    })

    // 重采样到16kHz单声道
    const resampledData = this.resampleAudio(audioBuffer, targetSampleRate)
    console.log('[AudioConverter] 重采样完成:', {
      targetSampleRate,
      newLength: resampledData.length,
      estimatedDuration: resampledData.length / targetSampleRate
    })

    const bytesPerSample = bitDepth / 8
    const blockAlign = numberOfChannels * bytesPerSample

    const buffer = new ArrayBuffer(44 + resampledData.length * numberOfChannels * bytesPerSample)
    const view = new DataView(buffer)

    // WAV文件头
    const writeString = (offset: number, string: string) => {
      for (let i = 0; i < string.length; i++) {
        view.setUint8(offset + i, string.charCodeAt(i))
      }
    }

    writeString(0, 'RIFF')
    view.setUint32(4, buffer.byteLength - 8, true)
    writeString(8, 'WAVE')
    writeString(12, 'fmt ')
    view.setUint32(16, 16, true)
    view.setUint16(20, format, true)
    view.setUint16(22, numberOfChannels, true)
    view.setUint32(24, targetSampleRate, true) // 使用目标采样率
    view.setUint32(28, targetSampleRate * blockAlign, true)
    view.setUint16(32, blockAlign, true)
    view.setUint16(34, bitDepth, true)
    writeString(36, 'data')
    view.setUint32(40, resampledData.length * numberOfChannels * bytesPerSample, true)

    // 写入重采样后的音频数据
    let offset = 44
    for (let i = 0; i < resampledData.length; i++) {
      const sample = Math.max(-1, Math.min(1, resampledData[i]))
      view.setInt16(offset, sample < 0 ? sample * 0x8000 : sample * 0x7FFF, true)
      offset += 2
    }

    return new Blob([buffer], { type: 'audio/wav' })
  }

  /**
   * 重采样音频到指定采样率并转换为单声道
   */
  private static resampleAudio(audioBuffer: AudioBuffer, targetSampleRate: number): Float32Array {
    const originalSampleRate = audioBuffer.sampleRate
    const originalLength = audioBuffer.length
    const targetLength = Math.round(originalLength * targetSampleRate / originalSampleRate)

    console.log('[AudioConverter] 重采样参数:', {
      originalSampleRate,
      targetSampleRate,
      originalLength,
      targetLength,
      ratio: targetSampleRate / originalSampleRate
    })

    // 如果采样率相同且已经是单声道，直接返回
    if (Math.abs(originalSampleRate - targetSampleRate) < 1 && audioBuffer.numberOfChannels === 1) {
      console.log('[AudioConverter] 采样率和声道数已符合要求，无需转换')
      return audioBuffer.getChannelData(0)
    }

    // 先转换为单声道
    const monoData = this.convertToMono(audioBuffer)

    // 如果采样率相同，直接返回单声道数据
    if (Math.abs(originalSampleRate - targetSampleRate) < 1) {
      console.log('[AudioConverter] 采样率相同，只需转换声道')
      return monoData
    }

    // 重采样
    const resampledData = new Float32Array(targetLength)
    const ratio = originalLength / targetLength

    for (let i = 0; i < targetLength; i++) {
      const originalIndex = i * ratio
      const index = Math.floor(originalIndex)
      const fraction = originalIndex - index

      if (index + 1 < originalLength) {
        // 线性插值
        resampledData[i] = monoData[index] * (1 - fraction) + monoData[index + 1] * fraction
      } else {
        resampledData[i] = monoData[index]
      }
    }

    console.log('[AudioConverter] 重采样完成')
    return resampledData
  }

  /**
   * 将多声道音频转换为单声道
   */
  private static convertToMono(audioBuffer: AudioBuffer): Float32Array {
    const numberOfChannels = audioBuffer.numberOfChannels
    const length = audioBuffer.length
    const monoData = new Float32Array(length)

    if (numberOfChannels === 1) {
      // 已经是单声道，直接复制
      return new Float32Array(audioBuffer.getChannelData(0))
    }

    console.log('[AudioConverter] 转换为单声道:', numberOfChannels, '→ 1')

    // 多声道混合为单声道
    for (let i = 0; i < length; i++) {
      let sample = 0
      for (let channel = 0; channel < numberOfChannels; channel++) {
        sample += audioBuffer.getChannelData(channel)[i]
      }
      monoData[i] = sample / numberOfChannels
    }

    return monoData
  }
}

/**
 * 语音识别工具类
 */
export class SpeechRecognitionUtils {

  /**
   * 从语音识别响应中提取纯文本
   */
  static extractTextFromResponse(response: SpeechRecognitionResponse): string {
    if (!response.text) {
      return ''
    }

    try {
      // 检查是否是JSON格式的详细结果
      if (response.text.startsWith('{')) {
        const parsedResult = JSON.parse(response.text)
        console.log('[SpeechRecognitionUtils] 解析详细结果:', parsedResult)

        // 提取sentences中的text并合并
        if (parsedResult.sentences && Array.isArray(parsedResult.sentences)) {
          const extractedText = parsedResult.sentences
            .map((sentence: any) => sentence.text || '')
            .join('')
            .trim()

          console.log('[SpeechRecognitionUtils] 提取的文本:', extractedText)
          return extractedText
        }

        // 如果没有sentences，尝试其他可能的字段
        if (parsedResult.text) {
          return parsedResult.text
        }

        if (parsedResult.result) {
          return parsedResult.result
        }
      }

      // 如果不是JSON格式，直接返回
      return response.text.trim()

    } catch (error) {
      console.warn('[SpeechRecognitionUtils] 解析语音识别结果失败，使用原始text:', error)
      return response.text.trim()
    }
  }

  /**
   * 验证识别结果是否有效
   */
  static isValidRecognitionResult(text: string): boolean {
    if (!text || text.trim().length === 0) {
      return false
    }

    // 过滤掉一些无效的识别结果
    const invalidPatterns = [
      /^[\s\.,，。！？!?]*$/, // 只有标点符号
      /^(嗯|啊|呃|额|哦|噢)[\s\.,，。！？!?]*$/i, // 语气词
      /^[\d\s\.,，。！？!?]*$/ // 只有数字和标点
    ]

    return !invalidPatterns.some(pattern => pattern.test(text.trim()))
  }
}

/**
 * 语音识别错误类型
 */
export class SpeechRecognitionError extends Error {
  constructor(
    message: string,
    public code: string,
    public details?: any
  ) {
    super(message)
    this.name = 'SpeechRecognitionError'
  }
}
