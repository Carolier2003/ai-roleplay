<template>
  <div class="user-module">
    <!-- 用户信息区域 -->
    <div class="user-info">
      <div class="user-avatar-container">
        <n-avatar
          :size="48"
          :round="authStore.isLoggedIn"
          :src="userAvatar"
          class="user-avatar"
          :class="{ 'guest-avatar': !authStore.isLoggedIn }"
          @error="handleAvatarError"
          @load="handleAvatarLoad"
        >
          <template #fallback>
            <div class="avatar-fallback" :class="{ 'guest-fallback': !authStore.isLoggedIn, 'user-fallback': authStore.isLoggedIn }">
              <div v-if="!authStore.isLoggedIn" class="guest-avatar-content">
                <div class="avatar-eyes">• •</div>
                <div class="avatar-mouth">‿</div>
              </div>
              <div v-else class="user-avatar-content">
                <div class="avatar-eyes">◉ ◉</div>
                <div class="avatar-mouth">◡</div>
              </div>
            </div>
          </template>
        </n-avatar>
      </div>
      
      <div class="user-details">
        <div class="user-name">{{ displayName }}</div>
        <div class="user-status">
          <span class="status-dot" :class="statusClass"></span>
          <span class="status-text">{{ statusText }}</span>
        </div>
        
        <!-- 聊天次数显示已移除 -->
      </div>
      
      <div class="user-actions">
        <n-button
          v-if="authStore.isLoggedIn"
          text
          size="small"
          @click="showSettings = !showSettings"
          class="settings-btn"
        >
          <template #icon>
            <span>⚙️</span>
          </template>
        </n-button>
        <n-button
          v-else
          size="small"
          type="primary"
          @click="authStore.showLoginModal()"
          class="login-btn"
        >
          登录
        </n-button>
      </div>
    </div>
    
    <!-- 设置面板 -->
    <div v-if="showSettings && authStore.isLoggedIn" class="settings-panel">
      <div class="settings-header">
        <span>设置选项</span>
      </div>
      
      <div class="settings-list">
        <div class="setting-item" @click="editProfile">
          <span class="setting-icon">👤</span>
          <span class="setting-label">个人资料</span>
          <span class="setting-action">编辑</span>
        </div>
        
        
        <div class="setting-item" @click="logout">
          <span class="setting-icon">🚪</span>
          <span class="setting-label">退出登录</span>
          <span class="setting-action danger">退出</span>
        </div>
      </div>
    </div>
    
    <!-- 聊呗标识栏 -->
    <div class="brand-section">
      <div class="brand-logo">
        <div class="brand-avatar-content">
          <div class="brand-eyes">◉ ◉</div>
          <div class="brand-mouth">◡</div>
        </div>
      </div>
      <div class="brand-text">
        <div class="brand-name">聊呗</div>
        <div class="brand-desc">AI角色扮演</div>
      </div>
    </div>
    
    <!-- Kimi风格个人中心弹窗 -->
    <KimiProfileCenter
      v-model:visible="showKimiProfile"
      @success="onKimiProfileSuccess"
    />
    
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { NAvatar, NButton, useMessage, useDialog } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import KimiProfileCenter from './KimiProfileCenter.vue'

const authStore = useAuthStore()
const chatStore = useChatStore()
const message = useMessage()
const dialog = useDialog()

const showSettings = ref(false)
const showKimiProfile = ref(false)
// 聊天统计相关代码已移除

// 计算属性
const displayName = computed(() => {
  if (authStore.isLoggedIn && authStore.userInfo) {
    return authStore.userInfo.displayName || authStore.userInfo.userAccount || '用户'
  }
  return '游客'
})

const userAvatar = computed(() => {
  if (authStore.isLoggedIn && authStore.userInfo?.avatarUrl) {
    const avatarUrl = authStore.userInfo.avatarUrl.trim()
    // 验证URL格式
    if (avatarUrl && (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://') || avatarUrl.startsWith('data:image/'))) {
      console.log('[UserModule] 使用用户自定义头像:', avatarUrl)
      return avatarUrl
    }
  }
  // 所有用户（包括游客和没有头像的登录用户）都使用默认头像
  console.log('[UserModule] 使用默认头像 fallback')
  return undefined // 使用 fallback 模板中的头像
})

const statusClass = computed(() => {
  return authStore.isLoggedIn ? 'online' : 'offline'
})

const statusText = computed(() => {
  return authStore.isLoggedIn ? '在线' : '游客模式'
})

// 方法
const editProfile = () => {
  console.log('[UserModule] 打开Kimi风格个人中心')
  showKimiProfile.value = true
  showSettings.value = false // 关闭设置面板
}



const logout = async () => {
  try {
    await authStore.logout()
    showSettings.value = false
    
    // 清除聊天状态
    chatStore.clearMessages()
    
    message.success('已退出登录')
    
    // 强制更新页面状态
    await nextTick()
  } catch (error) {
    console.error('[UserModule] 退出登录失败:', error)
    message.error('退出登录失败')
  }
}

// Kimi个人中心更新成功回调
const onKimiProfileSuccess = () => {
  console.log('[UserModule] Kimi个人中心更新成功')
  message.success('个人资料更新成功')
  
  // 可以在这里做一些额外的处理，比如刷新用户信息显示
}

// 头像加载成功处理
const handleAvatarLoad = () => {
  console.log('[UserModule] 头像加载成功:', userAvatar.value)
}

// 头像加载失败处理
const handleAvatarError = (error: Event) => {
  console.error('[UserModule] 头像加载失败:', userAvatar.value, error)
  
  // 如果是阿里云OSS链接，尝试添加时间戳
  if (userAvatar.value && userAvatar.value.includes('aliyuncs.com') && !userAvatar.value.includes('?t=')) {
    const timestamp = Date.now()
    const newUrl = `${userAvatar.value}?t=${timestamp}`
    console.log('[UserModule] 尝试添加时间戳重新加载头像:', newUrl)
    
    // 更新用户信息中的头像URL
    if (authStore.userInfo && authStore.userInfo.avatarUrl === userAvatar.value) {
      authStore.userInfo.avatarUrl = newUrl
      localStorage.setItem('USER_INFO', JSON.stringify(authStore.userInfo))
    }
  }
}
</script>

<style scoped>
.user-module {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar-container {
  flex-shrink: 0;
}

.user-avatar {
  border: 2px solid #e5e7eb;
  transition: all 0.3s ease;
}

.user-avatar:hover {
  transform: scale(1.05);
  border-color: #3b82f6;
}

.avatar-fallback {
  font-size: 24px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.guest-fallback {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 50%, #fecfef 100%);
  color: #fff;
}

.user-fallback {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.guest-avatar-content,
.user-avatar-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: monospace;
}

.avatar-eyes {
  font-size: 12px;
  margin-bottom: 2px;
  letter-spacing: 2px;
}

.avatar-mouth {
  font-size: 10px;
}

.guest-avatar {
  border-radius: 12px !important;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 2px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.online {
  background-color: #10b981;
}

.status-dot.offline {
  background-color: #9ca3af;
}

.chat-limit,
.chat-stats {
  font-size: 11px;
  color: #9ca3af;
}

.chat-limit {
  color: #f59e0b;
}

.user-actions {
  flex-shrink: 0;
}

.settings-btn {
  color: #6b7280;
}

.settings-btn:hover {
  color: #3b82f6;
}

.login-btn {
  font-size: 12px;
  height: 28px;
}

.settings-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.settings-header {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.setting-item:hover {
  background-color: #f3f4f6;
}

.setting-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.setting-label {
  flex: 1;
  font-size: 14px;
  color: #374151;
}

.setting-action {
  font-size: 12px;
  color: #6b7280;
}

.setting-action.danger {
  color: #ef4444;
}

.brand-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  color: white;
}

.brand-avatar-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: monospace;
}

.brand-eyes {
  font-size: 8px;
  margin-bottom: 1px;
  letter-spacing: 1px;
}

.brand-mouth {
  font-size: 8px;
}

.brand-text {
  flex: 1;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 2px;
}

.brand-desc {
  font-size: 12px;
  color: #9ca3af;
}
</style>
