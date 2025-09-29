/**
 * 流式聊天 Hook
 * 使用后端流式聊天接口
 */

import { ref } from 'vue'

export interface ChatStreamOptions {
  characterId: number
  message: string
  onMessage?: (content: string) => void
  onComplete?: () => void
  onError?: (error: string) => void
}

export function useChatStream() {
  const isStreaming = ref(false)
  const currentContent = ref('')
  
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
  let abortController: AbortController | null = null

  // 开始流式聊天
  const startStreaming = async (options: ChatStreamOptions): Promise<void> => {
    console.log('[useChatStream] 开始流式聊天:', options)
    
    if (isStreaming.value) {
      console.warn('[useChatStream] 已在流式聊天中')
      return
    }

    try {
      isStreaming.value = true
      currentContent.value = ''
      
      // 创建AbortController用于取消请求
      abortController = new AbortController()

      // 获取认证token
      const token = localStorage.getItem('ACCESS_TOKEN')
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache'
      }
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      // 发送流式聊天请求
      const response = await fetch('http://localhost:18080/api/chat/stream', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          characterId: options.characterId,
          message: options.message
        }),
        signal: abortController.signal
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`HTTP ${response.status}: ${response.statusText} - ${errorText}`)
      }

      if (!response.body) {
        throw new Error('Response body is null')
      }

      // 处理流式响应
      reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      console.log('[useChatStream] 开始读取流式响应')

      while (true) {
        const { done, value } = await reader.read()
        
        if (done) {
          console.log('[useChatStream] 流式响应完成')
          options.onComplete?.()
          break
        }

        // 解码数据块
        const chunk = decoder.decode(value, { stream: true })
        buffer += chunk
        
        // 按行分割处理
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 保留最后一个不完整的行
        
        for (const line of lines) {
          processStreamLine(line, options)
        }
      }

    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.log('[useChatStream] 流式聊天被取消')
      } else {
        console.error('[useChatStream] 流式聊天失败:', error)
        options.onError?.(error instanceof Error ? error.message : '流式聊天失败')
      }
    } finally {
      cleanup()
    }
  }

  // 处理流式数据行
  const processStreamLine = (line: string, options: ChatStreamOptions) => {
    const trimmedLine = line.trim()
    
    if (!trimmedLine) {
      return // 跳过空行
    }
    
    console.log('[useChatStream] 处理行:', trimmedLine)
    
    // 处理 Server-Sent Events 格式
    if (trimmedLine.startsWith('data:')) {
      let content = trimmedLine.slice(5).trim() // 移除 'data:' 前缀
      
      // 处理重复的 data: 前缀
      while (content.startsWith('data:')) {
        content = content.slice(5).trim()
      }
      
      // 处理结束标记
      if (content === '[DONE]') {
        console.log('[useChatStream] 收到结束标记 [DONE]')
        options.onComplete?.()
        return
      }
      
      // 检查是否为错误消息
      if (content.startsWith('error:')) {
        const errorMessage = content.substring(6)
        console.log('[useChatStream] 收到错误消息:', errorMessage)
        options.onError?.(errorMessage)
        return
      }
      
      // 处理空的 data: 行
      if (!content) {
        return
      }
      
      // 尝试解析 JSON，如果失败则当作纯文本处理
      try {
        const jsonData = JSON.parse(content)
        console.log('[useChatStream] 解析JSON数据:', jsonData)
        
        if (jsonData.content) {
          currentContent.value += jsonData.content
          options.onMessage?.(jsonData.content)
        } else if (jsonData.error) {
          options.onError?.(jsonData.error)
        }
      } catch (parseError) {
        // 如果不是 JSON，当作纯文本处理
        console.log('[useChatStream] 处理纯文本数据:', content)
        currentContent.value += content
        options.onMessage?.(content)
      }
    } else if (trimmedLine.startsWith('event:')) {
      const eventType = trimmedLine.slice(6).trim()
      console.log('[useChatStream] 收到事件:', eventType)
      
      if (eventType === 'close' || eventType === 'end') {
        console.log('[useChatStream] 收到关闭事件')
        options.onComplete?.()
        return
      }
    }
  }

  // 停止流式聊天
  const stopStreaming = (): void => {
    console.log('[useChatStream] 停止流式聊天')
    
    if (abortController) {
      abortController.abort()
    }
    
    cleanup()
  }

  // 获取当前内容
  const getCurrentContent = (): string => {
    return currentContent.value
  }

  // 清理资源
  const cleanup = (): void => {
    console.log('[useChatStream] 清理聊天流资源')
    
    // 关闭reader
    if (reader) {
      reader.cancel().catch(error => {
        console.warn('[useChatStream] 关闭reader失败:', error)
      })
      reader = null
    }

    // 重置AbortController
    abortController = null

    // 重置状态
    isStreaming.value = false
  }

  // 发送普通聊天消息（非流式）
  const sendMessage = async (characterId: number, message: string): Promise<string> => {
    console.log('[useChatStream] 发送普通聊天消息')
    
    try {
      const token = localStorage.getItem('ACCESS_TOKEN')
      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      }
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch('http://localhost:18080/api/chat/message', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          characterId,
          message
        })
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const result = await response.json()
      
      if (result.code !== 200) {
        throw new Error(result.message || '发送消息失败')
      }

      return result.data.content || ''

    } catch (error) {
      console.error('[useChatStream] 发送普通消息失败:', error)
      throw error
    }
  }

  return {
    isStreaming,
    currentContent,
    startStreaming,
    stopStreaming,
    getCurrentContent,
    sendMessage,
    cleanup
  }
}
