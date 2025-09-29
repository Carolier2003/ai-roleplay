/**
 * TTS播放器 Hook
 * 支持音频队列播放，边收文字边播声音
 * 使用 AudioContext 实现专业音频处理和淡入淡出效果
 */

import { ref } from 'vue'

// 开发环境测试：验证TTS文本过滤功能
if (process.env.NODE_ENV === 'development') {
  console.log('🚀 TTS文本过滤功能已启用 - 控制台输入 testTTSFiltering() 测试')
  // 全局暴露测试函数
  ;(window as any).testTTSFiltering = () => {
    const { testTextFiltering } = useTTSPlayer()
    if (testTextFiltering) {
      testTextFiltering()
    } else {
      console.error('testTextFiltering 函数未找到')
    }
  }

  // 立即运行测试
  setTimeout(() => {
    console.log('🔧 自动运行TTS文本过滤测试...')
    ;(window as any).testTTSFiltering()
  }, 1000)
}

export interface TTSQueueItem {
  text: string
  characterId: number
  isFirst: boolean
  audioBlob?: Blob
  audioUrl?: string
  synthesizing?: boolean // 是否正在合成中
  synthesizePromise?: Promise<Blob | null> // 合成Promise
}

// 限流器接口
interface RateLimiter {
  tokens: number
  lastRefill: number
  maxTokens: number
  refillRate: number // tokens per second
}

// 重试配置接口
interface RetryConfig {
  maxRetries: number
  baseDelay: number // 基础延迟（毫秒）
  maxDelay: number // 最大延迟（毫秒）
  backoffFactor: number // 退避因子
}

// 重试状态接口
interface RetryState {
  attempt: number
  lastError?: Error
}

export function useTTSPlayer() {
  const isPlaying = ref(false)
  const currentItem = ref<TTSQueueItem | null>(null)
  const queueLength = ref(0)
  
  // 音频队列播放
  const audioQueue: TTSQueueItem[] = []
  let isProcessingQueue = false
  
  // 队列清空回调
  let onQueueEmpty: (() => void) | null = null
  
  // 限流器：最大1次/2秒（进一步降低频率减少400错误）
  const rateLimiter: RateLimiter = {
    tokens: 1,
    lastRefill: Date.now(),
    maxTokens: 1,
    refillRate: 0.5
  }

  // 重试配置
  const retryConfig: RetryConfig = {
    maxRetries: 3, // 最多重试3次
    baseDelay: 1000, // 基础延迟1秒
    maxDelay: 10000, // 最大延迟10秒
    backoffFactor: 2 // 指数退避因子
  }
  
  // 创建单一 AudioContext 实例并复用
  let audioContext: AudioContext | null = null
  let currentSource: AudioBufferSourceNode | null = null
  let currentGainNode: GainNode | null = null
  
  const getAudioContext = (): AudioContext => {
    if (!audioContext) {
      audioContext = new (window.AudioContext || (window as any).webkitAudioContext)()
      console.log('[useTTSPlayer] AudioContext 创建成功')
    }
    
    // 确保 AudioContext 处于运行状态
    if (audioContext.state === 'suspended') {
      audioContext.resume()
      console.log('[useTTSPlayer] AudioContext 已恢复')
    }
    
    return audioContext
  }

  // 限流器：获取令牌
  const acquireToken = async (): Promise<void> => {
    return new Promise((resolve) => {
      const tryAcquire = () => {
        const now = Date.now()
        const timePassed = (now - rateLimiter.lastRefill) / 1000
        
        // 填充令牌：每秒填充refillRate个令牌
        const tokensToAdd = Math.floor(timePassed * rateLimiter.refillRate)
        if (tokensToAdd > 0) {
          rateLimiter.tokens = Math.min(rateLimiter.maxTokens, rateLimiter.tokens + tokensToAdd)
          rateLimiter.lastRefill = now
        }
        
        // 尝试获取令牌
        if (rateLimiter.tokens > 0) {
          rateLimiter.tokens--
          console.log('[useTTSPlayer] 获取令牌成功，剩余:', rateLimiter.tokens)
          resolve()
                } else {
                  // 没有令牌，2000ms后重试（适应每两秒1次的频率）
                  console.log('[useTTSPlayer] 令牌不足，2000ms后重试')
                  setTimeout(tryAcquire, 2000)
                }
      }
      
      tryAcquire()
    })
  }

  // 判断错误是否可重试
  const isRetryableError = (error: Error): boolean => {
    const message = error.message.toLowerCase()

    // 网络相关错误 - 可重试
    if (message.includes('network') ||
        message.includes('timeout') ||
        message.includes('连接') ||
        message.includes('fetch')) {
      return true
    }

    // HTTP状态码相关错误 - 部分可重试
    if (message.includes('500') || // 服务器内部错误
        message.includes('502') || // 网关错误
        message.includes('503') || // 服务不可用
        message.includes('504')) { // 网关超时
      return true
    }

    // 客户端错误 - 400降级，其他不可重试
    if (message.includes('401') || // 未授权
        message.includes('403') || // 禁止访问
        message.includes('404')) { // 未找到
      return false
    }

    // 400错误特殊处理
    if (message.includes('400')) {
      // 检查是否为"不适合语音合成"错误 - 直接跳过
      if (message.includes('不适合语音合成')) {
        console.log('[useTTSPlayer] 400错误-不适合语音合成，直接跳过:', message)
        return false // 不重试，直接返回null
      }

      // 检查是否为429限流错误 - 可重试
      if (message.includes('429') || message.includes('rate limit') || message.includes('throttling')) {
        console.log('[useTTSPlayer] 400错误-429限流，可重试:', message)
        return true // 可重试
      }

      // 其他400错误 - 降级策略，不重试但返回null而不是抛异常
      console.log('[useTTSPlayer] 400错误-其他，直接跳过:', message)
      return false // 不重试，但不会抛异常
    }

    // 其他未知错误 - 可重试
    return true
  }

  // 计算重试延迟（指数退避）
  const calculateRetryDelay = (attempt: number): number => {
    const delay = retryConfig.baseDelay * Math.pow(retryConfig.backoffFactor, attempt - 1)
    return Math.min(delay, retryConfig.maxDelay)
  }

  // 带重试的异步操作执行器
  const executeWithRetry = async <T>(
    operation: () => Promise<T>,
    context: string,
    retryState: RetryState = { attempt: 0 }
  ): Promise<T | null> => {
    try {
      const result = await operation()
      
      // 成功时重置重试状态
      if (retryState.attempt > 0) {
        console.log(`[useTTSPlayer] ${context} 重试成功，总尝试次数: ${retryState.attempt + 1}`)
      }
      
      return result
    } catch (error) {
      const currentError = error as Error
      retryState.lastError = currentError
      retryState.attempt++
      
      console.warn(`[useTTSPlayer] ${context} 第${retryState.attempt}次尝试失败:`, currentError.message)
      
      // 检查是否应该重试
      const shouldRetry = retryState.attempt < retryConfig.maxRetries && isRetryableError(currentError)
      
      if (shouldRetry) {
        const delay = calculateRetryDelay(retryState.attempt)
        console.log(`[useTTSPlayer] ${context} 将在${delay}ms后进行第${retryState.attempt + 1}次重试`)
        
        // 等待指定时间后重试
        await new Promise(resolve => setTimeout(resolve, delay))
        return executeWithRetry(operation, context, retryState)
      } else {
        // 特殊处理400错误
        if (retryState.lastError && retryState.lastError.message.includes('400')) {
          // 检查是否为429限流错误
          if (retryState.lastError.message.includes('429') ||
              retryState.lastError.message.includes('rate limit') ||
              retryState.lastError.message.includes('throttling')) {
            // 429限流错误：指数退避后最终跳过
            if (retryState.attempt >= retryConfig.maxRetries) {
              console.log(`[TTS-429] ${context} 最终限流，跳过`)
              return null
            }
            // 继续重试，使用指数退避
            const delay = calculateRetryDelay(retryState.attempt)
            console.log(`[TTS-429] ${context} 退避${delay}ms后重试（第${retryState.attempt + 1}次）`)
            await new Promise(resolve => setTimeout(resolve, delay))
            return executeWithRetry(operation, context, retryState)
          }

          // 检查是否为不适合语音合成错误
          if (retryState.lastError.message.includes('不适合语音合成')) {
            console.log(`[useTTSPlayer] ${context} 不适合语音合成，直接跳过`)
            return null
          }

          // 其他400错误：直接跳过
          console.log(`[useTTSPlayer] ${context} 其他400错误，跳过`)
          return null
        }

        // 不再重试，抛出最后的错误
        if (retryState.attempt >= retryConfig.maxRetries) {
          console.error(`[useTTSPlayer] ${context} 达到最大重试次数(${retryConfig.maxRetries})，放弃重试`)
        } else {
          console.error(`[useTTSPlayer] ${context} 错误不可重试:`, currentError.message)
        }
        throw currentError
      }
    }
  }

  // 添加到播放队列
  const addToQueue = async (text: string, characterId: number, isFirst: boolean = false): Promise<void> => {
    console.log('[useTTSPlayer] 添加到播放队列:', { text, characterId, isFirst })
    
    if (!text || text.trim().length === 0) {
      console.warn('[useTTSPlayer] 文本为空，跳过')
      return
    }

    const trimmedText = text.trim()
    
    // 队列去重检查 - 避免重复添加相同文本
    const isDuplicate = audioQueue.some(item => item.text === trimmedText)
    if (isDuplicate) {
      console.warn('[useTTSPlayer] 检测到重复文本，跳过:', trimmedText)
      return
    }

    const queueItem: TTSQueueItem = {
      text: trimmedText,
      characterId,
      isFirst,
      synthesizing: false
    }

    audioQueue.push(queueItem)
    queueLength.value = audioQueue.length
    
    console.log('[useTTSPlayer] 队列长度:', queueLength.value)

    // 立即启动并行合成（不等待播放）
    startSynthesisInBackground(queueItem)

    // 开始处理播放队列
    if (!isProcessingQueue) {
      processQueue()
    }
  }

  // 后台并行合成语音
  const startSynthesisInBackground = async (item: TTSQueueItem): Promise<void> => {
    if (item.synthesizing || item.audioBlob) {
      return // 已经在合成中或已完成
    }

    item.synthesizing = true
    console.log('[useTTSPlayer] 开始后台合成:', item.text.substring(0, 30) + '...')

    try {
      // 获取令牌（限流）
      await acquireToken()
      
      // 开始合成
      item.synthesizePromise = synthesizeText(item.text, item.characterId)
      const audioBlob = await item.synthesizePromise
      
      if (audioBlob) {
        item.audioBlob = audioBlob
        console.log('[useTTSPlayer] 后台合成完成:', item.text.substring(0, 30) + '...')
      } else {
        console.warn('[useTTSPlayer] 后台合成失败:', item.text.substring(0, 30) + '...')
      }
    } catch (error) {
      console.error('[useTTSPlayer] 后台合成错误:', error, item.text.substring(0, 30) + '...')
    } finally {
      item.synthesizing = false
    }
  }

  // 处理播放队列
  const processQueue = async (): Promise<void> => {
    if (isProcessingQueue) {
      return
    }

    isProcessingQueue = true
    console.log('[useTTSPlayer] 开始处理播放队列')

    while (audioQueue.length > 0) {
      const item = audioQueue.shift()
      if (!item) continue

      currentItem.value = item
      queueLength.value = audioQueue.length

      try {
        console.log('[useTTSPlayer] 播放队列项:', item.text.substring(0, 30) + '...')
        
        let audioBlob: Blob | null = null

        // 检查是否已经有合成好的音频
        if (item.audioBlob) {
          audioBlob = item.audioBlob
          console.log('[useTTSPlayer] 使用已合成的音频')
        } else if (item.synthesizePromise) {
          // 等待后台合成完成
          console.log('[useTTSPlayer] 等待后台合成完成...')
          audioBlob = await item.synthesizePromise
        } else {
          // 如果后台合成还没开始，立即合成
          console.log('[useTTSPlayer] 立即合成音频...')
          await acquireToken()
          audioBlob = await synthesizeText(item.text, item.characterId)
        }

        if (!audioBlob) {
          console.warn('[useTTSPlayer] 语音合成失败，跳过:', item.text.substring(0, 30) + '...')
          continue
        }

        item.audioBlob = audioBlob

        // 播放音频 - 直接传递 Blob，不再创建 URL
        await playAudio(audioBlob)
        
        console.log('[useTTSPlayer] 播放完成:', item.text.substring(0, 30) + '...')

      } catch (error) {
        console.error('[useTTSPlayer] 处理队列项失败:', error, item.text.substring(0, 30) + '...')
      }
    }

    isProcessingQueue = false
    currentItem.value = null
    queueLength.value = 0
    
    console.log('[useTTSPlayer] 播放队列处理完成')
    
    // 触发队列清空回调
    onQueueEmpty?.()
  }

  // 文本预处理函数 - 过滤Markdown符号和无效内容
  const filterText = (text: string): string | null => {
    const originalText = text.trim()
    
    // 长度检查
    if (originalText.length < 2) {
      console.log(`[TTS-Skip] 非法句子:"${originalText}"→ 已跳过`)
      return null
    }

    // 定义过滤规则
    const mdSymbols = /[\*\#\(\)\[\]\{\}「」【】～✨❤️★☆]/g
    const emoji = /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/gu
    const onlyNumSym = /^[\d\.\,\!\?\s\-\+\%\(\)]+$/g

    // 过滤Markdown符号和emoji
    let filteredText = originalText
      .replace(mdSymbols, '')     // 移除Markdown符号
      .replace(emoji, '')         // 移除emoji
      .replace(/\s+/g, ' ')       // 统一空格
      .trim()

    // 检查是否为纯数字/符号句子
    if (onlyNumSym.test(filteredText) || filteredText.length < 2) {
      console.log(`[TTS-Skip] 非法句子:"${originalText}"→ 已跳过`)
      return null
    }

    // 打印过滤结果
    if (filteredText !== originalText) {
      console.log(`[TTS-Filter] 原句:"${originalText}"→ 过滤后:"${filteredText}"`)
    }

    return filteredText
  }

  // 句子合法性检查（前端自保）
  const validateTextForTTS = (text: string): { valid: boolean; reason?: string; filteredText?: string } => {
    const trimmedText = text.trim()

    // 长度检查
    if (trimmedText.length < 2) {
      return { valid: false, reason: '文本长度过短' }
    }

    // 纯符号/数字/emoji检查
    const hasValidChars = /[\u4e00-\u9fa5a-zA-Z]/.test(trimmedText)
    if (!hasValidChars) {
      return { valid: false, reason: '纯符号/数字/emoji' }
    }

    // Markdown和emoji过滤
    const filteredText = trimmedText
      .replace(/\*\*(.*?)\*\*/g, '$1') // 粗体 **text**
      .replace(/\*(.*?)\*/g, '$1')     // 斜体 *text*
      .replace(/`(.*?)`/g, '$1')       // 代码 `code`
      .replace(/~~(.*?)~~/g, '$1')     // 删除线 ~~text~~
      .replace(/#{1,6}\s*/g, '')       // 标题 # ## ###
      .replace(/!\[.*?\]\(.*?\)/g, '')  // 图片 ![alt](url)
      .replace(/\[.*?\]\(.*?\)/g, '$1') // 链接 [text](url) -> text
      .replace(/[✨🎉❤️💕💖🌟⭐🎊💯🔥💪👍👎👌✌️👏🙌🤝👀💭🎤🎵🎶💃✈️🚀🌈☀️⛅🌙⭐🌟⚡🔥💥❄️🌊💨🍀🌸🌺🌻🌼🌷🌹🌺🍎🍊🍋🍌🍉🍇🍓🍈🍒🍑🍍🥝🥑🍅🍆🥒🥕🌽🌶️🥔🍠🥐🍞🥖🥨🥯🧀🥚🍳🥞🥓🥩🍗🍖🦴🌭🍔🍟🍕🫓🥙🌮🌯🫔🥗🥘🫕🍝🍜🍲🍛🍣🍱🥟🦪🍤🍙🍚🍘🍥🥠🥮🍢🍡🍧🍨🍦🥧🧁🍰🎂🍮🍭🍬🍫🍿🍩🍪🌰🥜🍯🥛🍼☕🍵🧃🥤🧋🍶🍺🍻🥂🍷🥃🍸🍹🧉🍾🧊🥄🍴🍽️🥣🥡🥢🍽️🥄]/g, '') // 移除emoji
      .replace(/\s+/g, ' ')           // 统一空格
      .trim()

    // 过滤后长度检查
    if (filteredText.length < 2) {
      return { valid: false, reason: '过滤后文本过短' }
    }

    if (filteredText !== trimmedText) {
      console.log(`[TTS-Skip] 非法句子:"${trimmedText}"→ 已过滤`)
    }

    return { valid: true, filteredText }
  }

  // 合成语音（带重试机制）
  const synthesizeText = async (text: string, characterId: number): Promise<Blob | null> => {
    console.log('[useTTSPlayer] 开始语音合成:', text.substring(0, 50) + '...')

    // 文本预处理 - 在真正fetch之前先过滤
    const filteredText = filterText(text)
    if (!filteredText) {
      return null // 直接返回null，不再请求网络
    }

    // 使用过滤后的文本进行合法性检查
    const validation = validateTextForTTS(filteredText)
    if (!validation.valid) {
      console.log(`[TTS-Skip] 非法句子:"${filteredText}" | 原因:${validation.reason}`)
      return null
    }

    const finalText = validation.filteredText || filteredText
    if (finalText !== text) {
      console.log(`[TTS-Skip] 文本已过滤:"${text}" → "${finalText}"`)
    }

    try {
      // 使用重试机制执行TTS合成
      const audioBlob = await executeWithRetry(async (): Promise<Blob | null> => {
        const token = localStorage.getItem('ACCESS_TOKEN')
        const headers: Record<string, string> = {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
        
        if (token) {
          headers['Authorization'] = `Bearer ${token}`
        }

        // 构建URL参数
        const params = new URLSearchParams({
          text: finalText,
          languageType: 'Chinese'
        })

        // 使用角色TTS接口
        const response = await fetch(`http://localhost:18080/api/tts/synthesize/character/${characterId}`, {
          method: 'POST',
          headers,
          body: params
        })

        if (!response.ok) {
          // 400-详情：抓取阿里云的详细错误信息
          let errorMessage = `TTS请求失败: ${response.status}`
          let errorText = ''
          try {
            errorText = await response.text()
            console.log(`[TTS-400] text="${finalText}" | status=${response.status} | message=${errorText}`)
            errorMessage += ` | ${errorText}`
          } catch (e) {
            console.warn('[TTS-400] 无法获取错误详情:', e)
          }
          
          // 429限流错误处理 - 指数退避
          if (response.status === 429 || 
              errorText.toLowerCase().includes('rate limit') || 
              errorText.toLowerCase().includes('throttling')) {
            
            // 检查重试次数
            const retryCount = (globalThis as any).ttsRetryCount || 0
            if (retryCount >= 3) {
              console.log(`[TTS-429] 最终限流，跳过句子:"${finalText}"`)
              return null
            }
            
            // 计算退避延迟：1s, 2s, 4s
            const delays = [1000, 2000, 4000]
            const delay = delays[retryCount]
            
            console.log(`[TTS-429] 第${retryCount + 1}次失败，${delay/1000}s后重试...`)
            
            // 增加重试计数
            ;(globalThis as any).ttsRetryCount = retryCount + 1
            
            // 等待后重试
            await new Promise(resolve => setTimeout(resolve, delay))
            return await executeWithRetry(async () => {
              // 递归调用自身进行重试
              return await synthesizeText(text, characterId)
            }, `TTS合成重试[${text.substring(0, 20)}...]`)
          }
          
          // 非429的400错误（如「不适合合成」）直接返回null
          if (response.status === 400) {
            console.log(`[TTS-400] 非限流400错误，跳过句子:"${finalText}"`)
            return null
          }
          
          throw new Error(errorMessage)
        }

        const result = await response.json()
        
        if (result.code !== 200) {
          throw new Error(result.message || 'TTS合成失败')
        }

        const audioUrl = result.data?.audioUrl
        if (!audioUrl) {
          throw new Error('未获取到音频URL')
        }

        console.log('[useTTSPlayer] 获取到音频URL:', audioUrl)

        // 下载音频文件（也使用重试机制）
        const audioResponse = await fetch(audioUrl)
        if (!audioResponse.ok) {
          throw new Error(`下载音频失败: ${audioResponse.status}`)
        }

        const audioBlob = await audioResponse.blob()
        console.log('[useTTSPlayer] 音频下载完成，大小:', audioBlob.size, 'bytes')
        
        // 成功时重置重试计数
        ;(globalThis as any).ttsRetryCount = 0
        
        return audioBlob
      }, `TTS合成[${text.substring(0, 20)}...]`)
      
      return audioBlob
    } catch (error) {
      console.error('[useTTSPlayer] 语音合成最终失败:', error)
      // 失败时也重置重试计数
      ;(globalThis as any).ttsRetryCount = 0
      return null
    }
  }

  // 播放音频 - 使用 AudioContext 实现淡入淡出效果
  const playAudio = async (audioBlob: Blob): Promise<void> => {
    return new Promise(async (resolve, reject) => {
      try {
        console.log('[useTTSPlayer] 开始播放音频')

        // 首次播放时，先等待两秒，为后面的语音留足准备时间
        if (typeof (globalThis as any).firstPlayDelay === 'undefined') {
          (globalThis as any).firstPlayDelay = true
        }
        if ((globalThis as any).firstPlayDelay) {
          console.log('[useTTSPlayer] 首次播放，等待2秒准备...')
          await new Promise(resolve => setTimeout(resolve, 2000))
          ;(globalThis as any).firstPlayDelay = false
        }

        // 停止当前播放
        if (currentSource) {
          currentSource.stop()
          currentSource.disconnect()
          currentSource = null
        }

        if (currentGainNode) {
          currentGainNode.disconnect()
          currentGainNode = null
        }

        // 获取 AudioContext
        const ctx = getAudioContext()
        
        // 将 Blob 转换为 ArrayBuffer
        const arrayBuffer = await audioBlob.arrayBuffer()
        console.log('[useTTSPlayer] 音频数据大小:', arrayBuffer.byteLength, 'bytes')
        
        // 解码音频数据
        const audioBuffer = await ctx.decodeAudioData(arrayBuffer)
        console.log('[useTTSPlayer] 音频解码成功，时长:', audioBuffer.duration.toFixed(2), 's')
        
        // 创建音频节点链路：BufferSource → GainNode → Destination
        currentSource = ctx.createBufferSource()
        currentGainNode = ctx.createGain()
        
        // 连接音频节点
        currentSource.connect(currentGainNode)
        currentGainNode.connect(ctx.destination)
        
        // 设置音频缓冲区
        currentSource.buffer = audioBuffer
        
        // 设置淡入效果：从 0 线性 ramp 到 1，时长 80ms
        const fadeInDuration = 0.08 // 80ms
        const fadeOutDuration = 0.05 // 50ms
        const now = ctx.currentTime
        
        currentGainNode.gain.setValueAtTime(0, now)
        currentGainNode.gain.linearRampToValueAtTime(1, now + fadeInDuration)
        
        // 设置淡出效果：播放结束前 50ms 开始淡出
        const fadeOutStartTime = now + audioBuffer.duration - fadeOutDuration
        currentGainNode.gain.setValueAtTime(1, fadeOutStartTime)
        currentGainNode.gain.linearRampToValueAtTime(0, now + audioBuffer.duration)
        
        console.log('[useTTSPlayer] 音频节点链路已建立，开始播放')
        isPlaying.value = true
        
        // 播放结束回调
        currentSource.onended = () => {
          console.log('[useTTSPlayer] 音频播放结束')
          isPlaying.value = false
          
          // 清理资源
          if (currentSource) {
            currentSource.disconnect()
            currentSource = null
          }
          if (currentGainNode) {
            currentGainNode.disconnect()
            currentGainNode = null
          }
          
          resolve()
        }
        
        // 开始播放
        currentSource.start(0)
        console.log('[useTTSPlayer] 音频开始播放，淡入时长:', fadeInDuration * 1000, 'ms，淡出时长:', fadeOutDuration * 1000, 'ms')
        
      } catch (error) {
        console.error('[useTTSPlayer] 音频播放错误:', error)
        isPlaying.value = false
        
        // 清理资源
        if (currentSource) {
          currentSource.disconnect()
          currentSource = null
        }
        if (currentGainNode) {
          currentGainNode.disconnect()
          currentGainNode = null
        }
        
        reject(error)
      }
    })
  }

  // 流式TTS合成（实验性功能）
  const streamingSynthesize = async (text: string, characterId: number): Promise<void> => {
    console.log('[useTTSPlayer] 开始流式TTS合成:', text)
    
    try {
      const token = localStorage.getItem('ACCESS_TOKEN')
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      }
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch('http://localhost:18080/api/tts/synthesize/stream', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          text,
          characterId,
          languageType: 'Chinese'
        })
      })

      if (!response.ok) {
        throw new Error(`流式TTS请求失败: ${response.status}`)
      }

      if (!response.body) {
        throw new Error('Response body is null')
      }

      // 处理流式响应
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        
        if (done) {
          console.log('[useTTSPlayer] 流式TTS完成')
          break
        }

        const chunk = decoder.decode(value, { stream: true })
        buffer += chunk
        
        // 处理SSE数据
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data && data !== '[DONE]') {
              try {
                const jsonData = JSON.parse(data)
                if (jsonData.audioData) {
                  // 播放音频块
                  playAudioChunk(jsonData.audioData)
                }
              } catch (error) {
                console.warn('[useTTSPlayer] 解析流式TTS数据失败:', error)
              }
            }
          }
        }
      }

    } catch (error) {
      console.error('[useTTSPlayer] 流式TTS合成失败:', error)
      throw error
    }
  }

  // 播放音频块（流式）
  const playAudioChunk = (base64AudioData: string): void => {
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
      chunkAudio.volume = 1.0
      
      chunkAudio.play().catch(error => {
        console.warn('[useTTSPlayer] 播放音频块失败:', error)
      })
      
      // 清理URL对象
      chunkAudio.addEventListener('ended', () => {
        URL.revokeObjectURL(audioUrl)
      })
      
    } catch (error) {
      console.error('[useTTSPlayer] 解析音频数据块失败:', error)
    }
  }

  // 停止播放
  const stopPlaying = (): void => {
    console.log('[useTTSPlayer] 停止播放')
    
    // 停止当前音频源
    if (currentSource) {
      try {
        currentSource.stop()
      } catch (error) {
        // 忽略已经停止的错误
      }
      currentSource.disconnect()
      currentSource = null
    }
    
    // 断开增益节点
    if (currentGainNode) {
      currentGainNode.disconnect()
      currentGainNode = null
    }
    
    isPlaying.value = false
    currentItem.value = null
  }

  // 清空队列
  const clearQueue = (): void => {
    console.log('[useTTSPlayer] 清空播放队列')
    
    // 停止当前播放
    stopPlaying()
    
    // 清空队列
    audioQueue.splice(0)
    queueLength.value = 0
    currentItem.value = null
    isProcessingQueue = false
    
    // 清理URL对象
    audioQueue.forEach(item => {
      if (item.audioUrl) {
        URL.revokeObjectURL(item.audioUrl)
      }
    })
  }

  // 暂停播放
  const pausePlaying = (): void => {
    if (currentSource) {
      try {
        currentSource.stop()
        console.log('[useTTSPlayer] 播放已暂停')
      } catch (error) {
        console.warn('[useTTSPlayer] 暂停播放时出错:', error)
      }
    }
  }

  // 恢复播放
  const resumePlaying = (): void => {
    // AudioContext 的 BufferSource 一旦停止就无法恢复，需要重新创建
    console.log('[useTTSPlayer] AudioContext 不支持暂停/恢复，需要重新播放')
  }

  // 设置音量
  const setVolume = (volume: number): void => {
    const vol = Math.max(0, Math.min(1, volume))
    if (currentGainNode) {
      currentGainNode.gain.value = vol
    }
    console.log('[useTTSPlayer] 音量设置为:', vol)
  }

  // 清理资源
  const cleanup = (): void => {
    console.log('[useTTSPlayer] 清理TTS播放器资源')
    
    clearQueue()
    onQueueEmpty = null
  }

  // 测试函数：验证TTS文本过滤功能
  const testTextFiltering = (): void => {
    console.log('=== TTS文本过滤测试 ===')

    // 测试非法句子
    const testCases = [
      { text: '2.', expected: null }, // 应该被跳过
      { text: '###', expected: null }, // 应该被跳过
      { text: '✨', expected: null }, // 应该被跳过
      { text: '**粗体**', expected: '粗体' }, // 应该被过滤为"粗体"
      { text: '*斜体*文本', expected: '斜体文本' }, // 应该被过滤
      { text: '正常中文文本', expected: '正常中文文本' }, // 应该正常通过
      { text: '- **伤害**：190', expected: '伤害：190' }, // markdown符号，应该被过滤
      { text: 'Requests rate limit exceeded', expected: 'Requests rate limit exceeded' }, // 正常文本
      { text: '「」【】()～', expected: null }, // 纯符号，应该被跳过
      { text: '❤️★☆', expected: null } // 纯emoji，应该被跳过
    ]

    testCases.forEach((testCase, index) => {
      const result = filterText(testCase.text)
      console.log(`测试${index + 1}: "${testCase.text}"`)
      
      if (result === null) {
        console.log(`  结果: ❌跳过`)
      } else {
        console.log(`  结果: ✅通过 | 过滤后: "${result}"`)
      }

      // 验证期望结果
      if (testCase.expected === null) {
        if (result !== null) {
          console.warn(`  ⚠️期望跳过，实际通过`)
        }
      } else {
        if (result !== testCase.expected) {
          console.warn(`  ⚠️期望"${testCase.expected}"，实际"${result}"`)
        }
      }
    })

    console.log('=== 测试完成 ===')
    console.log('💡 提示：输入 testTTSFiltering() 可以重新运行测试')
  }

  return {
    isPlaying,
    currentItem,
    queueLength,
    addToQueue,
    streamingSynthesize,
    stopPlaying,
    clearQueue,
    pausePlaying,
    resumePlaying,
    setVolume,
    cleanup,
    // 设置队列清空回调
    set onQueueEmpty(callback: (() => void) | null) {
      onQueueEmpty = callback
    },
    // 测试函数（仅开发环境可用）
    testTextFiltering: process.env.NODE_ENV === 'development' ? testTextFiltering : undefined
  }
}
