<template>
  <div class="simple-avatar-uploader">
    <!-- 头像显示区域 -->
    <div 
      class="avatar-container"
      @click="triggerFileInput"
      :class="{ 'uploading': uploading }"
    >
      <n-avatar
        :size="size"
        :src="currentAvatarUrl"
        class="profile-avatar"
      >
        <div v-if="!currentAvatarUrl" class="avatar-fallback">
          <div class="avatar-eyes">◉ ◉</div>
          <div class="avatar-mouth">◡</div>
        </div>
      </n-avatar>
      
      <!-- 上传遮罩 -->
      <div class="upload-overlay">
        <n-spin v-if="uploading" size="small" />
        <n-icon v-else size="20" color="#fff">
          <svg viewBox="0 0 24 24">
            <path d="M14,2H6A2,2 0 0,0 4,4V20A2,2 0 0,0 6,22H18A2,2 0 0,0 20,20V8L14,2M18,20H6V4H13V9H18V20Z" fill="currentColor"/>
          </svg>
        </n-icon>
        <span class="upload-text">{{ uploading ? '上传中...' : '点击更换' }}</span>
      </div>
    </div>
    
    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      @change="handleFileSelect"
      style="display: none"
    />
    
    <!-- 提示文本 -->
    <div class="upload-hint">
      <n-text depth="3" style="font-size: 12px;">
        点击头像更换，支持 JPG、PNG 格式，最大 5MB
      </n-text>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NAvatar, NIcon, NSpin, NText, useMessage } from 'naive-ui'
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

const message = useMessage()
const authStore = useAuthStore()

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

<style scoped>
.simple-avatar-uploader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-container {
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.avatar-container:hover {
  transform: scale(1.05);
}

.avatar-container.uploading {
  cursor: not-allowed;
}

.profile-avatar {
  border: 3px solid #f0f0f0;
  transition: all 0.3s ease;
}

.avatar-container:hover .profile-avatar {
  border-color: #18a058;
}

.avatar-fallback {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: monospace;
  width: 100%;
  height: 100%;
}

.avatar-eyes {
  font-size: 20px;
  margin-bottom: 4px;
  letter-spacing: 4px;
}

.avatar-mouth {
  font-size: 16px;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  gap: 4px;
}

.avatar-container:hover .upload-overlay {
  opacity: 1;
}

.avatar-container.uploading .upload-overlay {
  opacity: 1;
}

.upload-text {
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

.upload-hint {
  text-align: center;
  max-width: 200px;
}
</style>
