<template>
  <div class="chat-sidebar">
    <!-- 用户模块 -->
    <UserModule />
    
    <!-- 角色列表 -->
    <div class="character-section">
      <div class="section-header">
        <h3 class="section-title">角色列表</h3>
      </div>
      
      <div class="character-list">
        <div
          v-for="character in characters"
          :key="character.id"
          class="character-item"
          :class="{ active: character.id === currentCharacterId }"
          @click="selectCharacter(character.id)"
        >
          <div class="avatar-container">
            <n-avatar
              :size="48"
              :src="character.avatar"
              :fallback-src="'/src/assets/characters/default.svg'"
              class="character-avatar"
            />
            <div v-if="character.unread > 0" class="unread-badge">
              {{ character.unread > 99 ? '99+' : character.unread }}
            </div>
          </div>
          
          <div class="character-info">
            <div class="character-name">{{ character.name }}</div>
            <div class="character-desc">{{ character.description }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NAvatar } from 'naive-ui'
import { useChatStore } from '@/stores/chat'
import UserModule from './UserModule.vue'

const router = useRouter()
const chatStore = useChatStore()

const characters = computed(() => chatStore.characters)
const currentCharacterId = computed(() => chatStore.currentCharacterId)

const selectCharacter = (characterId: number) => {  // ✅ 使用 number 类型
  chatStore.setCurrentCharacter(characterId)
  router.push(`/chat/${characterId}`)  // ✅ 传递数字 ID
}
</script>

<style scoped>
.chat-sidebar {
  width: 280px;
  height: 100vh;
  background: var(--gray-50);
  border-right: 1px solid var(--gray-200);
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 16px;
}

.character-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  padding: 16px 16px 8px 16px;
  border-bottom: 1px solid var(--gray-200);
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--gray-900);
}

.character-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  
  /* 自定义滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}

.character-list::-webkit-scrollbar {
  width: 6px;
}

.character-list::-webkit-scrollbar-track {
  background: transparent;
}

.character-list::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 3px;
}

.character-list::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}

.character-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
  border: 2px solid transparent;
}

.character-item:hover {
  transform: scale(1.02);
  box-shadow: var(--shadow-base);
}

.character-item.active {
  border: 2px solid var(--primary-500);
  background: linear-gradient(135deg, var(--primary-50), white);
  box-shadow: var(--shadow-lg);
}

.avatar-container {
  position: relative;
  margin-right: var(--spacing-md);
}

.character-avatar {
  border-radius: 50% !important;
  transition: transform 0.2s ease;
}

.character-item:hover .character-avatar {
  transform: scale(1.05);
}

.character-item.active .character-avatar {
  border: 3px solid var(--primary-500);
  background: linear-gradient(135deg, var(--primary-400), var(--primary-600));
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff4757;
  color: white;
  border-radius: 50%;
  min-width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
  border: 2px solid white;
}

.character-info {
  flex: 1;
  min-width: 0;
}

.character-name {
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--gray-800);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.character-desc {
  font-size: var(--font-sm);
  color: var(--gray-500);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.character-item.active .character-name {
  color: var(--primary-600);
}

.character-item.active .character-desc {
  color: var(--primary-500);
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .chat-sidebar {
    width: 100%;
    max-width: 280px;
  }
}
</style>
