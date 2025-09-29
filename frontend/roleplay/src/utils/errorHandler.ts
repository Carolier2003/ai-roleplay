/**
 * 前端错误处理工具
 * 统一处理后端错误码，提供用户友好的错误提示
 */

// 后端错误码枚举（与后端保持一致）
export enum ErrorCode {
  // 通用错误码 (1000-1999)
  SUCCESS = 200,
  SYSTEM_ERROR = 1000,
  PARAM_ERROR = 1001,
  DATA_NOT_FOUND = 1002,
  OPERATION_FAILED = 1003,
  
  // 用户相关错误码 (2000-2999)
  USER_NOT_FOUND = 2000,
  USER_ACCOUNT_EXISTS = 2001,
  USER_EMAIL_EXISTS = 2002,
  USER_PASSWORD_ERROR = 2003,
  USER_ACCOUNT_DISABLED = 2004,
  USER_LOGIN_EXPIRED = 2005,
  
  // 认证授权相关错误码 (3000-3999)
  AUTH_FAILED = 3000,
  TOKEN_INVALID = 3001,
  TOKEN_EXPIRED = 3002,
  PERMISSION_DENIED = 3003,
  LOGIN_REQUIRED = 3004,
  
  // 个人资料相关错误码 (4000-4099)
  PROFILE_UPDATE_FAILED = 4000,
  PROFILE_EMAIL_TAKEN = 4001,
  PROFILE_INVALID_AVATAR_URL = 4002,
  PROFILE_INVALID_PHONE = 4003,
  PROFILE_INVALID_BIRTHDAY = 4004,
  
  // 聊天相关错误码 (5000-5999)
  CHAT_CHARACTER_NOT_FOUND = 5000,
  CHAT_MESSAGE_EMPTY = 5001,
  CHAT_HISTORY_LOAD_FAILED = 5002,
  CHAT_SEND_FAILED = 5003,
  
  // 语音识别相关错误码 (6000-6999)
  SPEECH_FILE_EMPTY = 6000,
  SPEECH_FILE_TOO_LARGE = 6001,
  SPEECH_FORMAT_UNSUPPORTED = 6002,
  SPEECH_RECOGNITION_FAILED = 6003,
}

// 后端错误响应格式
export interface BackendErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

// 前端统一错误格式
export interface FrontendError {
  code: number
  message: string
  userMessage: string
  type: 'business' | 'network' | 'auth' | 'system'
  shouldShowModal?: boolean
  shouldLogin?: boolean
}

// 错误码到用户友好消息的映射
const ERROR_MESSAGE_MAP: Record<number, string> = {
  // 通用错误
  [ErrorCode.SYSTEM_ERROR]: '系统繁忙，请稍后重试',
  [ErrorCode.PARAM_ERROR]: '请求参数有误，请检查后重试',
  [ErrorCode.DATA_NOT_FOUND]: '请求的数据不存在',
  [ErrorCode.OPERATION_FAILED]: '操作失败，请稍后重试',
  
  // 用户相关错误
  [ErrorCode.USER_NOT_FOUND]: '账号或密码错误，请重新输入',
  [ErrorCode.USER_ACCOUNT_EXISTS]: '该账号已存在，请使用其他账号',
  [ErrorCode.USER_EMAIL_EXISTS]: '该邮箱已被使用，请使用其他邮箱',
  [ErrorCode.USER_PASSWORD_ERROR]: '账号或密码错误，请重新输入',
  [ErrorCode.USER_ACCOUNT_DISABLED]: '账号已被禁用，请联系管理员',
  [ErrorCode.USER_LOGIN_EXPIRED]: '登录已过期，请重新登录',
  
  // 认证授权相关错误
  [ErrorCode.AUTH_FAILED]: '认证失败，请重新登录',
  [ErrorCode.TOKEN_INVALID]: '登录状态无效，请重新登录',
  [ErrorCode.TOKEN_EXPIRED]: '登录已过期，请重新登录',
  [ErrorCode.PERMISSION_DENIED]: '权限不足，无法执行此操作',
  [ErrorCode.LOGIN_REQUIRED]: '请先登录后再进行操作',
  
  // 个人资料相关错误
  [ErrorCode.PROFILE_UPDATE_FAILED]: '个人资料更新失败，请稍后重试',
  [ErrorCode.PROFILE_EMAIL_TAKEN]: '该邮箱已被其他用户使用',
  [ErrorCode.PROFILE_INVALID_AVATAR_URL]: '头像链接格式不正确',
  [ErrorCode.PROFILE_INVALID_PHONE]: '手机号格式不正确',
  [ErrorCode.PROFILE_INVALID_BIRTHDAY]: '生日格式不正确',
  
  // 聊天相关错误
  [ErrorCode.CHAT_CHARACTER_NOT_FOUND]: '角色不存在，请刷新页面重试',
  [ErrorCode.CHAT_MESSAGE_EMPTY]: '消息内容不能为空',
  [ErrorCode.CHAT_HISTORY_LOAD_FAILED]: '聊天记录加载失败，请刷新重试',
  [ErrorCode.CHAT_SEND_FAILED]: '消息发送失败，请稍后重试',
  
  // 语音识别相关错误
  [ErrorCode.SPEECH_FILE_EMPTY]: '请选择音频文件',
  [ErrorCode.SPEECH_FILE_TOO_LARGE]: '音频文件过大，请选择小于10MB的文件',
  [ErrorCode.SPEECH_FORMAT_UNSUPPORTED]: '不支持的音频格式，请使用WAV或MP3格式',
  [ErrorCode.SPEECH_RECOGNITION_FAILED]: '语音识别失败，请重新录制',
}

// HTTP状态码到用户友好消息的映射
const HTTP_STATUS_MESSAGE_MAP: Record<number, string> = {
  400: '请求参数错误',
  401: '登录已过期，请重新登录',
  403: '权限不足，无法访问',
  404: '请求的资源不存在',
  408: '请求超时，请稍后重试',
  429: '请求过于频繁，请稍后重试',
  500: '服务器内部错误，请稍后重试',
  502: '服务器暂时不可用，请稍后重试',
  503: '服务器维护中，请稍后重试',
  504: '服务器响应超时，请稍后重试',
}

/**
 * 错误处理工具类
 */
export class ErrorHandler {
  
  /**
   * 解析后端错误响应
   */
  static parseBackendError(error: any): FrontendError {
    console.log('[ErrorHandler] 解析后端错误:', error)
    
    // 如果是网络错误
    if (!error.response) {
      return {
        code: 0,
        message: error.message || '网络连接失败',
        userMessage: '网络连接失败，请检查网络后重试',
        type: 'network'
      }
    }
    
    const response = error.response
    const status = response.status
    const data = response.data
    
    // 如果有后端标准错误格式
    if (data && typeof data === 'object') {
      // 检查是否有错误码
      let errorCode: number | undefined
      let errorMessage = ''
      
      // 尝试从不同字段获取错误信息
      if (data.error && typeof data.error === 'string') {
        // 如果error字段是错误码枚举名称，尝试转换
        const enumValue = Object.entries(ErrorCode).find(([key, value]) => key === data.error)?.[1]
        if (enumValue && typeof enumValue === 'number') {
          errorCode = enumValue
        }
        errorMessage = data.message || data.error
      } else if (typeof data.code === 'number') {
        errorCode = data.code
        errorMessage = data.message || ''
      }
      
      // 如果有错误码，使用映射的用户友好消息
      if (errorCode && ERROR_MESSAGE_MAP[errorCode]) {
        return {
          code: errorCode,
          message: errorMessage,
          userMessage: ERROR_MESSAGE_MAP[errorCode],
          type: this.getErrorType(errorCode),
          shouldLogin: this.shouldShowLogin(errorCode)
        }
      }
      
      // 如果有消息但没有错误码，处理消息
      if (errorMessage) {
        // 对于登录相关的错误，统一显示"账号或密码错误"
        let userMessage = errorMessage
        if (status === 400 && this.isLoginError(errorMessage)) {
          userMessage = '账号或密码错误，请重新输入'
        }
        
        return {
          code: status,
          message: errorMessage,
          userMessage: userMessage,
          type: this.getErrorTypeByStatus(status),
          shouldLogin: status === 401
        }
      }
    }
    
    // 使用HTTP状态码映射
    const userMessage = HTTP_STATUS_MESSAGE_MAP[status] || `请求失败 (${status})`
    
    return {
      code: status,
      message: `HTTP ${status}`,
      userMessage,
      type: this.getErrorTypeByStatus(status),
      shouldLogin: status === 401
    }
  }
  
  /**
   * 根据错误码判断错误类型
   */
  private static getErrorType(code: number): FrontendError['type'] {
    if (code >= 3000 && code < 4000) {
      return 'auth'
    } else if (code >= 2000 && code < 6000) {
      return 'business'
    } else if (code >= 1000 && code < 2000) {
      return 'system'
    } else {
      return 'business'
    }
  }
  
  /**
   * 根据HTTP状态码判断错误类型
   */
  private static getErrorTypeByStatus(status: number): FrontendError['type'] {
    if (status === 401 || status === 403) {
      return 'auth'
    } else if (status >= 400 && status < 500) {
      return 'business'
    } else if (status >= 500) {
      return 'system'
    } else {
      return 'network'
    }
  }
  
  /**
   * 判断是否应该显示登录弹窗
   */
  private static shouldShowLogin(code: number): boolean {
    return code >= 3000 && code < 4000 // 认证相关错误
  }

  /**
   * 判断是否是登录相关的错误消息
   */
  private static isLoginError(message: string): boolean {
    const loginErrorKeywords = [
      '密码错误',
      '用户不存在',
      '账号不存在',
      '密码不正确',
      '用户名不存在',
      '账号或密码错误',
      '登录失败',
      '认证失败',
      'password',
      'user not found',
      'invalid credentials',
      'authentication failed',
      'login failed'
    ]
    
    const lowerMessage = message.toLowerCase()
    return loginErrorKeywords.some(keyword => 
      lowerMessage.includes(keyword.toLowerCase())
    )
  }
  
  /**
   * 处理错误并显示用户提示
   */
  static handleError(error: any, context?: string): FrontendError {
    const frontendError = this.parseBackendError(error)
    
    console.error(`[ErrorHandler] ${context || ''}错误处理:`, {
      原始错误: error,
      解析结果: frontendError
    })
    
    return frontendError
  }
  
  /**
   * 检查是否为特定错误码
   */
  static isErrorCode(error: any, code: ErrorCode): boolean {
    const frontendError = this.parseBackendError(error)
    return frontendError.code === code
  }
  
  /**
   * 检查是否为认证错误
   */
  static isAuthError(error: any): boolean {
    const frontendError = this.parseBackendError(error)
    return frontendError.type === 'auth' || frontendError.shouldLogin === true
  }
  
  /**
   * 检查是否为网络错误
   */
  static isNetworkError(error: any): boolean {
    const frontendError = this.parseBackendError(error)
    return frontendError.type === 'network'
  }
  
  /**
   * 获取用户友好的错误消息
   */
  static getUserMessage(error: any): string {
    const frontendError = this.parseBackendError(error)
    return frontendError.userMessage
  }
}

/**
 * 错误处理装饰器
 * 用于自动处理API调用的错误
 */
export function handleApiError(context?: string) {
  return function (target: any, propertyName: string, descriptor: PropertyDescriptor) {
    const method = descriptor.value
    
    descriptor.value = async function (...args: any[]) {
      try {
        return await method.apply(this, args)
      } catch (error) {
        const frontendError = ErrorHandler.handleError(error, context || propertyName)
        throw frontendError
      }
    }
  }
}

export default ErrorHandler
