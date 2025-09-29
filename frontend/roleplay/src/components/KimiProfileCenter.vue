<template>
  <n-modal
    :show="visible"
    @update:show="(value: boolean) => $emit('update:visible', value)"
    :mask-closable="true"
    preset="card"
    title="个人中心"
    class="kimi-profile-modal"
    style="width: 480px"
  >
    <div class="kimi-profile-content">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <!-- 头像预览显示 -->
        <div class="avatar-preview-container">
          <div 
            class="avatar-display"
            @click="triggerFileInput"
          >
            <n-avatar
              :size="80"
              :src="displayAvatarUrl"
              class="profile-avatar"
              :class="{ 'has-preview': !!avatarPreview.previewUrl.value }"
              @error="handleAvatarError"
              @load="handleAvatarLoad"
            >
              <template #fallback>
                <div class="avatar-fallback">
                  <div class="avatar-eyes">◉ ◉</div>
                  <div class="avatar-mouth">◡</div>
                </div>
              </template>
            </n-avatar>
            
            
            <!-- 上传图标悬停效果 -->
            <div class="upload-overlay">
              <n-icon size="24" color="white">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 2C13.1 2 14 2.9 14 4V10H16L12 14L8 10H10V4C10 2.9 10.9 2 12 2ZM21 15V18C21 19.1 20.1 20 19 20H5C3.9 20 3 19.1 3 18V15C3 13.9 3.9 13 5 13H7.14L8.83 14.83L12 18L15.17 14.83L16.86 13H19C20.1 13 21 13.9 21 15ZM19 16C18.4 16 18 16.4 18 17C18 17.6 18.4 18 19 18C19.6 18 20 17.6 20 17C20 16.4 19.6 16 19 16Z" fill="currentColor"/>
                </svg>
              </n-icon>
            </div>
            
            <!-- 加载状态 -->
            <div v-if="avatarPreview.isLoading.value" class="loading-overlay">
              <n-spin size="small" />
            </div>
          </div>
          
          <!-- 文件输入 -->
          <input
            ref="fileInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            @change="handleFileSelect"
            style="display: none"
          />
        </div>
        
        
      </div>

      <!-- 昵称区域 -->
      <div class="nickname-section">
        <div class="section-label">昵称</div>
        <div class="nickname-input-container">
          <n-input
            v-model:value="localProfile.displayName"
            placeholder="请输入昵称"
            maxlength="50"
            show-count
            @input="handleNicknameChange"
            @blur="handleNicknameSave"
            class="nickname-input"
          />
          <div v-if="nicknameSaving" class="saving-indicator">
            <n-spin size="small" />
            <span>保存中...</span>
          </div>
          <div v-else-if="nicknameSaved" class="saved-indicator">
            <n-icon size="14" color="#18a058">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" fill="currentColor"/>
              </svg>
            </n-icon>
            <span>已保存</span>
          </div>
        </div>
      </div>

      <!-- 邮箱区域 -->
      <div class="email-section">
        <div class="section-label">邮箱</div>
        <n-input
          v-model:value="localProfile.email"
          placeholder="请输入邮箱地址"
          @blur="handleEmailChange"
          class="email-input"
        />
      </div>

      <!-- 暂时隐藏数据库中不存在的字段 -->
      <!-- 个人简介区域 -->
      <!-- <div class="bio-section">
        <div class="section-label">个人简介</div>
        <n-input
          v-model:value="localProfile.bio"
          type="textarea"
          placeholder="介绍一下自己吧..."
          maxlength="200"
          show-count
          :autosize="{ minRows: 3, maxRows: 5 }"
          @blur="handleBioChange"
          class="bio-input"
        />
      </div> -->

      <!-- 其他信息区域 -->
      <!-- <div class="other-info-section">
        <div class="info-row">
          <div class="info-item">
            <div class="section-label">性别</div>
            <n-select
              v-model:value="localProfile.gender"
              placeholder="请选择性别"
              :options="genderOptions"
              @update:value="handleGenderChange"
              class="gender-select"
            />
          </div>
          <div class="info-item">
            <div class="section-label">生日</div>
            <n-date-picker
              v-model:formatted-value="localProfile.birthday"
              value-format="yyyy-MM-dd"
              type="date"
              placeholder="请选择生日"
              clearable
              :is-date-disabled="(ts: number) => ts > Date.now()"
              @update:formatted-value="handleBirthdayChange"
              class="birthday-picker"
            />
          </div>
        </div>
        
        <div class="info-row">
          <div class="info-item full-width">
            <div class="section-label">手机号</div>
            <n-input
              v-model:value="localProfile.phoneNumber"
              placeholder="请输入手机号码"
              maxlength="11"
              @blur="handlePhoneChange"
              class="phone-input"
            />
          </div>
        </div>
      </div> -->

      <!-- 操作按钮区域 -->
      <div class="action-section">
        <n-button
          type="primary"
          size="large"
          :loading="saving"
          @click="handleSaveAll"
          class="save-btn"
        >
          保存
        </n-button>
        
        <n-button
          type="error"
          size="large"
          @click="handleLogout"
          class="logout-btn"
        >
          退出登录
        </n-button>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, nextTick } from 'vue'
import {
  NModal,
  NInput,
  NButton,
  NAvatar,
  NSelect,
  NDatePicker,
  NText,
  NIcon,
  NSpin,
  useMessage,
  useDialog
} from 'naive-ui'
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

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const authStore = useAuthStore()
const message = useMessage()
const dialog = useDialog()

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
  // 暂时注释掉数据库中不存在的字段
  // bio: '',
  // gender: 'U',
  // birthday: '',
  // phoneNumber: ''
})

// 原始数据（用于比较是否有变更）
const originalProfile = reactive<UpdateProfileRequest>({
  displayName: '',
  email: '',
  avatarUrl: ''
  // 暂时注释掉数据库中不存在的字段
  // bio: '',
  // gender: 'U',
  // birthday: '',
  // phoneNumber: ''
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
        // 暂时注释掉数据库中不存在的字段
        // bio: userInfo.bio || '',
        // gender: userInfo.gender || 'U',
        // birthday: userInfo.birthday || '',
        // phoneNumber: userInfo.phoneNumber || ''
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
        // 暂时注释掉数据库中不存在的字段
        // bio: profile.bio || '',
        // gender: profile.gender || 'U',
        // birthday: profile.birthday || '',
        // phoneNumber: profile.phoneNumber || ''
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
      // 暂时注释掉数据库中不存在的字段
      // bio: originalProfile.bio,
      // gender: originalProfile.gender,
      // birthday: originalProfile.birthday,
      // phoneNumber: originalProfile.phoneNumber
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

// === 原有的头像处理方法（保留兼容性） ===

// 头像URL变更处理
const handleAvatarUrlChange = () => {
  showAvatarInput.value = false
  
  // 验证头像URL格式
  if (localProfile.avatarUrl && localProfile.avatarUrl.trim()) {
    const url = localProfile.avatarUrl.trim()
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      message.warning('头像URL必须以 http:// 或 https:// 开头')
      localProfile.avatarUrl = originalProfile.avatarUrl
      return
    }
  }
  
  if (localProfile.avatarUrl !== originalProfile.avatarUrl) {
    console.log('[KimiProfileCenter] 头像URL已变更，等待保存:', localProfile.avatarUrl)
  }
}

// 头像上传成功处理
const handleAvatarUploadSuccess = (response: AvatarUploadResponse) => {
  console.log('[KimiProfileCenter] 头像上传成功:', response)
  
  // 更新本地头像URL
  localProfile.avatarUrl = response.avatarUrl
  originalProfile.avatarUrl = response.avatarUrl
  
  message.success('头像上传成功！')
}

// 头像上传失败处理
const handleAvatarUploadError = (error: any) => {
  console.error('[KimiProfileCenter] 头像上传失败:', error)
  // 错误消息已在AvatarUploader组件中处理
}

// 头像删除成功处理
const handleAvatarDeleteSuccess = () => {
  console.log('[KimiProfileCenter] 头像删除成功')
  
  // 更新本地头像URL
  localProfile.avatarUrl = ''
  originalProfile.avatarUrl = ''
  
  message.success('头像删除成功！')
}

// 头像删除失败处理
const handleAvatarDeleteError = (error: any) => {
  console.error('[KimiProfileCenter] 头像删除失败:', error)
  // 错误消息已在AvatarUploader组件中处理
}

// 其他字段变更处理
const handleEmailChange = () => {
  console.log('[KimiProfileCenter] 邮箱已变更，等待保存')
}

const handleBioChange = () => {
  console.log('[KimiProfileCenter] 个人简介已变更，等待保存')
}

const handleGenderChange = () => {
  console.log('[KimiProfileCenter] 性别已变更，等待保存')
}

const handleBirthdayChange = () => {
  console.log('[KimiProfileCenter] 生日已变更，等待保存')
}

const handlePhoneChange = () => {
  console.log('[KimiProfileCenter] 手机号已变更，等待保存')
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
const handleLogout = () => {
  dialog.warning({
    title: '确认退出',
    content: '确定要退出登录吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await authStore.logout()
        emit('update:visible', false)
        message.success('已退出登录')
      } catch (error) {
        console.error('[KimiProfileCenter] 退出登录失败:', error)
        message.error('退出登录失败')
      }
    }
  })
}
</script>

<style scoped>
.kimi-profile-content {
  padding: 8px 0;
}

/* 头像区域 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.avatar-preview-container {
  position: relative;
  margin-bottom: 16px;
}

.avatar-display {
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.avatar-display:hover {
  transform: scale(1.05);
}

.profile-avatar {
  border: 3px solid #f0f0f0;
  transition: all 0.3s ease;
}

.profile-avatar:hover {
  border-color: #18a058;
}

.profile-avatar.has-preview {
  border-color: #18a058;
  box-shadow: 0 0 0 2px rgba(24, 160, 88, 0.2);
}

/* 上传图标悬停效果 */
.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 3;
}

.avatar-display:hover .upload-overlay {
  opacity: 1;
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  z-index: 5;
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

.avatar-edit-btn {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 28px;
  height: 28px;
  background: #18a058;
  border: 2px solid #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
  transition: all 0.3s ease;
}

.avatar-edit-btn:hover {
  background: #36ad6a;
  transform: scale(1.1);
}

.avatar-url-toggle {
  margin-top: 8px;
}

.avatar-input-section {
  width: 100%;
  max-width: 300px;
}

/* 表单区域 */
.nickname-section,
.email-section,
.bio-section,
.other-info-section {
  margin-bottom: 24px;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.nickname-input-container {
  position: relative;
}

.nickname-input,
.email-input,
.bio-input,
.phone-input {
  width: 100%;
}

.saving-indicator,
.saved-indicator {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.saved-indicator {
  color: #18a058;
}

/* 其他信息区域 */
.info-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-item {
  flex: 1;
}

.info-item.full-width {
  flex: none;
  width: 100%;
}

.gender-select,
.birthday-picker {
  width: 100%;
}

/* 操作按钮区域 */
.action-section {
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.save-btn,
.logout-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

.logout-btn {
  background: #d03050;
  border-color: #d03050;
}

.logout-btn:hover {
  background: #e63946;
  border-color: #e63946;
}

.logout-btn:focus {
  background: #d03050;
  border-color: #d03050;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .kimi-profile-modal {
    width: 90vw !important;
    max-width: 400px !important;
  }
  
  .info-row {
    flex-direction: column;
    gap: 16px;
  }
  
  .info-item {
    width: 100%;
  }
}

/* 输入框样式优化 */
:deep(.n-input) {
  transition: all 0.3s ease;
}

:deep(.n-input:hover) {
  border-color: #18a058;
}

:deep(.n-input.n-input--focus) {
  border-color: #18a058;
  box-shadow: 0 0 0 2px rgba(24, 160, 88, 0.2);
}

:deep(.n-select) {
  transition: all 0.3s ease;
}

:deep(.n-date-picker) {
  width: 100%;
}

/* 模态框样式 */
:deep(.n-card) {
  border-radius: 16px;
}

:deep(.n-card-header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.n-card__content) {
  padding: 24px;
}
</style>
