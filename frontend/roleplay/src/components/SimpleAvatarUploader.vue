<template>
  <div class="flex flex-col items-center gap-3">
    <!-- 头像显示区域 -->
    <div 
      class="relative cursor-pointer transition-transform duration-300 hover:scale-105 group"
      :class="{ 'cursor-not-allowed': uploading }"
      @click="triggerFileInput"
      :style="{ width: `${size}px`, height: `${size}px` }"
    >
      <div 
        class="w-full h-full rounded-full overflow-hidden border-[3px] border-gray-100 transition-colors duration-300 group-hover:border-green-500"
      >
        <img 
          v-if="currentAvatarUrl" 
          :src="currentAvatarUrl" 
          class="w-full h-full object-cover"
          alt="Avatar"
        />
        <div v-else class="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-600 text-white font-mono">
          <div class="text-xl mb-1 tracking-widest">◉ ◉</div>
          <div class="text-base">◡</div>
        </div>
      </div>
      
      <!-- 上传遮罩 -->
      <div 
        class="absolute inset-0 bg-black/60 rounded-full flex flex-col items-center justify-center opacity-0 transition-opacity duration-300 gap-1"
        :class="{ 'opacity-100': uploading, 'group-hover:opacity-100': !uploading }"
      >
        <div v-if="uploading" class="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
        <svg v-else viewBox="0 0 24 24" class="w-5 h-5 text-white">
          <path d="M14,2H6A2,2 0 0,0 4,4V20A2,2 0 0,0 6,22H18A2,2 0 0,0 20,20V8L14,2M18,20H6V4H13V9H18V20Z" fill="currentColor"/>
        </svg>
        <span class="text-white text-xs font-medium">{{ uploading ? '上传中...' : '点击更换' }}</span>
      </div>
    </div>
    
    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      @change="handleFileSelect"
      class="hidden"
    />
    
    <!-- 提示文本 -->
    <div class="text-center max-w-[200px]">
      <span class="text-xs text-gray-400">
        点击头像更换，支持 JPG、PNG 格式，最大 5MB
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { AvatarAPI, AvatarUtils } from '@/api/avatar'
import type { AvatarUploadResponse } from '@/api/avatar'

interface Props {
  size?: number
}

interface Emits {
  (e: 'upload-success', response: AvatarUploadResponse): void
  (e: 'upload-error', error: any): void
}

const props = withDefaults(defineProps<Props>(), {
  size: 80
})

const emit = defineEmits<Emits>()

const authStore = useAuthStore()

// Simple replacement for useMessage
const message = {
  success: (msg: string) => console.log('Success:', msg),
  error: (msg: string) => {
    console.error('Error:', msg)
    alert(msg)
  }
}

// 响应式数据
const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)

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
  
  console.log('[SimpleAvatarUploader] 选择文件:', file.name, 'size:', file.size)
  
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
    
    message.success('头像更换成功！')
    emit('upload-success', response)
    
    console.log('[SimpleAvatarUploader] 头像上传成功:', response)
    
  } catch (error: any) {
    console.error('[SimpleAvatarUploader] 头像上传失败:', error)
    
    const { ErrorHandler } = await import('@/utils/errorHandler')
    const userMessage = ErrorHandler.getUserMessage(error)
    
    message.error(userMessage || '头像上传失败，请稍后重试')
    emit('upload-error', error)
  } finally {
    uploading.value = false
    // 清空文件输入，允许重复选择同一文件
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
}
</script>
