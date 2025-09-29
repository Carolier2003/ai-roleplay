/**
 * 录音功能 Hook
 * 使用浏览器原生 MediaRecorder API
 */

import { ref } from 'vue'

export interface RecorderOptions {
  sampleRate?: number
  channelCount?: number
  mimeType?: string
}

export function useRecorder(options: RecorderOptions = {}) {
  const isRecording = ref(false)
  const isSupported = ref(false)
  
  let mediaRecorder: MediaRecorder | null = null
  let audioStream: MediaStream | null = null
  let audioChunks: Blob[] = []
  let audioContext: AudioContext | null = null
  let audioWorkletNode: AudioWorkletNode | null = null
  let source: MediaStreamAudioSourceNode | null = null
  
  // 音频数据回调
  let onAudioData: ((audioData: ArrayBuffer) => void) | null = null

  // 检查浏览器支持
  const checkSupport = () => {
    isSupported.value = !!(
      navigator.mediaDevices &&
      navigator.mediaDevices.getUserMedia &&
      window.MediaRecorder
    )
    return isSupported.value
  }

  // 开始录音
  const startRecording = async (): Promise<void> => {
    console.log('[useRecorder] 开始录音')
    
    if (!checkSupport()) {
      throw new Error('浏览器不支持录音功能')
    }

    if (isRecording.value) {
      console.warn('[useRecorder] 已在录音中')
      return
    }

    try {
      // 获取音频流
      audioStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: options.sampleRate || 16000,
          channelCount: options.channelCount || 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })

      console.log('[useRecorder] 获取音频流成功')

      // 创建 AudioContext 用于实时音频数据处理
      audioContext = new AudioContext({
        sampleRate: options.sampleRate || 16000
      })
      
      source = audioContext.createMediaStreamSource(audioStream)
      
      try {
        // 加载 AudioWorklet 处理器
        await audioContext.audioWorklet.addModule('/audio-processor.js')
        
        // 创建 AudioWorklet 节点 - 替代已弃用的 ScriptProcessorNode
        audioWorkletNode = new AudioWorkletNode(audioContext, 'audio-processor')
        
        // 监听来自 AudioWorklet 的消息
        audioWorkletNode.port.onmessage = (event) => {
          if (!isRecording.value || !onAudioData) return
          
          if (event.data.type === 'audiodata') {
            // 发送音频数据
            onAudioData(event.data.data)
          }
        }
        
        // 连接音频节点 - 只连接到处理器，不连接到扬声器
        source.connect(audioWorkletNode)
        // 注意：不连接到 destination，避免录音时播放自己的声音
        
        console.log('[useRecorder] AudioWorklet 处理器加载成功')
      } catch (error) {
        console.warn('[useRecorder] AudioWorklet 不支持，回退到 ScriptProcessorNode:', error)
        
        // 回退到 ScriptProcessorNode（兼容性处理）
        const bufferSize = 4096
        const processor = audioContext.createScriptProcessor(bufferSize, 1, 1)
        
        processor.onaudioprocess = (event) => {
          if (!isRecording.value || !onAudioData) return
          
          const inputBuffer = event.inputBuffer
          const inputData = inputBuffer.getChannelData(0)
          
          // 转换为 ArrayBuffer
          const arrayBuffer = new ArrayBuffer(inputData.length * 2)
          const view = new DataView(arrayBuffer)
          
          for (let i = 0; i < inputData.length; i++) {
            const sample = Math.max(-1, Math.min(1, inputData[i]))
            view.setInt16(i * 2, sample < 0 ? sample * 0x8000 : sample * 0x7FFF, true)
          }
          
          // 发送音频数据
          onAudioData(arrayBuffer)
        }
        
        // 连接音频节点 - 只连接到处理器，不连接到扬声器
        source.connect(processor)
        // 注意：不连接到 destination，避免录音时播放自己的声音
      }

      // 创建 MediaRecorder 用于录音文件保存
      const mimeType = options.mimeType || 'audio/webm;codecs=opus'
      mediaRecorder = new MediaRecorder(audioStream, {
        mimeType: MediaRecorder.isTypeSupported(mimeType) ? mimeType : 'audio/webm'
      })

      audioChunks = []

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunks.push(event.data)
        }
      }

      mediaRecorder.onerror = (event) => {
        console.error('[useRecorder] MediaRecorder错误:', event)
      }

      // 开始录音
      mediaRecorder.start(100) // 每100ms收集一次数据
      isRecording.value = true

      console.log('[useRecorder] 录音开始成功')

    } catch (error) {
      console.error('[useRecorder] 开始录音失败:', error)
      cleanup()
      throw error
    }
  }

  // 停止录音
  const stopRecording = async (): Promise<Blob | null> => {
    console.log('[useRecorder] 停止录音')
    
    if (!isRecording.value) {
      console.warn('[useRecorder] 未在录音中')
      return null
    }

    return new Promise((resolve) => {
      if (!mediaRecorder) {
        resolve(null)
        return
      }

      mediaRecorder.onstop = () => {
        console.log('[useRecorder] 录音停止，音频块数量:', audioChunks.length)
        
        if (audioChunks.length > 0) {
          const audioBlob = new Blob(audioChunks, { 
            type: mediaRecorder?.mimeType || 'audio/webm' 
          })
          console.log('[useRecorder] 生成音频文件，大小:', audioBlob.size, 'bytes')
          resolve(audioBlob)
        } else {
          console.warn('[useRecorder] 没有音频数据')
          resolve(null)
        }
        
        cleanup()
      }

      mediaRecorder.stop()
      isRecording.value = false
    })
  }

  // 暂停录音
  const pauseRecording = () => {
    if (mediaRecorder && isRecording.value) {
      mediaRecorder.pause()
      console.log('[useRecorder] 录音已暂停')
    }
  }

  // 恢复录音
  const resumeRecording = () => {
    if (mediaRecorder && isRecording.value) {
      mediaRecorder.resume()
      console.log('[useRecorder] 录音已恢复')
    }
  }

  // 清理资源
  const cleanup = () => {
    console.log('[useRecorder] 清理录音资源')
    
    // 停止音频处理
    if (audioWorkletNode) {
      audioWorkletNode.disconnect()
      audioWorkletNode.port.close()
      audioWorkletNode = null
    }
    
    if (source) {
      source.disconnect()
      source = null
    }
    
    if (audioContext && audioContext.state !== 'closed') {
      audioContext.close()
      audioContext = null
    }

    // 停止媒体流
    if (audioStream) {
      audioStream.getTracks().forEach(track => {
        track.stop()
        console.log('[useRecorder] 音频轨道已停止')
      })
      audioStream = null
    }

    // 清理录音器
    if (mediaRecorder) {
      if (mediaRecorder.state !== 'inactive') {
        mediaRecorder.stop()
      }
      mediaRecorder = null
    }

    audioChunks = []
    isRecording.value = false
    onAudioData = null
  }

  // 获取音频设备列表
  const getAudioDevices = async () => {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices()
      return devices.filter(device => device.kind === 'audioinput')
    } catch (error) {
      console.error('[useRecorder] 获取音频设备失败:', error)
      return []
    }
  }

  // 检查权限
  const checkPermission = async (): Promise<boolean> => {
    try {
      const result = await navigator.permissions.query({ name: 'microphone' as PermissionName })
      return result.state === 'granted'
    } catch (error) {
      console.warn('[useRecorder] 无法检查麦克风权限:', error)
      return false
    }
  }

  // 初始化检查
  checkSupport()

  return {
    isRecording,
    isSupported,
    startRecording,
    stopRecording,
    pauseRecording,
    resumeRecording,
    cleanup,
    getAudioDevices,
    checkPermission,
    // 设置音频数据回调
    set onAudioData(callback: ((audioData: ArrayBuffer) => void) | null) {
      onAudioData = callback
    }
  }
}
