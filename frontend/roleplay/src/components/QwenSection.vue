<template>
  <div class="qwen-section">
    <!-- Section Header -->
    <div class="section-header">
      <div class="header-content">
        <span class="header-icon">✨</span>
        <h2 class="header-title">Qwen Assistant</h2>
      </div>
      <button 
        class="header-action-btn"
        @click="handleNewConversation"
        :disabled="loading"
        title="新对话"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14"/>
        </svg>
      </button>
    </div>

    <!-- Conversation List -->
    <div class="conversation-list">
      <div 
        v-for="conv in chatStore.qwenConversations"
        :key="conv.conversationId"
        class="conversation-card"
        :class="{ 'active': isActive(conv.conversationId) }"
        @click="switchConversation(conv.conversationId)"
      >
        <div class="card-header">
          <div class="icon-wrapper">
            <img :src="qwenCharacter?.avatar" alt="Qwen" class="character-avatar" />
          </div>
          <div class="card-content">
            <div class="title-row">
              <div v-if="editingId === conv.conversationId" class="edit-title-wrapper" @click.stop>
                <input 
                  ref="editInput"
                  v-model="editTitle"
                  class="edit-title-input"
                  @keyup.enter="saveTitle(conv)"
                  @blur="saveTitle(conv)"
                  @keyup.esc="cancelEdit"
                />
              </div>
              <div v-else class="conversation-title" :title="conv.title">{{ conv.title }}</div>
              
              <div class="conversation-time">{{ formatTime(conv.lastActiveTime) }}</div>
            </div>
            <div class="conversation-preview">{{ conv.lastMessage || '暂无消息' }}</div>
          </div>
        </div>
        
        <!-- Actions (visible on hover or active) -->
        <div class="card-actions" @click.stop>
          <button class="action-btn edit-btn" @click="startEdit(conv)" title="重命名">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
          </button>
          <button class="action-btn delete-btn" @click="confirmDelete(conv)" title="删除">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>
      </div>
      
      <!-- Empty State -->
      <div v-if="chatStore.qwenConversations.length === 0" class="empty-state">
        暂无对话记录
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showDeleteModal" class="modal-overlay" @click="cancelDelete">
          <div class="modal-container" @click.stop>
            <div class="modal-header">
              <div class="modal-icon-wrapper">
                <svg class="modal-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                  <line x1="12" y1="9" x2="12" y2="13"></line>
                  <line x1="12" y1="17" x2="12.01" y2="17"></line>
                </svg>
              </div>
              <h3 class="modal-title">确认删除</h3>
            </div>
            <div class="modal-body">
              <p class="modal-message">
                确定要删除对话 <strong>"{{ deleteTarget?.title }}"</strong> 吗？
              </p>
              <p class="modal-warning">此操作不可恢复</p>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel-btn" @click="cancelDelete">
                取消
              </button>
              <button class="modal-btn confirm-btn" @click="deleteConversation">
                删除
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'
import type { QwenConversationInfo } from '@/api/qwen'

const chatStore = useChatStore()
const router = useRouter()
const toast = useToast()
const loading = ref(false)
const editingId = ref<string | null>(null)
const editTitle = ref('')
const editInput = ref<HTMLInputElement | null>(null)
const showDeleteModal = ref(false)
const deleteTarget = ref<QwenConversationInfo | null>(null)

const qwenCharacter = computed(() => 
  chatStore.characters.find(c => c.id === 0)
)

const isActive = (conversationId: string) => {
  return chatStore.currentCharacterId === 0 && chatStore.currentQwenConversationId === conversationId
}

// Format time: "12:30", "Yesterday", "11/20"
const formatTime = (timestamp: number) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const isThisYear = date.getFullYear() === now.getFullYear()
  
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (isThisYear) {
    return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
  } else {
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'numeric', day: 'numeric' })
  }
}

const handleNewConversation = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const newId = await chatStore.createQwenConversation()
    // Switch to Qwen mode if not already
    if (chatStore.currentCharacterId !== 0) {
      router.push('/chat/0')
    }
    // Scroll to top or highlight new item
  } catch (error) {
    toast.error('创建对话失败')
  } finally {
    loading.value = false
  }
}

const switchConversation = async (conversationId: string) => {
  // If already active, do nothing
  if (isActive(conversationId)) return
  
  // Switch to Qwen mode if not already
  if (chatStore.currentCharacterId !== 0) {
    // Set current Qwen conversation ID first
    chatStore.switchQwenConversation(conversationId)
    router.push('/chat/0')
  } else {
    // Just switch conversation
    await chatStore.switchQwenConversation(conversationId)
  }
}

const startEdit = (conv: QwenConversationInfo) => {
  editingId.value = conv.conversationId
  editTitle.value = conv.title
  nextTick(() => {
    // Focus input
    const input = document.querySelector('.edit-title-input') as HTMLInputElement
    if (input) input.focus()
  })
}

const saveTitle = async (conv: QwenConversationInfo) => {
  if (!editingId.value) return
  
  const newTitle = editTitle.value.trim()
  if (newTitle && newTitle !== conv.title) {
    try {
      await chatStore.renameQwenConversation(conv.conversationId, newTitle)
      toast.success('重命名成功')
    } catch (error) {
      toast.error('重命名失败')
    }
  }
  editingId.value = null
}

const cancelEdit = () => {
  editingId.value = null
}

const confirmDelete = (conv: QwenConversationInfo) => {
  deleteTarget.value = conv
  showDeleteModal.value = true
}

const cancelDelete = () => {
  showDeleteModal.value = false
  deleteTarget.value = null
}

const deleteConversation = async () => {
  if (!deleteTarget.value) return
  
  try {
    await chatStore.deleteQwenConversation(deleteTarget.value.conversationId)
    toast.success('删除成功')
    showDeleteModal.value = false
    deleteTarget.value = null
  } catch (error) {
    toast.error('删除失败')
  }
}

onMounted(() => {
  // Load conversations if empty
  if (chatStore.qwenConversations.length === 0) {
    chatStore.loadQwenConversations()
  }
})
</script>

<style scoped>
.qwen-section {
  padding: 16px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  max-height: 400px; /* Limit height */
}

.section-header {
  margin-bottom: 12px;
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 20px;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: white;
  margin: 0;
  letter-spacing: 0.5px;
}

.header-action-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.header-action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.header-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.header-action-btn svg {
  width: 18px;
  height: 18px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px; /* Space for scrollbar */
}

/* Custom Scrollbar */
.conversation-list::-webkit-scrollbar {
  width: 4px;
}
.conversation-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}
.conversation-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.conversation-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;
  position: relative;
  border-left: 3px solid transparent;
}

.conversation-card:hover {
  background: white;
  transform: translateX(2px);
}

.conversation-card.active {
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-left-color: #667eea;
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.icon-wrapper {
  flex-shrink: 0;
}

.character-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
}

.card-content {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.conversation-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}

.edit-title-wrapper {
  flex: 1;
  margin-right: 8px;
}

.edit-title-input {
  width: 100%;
  padding: 2px 4px;
  font-size: 13px;
  border: 1px solid #667eea;
  border-radius: 4px;
  outline: none;
}

.conversation-time {
  font-size: 10px;
  color: #9ca3af;
  flex-shrink: 0;
}

.conversation-preview {
  font-size: 11px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  height: 16px; /* Fixed height for one line */
}

.card-actions {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 2px;
}

.conversation-card:hover .card-actions {
  opacity: 1;
}

.action-btn {
  padding: 4px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background: #f3f4f6;
  color: #4b5563;
}

.action-btn.delete-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.empty-state {
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  padding: 20px 0;
}

/* Delete Confirmation Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.modal-container {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  max-width: 400px;
  width: 100%;
  overflow: hidden;
  animation: modalSlideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  padding: 24px 24px 16px;
  text-align: center;
}

.modal-icon-wrapper {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-icon {
  width: 28px;
  height: 28px;
  color: #f59e0b;
  stroke-width: 2.5;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.modal-body {
  padding: 0 24px 24px;
  text-align: center;
}

.modal-message {
  font-size: 15px;
  color: #4b5563;
  margin: 0 0 8px;
  line-height: 1.5;
}

.modal-message strong {
  color: #111827;
  font-weight: 600;
}

.modal-warning {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.modal-footer {
  padding: 16px 24px 24px;
  display: flex;
  gap: 12px;
}

.modal-btn {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #f3f4f6;
  color: #6b7280;
}

.cancel-btn:hover {
  background: #e5e7eb;
  color: #4b5563;
}

.confirm-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  box-shadow: 0 4px 6px -1px rgba(239, 68, 68, 0.3);
}

.confirm-btn:hover {
  box-shadow: 0 6px 8px -1px rgba(239, 68, 68, 0.4);
  transform: translateY(-1px);
}

.confirm-btn:active {
  transform: translateY(0);
}

/* Modal Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: translateY(-20px) scale(0.95);
}
</style>
