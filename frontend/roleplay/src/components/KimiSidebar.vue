<template>
  <div 
    class="w-[280px] h-screen bg-gray-50 dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 flex flex-col transition-all duration-300"
    :class="{ 'hidden md:flex': !showMobile }"
  >
    <!-- 顶部：新会话按钮 -->
    <div class="p-4 border-b border-gray-200 dark:border-gray-700">
      <button 
        @click="handleNewConversation" 
        class="w-full flex items-center gap-3 px-4 py-3 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 hover:-translate-y-px transition-all shadow-sm"
      >
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z" fill="currentColor"/>
        </svg>
        <span>新会话</span>
      </button>
    </div>

    <!-- 中间：历史会话列表 -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-200 dark:border-gray-700">
        <h3 class="text-sm font-semibold text-gray-500 dark:text-gray-400 m-0">历史记录</h3>
      </div>
      
      <div class="flex-1 overflow-y-auto p-2 space-y-1">
        <div
          v-for="conversation in conversations"
          :key="conversation.id"
          class="flex items-center p-3 rounded-lg cursor-pointer transition-all duration-200 group relative"
          :class="[
            conversation.id === activeConversationId 
              ? 'bg-blue-50 dark:bg-blue-900/20 border-2 border-blue-500' 
              : 'hover:bg-gray-100 dark:hover:bg-gray-700 border-2 border-transparent'
          ]"
          @click="handleSelectConversation(conversation.id)"
        >
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200 mb-1 truncate">{{ conversation.title }}</div>
            <div class="text-xs text-gray-400 dark:text-gray-500">{{ formatTime(conversation.lastMessageTime) }}</div>
          </div>
          
          <!-- 三点菜单 -->
          <div class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 relative">
            <button 
              class="w-6 h-6 flex items-center justify-center rounded hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-400 hover:text-blue-600 dark:hover:text-blue-400"
              @click.stop="toggleConversationMenu(conversation.id)"
            >
              <svg viewBox="0 0 24 24" width="16" height="16">
                <path d="M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z" fill="currentColor"/>
              </svg>
            </button>
            
            <!-- Dropdown Menu -->
            <div v-if="activeMenuId === conversation.id" class="absolute right-0 top-full mt-1 w-32 bg-white dark:bg-gray-800 rounded-md shadow-lg border border-gray-100 dark:border-gray-700 py-1 z-10 overflow-hidden">
              <button 
                @click.stop="handleConversationAction('rename', conversation)"
                class="w-full px-4 py-2 text-left text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z" />
                </svg>
                重命名
              </button>
              <button 
                @click.stop="handleConversationAction('delete', conversation)"
                class="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center gap-2"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z" />
                </svg>
                删除
              </button>
            </div>
          </div>
        </div>
        
        <!-- 点击外部关闭菜单遮罩 -->
        <div v-if="activeMenuId" class="fixed inset-0 z-0" @click="activeMenuId = null"></div>
        
        <!-- 空状态 -->
        <div v-if="conversations.length === 0" class="text-center py-10 text-gray-400">
          <div class="text-3xl mb-3 opacity-60">💬</div>
          <p class="text-sm m-0">暂无历史记录</p>
        </div>
      </div>
    </div>

    <!-- 底部：我的面板 -->
    <div class="border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 transition-all duration-300">
      <div class="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors" @click="toggleProfile">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-full bg-gray-200 dark:bg-gray-600 flex items-center justify-center overflow-hidden transition-transform hover:scale-105" @click.stop="handleAvatarClick">
            <img v-if="profile.avatar" :src="profile.avatar" class="w-full h-full object-cover" alt="头像" />
            <svg v-else viewBox="0 0 24 24" width="24" height="24" class="text-gray-400">
              <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z" fill="currentColor"/>
            </svg>
          </div>
          <div class="flex-1">
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200">{{ profile.nickname || '用户' }}</div>
            <div class="text-xs text-gray-400 dark:text-gray-500">我的</div>
          </div>
        </div>
        <svg class="text-gray-400 transition-transform duration-200" :class="{ 'rotate-180': showProfile }" viewBox="0 0 24 24" width="16" height="16">
          <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z" fill="currentColor"/>
        </svg>
      </div>
      
      <!-- 展开的个人资料编辑 -->
      <div v-if="showProfile" class="p-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 transition-all duration-300">
        <div class="mb-3">
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">昵称</label>
          <input
            v-model="editProfile.nickname"
            type="text"
            placeholder="输入昵称"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-800 dark:text-gray-200 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all"
          />
        </div>
        
        <div class="mb-3">
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">邮箱</label>
          <input
            v-model="editProfile.email"
            type="email"
            placeholder="输入邮箱"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-800 dark:text-gray-200 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all"
          />
        </div>
        
        <div class="mb-3">
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">个人简介</label>
          <textarea
            v-model="editProfile.bio"
            placeholder="介绍一下自己"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-800 dark:text-gray-200 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all min-h-[60px] resize-y"
            rows="2"
          ></textarea>
        </div>
        
        <div class="flex gap-2 mt-4">
          <button @click="handleSaveProfile" class="flex-1 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700 transition-colors">保存</button>
          <button @click="handleCancelEdit" class="flex-1 py-2 bg-gray-100 dark:bg-gray-600 text-gray-600 dark:text-gray-200 rounded-md text-sm font-medium hover:bg-gray-200 dark:hover:bg-gray-500 transition-colors">取消</button>
        </div>
        
        <!-- 退出登录按钮 -->
        <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
          <button @click="handleLogout" class="w-full flex items-center justify-center gap-2 py-2.5 bg-red-500 text-white rounded-md text-sm font-medium hover:bg-red-600 transition-colors">
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
      class="hidden"
    />

    <!-- 重命名对话框 -->
    <Teleport to="body">
      <div v-if="showRenameModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4" @click.self="showRenameModal = false">
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-sm p-6 animate-scale-in">
          <h3 class="text-lg font-bold text-gray-900 dark:text-white mb-4">重命名会话</h3>
          <input
            v-model="renameTitle"
            type="text"
            placeholder="输入新标题"
            class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg mb-6 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 dark:bg-gray-700 dark:text-white"
            @keydown.enter="handleConfirmRename"
            ref="renameInputRef"
          />
          <div class="flex gap-3">
            <button @click="handleConfirmRename" class="flex-1 py-2.5 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors">确认</button>
            <button @click="showRenameModal = false" class="flex-1 py-2.5 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-200 rounded-lg font-medium hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">取消</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, h, nextTick } from 'vue'
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

import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const emit = defineEmits<Emits>()
const authStore = useAuthStore()
const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

// Simple replacement for useMessage
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
const activeMenuId = ref<string | null>(null)
const renameInputRef = ref<HTMLInputElement>()

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

const toggleConversationMenu = (id: string) => {
  activeMenuId.value = activeMenuId.value === id ? null : id
}

const handleConversationAction = (action: string, conversation: Conversation) => {
  activeMenuId.value = null
  switch (action) {
    case 'rename':
      renamingConversation.value = conversation
      renameTitle.value = conversation.title
      showRenameModal.value = true
      nextTick(() => {
        renameInputRef.value?.focus()
      })
      break
    case 'delete':
      handleDeleteConversation(conversation.id)
      break
  }
}

const handleDeleteConversation = async (conversationId: string) => {
  const confirmed = await confirm.warning('确定要删除这个会话吗？')
  if (confirmed) {
    conversations.value = conversations.value.filter(c => c.id !== conversationId)
    saveData()
    message.success('会话已删除')
  }
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
@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.animate-scale-in {
  animation: scaleIn 0.2s ease-out;
}
</style>
