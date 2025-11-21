<template>
  <div class="flex flex-col items-center gap-3">
    <!-- 头像显示区域 -->
    <div 
      class="relative cursor-pointer rounded-full overflow-hidden transition-transform duration-300 hover:scale-105 group"
      @click="triggerFileInput"
      :style="{ width: `${size}px`, height: `${size}px` }"
    >
      <img 
        v-if="currentAvatarUrl"
        :src="currentAvatarUrl"
        class="w-full h-full object-cover transition-opacity duration-300"
        :class="{ 'opacity-60': uploading }"
        alt="Avatar"
      />
      <div v-else class="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-600 text-white">
        <div class="text-base mb-0.5 tracking-widest">◉ ◉</div>
        <div class="text-xs">◡</div>
      </div>
      
      <!-- 上传遮罩 -->
      <div 
        class="absolute inset-0 bg-black/60 flex flex-col items-center justify-center opacity-0 transition-opacity duration-300 rounded-full"
        :class="{ 'opacity-100': !uploading && !currentAvatarUrl, 'group-hover:opacity-100': !uploading }"
        v-if="!uploading"
      >
        <svg viewBox="0 0 24 24" class="w-6 h-6 text-white" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C13.1 2 14 2.9 14 4V10H16L12 14L8 10H10V4C10 2.9 10.9 2 12 2ZM21 15V18C21 19.1 20.1 20 19 20H5C3.9 20 3 19.1 3 18V15C3 13.9 3.9 13 5 13H7.14L8.83 14.83L12 18L15.17 14.83L16.86 13H19C20.1 13 21 13.9 21 15ZM19 16C18.4 16 18 16.4 18 17C18 17.6 18.4 18 19 18C19.6 18 20 17.6 20 17C20 16.4 19.6 16 19 16Z" fill="currentColor"/>
        </svg>
        <div class="text-white text-xs mt-1">点击上传</div>
      </div>
      
      <!-- 上传进度遮罩 -->
      <div class="absolute inset-0 bg-white/90 flex flex-col items-center justify-center rounded-full" v-if="uploading">
        <div class="w-5 h-5 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
        <div class="text-xs mt-1 text-gray-600">上传中...</div>
      </div>
    </div>
    
    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/jpeg,image/jpg,image/png,image/webp"
      @change="handleFileSelect"
      class="hidden"
    />
    
    <!-- 操作按钮 -->
    <div class="flex gap-2 flex-wrap justify-center" v-if="showActions">
      <button
        class="px-3 py-1 text-xs font-medium text-indigo-600 bg-transparent border border-indigo-600 rounded hover:bg-indigo-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1 transition-colors"
        @click="triggerFileInput"
        :disabled="uploading"
      >
        <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C13.1 2 14 2.9 14 4V10H16L12 14L8 10H10V4C10 2.9 10.9 2 12 2Z" fill="currentColor"/>
        </svg>
        更换头像
      </button>
      
      <button
        class="px-3 py-1 text-xs font-medium text-red-600 bg-transparent border border-red-600 rounded hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1 transition-colors"
        @click="handleDeleteAvatar"
        :disabled="uploading || deleting || !currentAvatarUrl"
        v-if="currentAvatarUrl"
      >
        <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M6 19C6 20.1 6.9 21 8 21H16C17.1 21 18 20.1 18 19V7H6V19ZM19 4H15.5L14.5 3H9.5L8.5 4H5V6H19V4Z" fill="currentColor"/>
        </svg>
        删除头像
      </button>
    </div>
    
    <!-- 文件信息提示 -->
    <div class="text-center max-w-[200px]" v-if="showFileInfo">
      <span class="text-xs text-gray-400">
        支持 JPG、PNG、WebP 格式，文件大小不超过 10MB
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { AvatarAPI, AvatarUtils, type AvatarUploadResponse } from '@/api/avatar'
import { useAuthStore } from '@/stores/auth'

interface Props {
  size?: number
  showActions?: boolean
  showFileInfo?: boolean
}

interface Emits {
  (e: 'upload-success', response: AvatarUploadResponse): void
  (e: 'upload-error', error: any): void
  (e: 'delete-success'): void
  (e: 'delete-error', error: any): void
}

const props = withDefaults(defineProps<Props>(), {
  size: 80,
  showActions: true,
  showFileInfo: true
})

const emit = defineEmits<Emits>()

import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const authStore = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

// Simple replacement for useMessage and useDialog
const message = {
  success: (msg: string) => {
    console.log('Success:', msg)
    toast.success(msg)
  },
  error: (msg: string) => {
    console.error('Error:', msg)
    toast.error(msg)
  }
}

// 响应式数据
const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)
const deleting = ref(false)

// 计算属性
const currentAvatarUrl = computed(() => {
  return authStore.userInfo?.avatarUrl || null
})

// 触发文件选择
const triggerFileInput = () => {
  if (uploading.value) return
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  console.log('[AvatarUploader] 选择文件:', file.name, 'size:', file.size)
  
  try {
    // 验证文件
    const validation = AvatarUtils.validateImageFile(file)
    if (!validation.valid) {
      message.error(validation.message!)
      return
    }
    
    // 开始上传
    uploading.value = true
    
    // 调用上传API
    const response = await AvatarAPI.uploadAvatar(file)
    
    // 更新用户头像信息
    if (authStore.userInfo) {
      authStore.userInfo.avatarUrl = response.avatarUrl
      localStorage.setItem('USER_INFO', JSON.stringify(authStore.userInfo))
    }
    
    message.success('头像上传成功！')
    emit('upload-success', response)
    
    console.log('[AvatarUploader] 头像上传成功:', response)
    
  } catch (error: any) {
    console.error('[AvatarUploader] 头像上传失败:', error)
    
    // 使用错误处理器获取用户友好的错误消息
    const { ErrorHandler } = await import('@/utils/errorHandler')
    const userMessage = ErrorHandler.getUserMessage(error)
    
    message.error(userMessage || '头像上传失败，请稍后重试')
    emit('upload-error', error)
  } finally {
    uploading.value = false
    // 清空文件输入，允许重新选择相同文件
    if (target) {
      target.value = ''
    }
  }
}

// 删除头像
const handleDeleteAvatar = async () => {
  const confirmed = await confirm.warning(
    '确定要删除当前头像吗？删除后将显示默认头像。',
    '确认删除'
  )

  if (confirmed) {
    try {
      deleting.value = true
      
      const success = await AvatarAPI.deleteAvatar()
      
      if (success) {
        // 更新用户头像信息
        if (authStore.userInfo) {
          authStore.userInfo.avatarUrl = undefined
          localStorage.setItem('USER_INFO', JSON.stringify(authStore.userInfo))
        }
        
        message.success('头像删除成功！')
        emit('delete-success')
        
        console.log('[AvatarUploader] 头像删除成功')
      } else {
        message.error('头像删除失败，请稍后重试')
      }
      
    } catch (error: any) {
      console.error('[AvatarUploader] 头像删除失败:', error)
      
      // 使用错误处理器获取用户友好的错误消息
      const { ErrorHandler } = await import('@/utils/errorHandler')
      const userMessage = ErrorHandler.getUserMessage(error)
      
      message.error(userMessage || '头像删除失败，请稍后重试')
      emit('delete-error', error)
    } finally {
      deleting.value = false
    }
  }
}
</script>
