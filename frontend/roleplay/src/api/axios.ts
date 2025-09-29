import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { useAuthStore } from '@/stores/auth'

// 创建axios实例
const instance: AxiosInstance = axios.create({
  baseURL: 'http://localhost:18080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 自动附加Authorization头
    // 从localStorage获取token，避免store初始化问题
    const token = localStorage.getItem('ACCESS_TOKEN')
    
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('[axios] 附加Authorization头:', `Bearer ${token.substring(0, 20)}...`)
    } else {
      console.log('[axios] 未找到token，跳过Authorization头')
    }
    
    console.log('[axios] 发送请求:', config.method?.toUpperCase(), config.url)
    return config
  },
  (error) => {
    console.error('[axios] 请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    console.log('[axios] 收到响应:', response.status, response.config.url)
    return response
  },
  async (error) => {
    const originalRequest = error.config
    
    // 401错误 - token过期或无效
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      
      console.log('[axios] 收到401响应，尝试刷新token')
      
      // 获取refresh token
      const refreshToken = localStorage.getItem('REFRESH_TOKEN')
      
      if (refreshToken && !originalRequest.url?.includes('/api/auth/refresh')) {
        try {
          // 直接调用刷新API，避免循环依赖
          const refreshResponse = await axios.post('http://localhost:18080/api/auth/refresh', {
            refreshToken
          })
          
          if (refreshResponse.data.code === 200) {
            const { accessToken, refreshToken: newRefreshToken, user } = refreshResponse.data.data
            
            // 保存新的token
            localStorage.setItem('ACCESS_TOKEN', accessToken)
            localStorage.setItem('REFRESH_TOKEN', newRefreshToken)
            localStorage.setItem('USER_INFO', JSON.stringify(user))
            
            console.log('[axios] Token刷新成功，重试原请求')
            
            // 更新原请求的Authorization头
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${accessToken}`
            }
            
            // 重试原请求
            return instance(originalRequest)
          } else {
            throw new Error('刷新token失败')
          }
        } catch (refreshError) {
          console.error('[axios] Token刷新失败:', refreshError)
          handleAuthFailure()
        }
      } else {
        console.warn('[axios] 没有refresh token或刷新接口失败，直接跳转登录')
        handleAuthFailure()
      }
    }
    
    console.error('[axios] 响应错误:', error.response?.status, error.message)
    return Promise.reject(error)
  }
)

// 处理认证失败的统一方法
function handleAuthFailure() {
  console.log('[axios] 处理认证失败，清除数据并跳转登录页面')
  
  // 清除所有认证数据
  localStorage.removeItem('ACCESS_TOKEN')
  localStorage.removeItem('REFRESH_TOKEN')
  localStorage.removeItem('USER_INFO')
  
  // 通知用户，但不强制跳转页面
  if (typeof window !== 'undefined') {
    console.log('[axios] 认证失败，保持在当前页面')
    // 可以在这里触发登录弹窗，但需要访问 auth store
    // 这里只记录日志，具体的登录弹窗由 auth store 的 handleAuthError 处理
  }
}

export default instance