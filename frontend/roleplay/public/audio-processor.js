/**
 * AudioWorklet 处理器 - 替代已弃用的 ScriptProcessorNode
 * 用于实时音频数据处理和传输
 */

class AudioProcessor extends AudioWorkletProcessor {
  constructor() {
    super()
    this.bufferSize = 4096
    this.buffer = new Float32Array(this.bufferSize)
    this.bufferIndex = 0
  }

  process(inputs, outputs, parameters) {
    const input = inputs[0]
    const output = outputs[0]

    // 如果没有输入，返回 true 继续处理
    if (!input || input.length === 0) {
      return true
    }

    const inputChannel = input[0]
    const outputChannel = output[0]

    // 复制输入到输出（直通）
    if (outputChannel) {
      outputChannel.set(inputChannel)
    }

    // 处理音频数据
    for (let i = 0; i < inputChannel.length; i++) {
      this.buffer[this.bufferIndex] = inputChannel[i]
      this.bufferIndex++

      // 当缓冲区满时，发送数据
      if (this.bufferIndex >= this.bufferSize) {
        // 转换为 ArrayBuffer
        const arrayBuffer = new ArrayBuffer(this.bufferSize * 2)
        const view = new DataView(arrayBuffer)
        
        for (let j = 0; j < this.bufferSize; j++) {
          const sample = Math.max(-1, Math.min(1, this.buffer[j]))
          view.setInt16(j * 2, sample < 0 ? sample * 0x8000 : sample * 0x7FFF, true)
        }

        // 发送音频数据到主线程
        this.port.postMessage({
          type: 'audiodata',
          data: arrayBuffer
        })

        // 重置缓冲区
        this.bufferIndex = 0
      }
    }

    return true
  }
}

registerProcessor('audio-processor', AudioProcessor)
