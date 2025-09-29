/**
 * 语音识别配置
 */
export const speechConfig = {
  // 默认识别参数
  recognition: {
    model: 'fun-asr-realtime',
    format: 'wav', // 使用WAV格式，在客户端转换
    sampleRate: 16000,
    punctuationPredictionEnabled: true,
    semanticPunctuationEnabled: false,
    maxSentenceSilence: 1300,
    languageHints: ['zh-CN']
  },
  
  // 录音参数
  recording: {
    mimeType: 'audio/ogg;codecs=opus', // 改为Opus格式，浏览器和后端都支持
    audioBitsPerSecond: 128000,
    maxDuration: 60000, // 最大录音时长60秒
    minDuration: 500    // 最小录音时长0.5秒
  },
  
  // 超时设置
  timeout: {
    recognition: 30000,  // 识别超时30秒
    upload: 60000       // 上传超时60秒
  },
  
  // 文件限制
  file: {
    maxSize: 10 * 1024 * 1024, // 最大文件大小10MB
    supportedFormats: ['wav', 'mp3', 'pcm', 'opus', 'speex', 'aac', 'amr'] // 后端支持的格式
  },
  
  // 用户体验设置
  ui: {
    showConfidence: true,        // 显示置信度
    autoSendOnRecognition: true,  // 识别完成后自动发送
    vibrationOnStart: true,      // 开始录音时震动
    vibrationOnStop: true       // 停止录音时震动
  }
}

/**
 * 获取当前设备支持的音频格式（优先选择后端支持的格式）
 */
export function getSupportedAudioFormat(): string {
  const mediaRecorder = window.MediaRecorder
  
  if (!mediaRecorder) {
    return 'wav'
  }
  
  // 按优先级检查支持的格式（只选择后端明确支持的格式）
  const formats = [
    'audio/ogg;codecs=opus', // OGG/Opus格式，浏览器和后端都支持
    'audio/mp4',           // MP4格式，可能对应AAC
    'audio/wav',           // WAV格式，但浏览器支持有限
    'audio/mp3'            // MP3格式，浏览器支持有限
    // 注意：不使用WebM，因为后端不支持
  ]
  
  for (const format of formats) {
    if (mediaRecorder.isTypeSupported(format)) {
      // 返回文件扩展名
      if (format.includes('ogg')) return 'opus'
      if (format.includes('mp4')) return 'aac'
      if (format.includes('wav')) return 'wav'
      if (format.includes('mp3')) return 'mp3'
    }
  }
  
  return 'opus' // 默认返回Opus，录音时使用，最终会转换为WAV
}

/**
 * 检查浏览器是否支持语音录制
 */
export function isSpeechRecordingSupported(): boolean {
  return !!(
    navigator.mediaDevices &&
    navigator.mediaDevices.getUserMedia &&
    window.MediaRecorder
  )
}

/**
 * 检查是否支持Web Audio API
 */
export function isWebAudioSupported(): boolean {
  return !!(window.AudioContext || (window as any).webkitAudioContext)
}

/**
 * 获取推荐的录音配置
 */
export function getRecommendedRecordingConfig() {
  const supportedFormat = getSupportedAudioFormat()
  
  // 根据格式设置正确的MIME类型
  let mimeType = `audio/${supportedFormat}`
  if (supportedFormat === 'opus') {
    mimeType = 'audio/ogg;codecs=opus'
  } else if (supportedFormat === 'aac') {
    mimeType = 'audio/mp4'
  }
  
  return {
    ...speechConfig.recording,
    mimeType: mimeType,
    format: supportedFormat
  }
}
