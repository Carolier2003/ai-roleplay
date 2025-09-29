import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { ProfileAPI } from '@/api/profile'
import type { LoginRequest, RegisterRequest, UserResponse } from '@/api/auth'
import type { ProfileResponse, UpdateProfileRequest } from '@/api/profile'

export interface UserInfo {
  userId: number
  userAccount: string
  displayName: string
  avatarUrl?: string
  email?: string
  // 暂时注释掉数据库中不存在的字段
  // bio?: string
  // gender?: 'M' | 'F' | 'U'
  // birthday?: string
  // phoneNumber?: string
}

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string>('')
  const refreshTokenValue = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)
  const loginModalVisible = ref<boolean>(false)
  const loading = ref<boolean>(false)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

  // 初始化：从localStorage恢复状态
  const initAuth = () => {
    const savedToken = localStorage.getItem('ACCESS_TOKEN')
    const savedRefreshToken = localStorage.getItem('REFRESH_TOKEN')
    const savedUserInfo = localStorage.getItem('USER_INFO')

    if (savedToken) {
      token.value = savedToken
      console.log('[auth] 从localStorage恢复token:', savedToken.substring(0, 20) + '...')
    }
    
    if (savedRefreshToken) {
      refreshTokenValue.value = savedRefreshToken
      console.log('[auth] 从localStorage恢复refreshToken')
    }

    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo)
        console.log('[auth] 从localStorage恢复用户信息:', userInfo.value?.userAccount)
      } catch (error) {
        console.error('[auth] 解析用户信息失败:', error)
        clearAuthData()
      }
    }
  }

  // 清除认证数据
  const clearAuthData = () => {
    token.value = ''
    refreshTokenValue.value = ''
    userInfo.value = null
    
    localStorage.removeItem('ACCESS_TOKEN')
    localStorage.removeItem('REFRESH_TOKEN')
    localStorage.removeItem('USER_INFO')
    
    console.log('[auth] 已清除所有认证数据')
  }

  // 保存认证数据
  const saveAuthData = (accessToken: string, refreshToken: string, user: UserInfo) => {
    token.value = accessToken
    refreshTokenValue.value = refreshToken
    userInfo.value = user

    localStorage.setItem('ACCESS_TOKEN', accessToken)
    localStorage.setItem('REFRESH_TOKEN', refreshToken)
    localStorage.setItem('USER_INFO', JSON.stringify(user))
    
    console.log('[auth] 已保存认证数据:', {
      user: user.userAccount,
      tokenLength: accessToken.length,
      refreshTokenLength: refreshToken.length
    })
  }

  // 用户登录
  const login = async (loginData: LoginRequest): Promise<void> => {
    loading.value = true
    
    try {
      const response = await authApi.login(loginData)
      
      const user: UserInfo = {
        userId: response.user.userId,
        userAccount: response.user.userAccount,
        displayName: response.user.displayName,
        avatarUrl: response.user.avatarUrl || undefined // 如果没有头像，使用null让组件显示默认头像
      }
      
      saveAuthData(response.accessToken, response.refreshToken, user)
      loginModalVisible.value = false
      
      console.log('[auth] 登录成功:', user.userAccount)
      
      // ✅ 登录成功后加载角色列表并选择第一个角色
      try {
        const { useChatStore } = await import('@/stores/chat')
        const chatStore = useChatStore()
        await chatStore.loadCharacters()
        
        // 自动选择第一个角色（柯南）
        if (chatStore.characters.length > 0) {
          const firstCharacter = chatStore.characters[0]
          chatStore.setCurrentCharacter(firstCharacter.id)
          console.log('[auth] 自动选择第一个角色:', firstCharacter.name)
          
          // 加载该角色的历史记录
          try {
            await chatStore.loadMessages(firstCharacter.id)
            console.log('[auth] 加载第一个角色的历史记录成功')
          } catch (error) {
            console.warn('[auth] 加载角色历史记录失败:', error)
          }
        }
        
        console.log('[auth] 登录后加载角色列表成功')
      } catch (error) {
        console.warn('[auth] 登录后加载角色列表失败:', error)
      }
    } catch (error: any) {
      console.error('[auth] 登录失败:', error)
      // 直接抛出原始错误，让上层组件使用ErrorHandler处理
      throw error
    } finally {
      loading.value = false
    }
  }

  // 用户注册
  const register = async (registerData: RegisterRequest): Promise<UserResponse> => {
    loading.value = true
    
    try {
      const user = await authApi.register(registerData)
      console.log('[auth] 注册成功:', user.userAccount)
      
      // 注册成功后不自动登录，返回用户信息供后续使用
      return user
    } catch (error: any) {
      console.error('[auth] 注册失败:', error)
      // 直接抛出原始错误，让上层组件使用ErrorHandler处理
      throw error
    } finally {
      loading.value = false
    }
  }

  // 刷新token
  const refreshToken = async (): Promise<boolean> => {
    if (!refreshTokenValue.value) {
      return false
    }

    try {
      const response = await authApi.refreshToken(refreshTokenValue.value)
      
      const user: UserInfo = {
        userId: response.user.userId,
        userAccount: response.user.userAccount,
        displayName: response.user.displayName,
        avatarUrl: response.user.avatarUrl || undefined // 如果没有头像，使用null让组件显示默认头像
      }
      
      saveAuthData(response.accessToken, response.refreshToken, user)
      
      console.log('[auth] Token刷新成功')
      return true
    } catch (error) {
      console.error('[auth] Token刷新失败:', error)
      clearAuthData()
      return false
    }
  }

  // 退出登录
  const logout = async (): Promise<void> => {
    try {
      await authApi.logout()
    } catch (error) {
      console.warn('[auth] 退出登录请求失败:', error)
    } finally {
      clearAuthData()
      console.log('[auth] 已退出登录')
      
      // 不再强制跳转到登录页面，保持在当前聊天页面
      console.log('[auth] 退出登录，保持在当前页面')
    }
  }

  // 处理认证错误（由 axios 拦截器调用）
  const handleAuthError = () => {
    console.log('[auth] 处理认证错误，清除数据')
    clearAuthData()
    
    // 不再强制跳转，而是弹出登录弹窗
    console.log('[auth] 认证错误，弹出登录弹窗')
    showLoginModal()
  }

  // 显示登录弹窗
  const showLoginModal = () => {
    loginModalVisible.value = true
  }

  // 隐藏登录弹窗
  const hideLoginModal = () => {
    loginModalVisible.value = false
  }

  // 获取当前用户信息
  const fetchCurrentUser = async (): Promise<void> => {
    if (!token.value) return

    try {
      const user = await authApi.getCurrentUser()
      userInfo.value = {
        userId: user.userId,
        userAccount: user.userAccount,
        displayName: user.displayName,
        avatarUrl: user.avatarUrl || undefined // 如果没有头像，使用undefined让组件显示默认头像
      }
      
      localStorage.setItem('USER_INFO', JSON.stringify(userInfo.value))
    } catch (error) {
      console.error('[auth] 获取用户信息失败:', error)
      // 如果获取用户信息失败，可能token已过期
      await logout()
    }
  }

  // 获取完整的用户个人资料
  const fetchUserProfile = async (): Promise<ProfileResponse | null> => {
    if (!token.value || !userInfo.value) return null

    try {
      console.log('[auth] 获取完整用户个人资料')
      const profile = await ProfileAPI.getCurrentUserProfile()
      
      // 更新本地用户信息
      userInfo.value = {
        userId: profile.userId,
        userAccount: profile.userAccount,
        displayName: profile.displayName,
        avatarUrl: profile.avatarUrl || undefined,
        email: profile.email
        // 暂时注释掉数据库中不存在的字段
        // bio: profile.bio,
        // gender: profile.gender,
        // birthday: profile.birthday,
        // phoneNumber: profile.phoneNumber
      }
      
      localStorage.setItem('USER_INFO', JSON.stringify(userInfo.value))
      console.log('[auth] 获取完整用户个人资料成功:', profile.displayName)
      
      return profile
    } catch (error) {
      console.error('[auth] 获取用户个人资料失败:', error)
      return null
    }
  }

  // 更新用户个人资料
  const updateUserProfile = async (request: UpdateProfileRequest): Promise<ProfileResponse | null> => {
    if (!token.value || !userInfo.value) {
      throw new Error('用户未登录')
    }

    try {
      console.log('[auth] 更新用户个人资料:', request)
      const updatedProfile = await ProfileAPI.updateCurrentUserProfile(request)
      
      // 更新本地用户信息
      userInfo.value = {
        userId: updatedProfile.userId,
        userAccount: updatedProfile.userAccount,
        displayName: updatedProfile.displayName,
        avatarUrl: updatedProfile.avatarUrl || undefined,
        email: updatedProfile.email
        // 暂时注释掉数据库中不存在的字段
        // bio: updatedProfile.bio,
        // gender: updatedProfile.gender,
        // birthday: updatedProfile.birthday,
        // phoneNumber: updatedProfile.phoneNumber
      }
      
      localStorage.setItem('USER_INFO', JSON.stringify(userInfo.value))
      console.log('[auth] 更新用户个人资料成功:', updatedProfile.displayName)
      
      return updatedProfile
    } catch (error: any) {
      console.error('[auth] 更新用户个人资料失败:', error)
      throw new Error(error.message || '更新个人资料失败')
    }
  }

  // 检查邮箱是否可用
  const checkEmailAvailability = async (email: string): Promise<boolean> => {
    try {
      return await ProfileAPI.checkEmailAvailability(email)
    } catch (error: any) {
      console.error('[auth] 检查邮箱可用性失败:', error)
      throw new Error(error.message || '检查邮箱可用性失败')
    }
  }

  return {
    // 状态
    token,
    refreshTokenValue,
    userInfo,
    loginModalVisible,
    loading,
    
    // 计算属性
    isLoggedIn,
    
    // 方法
    initAuth,
    login,
    register,
    refreshToken,
    logout,
    handleAuthError,
    showLoginModal,
    hideLoginModal,
    fetchCurrentUser,
    clearAuthData,
    saveAuthData,
    
    // 个人资料方法
    fetchUserProfile,
    updateUserProfile,
    checkEmailAvailability
  }
})
