<template>
  <div class="chat-container">
    <!-- 移动端抽屉 -->
    <n-drawer
      v-model:show="showSidebar"
      :width="280"
      placement="left"
      class="mobile-drawer"
    >
      <ChatSidebar />
    </n-drawer>

    <!-- 桌面端侧边栏 -->
    <div class="desktop-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <ChatSidebar />
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 移动端顶部栏 -->
      <div class="mobile-header">
        <n-button
          text
          size="large"
          @click="showSidebar = true"
          class="menu-btn"
        >
          <template #icon>
            <svg viewBox="0 0 24 24" width="24" height="24">
              <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z" fill="currentColor"/>
            </svg>
          </template>
        </n-button>
        
        <div v-if="currentCharacter" class="current-character">
          <n-avatar
            :size="32"
            :src="currentCharacter.avatar"
            class="header-avatar"
          />
          <span class="header-name">{{ currentCharacter.name }}</span>
        </div>

        <!-- 操作按钮 -->
        <div class="header-actions">
          <n-dropdown :options="clearOptions" @select="handleClearAction">
            <n-button text size="small" class="clear-btn">
              <template #icon>
                <svg viewBox="0 0 24 24" width="20" height="20">
                  <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z" fill="currentColor"/>
                </svg>
              </template>
            </n-button>
          </n-dropdown>
        </div>
      </div>

      <!-- 桌面端操作栏 -->
      <div class="desktop-header">
        <!-- ✅优化 侧边栏收起按钮 -->
        <n-button
          text
          size="large"
          @click="toggleSidebar"
          class="sidebar-toggle-btn"
          :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        >
          <template #icon>
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path v-if="sidebarCollapsed" d="M3,6H21V8H3V6M3,11H21V13H3V11M3,16H21V18H3V16Z" fill="currentColor"/>
              <path v-else d="M3,6H13V8H3V6M3,11H13V13H3V11M3,16H13V18H3V16M16,6V18L20,12L16,6Z" fill="currentColor"/>
            </svg>
          </template>
        </n-button>

        <div v-if="currentCharacter" class="current-character-info">
          <n-avatar
            :size="40"
            :src="currentCharacter.avatar"
            class="character-avatar"
          />
          <div class="character-details">
            <h2 class="character-name">{{ currentCharacter.name }}</h2>
            <p class="character-description">{{ currentCharacter.description }}</p>
          </div>
        </div>

        <div class="desktop-actions">
          <n-dropdown :options="clearOptions" @select="handleClearAction">
            <n-button secondary size="small" class="clear-btn">
              <template #icon>
                <svg viewBox="0 0 24 24" width="16" height="16">
                  <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z" fill="currentColor"/>
                </svg>
              </template>
              清空记录
            </n-button>
          </n-dropdown>
        </div>
      </div>

      <!-- 消息列表区域 -->
      <div class="messages-container" ref="messagesContainer">
        <div class="messages-list">
          <div v-if="currentMessages.length === 0" class="empty-state">
            <div class="empty-icon">💬</div>
            <div class="empty-text">
              <h3>开始与{{ currentCharacter?.name || '角色' }}对话</h3>
              <p>发送消息开始你们的对话吧！</p>
            </div>
          </div>
          
          <ChatMessage
            v-for="message in currentMessages"
            :key="message.id"
            :message="message"
            :character="currentCharacter"
          />
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <ChatInputBar :current-character-id="currentCharacter?.id" />
      </div>
    </div>

    <!-- 登录弹窗 -->
    <LoginModal />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { NDrawer, NButton, NAvatar, NDropdown, useMessage, useDialog } from 'naive-ui'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import ChatSidebar from '@/components/ChatSidebar.vue'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatInputBar from '@/components/ChatInputBar.vue'
import LoginModal from '@/components/LoginModal.vue'

const route = useRoute()
const chatStore = useChatStore()
const authStore = useAuthStore()
const message = useMessage()
const dialog = useDialog()

// 接收路由参数
const props = defineProps<{
  characterId: number  // ✅ 使用 number 类型
}>()

// 响应式状态
const showSidebar = ref(false)
const messagesContainer = ref<HTMLElement>()
// ✅优化 桌面端侧边栏收起状态
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

// ✅优化 自动滚动逻辑已移至chatStore.appendToStream方法中
// 每收到流式内容都会自动滚动，无需在此处监听streamingId

// ✅优化 切换侧边栏收起状态
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  // 保存状态到 localStorage
  localStorage.setItem('sidebarCollapsed', String(sidebarCollapsed.value))
}

// 发送待处理消息
const sendPendingMessage = async () => {
  const pending = chatStore.pendingMessage
  if (!pending || !authStore.isLoggedIn) {
    return
  }
  
  console.log('[Chat] 发送待处理消息:', pending)
  
  try {
    // 清除待处理消息并触发输入框发送
    const content = pending.content
    chatStore.clearPendingMessage()
    
    // 模拟用户在输入框中输入并发送
    // 这里可以通过 ref 调用 ChatInputBar 的方法，或者直接在这里处理
    // 为了简化，我们直接在这里调用相同的发送逻辑
    
    // 滚动到底部
    await scrollToBottom()
    
  } catch (error) {
    console.error('[Chat] 发送待处理消息失败:', error)
  }
}

// 监听器 - 监听角色切换并加载历史记录
watch(() => props.characterId, async (newCharacterId, oldCharacterId) => {
  if (newCharacterId && newCharacterId !== oldCharacterId) {
    console.log('[Chat] 切换到角色:', newCharacterId, '从:', oldCharacterId)
    chatStore.setCurrentCharacter(newCharacterId)
    
    // 如果用户已登录，立即加载该角色的历史记录
    if (authStore.isLoggedIn) {
      try {
        console.log('[Chat] 开始加载角色历史记录:', newCharacterId)
        const historyResponse = await chatStore.loadMessages(newCharacterId)
        console.log('[Chat] 角色历史记录加载完成:', historyResponse)
        
        // 滚动到底部
        nextTick(() => {
          scrollToBottom()
        })
      } catch (error) {
        console.error('[Chat] 加载角色历史记录失败:', error)
      }
    }
  }
}, { immediate: true })

// 监听消息变化，自动滚动到底部
watch(() => chatStore.messageList.length, () => {
  scrollToBottom()
})

// 监听登录状态变化，处理待发送消息
watch(() => authStore.loginModalVisible, (visible) => {
  if (!visible && authStore.isLoggedIn && chatStore.pendingMessage) {
    console.log('[Chat] 登录成功，准备发送待处理消息')
    setTimeout(() => {
      sendPendingMessage()
    }, 500) // 稍微延迟一下，确保UI更新完成
  }
})

// 清空操作选项
const clearOptions = [
  {
    label: '清空当前角色记录',
    key: 'current',
    icon: () => '🗑️'
  },
  {
    label: '清空所有记录',
    key: 'all',
    icon: () => '🚮'
  }
]

// 处理清空操作
const handleClearAction = (key: string) => {
  if (!authStore.isLoggedIn) {
    message.warning('请先登录后再进行清空操作')
    return
  }

  if (key === 'current') {
    handleClearCurrentCharacter()
  } else if (key === 'all') {
    handleClearAllChats()
  }
}

// 清空当前角色记录
const handleClearCurrentCharacter = () => {
  if (!currentCharacter.value) {
    message.warning('请先选择一个角色')
    return
  }

  dialog.warning({
    title: '确认清空',
    content: `确定要清空与 ${currentCharacter.value.name} 的所有聊天记录吗？此操作不可撤销。`,
    positiveText: '确定清空',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await chatStore.clearCurrentCharacterMessages(currentCharacter.value!.id)
        message.success(`已清空与 ${currentCharacter.value!.name} 的聊天记录`)
      } catch (error) {
        console.error('[Chat] 清空当前角色记录失败:', error)
        message.error('清空失败，请重试')
      }
    }
  })
}

// 清空所有聊天记录
const handleClearAllChats = () => {
  dialog.warning({
    title: '确认清空',
    content: '确定要清空所有角色的聊天记录吗？此操作不可撤销。',
    positiveText: '确定清空',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await chatStore.clearAllMessages()
        message.success('已清空所有聊天记录')
      } catch (error) {
        console.error('[Chat] 清空所有记录失败:', error)
        message.error('清空失败，请重试')
      }
    }
  })
}

// 监听屏幕尺寸变化
const handleResize = () => {
  if (window.innerWidth > 768) {
    showSidebar.value = false
  }
}

onMounted(async () => {
  console.log('[Chat] Chat组件开始挂载')
  console.log('[Chat] Props:', props)
  console.log('[Chat] Route params:', route.params)
  
  // ✅优化 恢复侧边栏收起状态
  const savedCollapsed = localStorage.getItem('sidebarCollapsed')
  if (savedCollapsed !== null) {
    sidebarCollapsed.value = savedCollapsed === 'true'
  }
  
  // 设置当前角色（历史记录加载由watch处理）
  let characterId: number | null = null
  if (props.characterId) {
    console.log('[Chat] 设置当前角色:', props.characterId)
    characterId = props.characterId
    // 不需要手动设置，watch会处理
  } else if (route.params.characterId) {
    console.log('[Chat] 从路由参数设置角色:', route.params.characterId)
    characterId = Number(route.params.characterId)
    chatStore.setCurrentCharacter(characterId)
  }
  
  console.log('[Chat] 当前角色:', chatStore.currentCharacter)
  console.log('[Chat] 当前消息列表:', chatStore.currentMessages)
  
  // 历史记录加载现在由watch处理，这里不再重复加载
  
  // 监听屏幕尺寸变化
  window.addEventListener('resize', handleResize)
  
  // 初始滚动到底部
  scrollToBottom()
  
  console.log('[Chat] Chat组件已挂载完成, characterId:', characterId)
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  background: var(--gray-50, #f9fafb);
}

/* 桌面端侧边栏 */
.desktop-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--gray-200, #e5e7eb);
  background: white;
  transition: all 0.3s ease;
  overflow: hidden;
}

/* ✅优化 侧边栏收起状态 */
.desktop-sidebar.collapsed {
  width: 0;
  border-right: none;
}

/* ✅优化 收起时隐藏内部内容 */
.desktop-sidebar.collapsed .chat-sidebar {
  display: none;
}

/* 移动端隐藏桌面侧边栏 */
@media (max-width: 768px) {
  .desktop-sidebar {
    display: none;
  }
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 移动端顶部栏 */
.mobile-header {
  display: none;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--gray-200, #e5e7eb);
  background: white;
  gap: 12px;
}

@media (max-width: 768px) {
  .mobile-header {
    display: flex;
  }
}

/* 桌面端操作栏 */
.desktop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--gray-200, #e5e7eb);
  background: white;
  gap: 16px;
}

/* ✅优化 侧边栏切换按钮样式 */
.sidebar-toggle-btn {
  color: var(--gray-600) !important;
  transition: all 0.2s ease;
}

.sidebar-toggle-btn:hover {
  color: var(--primary-500) !important;
  background: var(--primary-50) !important;
}

@media (max-width: 768px) {
  .desktop-header {
    display: none;
  }
}

.current-character-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.character-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.character-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--gray-900, #111827);
}

.character-description {
  margin: 0;
  font-size: 14px;
  color: var(--gray-500, #6b7280);
}

.header-actions,
.desktop-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-btn {
  color: var(--gray-600, #6b7280);
}

.current-character {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-avatar {
  border: 2px solid var(--primary-500, #1677ff);
}

.header-name {
  font-weight: 500;
  color: var(--gray-900, #111827);
}

/* 消息列表区域 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.messages-list {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 16px;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--gray-500, #6b7280);
  gap: 16px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.5;
}

.empty-text h3 {
  font-size: 18px;
  font-weight: 500;
  color: var(--gray-700, #374151);
  margin-bottom: 8px;
}

.empty-text p {
  font-size: 14px;
  color: var(--gray-500, #6b7280);
}

/* 输入区域 */
.input-area {
  border-top: 1px solid var(--gray-200, #e5e7eb);
  background: var(--gray-50, #f9fafb);
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

@media (max-width: 768px) {
  .input-area {
    padding: 16px 20px;
    gap: 16px;
  }
}

/* 语音通话区域 */
.voice-call-section {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 0;
}

/* 移动端抽屉 */
.mobile-drawer :deep(.n-drawer-body) {
  padding: 0;
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: var(--gray-100, #f3f4f6);
}

.messages-container::-webkit-scrollbar-thumb {
  background: var(--gray-300, #d1d5db);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: var(--gray-400, #9ca3af);
}
</style>