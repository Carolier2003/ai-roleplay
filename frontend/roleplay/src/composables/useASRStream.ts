/**
 * 流式语音识别 Hook
 * 使用后端流式ASR接口
 */

import { ref } from 'vue'

export interface ASRStreamOptions {
  model?: string
  sampleRate?: number
  languageHints?: string[]
  onPartialResult?: (text: string) => void
  onFinalResult?: (text: string) => void
  onError?: (error: string) => void
}

export function useASRStream() {
  const isStreaming = ref(false)
  const currentSessionId = ref<string | null>(null)
  
  let partialText = ''
  let finalText = ''
  let options: ASRStreamOptions = {}

  // 开始流式识别
  const startStreaming = async (streamOptions: ASRStreamOptions): Promise<void> => {
    console.log('[useASRStream] 开始流式语音识别')
    
    if (isStreaming.value) {
      console.warn('[useASRStream] 已在流式识别中')
      return
    }

    options = streamOptions

    try {
      // 创建流式识别会话
      const sessionId = await createStreamingSession()
      currentSessionId.value = sessionId
      
      // 重置状态
      partialText = ''
      finalText = ''
      isStreaming.value = true

      console.log('[useASRStream] 流式识别会话创建成功:', sessionId)

    } catch (error) {
      console.error('[useASRStream] 开始流式识别失败:', error)
      cleanup()
      throw error
    }
  }

  // 创建流式识别会话
  const createStreamingSession = async (): Promise<string> => {
    return new Promise((resolve, reject) => {
      try {
        // 获取认证token
        const token = localStorage.getItem('ACCESS_TOKEN')
        const headers: Record<string, string> = {
          'Content-Type': 'application/json'
        }
        
        if (token) {
          headers['Authorization'] = `Bearer ${token}`
        }

        // 构建请求体
        const requestBody = {
          model: options.model || 'fun-asr-realtime',
          format: 'wav', // 必需字段
          sampleRate: options.sampleRate || 16000,
          semanticPunctuationEnabled: false,
          punctuationPredictionEnabled: true,
          maxSentenceSilence: 1300,
          languageHints: options.languageHints || ['zh', 'en']
        }

        // 使用fetch发送POST请求创建SSE连接
        fetch('http://localhost:18080/api/speech/streaming/create', {
          method: 'POST',
          headers,
          body: JSON.stringify(requestBody)
        }).then(response => {
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`)
          }

          if (!response.body) {
            throw new Error('Response body is null')
          }

          // 处理SSE流
          const reader = response.body.getReader()
          const decoder = new TextDecoder()
          let buffer = ''
          let sessionId = ''

          const processStream = async () => {
            try {
              while (true) {
                const { done, value } = await reader.read()
                
                if (done) {
                  console.log('[useASRStream] SSE流结束')
                  break
                }

                // 解码数据块
                const chunk = decoder.decode(value, { stream: true })
                buffer += chunk
                
                // 按行分割处理SSE数据
                const lines = buffer.split('\n')
                buffer = lines.pop() || ''
                
                for (const line of lines) {
                  if (line.startsWith('data:')) {
                    const data = line.slice(5).trim()
                    if (data && data !== '[DONE]') {
                      try {
                        const jsonData = JSON.parse(data)
                        console.log('[useASRStream] 收到SSE数据:', jsonData)

                        if (jsonData.type === 'CONNECTED' && jsonData.requestId) {
                          sessionId = jsonData.requestId // 使用requestId作为sessionId
                          console.log('[useASRStream] 会话创建成功:', sessionId)
                          resolve(sessionId)
                        } else if (jsonData.type === 'RESULT') {
                          // 识别结果
                          const text = jsonData.result?.text || jsonData.text || ''
                          if (text) {
                            partialText = text
                            console.log('[useASRStream] 识别结果:', text)
                            options.onPartialResult?.(text)
                          }
                        } else if (jsonData.type === 'COMPLETE') {
                          // 识别完成
                          console.log('[useASRStream] 识别完成')
                          finalText = partialText
                          options.onFinalResult?.(finalText)
                        } else if (jsonData.type === 'ERROR') {
                          console.error('[useASRStream] 识别错误:', jsonData.error || jsonData.message)
                          options.onError?.(jsonData.error || jsonData.message)
                        }
                      } catch (parseError) {
                        console.warn('[useASRStream] 解析SSE数据失败:', parseError, data)
                      }
                    }
                  } else if (line.startsWith('event:')) {
                    const eventType = line.slice(6).trim()
                    console.log('[useASRStream] 收到事件:', eventType)
                  }
                }
              }
            } catch (error) {
              console.error('[useASRStream] 处理SSE流失败:', error)
              if (!sessionId) {
                reject(error)
              }
            }
          }

          processStream()

          // 超时处理
          setTimeout(() => {
            if (!sessionId) {
              reader.cancel()
              reject(new Error('创建流式识别会话超时'))
            }
          }, 10000) // 10秒超时

        }).catch(error => {
          console.error('[useASRStream] 创建流式识别会话失败:', error)
          reject(error)
        })

      } catch (error) {
        console.error('[useASRStream] 创建流式识别会话异常:', error)
        reject(error)
      }
    })
  }

  // 发送音频数据
  const sendAudioData = async (audioData: ArrayBuffer): Promise<void> => {
    if (!isStreaming.value || !currentSessionId.value) {
      console.warn('[useASRStream] 未在流式识别中，忽略音频数据')
      return
    }

    try {
      // 获取认证token
      const token = localStorage.getItem('ACCESS_TOKEN')
      const headers: Record<string, string> = {
        'Content-Type': 'application/octet-stream'
      }
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      // 发送音频数据到后端
      const response = await fetch(
        `http://localhost:18080/api/speech/streaming/${currentSessionId.value}/audio`,
        {
          method: 'POST',
          headers,
          body: audioData
        }
      )

      if (!response.ok) {
        throw new Error(`发送音频数据失败: ${response.status}`)
      }

      console.log('[useASRStream] 音频数据发送成功，大小:', audioData.byteLength)

    } catch (error) {
      console.error('[useASRStream] 发送音频数据失败:', error)
      options.onError?.(`发送音频数据失败: ${error}`)
    }
  }

  // 停止流式识别
  const stopStreaming = async (): Promise<string> => {
    console.log('[useASRStream] 停止流式识别')
    
    if (!isStreaming.value || !currentSessionId.value) {
      console.warn('[useASRStream] 未在流式识别中')
      const result = finalText || partialText
      console.log('[useASRStream] 返回当前文本:', result)
      return result
    }

    try {
      // 停止流式识别
      const token = localStorage.getItem('ACCESS_TOKEN')
      const headers: Record<string, string> = {}
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch(
        `http://localhost:18080/api/speech/streaming/${currentSessionId.value}/stop`,
        {
          method: 'POST',
          headers
        }
      )

      if (!response.ok) {
        console.warn('[useASRStream] 停止流式识别请求失败:', response.status)
      }

      console.log('[useASRStream] 流式识别已停止')

    } catch (error) {
      console.error('[useASRStream] 停止流式识别失败:', error)
    }

    // 在cleanup之前保存最终文本
    const result = finalText || partialText
    console.log('[useASRStream] 返回最终文本:', result)
    
    // 清理资源
    cleanup()
    
    return result
  }

  // 获取当前识别文本
  const getCurrentText = (): string => {
    return finalText || partialText
  }

  // 清理资源
  const cleanup = (): void => {
    console.log('[useASRStream] 清理ASR流资源')
    
    // 如果有活动会话，尝试关闭
    if (currentSessionId.value) {
      const sessionId = currentSessionId.value
      
      // 异步关闭会话，不等待结果
      const token = localStorage.getItem('ACCESS_TOKEN')
      const headers: Record<string, string> = {}
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      fetch(`http://localhost:18080/api/speech/streaming/${sessionId}`, {
        method: 'DELETE',
        headers
      }).catch(error => {
        console.warn('[useASRStream] 关闭会话失败:', error)
      })
    }

    // 重置状态
    isStreaming.value = false
    currentSessionId.value = null
    partialText = ''
    finalText = ''
    options = {}
  }

  // 获取识别状态
  const getStatus = async () => {
    try {
      const token = localStorage.getItem('ACCESS_TOKEN')
      const headers: Record<string, string> = {}
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch('http://localhost:18080/api/speech/streaming/status', {
        headers
      })

      if (response.ok) {
        const result = await response.json()
        return result.data
      }
    } catch (error) {
      console.error('[useASRStream] 获取识别状态失败:', error)
    }
    
    return null
  }

  return {
    isStreaming,
    currentSessionId,
    startStreaming,
    sendAudioData,
    stopStreaming,
    getCurrentText,
    cleanup,
    getStatus
  }
}
