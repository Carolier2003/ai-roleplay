<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto overflow-x-hidden bg-black/50 backdrop-blur-sm p-4 sm:p-0" @click.self="$emit('update:visible', false)">
      <div class="relative w-full max-w-md transform rounded-2xl bg-white p-6 text-left shadow-xl transition-all sm:my-8">
        <!-- Close button -->
        <button @click="$emit('update:visible', false)" class="absolute top-4 right-4 text-gray-400 hover:text-gray-600 focus:outline-none">
          <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <div class="text-center mb-6">
          <h3 class="text-xl font-bold leading-6 text-gray-900">个人中心</h3>
        </div>

        <div class="flex flex-col gap-6">
          <!-- 头像区域 -->
          <div class="flex flex-col items-center">
            <div class="relative mb-4">
              <div 
                class="relative w-20 h-20 rounded-full overflow-hidden border-[3px] border-gray-100 cursor-pointer group transition-all duration-300 hover:scale-105"
                :class="{ 'border-green-500 shadow-[0_0_0_2px_rgba(24,160,88,0.2)]': !!avatarPreview.previewUrl.value }"
                @click="triggerFileInput"
              >
                <img 
                  v-if="displayAvatarUrl" 
                  :src="displayAvatarUrl" 
                  class="w-full h-full object-cover"
                  @error="handleAvatarError"
                  @load="handleAvatarLoad"
                  alt="Avatar"
                />
                <div v-else class="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-600 text-white font-mono">
                  <div class="text-xl mb-1 tracking-widest">◉ ◉</div>
                  <div class="text-base">◡</div>
                </div>
                
                <!-- 上传图标悬停效果 -->
                <div class="absolute inset-0 bg-black/60 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                  <svg class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 2C13.1 2 14 2.9 14 4V10H16L12 14L8 10H10V4C10 2.9 10.9 2 12 2ZM21 15V18C21 19.1 20.1 20 19 20H5C3.9 20 3 19.1 3 18V15C3 13.9 3.9 13 5 13H7.14L8.83 14.83L12 18L15.17 14.83L16.86 13H19C20.1 13 21 13.9 21 15ZM19 16C18.4 16 18 16.4 18 17C18 17.6 18.4 18 19 18C19.6 18 20 17.6 20 17C20 16.4 19.6 16 19 16Z" fill="currentColor"/>
                  </svg>
                </div>
                
                <!-- 加载状态 -->
                <div v-if="avatarPreview.isLoading.value" class="absolute inset-0 bg-white/80 flex items-center justify-center z-10">
                  <div class="w-5 h-5 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
                </div>
              </div>
              
              <!-- 文件输入 -->
              <input
                ref="fileInputRef"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                @change="handleFileSelect"
                class="hidden"
              />
            </div>
          </div>

          <!-- 昵称区域 -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium text-gray-700">昵称</label>
            <div class="relative">
              <input
                v-model="localProfile.displayName"
                type="text"
                placeholder="请输入昵称"
                maxlength="50"
                @input="handleNicknameChange"
                @blur="handleNicknameSave"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
              />
              <div class="absolute right-3 top-1/2 transform -translate-y-1/2 flex items-center text-xs">
                <div v-if="nicknameSaving" class="flex items-center text-gray-400">
                  <div class="w-3 h-3 border-2 border-gray-400 border-t-transparent rounded-full animate-spin mr-1"></div>
                  <span>保存中...</span>
                </div>
                <div v-else-if="nicknameSaved" class="flex items-center text-green-600">
                  <svg class="w-3.5 h-3.5 mr-1" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" fill="currentColor"/>
                  </svg>
                  <span>已保存</span>
                </div>
                <span v-else class="text-gray-400">{{ localProfile.displayName.length }}/50</span>
              </div>
            </div>
          </div>

          <!-- 邮箱区域 -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium text-gray-700">邮箱</label>
            <input
              v-model="localProfile.email"
              type="email"
              placeholder="请输入邮箱地址"
              @blur="handleEmailChange"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
            />
          </div>

          <!-- 操作按钮区域 -->
          <div class="flex flex-col gap-3 mt-4">
            <button
              @click="handleSaveAll"
              :disabled="saving"
              class="w-full py-2.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 active:bg-indigo-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium flex items-center justify-center gap-2"
            >
              <div v-if="saving" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              保存
            </button>
            
            <button
              @click="handleLogout"
              class="w-full py-2.5 bg-red-50 text-red-600 border border-red-200 rounded-lg hover:bg-red-100 active:bg-red-200 transition-colors font-medium"
            >
              退出登录
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { GENDER_OPTIONS } from '@/api/profile'
import type { UpdateProfileRequest } from '@/api/profile'
import { AvatarAPI, type AvatarUploadResponse } from '@/api/avatar'
import { useAvatarPreview } from '@/composables/useAvatarPreview'

interface Props {
  visible: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}

import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const authStore = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

// Simple replacement for useMessage and useDialog
const message = {
  warning: (msg: string) => {
    console.warn('Warning:', msg)
    toast.warning(msg)
  },
  error: (msg: string) => {
    console.error('Error:', msg)
    toast.error(msg)
  },
  success: (msg: string) => {
    console.log('Success:', msg)
    toast.success(msg)
  }
}

// 状态
const saving = ref(false)
const nicknameSaving = ref(false)
const nicknameSaved = ref(false)

// 头像预览功能
const avatarPreview = useAvatarPreview()
const fileInputRef = ref<HTMLInputElement>()

// 当前文件对象（用于保存）
const currentFile = ref<File | null>(null)

// 性别选项
const genderOptions = GENDER_OPTIONS.map(option => ({
  label: option.label,
  value: option.value
}))

// 计算属性 - 显示的头像URL（预览优先，然后是最新的用户头像）
const displayAvatarUrl = computed(() => {
  // 1. 优先显示预览图片
  if (avatarPreview.previewUrl.value) {
    return avatarPreview.previewUrl.value
  }
  
  // 2. 显示本地资料中的头像URL
  if (localProfile.avatarUrl) {
    return localProfile.avatarUrl
  }
  
  // 3. 显示全局用户信息中的头像URL（确保实时更新）
  if (authStore.userInfo?.avatarUrl) {
    return authStore.userInfo.avatarUrl
  }
  
  // 4. 没有头像时返回null，使用fallback
  return null
})

// 本地数据
const localProfile = reactive<UpdateProfileRequest>({
  displayName: '',
  email: '',
  avatarUrl: ''
})

// 原始数据（用于比较是否有变更）
const originalProfile = reactive<UpdateProfileRequest>({
  displayName: '',
  email: '',
  avatarUrl: ''
})

// 监听弹窗显示状态
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    loadUserProfile()
  } else {
    // 关闭弹窗时清理预览
    handleCancelPreview()
  }
})

// 加载用户资料
const loadUserProfile = async () => {
  try {
    console.log('[KimiProfileCenter] 加载用户资料')
    
    // 先从store获取基本信息
    const userInfo = authStore.userInfo
    if (userInfo) {
      const profileData = {
        displayName: userInfo.displayName || '',
        email: userInfo.email || '',
        avatarUrl: userInfo.avatarUrl || ''
      }
      
      Object.assign(localProfile, profileData)
      Object.assign(originalProfile, profileData)
    }
    
    // 尝试获取完整的用户资料
    const profile = await authStore.fetchUserProfile()
    if (profile) {
      const profileData = {
        displayName: profile.displayName || '',
        email: profile.email || '',
        avatarUrl: profile.avatarUrl || ''
      }
      
      Object.assign(localProfile, profileData)
      Object.assign(originalProfile, profileData)
    }
    
    console.log('[KimiProfileCenter] 用户资料加载完成:', localProfile)
  } catch (error) {
    console.error('[KimiProfileCenter] 加载用户资料失败:', error)
    message.error('加载用户资料失败')
  }
}

// 昵称变更处理
let nicknameTimer: NodeJS.Timeout | null = null
const handleNicknameChange = () => {
  nicknameSaved.value = false
  
  // 清除之前的定时器
  if (nicknameTimer) {
    clearTimeout(nicknameTimer)
  }
  
  // 设置新的定时器，500ms后自动保存
  nicknameTimer = setTimeout(() => {
    handleNicknameSave()
  }, 500)
}

// 昵称保存
const handleNicknameSave = async () => {
  if (nicknameTimer) {
    clearTimeout(nicknameTimer)
    nicknameTimer = null
  }
  
  if (!localProfile.displayName.trim()) {
    message.warning('昵称不能为空')
    return
  }
  
  if (localProfile.displayName === originalProfile.displayName) {
    return // 没有变更
  }
  
  try {
    nicknameSaving.value = true
    
    await authStore.updateUserProfile({
      displayName: localProfile.displayName,
      email: originalProfile.email,
      avatarUrl: originalProfile.avatarUrl
    })
    
    originalProfile.displayName = localProfile.displayName
    nicknameSaved.value = true
    
    // 2秒后隐藏保存状态
    setTimeout(() => {
      nicknameSaved.value = false
    }, 2000)
    
    console.log('[KimiProfileCenter] 昵称保存成功')
  } catch (error: any) {
    console.error('[KimiProfileCenter] 昵称保存失败:', error)
    message.error(error.message || '昵称保存失败')
    // 恢复原始值
    localProfile.displayName = originalProfile.displayName
  } finally {
    nicknameSaving.value = false
  }
}

// === 新的头像预览处理方法 ===

// 触发文件选择
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  console.log('[KimiProfileCenter] 选择文件进行预览:', file.name, 'size:', file.size)
  
  // 使用预览钩子处理文件
  const success = avatarPreview.previewFile(file)
  if (success) {
    currentFile.value = file
    console.log('[KimiProfileCenter] 文件预览成功，等待保存')
  }
  
  // 清空文件输入，允许重复选择同一文件
  if (target) {
    target.value = ''
  }
}

// 取消预览（关闭弹窗时使用）
const handleCancelPreview = () => {
  console.log('[KimiProfileCenter] 取消头像预览')
  
  avatarPreview.clearPreview()
  currentFile.value = null
}

// 头像加载成功处理
const handleAvatarLoad = () => {
  console.log('[KimiProfileCenter] 头像加载成功:', displayAvatarUrl.value)
}

// 头像加载失败处理
const handleAvatarError = (error: Event) => {
  console.error('[KimiProfileCenter] 头像加载失败:', displayAvatarUrl.value, error)
  
  // 尝试添加时间戳强制刷新
  if (displayAvatarUrl.value && !displayAvatarUrl.value.includes('?t=')) {
    const timestamp = Date.now()
    const newUrl = `${displayAvatarUrl.value}?t=${timestamp}`
    console.log('[KimiProfileCenter] 尝试添加时间戳重新加载:', newUrl)
    
    // 更新头像URL
    if (localProfile.avatarUrl === displayAvatarUrl.value) {
      localProfile.avatarUrl = newUrl
    }
    if (authStore.userInfo && authStore.userInfo.avatarUrl === displayAvatarUrl.value) {
      authStore.userInfo.avatarUrl = newUrl
      localStorage.setItem('USER_INFO', JSON.stringify(authStore.userInfo))
    }
  }
}

// 其他字段变更处理
const handleEmailChange = () => {
  console.log('[KimiProfileCenter] 邮箱已变更，等待保存')
}

// 保存所有更改
const handleSaveAll = async () => {
  if (!localProfile.displayName.trim()) {
    message.warning('昵称不能为空')
    return
  }
  
  try {
    saving.value = true
    console.log('[KimiProfileCenter] 开始保存所有更改:', localProfile)
    
    // 如果有头像预览，先保存头像
    if (avatarPreview.previewUrl.value) {
      console.log('[KimiProfileCenter] 检测到头像预览，先保存头像')
      
      if (currentFile.value) {
        // 文件上传
        console.log('[KimiProfileCenter] 上传文件头像')
        const response = await AvatarAPI.uploadAvatar(currentFile.value)
        
        // 更新本地头像URL
        localProfile.avatarUrl = response.avatarUrl
        
        // 更新用户信息
        if (authStore.userInfo) {
          authStore.userInfo.avatarUrl = response.avatarUrl
          localStorage.setItem('USER_INFO', JSON.stringify(authStore.userInfo))
        }
        
        console.log('[KimiProfileCenter] 头像上传成功:', response)
      }
      
      // 清理预览状态
      avatarPreview.clearPreview()
      currentFile.value = null
    }
    
    // 保存用户资料
    await authStore.updateUserProfile(localProfile)
    
    // 重新拉取最新用户信息
    console.log('[KimiProfileCenter] 重新拉取用户信息')
    await authStore.fetchUserProfile()
    
    // 更新本地数据为最新数据
    const latestUserInfo = authStore.userInfo
    if (latestUserInfo) {
      const profileData = {
        displayName: latestUserInfo.displayName || '',
        email: latestUserInfo.email || '',
        avatarUrl: latestUserInfo.avatarUrl || ''
      }
      
      console.log('[KimiProfileCenter] 更新本地数据:', profileData)
      console.log('[KimiProfileCenter] 最新头像URL:', profileData.avatarUrl)
      
      Object.assign(localProfile, profileData)
      Object.assign(originalProfile, profileData)
      
      // 确保头像立即更新显示
      console.log('[KimiProfileCenter] 当前显示的头像URL:', displayAvatarUrl.value)
    }
    
    message.success('已保存')
    emit('success')
    
    // 等待一小段时间确保所有状态更新完成
    await nextTick()
    
    // 强制触发响应式更新
    console.log('[KimiProfileCenter] 强制触发头像更新')
    
    // 自动关闭弹窗
    console.log('[KimiProfileCenter] 保存成功，自动关闭弹窗')
    emit('update:visible', false)
    
    console.log('[KimiProfileCenter] 所有更改保存成功')
  } catch (error: any) {
    console.error('[KimiProfileCenter] 保存失败:', error)
    
    // 使用错误处理器获取用户友好的错误消息
    const { ErrorHandler } = await import('@/utils/errorHandler')
    const userMessage = ErrorHandler.getUserMessage(error)
    
    message.error(userMessage || '保存失败')
  } finally {
    saving.value = false
  }
}

// 退出登录
const handleLogout = async () => {
  const confirmed = await confirm.warning('确定要退出登录吗？', '确认退出')
  if (confirmed) {
    try {
      await authStore.logout()
      emit('update:visible', false)
      message.success('已退出登录')
    } catch (error) {
      console.error('[KimiProfileCenter] 退出登录失败:', error)
      message.error('退出登录失败')
    }
  }
}
</script>
