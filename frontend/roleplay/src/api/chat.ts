import axios from './axios'

// 角色接口
export interface Character {
  id: number  // ✅ 使用 number 类型
  name: string
  avatar: string
  unread?: number  // 前端使用的未读消息数
  description?: string
}

// 聊天消息接口
export interface ChatMessage {
  id: string
  characterId: number  // ✅ 使用 number 类型
  content: string
  isUser: boolean
  timestamp: number
  streaming?: boolean
  audioUrl?: string
  // 语音相关字段
  isVoiceMessage?: boolean  // 是否为语音消息
  voiceDuration?: number    // 语音时长（秒）
  voice?: string           // 使用的音色
  languageType?: string    // 语言类型
}

// 发送消息请求 - 匹配后端 ChatRequest
export interface SendMessageRequest {
  characterId: number  // ✅ 使用 number 类型，匹配后端 Long characterId
  message: string      // ✅ 匹配后端字段名 message（不是 content）
  enableTts?: boolean  // ✅ 可选，是否启用语音合成
  enableRag?: boolean  // ✅ 可选，是否启用RAG知识检索
  languageType?: string // ✅ 可选，语言类型
  // ✅ 不需要 userId，后端从 JWT token 中获取
}

// 发送消息响应
export interface SendMessageResponse {
  messageId: string
  content: string
  conversationId: string
  timestamp: number
  audioUrl?: string
}

// 流式响应数据
export interface StreamResponse {
  type: 'message' | 'end' | 'error' | 'tts'
  content?: string
  messageId?: string
  error?: string
  // TTS相关字段
  audioUrl?: string
  voice?: string
  duration?: number
  success?: boolean
}

/**
 * 获取角色列表
 */
export const getCharacterList = async (): Promise<Character[]> => {
  console.log('[chatApi] 获取角色列表')
  
  const response = await axios.get('/api/characters')
  
  // API直接返回角色数组，不是包装格式
  if (Array.isArray(response.data)) {
    console.log('[chatApi] 获取角色列表成功:', response.data)
    return response.data
  } else if (response.data.code === 200) {
    // 兼容包装格式
    console.log('[chatApi] 获取角色列表成功:', response.data.data)
    return response.data.data
  } else {
    throw new Error(response.data.message || '获取角色列表失败')
  }
}

/**
 * 发送聊天消息到后端
 */
export const sendMessage = async (data: SendMessageRequest): Promise<SendMessageResponse> => {
  console.log('[chatApi] 发送聊天消息:', data)
  
  const response = await axios.post('/api/chat/message', data)
  
  if (response.data.code === 200) {
    console.log('[chatApi] 消息发送成功:', response.data.data)
    return response.data.data
  } else {
    throw new Error(response.data.message || '消息发送失败')
  }
}

/**
 * 发送流式聊天消息
 * 使用 fetch + ReadableStream 处理 Server-Sent Events
 */
export const sendStreamMessage = async (
  data: SendMessageRequest,
  onMessage: (chunk: StreamResponse) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<void> => {
  console.log('[chatApi] 发送流式聊天消息:', data)
  
  // 优先尝试使用 fetch + ReadableStream，如果失败则回退到 EventSource
  try {
    await sendStreamMessageWithFetch(data, onMessage, onError, onComplete)
  } catch (error) {
    console.warn('[chatApi] fetch 方式失败，尝试 EventSource 方式:', error)
    await sendStreamMessageWithEventSource(data, onMessage, onError, onComplete)
  }
}

/**
 * 使用 fetch + ReadableStream 发送流式消息
 */
const sendStreamMessageWithFetch = async (
  data: SendMessageRequest,
  onMessage: (chunk: StreamResponse) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<void> => {
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
  
  try {
    // 获取 token（游客模式可以为空）
    const token = localStorage.getItem('ACCESS_TOKEN')
    
    // 构建请求头
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache'
    }
    
    // 如果有token则添加Authorization头
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch('http://localhost:18080/api/chat/stream', {
      method: 'POST',
      headers,
      body: JSON.stringify(data)
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`HTTP ${response.status}: ${response.statusText} - ${errorText}`)
    }

    if (!response.body) {
      throw new Error('Response body is null')
    }

    reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
        console.log('[chatApi] 流式响应完成 (done=true)')
        onComplete?.()
        break
      }

      // 解码数据块
      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk
      
      // 按行分割处理
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 保留最后一个不完整的行
      
      for (const line of lines) {
        processStreamLine(line, onMessage, onComplete)
      }
    }
    
  } catch (error) {
    console.error('[chatApi] fetch 流式消息发送失败:', error)
    onError?.(error as Error)
    throw error // 重新抛出错误以便回退到 EventSource
  } finally {
    // 确保清理资源
    if (reader) {
      try {
        await reader.cancel()
      } catch (e) {
        console.warn('[chatApi] 关闭 reader 失败:', e)
      }
    }
  }
}

/**
 * 使用 EventSource 发送流式消息（回退方案）
 * 注意：EventSource 不支持 POST 请求，所以这里需要特殊处理
 */
const sendStreamMessageWithEventSource = async (
  data: SendMessageRequest,
  onMessage: (chunk: StreamResponse) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<void> => {
  // EventSource不支持POST请求，且我们的接口只支持POST
  // 因此直接抛出错误，强制使用fetch方式
  const error = new Error('EventSource不支持POST请求，请使用fetch方式')
  onError?.(error)
  throw error
}

/**
 * 处理流式数据行
 */
const processStreamLine = (
  line: string,
  onMessage: (chunk: StreamResponse) => void,
  onComplete?: () => void
) => {
  const trimmedLine = line.trim()
  
  if (!trimmedLine) {
    return // 跳过空行
  }
  
  console.log('[chatApi] 处理行:', trimmedLine)
  
  // 处理 Server-Sent Events 格式
  if (trimmedLine.startsWith('data:')) {
    let content = trimmedLine.slice(5).trim() // 移除 'data:' 前缀
    
    // 处理重复的 data: 前缀（后端可能发送 "data:data:content"）
    while (content.startsWith('data:')) {
      content = content.slice(5).trim()
    }
    
    // 处理结束标记
    if (content === '[DONE]') {
      console.log('[chatApi] 收到结束标记 [DONE]')
      onComplete?.()
      return
    }
    
    // 检查是否为错误消息
    if (content.startsWith('error:')) {
      const errorMessage = content.substring(6) // 移除 'error:' 前缀
      console.log('[chatApi] 收到错误消息:', errorMessage)
      const streamData: StreamResponse = {
        type: 'error',
        error: errorMessage
      }
      onMessage(streamData)
      return
    }
    
    // 处理空的 data: 行
    if (!content) {
      return
    }
    
    // 尝试解析 JSON，如果失败则当作纯文本处理
    try {
      const jsonData = JSON.parse(content)
      console.log('[chatApi] 解析JSON数据:', jsonData)
      
      // 处理 TTS 事件
      if (jsonData.type === 'tts') {
        const streamData: StreamResponse = {
          type: 'tts',
          audioUrl: jsonData.audioUrl,
          voice: jsonData.voice,
          duration: jsonData.duration,
          success: jsonData.success,
          error: jsonData.error
        }
        console.log('[chatApi] 收到TTS事件:', streamData)
        onMessage(streamData)
      }
      // 处理 JSON 格式的响应
      else if (jsonData.content) {
        const streamData: StreamResponse = {
          type: 'message',
          content: jsonData.content,
          messageId: jsonData.messageId
        }
        onMessage(streamData)
      } else if (jsonData.error) {
        const streamData: StreamResponse = {
          type: 'error',
          error: jsonData.error
        }
        onMessage(streamData)
      }
    } catch (parseError) {
      // 如果不是 JSON，当作纯文本处理
      console.log('[chatApi] 处理纯文本数据:', content)
      const streamData: StreamResponse = {
        type: 'message',
        content: content
      }
      onMessage(streamData)
    }
  } else if (trimmedLine.startsWith('event:')) {
    const eventType = trimmedLine.slice(6).trim()
    console.log('[chatApi] 收到事件:', eventType)
    
    if (eventType === 'close' || eventType === 'end') {
      console.log('[chatApi] 收到关闭事件')
      onComplete?.()
      return
    }
  }
}

/**
 * 聊天历史响应接口
 */
export interface ChatHistoryResponse {
  messages: ChatMessage[]
  total: number
  hasMore: boolean
  sourceStats: Record<string, number>
  queryDays?: number
}

/**
 * 获取对话历史 - 新接口，适配Spring AI
 */
export const getChatHistory = async (characterId: number): Promise<ChatHistoryResponse> => {
  console.log('[chatApi] 获取对话历史:', { characterId })
  
  const response = await axios.get('/api/chat/history', {
    params: { characterId }
  })
  
  // 直接返回后端数据，不包装在code中
  if (response.data && response.data.messages) {
    console.log('[chatApi] 获取对话历史成功:', {
      total: response.data.total,
      hasMore: response.data.hasMore,
      sourceStats: response.data.sourceStats
    })
    
    // 清理消息内容的辅助函数
    const cleanMessageContent = (content: string): string => {
      if (!content) return content;
      
      // 如果内容包含原始对象字符串，尝试提取实际内容
      if (content.includes('UserMessage{') || content.includes('AssistantMessage[')) {
        // 尝试从UserMessage中提取content
        const userMatch = content.match(/UserMessage\{content='([^']+)'/);
        if (userMatch) {
          return userMatch[1];
        }
        
        // 尝试从AssistantMessage中提取textContent - 精确匹配格式
        // 匹配: textC内容, metadata=...
        const assistantMatch = content.match(/textC([^,]+?)(?=, metadata|$)/);
        if (assistantMatch) {
          return assistantMatch[1].trim();
        }
        
        // 尝试匹配完整的AssistantMessage格式 - 更精确的匹配
        const assistantMatch2 = content.match(/AssistantMessage\s*\[[^\]]*textC([^,]+?)(?=, metadata|$)/);
        if (assistantMatch2) {
          return assistantMatch2[1].trim();
        }
        
        // 尝试匹配到metadata之前的所有内容 - 使用非贪婪匹配
        const assistantMatch3 = content.match(/textC(.+?)(?=, metadata|$)/);
        if (assistantMatch3) {
          return assistantMatch3[1].trim();
        }
        
        // 尝试匹配到字符串结尾的所有内容
        const assistantMatch4 = content.match(/textC(.+)$/);
        if (assistantMatch4) {
          return assistantMatch4[1].trim();
        }
        
        // 尝试匹配到metadata之前的所有内容 - 更宽泛的匹配
        const assistantMatch5 = content.match(/textC(.+?)(?=, metadata)/);
        if (assistantMatch5) {
          return assistantMatch5[1].trim();
        }
        
        // 尝试匹配到metadata之前的所有内容 - 最宽泛的匹配
        const assistantMatch6 = content.match(/textC(.+?)(?=, metadata)/);
        if (assistantMatch6) {
          return assistantMatch6[1].trim();
        }
        
        // 尝试匹配到metadata之前的所有内容 - 最宽泛的匹配
        const assistantMatch7 = content.match(/textC(.+?)(?=, metadata)/);
        if (assistantMatch7) {
          return assistantMatch7[1].trim();
        }
        
        // 尝试匹配到metadata之前的所有内容 - 最宽泛的匹配
        const assistantMatch8 = content.match(/textC(.+?)(?=, metadata)/);
        if (assistantMatch8) {
          return assistantMatch8[1].trim();
        }
        
        // 尝试匹配到metadata之前的所有内容 - 最宽泛的匹配
        const assistantMatch9 = content.match(/textC(.+?)(?=, metadata)/);
        if (assistantMatch9) {
          return assistantMatch9[1].trim();
        }
        
        // 尝试匹配到metadata之前的所有内容 - 最宽泛的匹配
        const assistantMatch10 = content.match(/textC(.+?)(?=, metadata)/);
        if (assistantMatch10) {
          return assistantMatch10[1].trim();
        }
        
        // 尝试匹配到metadata之前的所有内容 - 最宽泛的匹配
        const assistantMatch11 = content.match(/textC(.+?)(?=, metadata)/);
        if (assistantMatch11) {
          return assistantMatch11[1].trim();
        }
        
        // 如果无法提取，返回清理后的内容
        return content.replace(/UserMessage\{[^}]+\}/g, '').replace(/AssistantMessage\[[^\]]+\]/g, '').trim();
      }
      
      return content;
    };

    // 转换消息格式
    const messages = response.data.messages.map((msg: any) => ({
      id: msg.messageId,
      characterId: msg.characterId,
      content: cleanMessageContent(msg.content),
      isUser: msg.isUser,
      timestamp: msg.timestamp,
      streaming: false,
      audioUrl: msg.audioUrl,
      // 语音相关字段
      isVoiceMessage: msg.isVoiceMessage || msg.content?.includes('🎵'),
      voiceDuration: msg.voiceDuration,
      voice: msg.voice,
      languageType: msg.languageType
    }))
    
    return {
      messages,
      total: response.data.total,
      hasMore: response.data.hasMore,
      sourceStats: response.data.sourceStats,
      queryDays: response.data.queryDays
    }
  } else {
    throw new Error('获取对话历史失败：响应格式错误')
  }
}

/**
 * 清空当前角色的聊天记录
 */
export const clearCurrentCharacterChat = async (characterId: number) => {
  console.log('[chatApi] 清空当前角色聊天记录:', characterId)
  
  const response = await axios.delete(`/api/chat/conversation/${characterId}`)
  
  console.log('[chatApi] 清空当前角色聊天记录成功')
  return response.data
}

/**
 * 清空所有聊天记录
 */
export const clearAllChats = async () => {
  console.log('[chatApi] 清空所有聊天记录')
  
  const response = await axios.delete('/api/chat/conversation/all')
  
  console.log('[chatApi] 清空所有聊天记录成功')
  return response.data
}

/**
 * 删除对话（保留原有接口）
 */
export const deleteConversation = async (conversationId: string) => {
  console.log('[chatApi] 删除对话:', conversationId)
  
  const response = await axios.delete(`/api/chat/conversation/${conversationId}`)
  
  if (response.data.code === 200) {
    console.log('[chatApi] 删除对话成功')
    return response.data.data
  } else {
    throw new Error(response.data.message || '删除对话失败')
  }
}

// 更新语音时长请求接口
export interface UpdateVoiceDurationRequest {
  conversationId?: string  // 会话ID（可选，如果不提供会根据characterId生成）
  messageContent: string   // 消息内容（用于定位消息）
  voiceDuration: number    // 语音时长（秒）
  characterId?: number     // 角色ID（可选，用于生成会话ID）
}

// 更新语音时长响应接口
export interface UpdateVoiceDurationResponse {
  success: boolean
  conversationId: string
  voiceDuration: number
  message: string
}

/**
 * 更新消息的语音时长
 * 前端录音完成后调用此接口更新消息的语音时长
 */
export const updateVoiceDuration = async (data: UpdateVoiceDurationRequest): Promise<UpdateVoiceDurationResponse> => {
  console.log('[chatApi] 更新语音时长:', data)
  
  const response = await axios.post('/api/chat/update-voice-duration', data)
  
  if (response.data.success !== undefined) {
    // 后端直接返回结果对象，不包装在code中
    console.log('[chatApi] 更新语音时长成功:', response.data)
    return response.data
  } else if (response.data.code === 200) {
    // 兼容包装格式
    console.log('[chatApi] 更新语音时长成功:', response.data.data)
    return response.data.data
  } else {
    throw new Error(response.data.message || '更新语音时长失败')
  }
}
