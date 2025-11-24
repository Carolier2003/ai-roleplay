import axios from './axios'

// 类型定义
export interface LoginRequest {
  userAccount: string
  userPassword: string
}

export interface RegisterRequest {
  userAccount: string
  userPassword: string
  confirmPassword: string
}

export interface UserResponse {
  userId: number
  userAccount: string
  displayName: string
  avatarUrl?: string
  role?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: {
    userId: number
    userAccount: string
    displayName: string
    avatarUrl?: string
    role?: string
  }
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 用户登录
 */
export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  console.log('[authApi] 发起登录请求:', data.userAccount)

  const response = await axios.post<ApiResponse<LoginResponse>>('/api/auth/login', data)

  if (response.data.code === 200) {
    console.log('[authApi] 登录成功:', response.data.data.user.userAccount)
    return response.data.data
  } else {
    throw new Error(response.data.message || '登录失败')
  }
}

/**
 * 用户注册
 */
export const register = async (data: RegisterRequest): Promise<UserResponse> => {
  console.log('[authApi] 发起注册请求:', data.userAccount)

  const response = await axios.post<ApiResponse<UserResponse>>('/api/auth/register', data)

  if (response.data.code === 200) {
    console.log('[authApi] 注册成功:', response.data.data.userAccount)
    return response.data.data
  } else {
    throw new Error(response.data.message || '注册失败')
  }
}

/**
 * 刷新token
 */
export const refreshToken = async (refreshToken: string): Promise<LoginResponse> => {
  console.log('[authApi] 发起token刷新请求')

  const response = await axios.post<ApiResponse<LoginResponse>>('/api/auth/refresh', {
    refreshToken
  })

  if (response.data.code === 200) {
    console.log('[authApi] Token刷新成功')
    return response.data.data
  } else {
    throw new Error(response.data.message || 'Token刷新失败')
  }
}

/**
 * 获取当前用户信息
 */
export const getCurrentUser = async (): Promise<UserResponse> => {
  console.log('[authApi] 获取当前用户信息')

  const response = await axios.get<ApiResponse<UserResponse>>('/api/auth/me')

  if (response.data.code === 200) {
    return response.data.data
  } else {
    throw new Error(response.data.message || '获取用户信息失败')
  }
}

/**
 * 退出登录
 */
export const logout = async (): Promise<void> => {
  console.log('[authApi] 发起退出登录请求')

  try {
    await axios.post('/api/auth/logout')
    console.log('[authApi] 退出登录成功')
  } catch (error) {
    console.warn('[authApi] 退出登录请求失败，但将清除本地数据:', error)
  }
}