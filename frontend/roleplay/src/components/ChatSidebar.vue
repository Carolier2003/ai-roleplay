<template>
  <div class="w-full h-full bg-gray-50 flex flex-col p-4 gap-4 border-r border-gray-200">
    <!-- 用户模块 -->
    <UserModule />
    
    <!-- 角色列表 -->
    <div class="flex-1 flex flex-col min-h-0 bg-white rounded-xl shadow-sm overflow-hidden">
      <div class="p-4 border-b border-gray-100">
        <h3 class="text-base font-semibold text-gray-900 m-0">✨ AI 角色列表</h3>
      </div>
      
      <div class="flex-1 overflow-y-auto p-2 space-y-2 scroll-smooth custom-scrollbar">
        <div
          v-for="character in characters"
          :key="character.id"
          class="flex items-center p-3 rounded-lg cursor-pointer transition-all duration-200 border-2 border-transparent hover:shadow-sm hover:scale-[1.02]"
          :class="[
            character.id === currentCharacterId 
              ? 'border-indigo-500 bg-gradient-to-br from-indigo-50 to-white shadow-md' 
              : 'bg-white hover:bg-gray-50'
          ]"
          @click="selectCharacter(character.id)"
        >
          <div class="relative mr-3">
            <img
              :src="character.avatar"
              class="w-12 h-12 rounded-full object-cover transition-transform duration-200"
              :class="{ 'ring-2 ring-indigo-500 ring-offset-2': character.id === currentCharacterId }"
              alt="Avatar"
            />
            <div v-if="character.unread > 0" class="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[18px] h-[18px] flex items-center justify-center border-2 border-white shadow-sm">
              {{ character.unread > 99 ? '99+' : character.unread }}
            </div>
          </div>
          
          <div class="flex-1 min-w-0">
            <div class="text-sm font-semibold text-gray-800 mb-0.5 truncate" :class="{ 'text-indigo-600': character.id === currentCharacterId }">
              {{ character.name }}
            </div>
            <div class="text-xs text-gray-500 truncate" :class="{ 'text-indigo-500': character.id === currentCharacterId }">
              {{ character.description }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import UserModule from './UserModule.vue'

const router = useRouter()
const chatStore = useChatStore()

const characters = computed(() => chatStore.characters)
const currentCharacterId = computed(() => chatStore.currentCharacterId)

const selectCharacter = (characterId: number) => {
  chatStore.setCurrentCharacter(characterId)
  router.push(`/chat/${characterId}`)
}
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  @apply bg-transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400;
}
</style>
