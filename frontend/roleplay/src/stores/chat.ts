import { defineStore } from 'pinia'
import { ref, computed, nextTick } from 'vue'
import { getCharacterList, getChatHistory, clearCurrentCharacterChat, clearAllChats, type ChatHistoryResponse } from '@/api/chat'
import { qwenAPI, type QwenConversationInfo } from '@/api/qwen'

export interface Character {
  id: number  // ✅ 使用 number 类型，不要传字符串
  name: string
  avatar: string  // 前端显示用的头像路径
  avatarUrl?: string | null  // API返回的头像URL
  unread: number
  description?: string
  backgroundStory?: string
  personalityTraits?: string
  speakingStyle?: string
  expertiseArea?: string
  voiceStyle?: string
  status?: number
  displayName?: string
  complete?: boolean
  theme?: string // 角色主题色
}

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

export interface PendingMessage {
  content: string
  voiceBlob?: Blob
  characterId?: number  // ✅ 使用 number 类型
}

export const useChatStore = defineStore('chat', () => {
  // 状态
  const currentCharacterId = ref<number | null>(null)  // ✅ 使用 number 类型
  const messageList = ref<ChatMessage[]>([])
  const voiceMode = ref<'text' | 'voiceRecording' | 'voiceTranscribing'>('text')
  const pendingMessage = ref<PendingMessage | null>(null)
  // ✅优化 音频互斥播放 - 添加当前播放的消息ID
  const currentPlayingId = ref<string | null>(null)
  // ✅优化 自动滚动 - 添加正在流式输出的消息ID
  const streamingId = ref<string | null>(null)
  // RAG开关状态（默认开启）
  const enableRag = ref<boolean>(true)

  // Qwen 多会话状态
  const qwenConversations = ref<QwenConversationInfo[]>([])
  const currentQwenConversationId = ref<string | null>(null)

  // ✅优化 自动滚动 - 滚动到底部的方法
  const scrollToBottom = (smooth = true, force = false) => {
    nextTick(() => {
      const el = document.querySelector('.messages-container') as HTMLElement
      if (!el) return

      // 如果不是强制滚动，且用户滚到离底部超过 150px，就暂停自动滚动
      // 阈值从 50px 增加到 150px，避免平滑滚动时的计算误差导致误判
      if (!force && el.scrollHeight - el.scrollTop - el.clientHeight > 150) return

      el.scrollTo({
        top: el.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
    })
  }

  // ✅优化 自动滚动 - 追加流式内容并立即滚动
  const appendToStream = (messageId: string, delta: string) => {
    const message = messageList.value.find(m => m.id === messageId)
    if (!message) return

    message.content += delta

    // 强制触发响应式更新
    nextTick(() => {
      // 触发DOM更新
      // ❌ 禁用平滑滚动：流式输出时使用 instant 滚动，防止平滑滚动的动画延迟导致 scrollTop 计算滞后，
      // 从而误触发"用户向上滚动"的检测逻辑，导致自动滚动停止。
      scrollToBottom(false, false)
    })
  }

  // 初始化时恢复最后访问的角色
  const initLastCharacter = () => {
    const lastCharacterId = localStorage.getItem('LAST_CHARACTER_ID')
    if (lastCharacterId && !currentCharacterId.value) {
      const characterId = Number(lastCharacterId)
      if (!isNaN(characterId)) {
        currentCharacterId.value = characterId
        console.log('[ChatStore] 恢复最后访问的角色ID:', characterId)
      }
    }
  }
  // 角色列表 - 从API动态加载
  const characters = ref<Character[]>([])

  // 计算属性
  const currentCharacter = computed(() =>
    characters.value.find(c => c.id === currentCharacterId.value)
  )

  const currentMessages = computed(() =>
    messageList.value.filter(m => m.characterId === currentCharacterId.value)
  )

  // 方法
  const setCurrentCharacter = (characterId: number) => {  // ✅ 使用 number 类型
    currentCharacterId.value = characterId

    // 保存最后访问的角色ID到 localStorage
    localStorage.setItem('LAST_CHARACTER_ID', characterId.toString())
    console.log('[ChatStore] 保存最后访问的角色ID:', characterId)

    // ✅ ID=0 是通用AI助手（类似ChatGPT），自动关闭 RAG
    // 因为通用AI不需要角色知识库，只有角色扮演才需要
    if (characterId === 0) {
      enableRag.value = false
      console.log('[ChatStore] 切换到AI助手，自动关闭RAG')
    }

    // 清除未读消息
    const character = characters.value.find(c => c.id === characterId)
    if (character) {
      character.unread = 0
    }
  }

  const addMessage = (message: Omit<ChatMessage, 'id' | 'timestamp'>) => {
    // ✅修复 使用更可靠的ID生成方法，避免同一毫秒内的ID冲突
    const timestamp = Date.now()
    const randomSuffix = Math.random().toString(36).substr(2, 9)
    const newMessage: ChatMessage = {
      ...message,
      id: `${timestamp}_${randomSuffix}`,
      timestamp: timestamp
    }

    // 🔍 调试日志：追踪消息创建
    console.log('[ChatStore] 创建新消息:', {
      messageId: newMessage.id,
      isUser: newMessage.isUser,
      content: newMessage.content.substring(0, 50) + '...',
      characterId: newMessage.characterId,
      streaming: newMessage.streaming
    })

    messageList.value.push(newMessage)

    // ✅优化 自动滚动 - 如果是流式消息，设置streamingId
    if (newMessage.streaming) {
      streamingId.value = newMessage.id
    }

    // 新消息加入时，强制滚动到底部
    scrollToBottom(true, true)

    // 🔍 调试日志：验证消息已添加到列表
    console.log('[ChatStore] 消息已添加到列表，当前总数:', messageList.value.length)
    console.log('[ChatStore] 最新消息列表 isUser 状态:',
      messageList.value.map(m => ({ id: m.id, isUser: m.isUser, content: m.content.substring(0, 20) }))
    )

    return newMessage
  }

  const updateMessage = (messageId: string, updates: Partial<ChatMessage>) => {
    const index = messageList.value.findIndex(m => m.id === messageId)
    if (index !== -1) {
      const oldMessage = messageList.value[index]
      const updatedMessage = { ...oldMessage, ...updates }

      // 🔍 调试日志：追踪消息更新
      console.log('[ChatStore] 更新消息:', {
        messageId: messageId,
        oldIsUser: oldMessage.isUser,
        newIsUser: updatedMessage.isUser,
        isUserChanged: oldMessage.isUser !== updatedMessage.isUser,
        updates: updates,
        oldContent: oldMessage.content.substring(0, 30) + '...',
        newContent: updatedMessage.content.substring(0, 30) + '...',
        streamingChanged: oldMessage.streaming !== updatedMessage.streaming
      })

      messageList.value[index] = updatedMessage

      // ✅优化 自动滚动 - 如果停止流式输出，清除streamingId并强制刷新
      if (oldMessage.streaming && !updatedMessage.streaming) {
        streamingId.value = null
        console.log('[ChatStore] 流式输出结束，触发最终刷新:', messageId)

        // 强制触发响应式更新，确保组件重新渲染
        nextTick(() => {
          // 通过修改消息的时间戳来强制更新
          const message = messageList.value[index]
          if (message) {
            message.timestamp = Date.now()
          }
        })
      }

      // 🔍 如果 isUser 字段发生了变化，记录警告
      if (oldMessage.isUser !== updatedMessage.isUser) {
        console.warn('🚨 [ChatStore] 警告：消息的 isUser 字段发生了变化！', {
          messageId: messageId,
          from: oldMessage.isUser,
          to: updatedMessage.isUser,
          updates: updates
        })
      }
    }
  }

  // ✅优化 自动滚动 - 停止流式输出
  const stopStream = (messageId: string) => {
    const message = messageList.value.find(m => m.id === messageId)
    if (message) {
      message.streaming = false
      streamingId.value = null
    }
  }

  const removeMessage = (messageId: string) => {
    const index = messageList.value.findIndex(m => m.id === messageId)
    if (index !== -1) {
      messageList.value.splice(index, 1)
    }
  }

  const setVoiceMode = (mode: typeof voiceMode.value) => {
    voiceMode.value = mode
  }

  const clearMessages = (characterId?: number) => {  // ✅ 使用 number 类型
    if (characterId) {
      messageList.value = messageList.value.filter(m => m.characterId !== characterId)
    } else {
      messageList.value = []
    }
  }

  const setPendingMessage = (message: PendingMessage) => {
    pendingMessage.value = message
  }

  const clearPendingMessage = () => {
    pendingMessage.value = null
  }

  // 角色头像映射 - 根据角色名称映射到本地头像
  const getCharacterAvatar = (character: any): string => {
    const avatarMap: Record<string, string> = {
      '江户川柯南': '/src/assets/characters/conan.svg',
      '泰拉瑞亚向导': '/src/assets/characters/terraria-guide.svg',
      '哈利·波特': '/src/assets/characters/harry.svg',
      '苏格拉底': '/src/assets/characters/socrates.svg',
      '爱因斯坦': '/src/assets/characters/einstein.svg'
    }

    // 如果API返回了头像URL，优先使用
    if (character.avatarUrl) {
      return character.avatarUrl
    }

    // 否则使用本地映射的头像
    return avatarMap[character.name] || '/src/assets/characters/default.svg'
  }

  // 角色主题色映射
  const getCharacterTheme = (name: string): string => {
    const themeMap: Record<string, string> = {
      '江户川柯南': 'blue',
      '泰拉瑞亚向导': 'green',
      '哈利·波特': 'amber',
      '苏格拉底': 'emerald',
      '爱因斯坦': 'indigo'
    }
    return themeMap[name] || 'gray'
  }

  // 加载角色列表
  const loadCharacters = async () => {
    try {
      console.log('[ChatStore] 开始加载角色列表')
      const characterList = await getCharacterList()

      // 处理API数据，添加前端需要的字段
      characters.value = characterList.map((char: any) => ({
        id: char.id,
        name: char.name,
        avatar: getCharacterAvatar(char),  // 映射头像路径
        avatarUrl: char.avatarUrl,
        unread: 0,  // 默认未读消息数为0
        description: char.description,
        backgroundStory: char.backgroundStory,
        personalityTraits: char.personalityTraits,
        speakingStyle: char.speakingStyle,
        expertiseArea: char.expertiseArea,
        voiceStyle: char.voiceStyle,
        status: char.status,
        displayName: char.displayName,
        complete: char.complete,
        theme: getCharacterTheme(char.name) // 分配主题色
      }))

      // ✅ 注入虚拟"Qwen"角色（ID=0，通用AI对话模式）
      const virtualAssistant: Character = {
        id: 0,
        name: 'Qwen',
        avatar: 'http://oss.kon-carol.xyz/airole0.png',
        avatarUrl: 'http://oss.kon-carol.xyz/airole0.png',
        unread: 0,
        description: '通用 AI 助手，可以回答各种问题',
        backgroundStory: '我是您的智能助手，随时为您服务。',
        personalityTraits: '乐于助人, 智能, 友好',
        speakingStyle: '简洁, 准确',
        expertiseArea: '通用知识',
        voiceStyle: 'default',
        status: 1,
        displayName: 'Qwen',
        complete: true,
        theme: 'blue'
      }

      // 将 Qwen 添加到列表最前面
      characters.value.unshift(virtualAssistant)

      console.log('[ChatStore] 角色列表加载成功:', characterList.length, '个角色')
      console.log('[ChatStore] 处理后的角色数据:', characters.value)
    } catch (error) {
      console.error('[ChatStore] 加载角色列表失败:', error)
      // 如果加载失败，设置一个默认的角色以防止应用崩溃
      characters.value = [{
        id: 1,
        name: '默认角色',
        avatar: '/src/assets/characters/default.webp',
        unread: 0,
        description: '角色加载失败，请刷新页面重试'
      }]
    }
  }

  // 加载聊天历史
  const loadMessages = async (characterId: number) => {
    try {
      console.log('[ChatStore] 开始加载聊天历史:', characterId)

      // 如果是 Qwen (ID=0)，传入 conversationId
      let conversationId: string | undefined = undefined
      if (characterId === 0 && currentQwenConversationId.value) {
        conversationId = currentQwenConversationId.value
        console.log('[ChatStore] 加载 Qwen 会话历史:', conversationId)
      }

      const historyResponse: ChatHistoryResponse = await getChatHistory(characterId, conversationId)

      // 清除当前角色的旧消息
      messageList.value = messageList.value.filter(m => m.characterId !== characterId)

      // 添加历史消息
      messageList.value.push(...historyResponse.messages)

      console.log('[ChatStore] 聊天历史加载完成:', {
        characterId,
        messageCount: historyResponse.messages.length,
        total: historyResponse.total,
        sourceStats: historyResponse.sourceStats
      })

      return historyResponse

    } catch (error) {
      console.error('[ChatStore] 加载聊天历史失败:', error)
      throw error
    }
  }

  // 清空当前角色的聊天记录
  const clearCurrentCharacterMessages = async (characterId: number) => {
    try {
      console.log('[ChatStore] 开始清空当前角色聊天记录:', characterId)

      // 调用后端API清空记录
      await clearCurrentCharacterChat(characterId)

      // 清空前端本地记录
      messageList.value = messageList.value.filter(m => m.characterId !== characterId)

      console.log('[ChatStore] 当前角色聊天记录清空完成')

    } catch (error) {
      console.error('[ChatStore] 清空当前角色聊天记录失败:', error)
      throw error
    }
  }

  // 清空所有聊天记录
  const clearAllMessages = async () => {
    try {
      console.log('[ChatStore] 开始清空所有聊天记录')

      // 调用后端API清空所有记录
      await clearAllChats()

      // 清空前端本地记录
      messageList.value = []

      console.log('[ChatStore] 所有聊天记录清空完成')

    } catch (error) {
      console.error('[ChatStore] 清空所有聊天记录失败:', error)
      throw error
    }
  }

  // 切换RAG功能
  const toggleRag = () => {
    enableRag.value = !enableRag.value
    console.log('[ChatStore] RAG功能已', enableRag.value ? '启用' : '禁用')
  }

  // ==================== Qwen 会话管理 ====================

  // 加载 Qwen 会话列表
  const loadQwenConversations = async () => {
    try {
      const response = await qwenAPI.listConversations()
      const list = response.data
      qwenConversations.value = list

      // 如果没有当前会话但有列表，默认选中第一个
      if (!currentQwenConversationId.value && list.length > 0) {
        currentQwenConversationId.value = list[0].conversationId
      }
      // 如果列表为空，创建一个新会话
      else if (list.length === 0) {
        await createQwenConversation()
      }
    } catch (error) {
      console.error('[ChatStore] 加载 Qwen 会话列表失败:', error)
    }
  }

  // 创建新 Qwen 会话
  const createQwenConversation = async () => {
    try {
      const response = await qwenAPI.createConversation()
      const res = response.data
      const newConv: QwenConversationInfo = {
        conversationId: res.conversationId,
        title: res.title || '新对话',
        lastMessage: '',
        lastActiveTime: res.createdAt,
        createdAt: res.createdAt,
        messageCount: 0
      }
      qwenConversations.value.unshift(newConv)
      currentQwenConversationId.value = res.conversationId

      // 清空当前消息列表
      if (currentCharacterId.value === 0) {
        messageList.value = []
      }

      return res.conversationId
    } catch (error) {
      console.error('[ChatStore] 创建 Qwen 会话失败:', error)
      throw error
    }
  }

  // 切换 Qwen 会话
  const switchQwenConversation = async (conversationId: string) => {
    if (currentQwenConversationId.value === conversationId) return

    currentQwenConversationId.value = conversationId

    // 如果当前是 Qwen 角色，重新加载消息
    if (currentCharacterId.value === 0) {
      await loadMessages(0)
    }
  }

  // 删除 Qwen 会话
  const deleteQwenConversation = async (conversationId: string) => {
    try {
      await qwenAPI.deleteConversation(conversationId)
      qwenConversations.value = qwenConversations.value.filter(c => c.conversationId !== conversationId)

      // 如果删除的是当前会话，切换到第一个
      if (currentQwenConversationId.value === conversationId) {
        if (qwenConversations.value.length > 0) {
          await switchQwenConversation(qwenConversations.value[0].conversationId)
        } else {
          // 如果删光了，创建一个新的
          await createQwenConversation()
        }
      }
    } catch (error) {
      console.error('[ChatStore] 删除 Qwen 会话失败:', error)
      throw error
    }
  }

  // 重命名 Qwen 会话
  const renameQwenConversation = async (conversationId: string, title: string) => {
    try {
      await qwenAPI.renameConversation(conversationId, title)
      const conv = qwenConversations.value.find(c => c.conversationId === conversationId)
      if (conv) {
        conv.title = title
      }
    } catch (error) {
      console.error('[ChatStore] 重命名 Qwen 会话失败:', error)
      throw error
    }
  }

  return {
    // 状态
    currentCharacterId,
    messageList,
    voiceMode,
    characters,
    pendingMessage,
    currentPlayingId,  // ✅优化 音频互斥播放
    streamingId,       // ✅优化 自动滚动
    scrollToBottom,    // ✅优化 自动滚动 - 滚动方法
    appendToStream,    // ✅优化 自动滚动 - 流式追加方法
    enableRag,         // RAG开关状态

    // 计算属性
    currentCharacter,
    currentMessages,

    // 方法
    initLastCharacter,
    setCurrentCharacter,
    addMessage,
    updateMessage,
    stopStream,     // ✅优化 自动滚动
    removeMessage,
    setVoiceMode,
    clearMessages,
    setPendingMessage,
    clearPendingMessage,
    loadCharacters,
    loadMessages,
    clearCurrentCharacterMessages,
    clearAllMessages,
    toggleRag,          // RAG切换方法

    // Qwen 会话相关
    qwenConversations,
    currentQwenConversationId,
    loadQwenConversations,
    createQwenConversation,
    switchQwenConversation,
    deleteQwenConversation,
    renameQwenConversation
  }
})
