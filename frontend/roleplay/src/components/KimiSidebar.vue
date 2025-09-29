<template>
  <div class="kimi-sidebar" :class="{ 'mobile-hidden': !showMobile }">
    <!-- 顶部：新会话按钮 -->
    <div class="sidebar-header">
      <button @click="handleNewConversation" class="new-chat-btn">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z" fill="currentColor"/>
        </svg>
        <span>新会话</span>
      </button>
    </div>

    <!-- 中间：历史会话列表 -->
    <div class="conversation-list">
      <div class="list-header">
        <h3>历史记录</h3>
      </div>
      
      <div class="conversations-scroll">
        <div
          v-for="conversation in conversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ active: conversation.id === activeConversationId }"
          @click="handleSelectConversation(conversation.id)"
        >
          <div class="conversation-content">
            <div class="conversation-title">{{ conversation.title }}</div>
            <div class="conversation-time">{{ formatTime(conversation.lastMessageTime) }}</div>
          </div>
          
          <!-- 三点菜单 -->
          <div class="conversation-menu">
            <n-dropdown
              :options="conversationMenuOptions"
              @select="(key) => handleConversationAction(key, conversation)"
              trigger="click"
              placement="bottom-start"
            >
              <button class="menu-trigger" @click.stop>
                <svg viewBox="0 0 24 24" width="16" height="16">
                  <path d="M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z" fill="currentColor"/>
                </svg>
              </button>
            </n-dropdown>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="conversations.length === 0" class="empty-conversations">
          <div class="empty-icon">💬</div>
          <p>暂无历史记录</p>
        </div>
      </div>
    </div>

    <!-- 底部：我的面板 -->
    <div class="profile-section">
      <div class="profile-header" @click="toggleProfile">
        <div class="profile-info">
          <div class="profile-avatar" @click.stop="handleAvatarClick">
            <img v-if="profile.avatar" :src="profile.avatar" alt="头像" />
            <svg v-else viewBox="0 0 24 24" width="24" height="24">
              <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z" fill="currentColor"/>
            </svg>
          </div>
          <div class="profile-text">
            <div class="profile-name">{{ profile.nickname || '用户' }}</div>
            <div class="profile-status">我的</div>
          </div>
        </div>
        <svg class="expand-icon" :class="{ expanded: showProfile }" viewBox="0 0 24 24" width="16" height="16">
          <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z" fill="currentColor"/>
        </svg>
      </div>
      
      <!-- 展开的个人资料编辑 -->
      <div v-if="showProfile" class="profile-edit">
        <div class="edit-field">
          <label>昵称</label>
          <input
            v-model="editProfile.nickname"
            type="text"
            placeholder="输入昵称"
            class="edit-input"
          />
        </div>
        
        <div class="edit-field">
          <label>邮箱</label>
          <input
            v-model="editProfile.email"
            type="email"
            placeholder="输入邮箱"
            class="edit-input"
          />
        </div>
        
        <div class="edit-field">
          <label>个人简介</label>
          <textarea
            v-model="editProfile.bio"
            placeholder="介绍一下自己"
            class="edit-textarea"
            rows="2"
          ></textarea>
        </div>
        
        <div class="edit-actions">
          <button @click="handleSaveProfile" class="save-btn">保存</button>
          <button @click="handleCancelEdit" class="cancel-btn">取消</button>
        </div>
        
        <!-- 退出登录按钮 -->
        <div class="logout-section">
          <button @click="handleLogout" class="logout-btn">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path d="M16,17V14H9V10H16V7L21,12L16,17M14,2A2,2 0 0,1 16,4V6H14V4H5V20H14V18H16V20A2,2 0 0,1 14,22H5A2,2 0 0,1 3,20V4A2,2 0 0,1 5,2H14Z" fill="currentColor"/>
            </svg>
            退出登录
          </button>
        </div>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      @change="handleAvatarUpload"
      style="display: none"
    />

    <!-- 重命名对话框 -->
    <n-modal v-model:show="showRenameModal">
      <div class="rename-modal">
        <h3>重命名会话</h3>
        <input
          v-model="renameTitle"
          type="text"
          placeholder="输入新标题"
          class="rename-input"
          @keydown.enter="handleConfirmRename"
        />
        <div class="rename-actions">
          <button @click="handleConfirmRename" class="confirm-btn">确认</button>
          <button @click="showRenameModal = false" class="cancel-btn">取消</button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, h } from 'vue'
import { NDropdown, NModal, useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

// 类型定义
interface Conversation {
  id: string
  title: string
  lastMessageTime: number
  characterId: number
  messages: Array<{
    id: string
    content: string
    isUser: boolean
    timestamp: number
  }>
}

interface Profile {
  nickname: string
  email: string
  bio: string
  avatar: string
}

interface Props {
  showMobile?: boolean
  activeConversationId?: string
}

interface Emits {
  selectConversation: [conversationId: string]
  newConversation: []
  updateProfile: [profile: Profile]
}

const props = withDefaults(defineProps<Props>(), {
  showMobile: true,
  activeConversationId: ''
})

const emit = defineEmits<Emits>()
const message = useMessage()
const authStore = useAuthStore()
const router = useRouter()

// 响应式数据
const conversations = ref<Conversation[]>([])
const profile = ref<Profile>({
  nickname: '',
  email: '',
  bio: '',
  avatar: ''
})
const editProfile = ref<Profile>({ ...profile.value })
const showProfile = ref(false)
const showRenameModal = ref(false)
const renameTitle = ref('')
const renamingConversation = ref<Conversation | null>(null)
const fileInput = ref<HTMLInputElement>()

// 会话菜单选项
const conversationMenuOptions = [
  {
    label: '重命名',
    key: 'rename',
    icon: () => h('svg', { viewBox: '0 0 24 24', width: 16, height: 16 }, [
      h('path', { d: 'M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z', fill: 'currentColor' })
    ])
  },
  {
    label: '删除',
    key: 'delete',
    icon: () => h('svg', { viewBox: '0 0 24 24', width: 16, height: 16 }, [
      h('path', { d: 'M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z', fill: 'currentColor' })
    ])
  }
]

// 计算属性
const sortedConversations = computed(() => {
  return [...conversations.value].sort((a, b) => b.lastMessageTime - a.lastMessageTime)
})

// 方法
const loadData = () => {
  // 从localStorage加载数据
  const savedConversations = localStorage.getItem('CHAT_CONVERSATIONS')
  if (savedConversations) {
    conversations.value = JSON.parse(savedConversations)
  }
  
  const savedProfile = localStorage.getItem('USER_PROFILE')
  if (savedProfile) {
    profile.value = JSON.parse(savedProfile)
    editProfile.value = { ...profile.value }
  }
}

const saveData = () => {
  localStorage.setItem('CHAT_CONVERSATIONS', JSON.stringify(conversations.value))
  localStorage.setItem('USER_PROFILE', JSON.stringify(profile.value))
}

const handleNewConversation = () => {
  emit('newConversation')
}

const handleSelectConversation = (conversationId: string) => {
  emit('selectConversation', conversationId)
}

const handleConversationAction = (action: string, conversation: Conversation) => {
  switch (action) {
    case 'rename':
      renamingConversation.value = conversation
      renameTitle.value = conversation.title
      showRenameModal.value = true
      break
    case 'delete':
      handleDeleteConversation(conversation.id)
      break
  }
}

const handleDeleteConversation = (conversationId: string) => {
  conversations.value = conversations.value.filter(c => c.id !== conversationId)
  saveData()
  message.success('会话已删除')
}

const handleConfirmRename = () => {
  if (renamingConversation.value && renameTitle.value.trim()) {
    renamingConversation.value.title = renameTitle.value.trim()
    saveData()
    showRenameModal.value = false
    message.success('重命名成功')
  }
}

const toggleProfile = () => {
  showProfile.value = !showProfile.value
  if (showProfile.value) {
    editProfile.value = { ...profile.value }
  }
}

const handleAvatarClick = () => {
  fileInput.value?.click()
}

const handleAvatarUpload = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      editProfile.value.avatar = e.target?.result as string
    }
    reader.readAsDataURL(file)
  }
}

const handleSaveProfile = () => {
  profile.value = { ...editProfile.value }
  saveData()
  showProfile.value = false
  emit('updateProfile', profile.value)
  message.success('个人资料已保存')
}

const handleCancelEdit = () => {
  editProfile.value = { ...profile.value }
  showProfile.value = false
}

const handleLogout = async () => {
  try {
    await authStore.logout()
    message.success('退出登录成功')
    router.push('/login')
  } catch (error) {
    console.error('[KimiSidebar] 退出登录失败:', error)
    message.error('退出登录失败')
  }
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric',
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit', 
    minute: '2-digit',
    hour12: false
  })
}

// 公开方法供父组件调用
const addConversation = (conversation: Conversation) => {
  conversations.value.unshift(conversation)
  saveData()
}

const updateConversation = (conversationId: string, updates: Partial<Conversation>) => {
  const index = conversations.value.findIndex(c => c.id === conversationId)
  if (index !== -1) {
    conversations.value[index] = { ...conversations.value[index], ...updates }
    saveData()
  }
}

// 暴露方法给父组件
defineExpose({
  addConversation,
  updateConversation
})

// 生命周期
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.kimi-sidebar {
  width: 280px;
  height: 100vh;
  background: #f8f9fa;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

:global(.dark) .kimi-sidebar {
  background: #1f2937;
  border-right-color: #374151;
}

/* 顶部新会话按钮 */
.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

:global(.dark) .sidebar-header {
  border-bottom-color: #374151;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #1677ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.new-chat-btn:hover {
  background: #1366d9;
  transform: translateY(-1px);
}

/* 会话列表 */
.conversation-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.list-header {
  padding: 16px 16px 8px;
  border-bottom: 1px solid #e5e7eb;
}

:global(.dark) .list-header {
  border-bottom-color: #374151;
}

.list-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  margin: 0;
  transition: color 0.3s ease;
}

:global(.dark) .list-header h3 {
  color: #9ca3af;
}

.conversations-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: transparent;
  border: 2px solid transparent;
}

.conversation-item:hover {
  background: #f3f4f6;
}

.conversation-item.active {
  background: #eff6ff;
  border-color: #1677ff;
}

:global(.dark) .conversation-item:hover {
  background: #374151;
}

:global(.dark) .conversation-item.active {
  background: #1e3a8a;
  border-color: #1677ff;
}

.conversation-content {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.3s ease;
}

:global(.dark) .conversation-title {
  color: #f9fafb;
}

.conversation-time {
  font-size: 12px;
  color: #9ca3af;
  transition: color 0.3s ease;
}

:global(.dark) .conversation-time {
  color: #6b7280;
}

.conversation-menu {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.conversation-item:hover .conversation-menu {
  opacity: 1;
}

.menu-trigger {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.menu-trigger:hover {
  background: #e5e7eb;
  color: #1677ff;
}

:global(.dark) .menu-trigger:hover {
  background: #4b5563;
}

/* 空状态 */
.empty-conversations {
  text-align: center;
  padding: 40px 20px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.empty-conversations p {
  font-size: 14px;
  margin: 0;
}

/* 个人资料部分 */
.profile-section {
  border-top: 1px solid #e5e7eb;
  background: white;
  transition: all 0.3s ease;
}

:global(.dark) .profile-section {
  border-top-color: #374151;
  background: #2d2d2d;
}

.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.profile-header:hover {
  background: #f9fafb;
}

:global(.dark) .profile-header:hover {
  background: #374151;
}

.profile-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s ease;
}

.profile-avatar:hover {
  transform: scale(1.05);
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar svg {
  color: #9ca3af;
}

.profile-text {
  flex: 1;
}

.profile-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  transition: color 0.3s ease;
}

:global(.dark) .profile-name {
  color: #f9fafb;
}

.profile-status {
  font-size: 12px;
  color: #9ca3af;
  transition: color 0.3s ease;
}

:global(.dark) .profile-status {
  color: #6b7280;
}

.expand-icon {
  color: #9ca3af;
  transition: all 0.2s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

/* 个人资料编辑 */
.profile-edit {
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
  transition: all 0.3s ease;
}

:global(.dark) .profile-edit {
  border-top-color: #374151;
  background: #374151;
}

.edit-field {
  margin-bottom: 12px;
}

.edit-field label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
  transition: color 0.3s ease;
}

:global(.dark) .edit-field label {
  color: #9ca3af;
}

.edit-input,
.edit-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  color: #1f2937;
  transition: all 0.2s ease;
}

.edit-input:focus,
.edit-textarea:focus {
  outline: none;
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}

:global(.dark) .edit-input,
:global(.dark) .edit-textarea {
  background: #4b5563;
  border-color: #6b7280;
  color: #f9fafb;
}

.edit-textarea {
  resize: vertical;
  min-height: 60px;
}

.edit-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.save-btn,
.cancel-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.save-btn {
  background: #1677ff;
  color: white;
}

.save-btn:hover {
  background: #1366d9;
}

.cancel-btn {
  background: #f3f4f6;
  color: #6b7280;
}

.cancel-btn:hover {
  background: #e5e7eb;
}

:global(.dark) .cancel-btn {
  background: #6b7280;
  color: #f9fafb;
}

:global(.dark) .cancel-btn:hover {
  background: #4b5563;
}

/* 退出登录区域 */
.logout-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

:global(.dark) .logout-section {
  border-top-color: #4b5563;
}

.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  padding: 10px 16px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background: #dc2626;
  transform: translateY(-1px);
}

.logout-btn:active {
  transform: translateY(0);
}

/* 重命名模态框 */
.rename-modal {
  background: white;
  border-radius: 12px;
  padding: 24px;
  max-width: 400px;
  width: 90vw;
  transition: all 0.3s ease;
}

:global(.dark) .rename-modal {
  background: #374151;
}

.rename-modal h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 16px 0;
  transition: color 0.3s ease;
}

:global(.dark) .rename-modal h3 {
  color: #f9fafb;
}

.rename-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  color: #1f2937;
  transition: all 0.2s ease;
}

.rename-input:focus {
  outline: none;
  border-color: #1677ff;
}

:global(.dark) .rename-input {
  background: #4b5563;
  border-color: #6b7280;
  color: #f9fafb;
}

.rename-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.confirm-btn {
  flex: 1;
  padding: 12px 24px;
  background: #1677ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.confirm-btn:hover {
  background: #1366d9;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .kimi-sidebar.mobile-hidden {
    transform: translateX(-100%);
  }
  
  .conversation-item {
    padding: 10px;
  }
  
  .profile-header {
    padding: 12px;
  }
  
  .profile-edit {
    padding: 12px;
  }
}

/* 滚动条样式 */
.conversations-scroll::-webkit-scrollbar {
  width: 4px;
}

.conversations-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.conversations-scroll::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}

.conversations-scroll::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

:global(.dark) .conversations-scroll::-webkit-scrollbar-thumb {
  background: #4b5563;
}

:global(.dark) .conversations-scroll::-webkit-scrollbar-thumb:hover {
  background: #6b7280;
}
</style>
