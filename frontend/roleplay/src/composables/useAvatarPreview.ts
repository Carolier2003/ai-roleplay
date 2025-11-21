import { ref, onUnmounted } from 'vue'

/**
 * 头像预览钩子
 * 支持文件选择和URL输入的实时预览功能
 */
import { useToast } from '@/composables/useToast'

/**
 * 头像预览钩子
 * 支持文件选择和URL输入的实时预览功能
 */
export function useAvatarPreview() {
  const toast = useToast()

  // Simple replacement for useMessage
  const message = {
    error: (msg: string) => {
      console.error('Error:', msg)
      toast.error(msg)
    }
  }

  // 预览状态
  const previewUrl = ref<string | null>(null)
  const isLoading = ref(false)
  const hasError = ref(false)

  // 存储当前的 ObjectURL 用于清理
  const currentObjectUrl = ref<string | null>(null)

  /**
   * 处理文件选择预览
   * @param file 选择的文件
   */
  const previewFile = (file: File): boolean => {
    console.log('[useAvatarPreview] 开始预览文件:', file.name, 'size:', file.size)

    // 清理之前的预览
    clearPreview()

    // 文件大小校验（10MB）
    const maxSize = 10 * 1024 * 1024
    if (file.size > maxSize) {
      message.error('图片文件大小不能超过 10MB')
      return false
    }

    // 文件类型校验
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
    if (!allowedTypes.includes(file.type)) {
      message.error('不支持的文件格式，请上传 JPG、PNG 或 WebP 格式的图片')
      return false
    }

    try {
      // 生成临时预览URL
      const objectUrl = URL.createObjectURL(file)
      currentObjectUrl.value = objectUrl
      previewUrl.value = objectUrl
      hasError.value = false

      console.log('[useAvatarPreview] 文件预览生成成功:', objectUrl)
      return true
    } catch (error) {
      console.error('[useAvatarPreview] 文件预览生成失败:', error)
      message.error('预览生成失败，请重试')
      return false
    }
  }

  /**
   * 处理URL输入预览
   * @param url 输入的图片URL
   */
  const previewUrl_input = (url: string): Promise<boolean> => {
    return new Promise((resolve) => {
      console.log('[useAvatarPreview] 开始预览URL:', url)

      // 清理之前的预览
      clearPreview()

      if (!url || !url.trim()) {
        resolve(false)
        return
      }

      const trimmedUrl = url.trim()

      // URL格式校验
      if (!trimmedUrl.startsWith('http://') &&
        !trimmedUrl.startsWith('https://') &&
        !trimmedUrl.startsWith('data:image/')) {
        message.error('请输入有效的图片链接（支持 http://、https:// 或 base64 格式）')
        resolve(false)
        return
      }

      isLoading.value = true
      hasError.value = false

      // 创建图片元素进行加载测试
      const img = new Image()

      img.onload = () => {
        console.log('[useAvatarPreview] URL预览加载成功:', trimmedUrl)
        previewUrl.value = trimmedUrl
        isLoading.value = false
        hasError.value = false
        resolve(true)
      }

      img.onerror = () => {
        console.error('[useAvatarPreview] URL预览加载失败:', trimmedUrl)
        isLoading.value = false
        hasError.value = true
        previewUrl.value = null
        message.error('图片链接无效或无法加载')
        resolve(false)
      }

      // 设置超时
      setTimeout(() => {
        if (isLoading.value) {
          console.error('[useAvatarPreview] URL预览加载超时:', trimmedUrl)
          isLoading.value = false
          hasError.value = true
          previewUrl.value = null
          message.error('图片加载超时，请检查网络连接')
          resolve(false)
        }
      }, 10000) // 10秒超时

      // 开始加载
      img.src = trimmedUrl
    })
  }

  /**
   * 清理预览
   */
  const clearPreview = () => {
    console.log('[useAvatarPreview] 清理预览')

    // 释放 ObjectURL 内存
    if (currentObjectUrl.value) {
      URL.revokeObjectURL(currentObjectUrl.value)
      currentObjectUrl.value = null
    }

    previewUrl.value = null
    isLoading.value = false
    hasError.value = false
  }

  /**
   * 重置所有状态
   */
  const reset = () => {
    console.log('[useAvatarPreview] 重置状态')
    clearPreview()
  }

  // 组件卸载时清理资源
  onUnmounted(() => {
    console.log('[useAvatarPreview] 组件卸载，清理资源')
    clearPreview()
  })

  return {
    // 状态
    previewUrl,
    isLoading,
    hasError,

    // 方法
    previewFile,
    previewUrlInput: previewUrl_input,
    clearPreview,
    reset
  }
}
