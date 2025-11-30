<template>
  <div class="companions-section">
    <!-- Section Header -->
    <div class="section-header">
      <div class="header-content">
        <span class="header-icon">✨</span>
        <h2 class="header-title">AI COMPANIONS</h2>
      </div>
    </div>

    <!-- Character List -->
    <div class="character-list">
      <div
        v-for="character in companions"
        :key="character.id"
        class="character-card"
        :class="{ 'active': isActive(character.id) }"
        @click="handleCharacterClick(character.id)"
      >
        <div class="character-avatar-wrapper">
          <img :src="character.avatar" :alt="character.name" class="character-avatar" />
          <span v-if="character.status === 1" class="online-indicator"></span>
        </div>
        <div class="character-info">
          <div class="character-name">{{ character.name }}</div>
          <div class="character-description">{{ character.description }}</div>
        </div>
        <span v-if="character.unread > 0" class="unread-count">{{ character.unread }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useChatStore, type Character } from '@/stores/chat'
import { useRouter } from 'vue-router'

const chatStore = useChatStore()
const router = useRouter()

// Filter out Qwen (ID=0) and only show roleplay companions
const companions = computed(() => 
  chatStore.characters.filter(c => c.id !== 0)
)

const isActive = (characterId: number) => {
  return chatStore.currentCharacterId === characterId
}

const handleCharacterClick = (characterId: number) => {
  if (chatStore.currentCharacterId !== characterId) {
    router.push(`/chat/${characterId}`)
  }
}
</script>

<style scoped>
.companions-section {
  padding: 16px 12px;
}

.section-header {
  margin-bottom: 16px;
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
  color: #1f2937;
  margin: 0;
  letter-spacing: 0.5px;
}

.character-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.character-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.character-card:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.character-card.active {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #7dd3fc;
  box-shadow: 0 4px 12px rgba(125, 211, 252, 0.2);
}

.character-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.character-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  object-fit: cover;
  border: 2px solid #e5e7eb;
}

.character-card.active .character-avatar {
  border-color: #7dd3fc;
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  background: #10b981;
  border: 2px solid white;
  border-radius: 50%;
}

.character-info {
  flex: 1;
  min-width: 0;
}

.character-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.character-description {
  font-size: 12px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-count {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}
</style>
