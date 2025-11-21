import apiClient from './axios'

/**
 * 个人资料相关接口
 */

// 个人资料响应接口
export interface ProfileResponse {
  userId: number
  userAccount: string
  displayName: string
  email?: string
  avatarUrl?: string
  bio?: string
  gender?: 'M' | 'F' | 'U'
  birthday?: string
  phoneNumber?: string
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
}

// 更新个人资料请求接口
export interface UpdateProfileRequest {
  displayName: string
  email?: string
  avatarUrl?: string
  bio?: string
  gender?: 'M' | 'F' | 'U'
  birthday?: string
  phoneNumber?: string
}

// 性别选项
export const GENDER_OPTIONS = [
  { label: '男', value: 'M' },
  { label: '女', value: 'F' },
  { label: '保密', value: 'U' }
] as const

/**
 * 个人资料API类
 */
export class ProfileAPI {

  /**
   * 获取当前用户个人资料
   */
  static async getCurrentUserProfile(): Promise<ProfileResponse> {
    console.log('[ProfileAPI] 获取当前用户个人资料')

    try {
      const response = await apiClient.get('/api/profile')

      // 检查响应状态
      if (response.status >= 200 && response.status < 300) {
        // 成功响应，检查数据格式
        if (response.data && typeof response.data === 'object') {
          // 如果有 code 字段，按标准格式处理
          if ('code' in response.data) {
            if (response.data.code === 200) {
              console.log('[ProfileAPI] 获取个人资料成功:', response.data.data)
              return response.data.data
            } else {
              throw new Error(response.data.message || '获取个人资料失败')
            }
          } else {
            // 没有 code 字段，直接返回数据
            console.log('[ProfileAPI] 获取个人资料成功:', response.data)
            return response.data
          }
        } else {
          throw new Error('服务器返回数据格式错误')
        }
      } else {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
    } catch (error: any) {
      console.error('[ProfileAPI] 获取个人资料失败:', error)

      // 处理网络错误或服务器错误
      if (error.response) {
        const errorData = error.response.data
        if (errorData && errorData.message) {
          throw new Error(errorData.message)
        } else if (errorData && errorData.error) {
          throw new Error(errorData.error)
        } else {
          throw new Error(`服务器错误: ${error.response.status}`)
        }
      } else if (error.message) {
        throw error
      } else {
        throw new Error('网络连接失败')
      }
    }
  }

  /**
   * 获取指定用户个人资料
   */
  static async getUserProfile(userId: number): Promise<ProfileResponse> {
    console.log('[ProfileAPI] 获取用户个人资料, userId:', userId)

    const response = await apiClient.get(`/api/profile/${userId}`)

    if (response.data.code !== 200) {
      throw new Error(response.data.message || '获取用户资料失败')
    }

    console.log('[ProfileAPI] 获取用户资料成功:', response.data.data)
    return response.data.data
  }

  /**
   * 更新当前用户个人资料
   */
  static async updateCurrentUserProfile(request: UpdateProfileRequest): Promise<ProfileResponse> {
    console.log('[ProfileAPI] 更新当前用户个人资料:', request)

    try {
      const response = await apiClient.put('/api/profile', request)

      // 检查响应状态
      if (response.status >= 200 && response.status < 300) {
        // 成功响应，检查数据格式
        if (response.data && typeof response.data === 'object') {
          // 如果有 code 字段，按标准格式处理
          if ('code' in response.data) {
            if (response.data.code === 200) {
              console.log('[ProfileAPI] 更新个人资料成功:', response.data.data)
              return response.data.data
            } else {
              throw new Error(response.data.message || '更新个人资料失败')
            }
          } else {
            // 没有 code 字段，直接返回数据
            console.log('[ProfileAPI] 更新个人资料成功:', response.data)
            return response.data
          }
        } else {
          throw new Error('服务器返回数据格式错误')
        }
      } else {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
    } catch (error: any) {
      console.error('[ProfileAPI] 更新个人资料失败:', error)

      // 处理网络错误或服务器错误
      if (error.response) {
        const errorData = error.response.data
        if (errorData && errorData.message) {
          throw new Error(errorData.message)
        } else if (errorData && errorData.error) {
          throw new Error(errorData.error)
        } else {
          throw new Error(`服务器错误: ${error.response.status}`)
        }
      } else if (error.message) {
        throw error
      } else {
        throw new Error('网络连接失败')
      }
    }
  }

  /**
   * 检查邮箱是否可用
   */
  static async checkEmailAvailability(email: string): Promise<boolean> {
    console.log('[ProfileAPI] 检查邮箱可用性:', email)

    const response = await apiClient.get('/api/profile/check-email', {
      params: { email }
    })

    if (response.data.code !== 200) {
      throw new Error(response.data.message || '检查邮箱可用性失败')
    }

    console.log('[ProfileAPI] 邮箱可用性检查结果:', response.data.data)
    return response.data.data
  }
}

/**
 * 个人资料工具类
 */
export class ProfileUtils {

  /**
   * 格式化性别显示
   */
  static formatGender(gender?: string): string {
    const option = GENDER_OPTIONS.find(opt => opt.value === gender)
    return option?.label || '保密'
  }

  /**
   * 格式化生日显示
   */
  static formatBirthday(birthday?: string): string {
    if (!birthday) return '未设置'

    try {
      const date = new Date(birthday)
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    } catch {
      return birthday
    }
  }

  /**
   * 计算年龄
   */
  static calculateAge(birthday?: string): number | null {
    if (!birthday) return null

    try {
      const birthDate = new Date(birthday)
      const today = new Date()
      let age = today.getFullYear() - birthDate.getFullYear()
      const monthDiff = today.getMonth() - birthDate.getMonth()

      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--
      }

      return age >= 0 ? age : null
    } catch {
      return null
    }
  }

  /**
   * 验证邮箱格式
   */
  static validateEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  }

  /**
   * 验证手机号格式
   */
  static validatePhoneNumber(phone: string): boolean {
    const phoneRegex = /^1[3-9]\d{9}$/
    return phoneRegex.test(phone)
  }

  /**
   * 验证生日格式
   */
  static validateBirthday(birthday: string): boolean {
    const birthdayRegex = /^\d{4}-\d{2}-\d{2}$/
    if (!birthdayRegex.test(birthday)) return false

    try {
      const date = new Date(birthday)
      const today = new Date()

      // 检查日期是否有效
      if (date.toISOString().slice(0, 10) !== birthday) return false

      // 检查日期是否在合理范围内（不能是未来日期，不能超过150岁）
      const minDate = new Date()
      minDate.setFullYear(today.getFullYear() - 150)

      return date >= minDate && date <= today
    } catch {
      return false
    }
  }

  /**
   * 获取头像显示URL
   */
  static getAvatarUrl(avatarUrl?: string): string | null {
    if (!avatarUrl || avatarUrl.trim() === '') {
      return null
    }

    // 如果是相对路径，转换为绝对路径
    if (avatarUrl.startsWith('/')) {
      return `${window.location.origin}${avatarUrl}`
    }

    // 如果是完整URL，直接返回
    if (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://')) {
      return avatarUrl
    }

    // 其他情况返回null，使用默认头像
    return null
  }
}
