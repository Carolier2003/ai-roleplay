/**
 * 错误处理组合式函数
 * 提供统一的错误处理逻辑和用户提示
 */

import { ref } from 'vue'
import { ErrorHandler, type FrontendError } from '@/utils/errorHandler'
import { useAuthStore } from '@/stores/auth'

// Simple replacement for Naive UI message/dialog
const message = {
  error: (msg: string) => {
    console.error('Error:', msg)
    alert(msg)
  },
  warning: (msg: string) => {
    console.warn('Warning:', msg)
    alert(msg)
  }
}

const dialog = {
  warning: (options: any) => {
    if (confirm(options.content)) {
      options.onPositiveClick?.()
    }
  },
  error: (options: any) => {
    alert(options.content)
  }
}

/**
 * 错误处理组合式函数
 */
export function useErrorHandler() {
  const loading = ref(false)
  const error = ref<FrontendError | null>(null)

  // 安全地初始化服务
  let authStore: any = null

  const initServices = () => {
    try {
      if (!authStore) {
        authStore = useAuthStore()
      }
      return true
    } catch (error) {
      console.warn('[useErrorHandler] 服务初始化失败，可能在组件外部调用:', error)
      return false
    }
  }

  /**
   * 处理API错误
   */
  const handleError = (apiError: any, context?: string): FrontendError => {
    const frontendError = ErrorHandler.handleError(apiError, context)
    error.value = frontendError

    // 根据错误类型进行不同处理
    switch (frontendError.type) {
      case 'auth':
        handleAuthError(frontendError)
        break
      case 'business':
        handleBusinessError(frontendError)
        break
      case 'network':
        handleNetworkError(frontendError)
        break
      case 'system':
        handleSystemError(frontendError)
        break
    }

    return frontendError
  }

  /**
   * 处理认证错误
   */
  const handleAuthError = (error: FrontendError) => {
    console.log('[useErrorHandler] 处理认证错误:', error)

    if (!initServices()) {
      console.error('[useErrorHandler] 无法初始化服务，直接输出错误:', error.userMessage)
      return
    }

    // 显示登录弹窗
    if (error.shouldLogin) {
      authStore.showLoginModal()
      // 不显示错误消息，直接弹出登录框
      return
    }

    // 其他认证错误显示消息
    message.error(error.userMessage)
  }

  /**
   * 处理业务错误
   */
  const handleBusinessError = (error: FrontendError) => {
    console.log('[useErrorHandler] 处理业务错误:', error)

    if (!initServices()) {
      console.error('[useErrorHandler] 无法初始化服务，直接输出错误:', error.userMessage)
      return
    }

    // 特殊处理某些业务错误
    if (error.code === 2000) { // 用户不存在
      dialog.warning({
        title: '用户不存在',
        content: error.userMessage,
        positiveText: '确定'
      })
      return
    }

    if (error.code === 5000) { // 角色不存在
      dialog.warning({
        title: '角色不存在',
        content: error.userMessage + '，页面将自动刷新',
        positiveText: '确定',
        onPositiveClick: () => {
          window.location.reload()
        }
      })
      return
    }

    // 默认显示错误消息
    message.error(error.userMessage)
  }

  /**
   * 处理网络错误
   */
  const handleNetworkError = (error: FrontendError) => {
    console.log('[useErrorHandler] 处理网络错误:', error)

    if (!initServices()) {
      console.error('[useErrorHandler] 无法初始化服务，直接输出错误:', error.userMessage)
      return
    }

    dialog.error({
      title: '网络连接失败',
      content: error.userMessage + '，请检查网络连接后重试',
      positiveText: '确定'
    })
  }

  /**
   * 处理系统错误
   */
  const handleSystemError = (error: FrontendError) => {
    console.log('[useErrorHandler] 处理系统错误:', error)

    if (!initServices()) {
      console.error('[useErrorHandler] 无法初始化服务，直接输出错误:', error.userMessage)
      return
    }

    dialog.error({
      title: '系统错误',
      content: error.userMessage,
      positiveText: '确定'
    })
  }

  /**
   * 包装API调用，自动处理错误
   */
  const withErrorHandling = async <T>(
    apiCall: () => Promise<T>,
    context?: string,
    options?: {
      showLoading?: boolean
      silent?: boolean // 静默处理，不显示错误提示
    }
  ): Promise<T | null> => {
    const { showLoading = false, silent = false } = options || {}

    try {
      if (showLoading) {
        loading.value = true
      }

      const result = await apiCall()
      error.value = null // 清除之前的错误
      return result
    } catch (apiError) {
      if (!silent) {
        handleError(apiError, context)
      } else {
        // 静默处理，只记录错误
        error.value = ErrorHandler.handleError(apiError, context)
      }
      return null
    } finally {
      if (showLoading) {
        loading.value = false
      }
    }
  }

  /**
   * 清除错误状态
   */
  const clearError = () => {
    error.value = null
  }

  /**
   * 检查是否为特定错误
   */
  const isError = (errorCode: number): boolean => {
    return error.value?.code === errorCode
  }

  /**
   * 检查是否为认证错误
   */
  const isAuthError = (): boolean => {
    return error.value?.type === 'auth'
  }

  /**
   * 检查是否为网络错误
   */
  const isNetworkError = (): boolean => {
    return error.value?.type === 'network'
  }

  return {
    // 状态
    loading,
    error,

    // 方法
    handleError,
    withErrorHandling,
    clearError,

    // 检查方法
    isError,
    isAuthError,
    isNetworkError,

    // 工具方法
    getUserMessage: (apiError: any) => ErrorHandler.getUserMessage(apiError),
    isErrorCode: (apiError: any, code: number) => ErrorHandler.isErrorCode(apiError, code)
  }
}

export default useErrorHandler
