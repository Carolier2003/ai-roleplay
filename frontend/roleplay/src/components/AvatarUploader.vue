<template>
  <div class="avatar-uploader">
    <!-- 头像显示区域 -->
    <div class="avatar-display" @click="triggerFileInput">
      <n-avatar
        :size="size"
        :src="currentAvatarUrl"
        class="avatar-image"
        :class="{ 'uploading': uploading }"
      >
        <template #fallback>
          <div class="avatar-fallback">
            <div class="avatar-eyes">◉ ◉</div>
            <div class="avatar-mouth">◡</div>
          </div>
        </template>
      </n-avatar>
      
      <!-- 上传遮罩 -->
      <div class="upload-overlay" v-if="!uploading">
        <n-icon size="24" color="white">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2C13.1 2 14 2.9 14 4V10H16L12 14L8 10H10V4C10 2.9 10.9 2 12 2ZM21 15V18C21 19.1 20.1 20 19 20H5C3.9 20 3 19.1 3 18V15C3 13.9 3.9 13 5 13H7.14L8.83 14.83L12 18L15.17 14.83L16.86 13H19C20.1 13 21 13.9 21 15ZM19 16C18.4 16 18 16.4 18 17C18 17.6 18.4 18 19 18C19.6 18 20 17.6 20 17C20 16.4 19.6 16 19 16Z" fill="currentColor"/>
          </svg>
        </n-icon>
        <div class="upload-text">点击上传</div>
      </div>
      
      <!-- 上传进度遮罩 -->
      <div class="uploading-overlay" v-if="uploading">
        <n-spin size="small" />
        <div class="uploading-text">上传中...</div>
      </div>
    </div>
    
    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/jpeg,image/jpg,image/png,image/webp"
      @change="handleFileSelect"
      style="display: none"
    />
    
    <!-- 操作按钮 -->
    <div class="avatar-actions" v-if="showActions">
      <n-button
        size="small"
        type="primary"
        ghost
        @click="triggerFileInput"
        :loading="uploading"
        :disabled="uploading"
      >
        <template #icon>
          <n-icon>
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C13.1 2 14 2.9 14 4V10H16L12 14L8 10H10V4C10 2.9 10.9 2 12 2Z" fill="currentColor"/>
            </svg>
          </n-icon>
        </template>
        更换头像
      </n-button>
      
      <n-button
        size="small"
        type="error"
        ghost
        @click="handleDeleteAvatar"
        :loading="deleting"
        :disabled="uploading || deleting || !currentAvatarUrl"
        v-if="currentAvatarUrl"
      >
        <template #icon>
          <n-icon>
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 19C6 20.1 6.9 21 8 21H16C17.1 21 18 20.1 18 19V7H6V19ZM19 4H15.5L14.5 3H9.5L8.5 4H5V6H19V4Z" fill="currentColor"/>
            </svg>
          </n-icon>
        </template>
        删除头像
      </n-button>
    </div>
    
    <!-- 文件信息提示 -->
    <div class="file-info" v-if="showFileInfo">
      <n-text depth="3" style="font-size: 12px;">
        支持 JPG、PNG、WebP 格式，文件大小不超过 10MB
      </n-text>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
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

const message = useMessage()
const dialog = useDialog()
const authStore = useAuthStore()

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
const handleDeleteAvatar = () => {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除当前头像吗？删除后将显示默认头像。',
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
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
  })
}
</script>

<style scoped>
.avatar-uploader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-display {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  transition: all 0.3s ease;
}

.avatar-display:hover {
  transform: scale(1.05);
}

.avatar-display:hover .upload-overlay {
  opacity: 1;
}

.avatar-image {
  transition: all 0.3s ease;
}

.avatar-image.uploading {
  opacity: 0.6;
}

.avatar-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
}

.avatar-eyes {
  margin-bottom: 2px;
  font-size: 16px;
}

.avatar-mouth {
  font-size: 12px;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 50%;
}

.upload-text {
  color: white;
  font-size: 12px;
  margin-top: 4px;
}

.uploading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.uploading-text {
  font-size: 12px;
  margin-top: 4px;
  color: #666;
}

.avatar-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.file-info {
  text-align: center;
  max-width: 200px;
}
</style>
