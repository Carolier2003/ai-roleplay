<template>
  <div class="flex h-screen bg-gray-50 overflow-hidden">
    <!-- 移动端遮罩层 -->
    <div 
      v-if="showSidebar" 
      class="fixed inset-0 bg-black/50 z-40 md:hidden"
      @click="showSidebar = false"
    ></div>

    <!-- 侧边栏 (移动端抽屉 + 桌面端侧边栏) -->
    <div 
      class="fixed inset-y-0 left-0 z-50 w-[280px] bg-white border-r border-gray-200 transform transition-transform duration-300 ease-in-out md:relative md:translate-x-0"
      :class="[
        showSidebar ? 'translate-x-0' : '-translate-x-full',
        sidebarCollapsed ? 'md:w-0 md:border-none md:overflow-hidden' : 'md:w-[280px]'
      ]"
    >
      <ChatSidebar />
    </div>

    <!-- 聊天区域 -->
    <div class="flex-1 flex flex-col min-w-0 bg-transparent relative">
      <!-- 移动端顶部栏 -->
      <div class="md:hidden flex items-center justify-between p-3 bg-white border-b border-gray-200">
        <button 
          @click="showSidebar = true"
          class="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <svg viewBox="0 0 24 24" width="24" height="24">
            <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z" fill="currentColor"/>
          </svg>
        </button>
        
        <div v-if="currentCharacter" class="flex items-center gap-2">
          <img 
            :src="currentCharacter.avatar" 
            class="w-8 h-8 rounded-full object-cover border border-gray-200"
            alt="Avatar"
          />
          <span class="font-medium text-gray-900">{{ currentCharacter.name }}</span>
        </div>

        <!-- 移动端操作按钮 -->
        <div class="relative group">
          <button class="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z" fill="currentColor"/>
            </svg>
          </button>
          <!-- 简单的下拉菜单实现 -->
          <div class="absolute right-0 top-full mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-100 py-1 hidden group-hover:block z-10">
            <button @click="handleClearCurrentCharacter" class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 flex items-center gap-2">
              <span>🗑️</span> 清空当前记录
            </button>
            <button @click="handleClearAllChats" class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 flex items-center gap-2">
              <span>🚮</span> 清空所有记录
            </button>
          </div>
        </div>
      </div>

      <!-- 桌面端操作栏 -->
      <div class="hidden md:flex items-center justify-between px-6 py-4 bg-white border-b border-gray-200">
        <div class="flex items-center gap-4">
          <button 
            @click="toggleSidebar"
            class="p-2 text-gray-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-all"
            :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
          >
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path v-if="sidebarCollapsed" d="M3,6H21V8H3V6M3,11H21V13H3V11M3,16H21V18H3V16Z" fill="currentColor"/>
              <path v-else d="M3,6H13V8H3V6M3,11H13V13H3V11M3,16H13V18H3V16M16,6V18L20,12L16,6Z" fill="currentColor"/>
            </svg>
          </button>

          <div v-if="currentCharacter" class="flex items-center gap-3">
            <img 
              :src="currentCharacter.avatar" 
              class="w-10 h-10 rounded-full object-cover border border-gray-200 shadow-sm"
              alt="Avatar"
            />
            <div>
              <h2 class="text-lg font-semibold text-gray-900 leading-tight">{{ currentCharacter.name }}</h2>
              <p class="text-sm text-gray-500 leading-tight">{{ currentCharacter.description }}</p>
            </div>
          </div>
        </div>

        <div class="relative group">
          <button class="flex items-center gap-1 px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z" fill="currentColor"/>
            </svg>
            清空记录
          </button>
          <!-- 下拉菜单 - 使用 padding 桥接间隙，防止鼠标移动时菜单消失 -->
          <div class="absolute right-0 top-full pt-2 w-48 hidden group-hover:block z-10">
            <div class="bg-white rounded-lg shadow-xl border border-gray-100 py-1">
              <button @click="handleClearCurrentCharacter" class="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-red-600 flex items-center gap-2 transition-colors">
                <span>🗑️</span> 清空当前角色记录
              </button>
              <button @click="handleClearAllChats" class="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-red-600 flex items-center gap-2 transition-colors">
                <span>🚮</span> 清空所有记录
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 消息列表区域 -->
      <div class="flex-1 overflow-y-auto p-4 md:p-6 space-y-6 scroll-smooth messages-container" ref="messagesContainer">
        <div v-if="currentMessages.length === 0" class="h-full flex flex-col items-center justify-center text-gray-400 space-y-4">
          <div class="text-6xl opacity-50">💬</div>
          <div class="text-center">
            <h3 class="text-lg font-medium text-gray-700 mb-2">开始与{{ currentCharacter?.name || '角色' }}对话</h3>
            <p class="text-sm text-gray-500">发送消息开始你们的对话吧！</p>
          </div>
        </div>
        
        <ChatMessage
          v-for="message in currentMessages"
          :key="message.id"
          :message="message"
          :character="currentCharacter"
        />
      </div>

      <!-- 输入区域 -->
      <div class="border-t border-gray-200 bg-gray-50/50 p-4 md:p-6">
        <div class="max-w-4xl mx-auto">
          <ChatInputBar :current-character-id="currentCharacter?.id" />
        </div>
      </div>
    </div>

    <!-- 登录弹窗 -->
    <LoginModal />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import ChatSidebar from '@/components/ChatSidebar.vue'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatInputBar from '@/components/ChatInputBar.vue'
import LoginModal from '@/components/LoginModal.vue'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const route = useRoute()
const chatStore = useChatStore()
const authStore = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

// 接收路由参数
const props = defineProps<{
  characterId: number
}>()

// 响应式状态
const showSidebar = ref(false)
const messagesContainer = ref<HTMLElement>()
const sidebarCollapsed = ref(false)

// 计算属性
const currentCharacter = computed(() => chatStore.currentCharacter)
const currentMessages = computed(() => chatStore.currentMessages)

// 方法
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebarCollapsed', String(sidebarCollapsed.value))
}

const sendPendingMessage = async () => {
  const pending = chatStore.pendingMessage
  if (!pending || !authStore.isLoggedIn) {
    return
  }
  
  try {
    chatStore.clearPendingMessage()
    await scrollToBottom()
  } catch (error) {
    console.error('[Chat] 发送待处理消息失败:', error)
  }
}

// 监听器
watch(() => props.characterId, async (newCharacterId, oldCharacterId) => {
  if (newCharacterId && newCharacterId !== oldCharacterId) {
    chatStore.setCurrentCharacter(newCharacterId)
    
    if (authStore.isLoggedIn) {
      try {
        await chatStore.loadMessages(newCharacterId)
        nextTick(() => {
          scrollToBottom()
        })
      } catch (error) {
        console.error('[Chat] 加载角色历史记录失败:', error)
      }
    }
  }
}, { immediate: true })

watch(() => chatStore.messageList.length, () => {
  scrollToBottom()
})

watch(() => authStore.loginModalVisible, (visible) => {
  if (!visible && authStore.isLoggedIn && chatStore.pendingMessage) {
    setTimeout(() => {
      sendPendingMessage()
    }, 500)
  }
})

// 清空操作
const handleClearCurrentCharacter = async () => {
  if (!authStore.isLoggedIn) {
    toast.warning('请先登录后再进行清空操作')
    return
  }
  if (!currentCharacter.value) return

  const confirmed = await confirm.danger(
    `确定要清空与 ${currentCharacter.value.name} 的所有聊天记录吗？此操作不可撤销。`,
    '清空当前角色记录'
  )

  if (confirmed) {
    try {
      await chatStore.clearCurrentCharacterMessages(currentCharacter.value.id)
      toast.success('记录已清空')
    } catch (error) {
      console.error('[Chat] 清空当前角色记录失败:', error)
      toast.error('清空失败，请重试')
    }
  }
}

const handleClearAllChats = async () => {
  if (!authStore.isLoggedIn) {
    toast.warning('请先登录后再进行清空操作')
    return
  }

  const confirmed = await confirm.danger(
    '确定要清空所有角色的聊天记录吗？此操作不可撤销。',
    '清空所有记录'
  )

  if (confirmed) {
    try {
      await chatStore.clearAllMessages()
      toast.success('所有记录已清空')
    } catch (error) {
      console.error('[Chat] 清空所有记录失败:', error)
      toast.error('清空失败，请重试')
    }
  }
}

const handleResize = () => {
  if (window.innerWidth > 768) {
    showSidebar.value = false
  }
}

onMounted(async () => {
  const savedCollapsed = localStorage.getItem('sidebarCollapsed')
  if (savedCollapsed !== null) {
    sidebarCollapsed.value = savedCollapsed === 'true'
  }
  
  let characterId: number | null = null
  if (props.characterId) {
    characterId = props.characterId
  } else if (route.params.characterId) {
    characterId = Number(route.params.characterId)
    chatStore.setCurrentCharacter(characterId)
  }
  
  window.addEventListener('resize', handleResize)
  scrollToBottom()
})
</script>

<style scoped>
/* 自定义滚动条 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  @apply bg-transparent;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400;
}
</style>