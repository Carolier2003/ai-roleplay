<template>
  <div class="w-full h-full bg-white/80 backdrop-blur-xl flex flex-col p-4 gap-4 border-r border-white/40 shadow-[4px_0_24px_rgba(0,0,0,0.02)]">
    <!-- 用户模块 -->
    <UserModule />
    
    <!-- 角色列表 -->
    <div class="flex-1 flex flex-col min-h-0 bg-white/40 backdrop-blur-sm rounded-2xl border border-white/60 shadow-sm overflow-hidden">
      <div class="px-5 py-4 border-b border-white/50 bg-white/30">
        <h3 class="text-xs font-bold text-gray-400 uppercase tracking-widest flex items-center gap-2">
          <span class="text-base filter drop-shadow-sm">✨</span> 
          <span class="bg-gradient-to-r from-violet-600 to-fuchsia-600 bg-clip-text text-transparent">AI Companions</span>
        </h3>
      </div>
      
      <div class="flex-1 overflow-y-auto p-3 space-y-2 scroll-smooth custom-scrollbar">
        <div
          v-for="character in characters"
          :key="character.id"
          class="group relative flex items-center p-3 rounded-xl cursor-pointer transition-all duration-300 border border-transparent hover:shadow-md hover:-translate-y-0.5"
          :class="[
            character.id === currentCharacterId 
              ? 'bg-gradient-to-r from-violet-500/10 via-fuchsia-500/5 to-white/50 border-violet-200/50 shadow-sm' 
              : 'bg-white/40 hover:bg-white/80 hover:border-white/60'
          ]"
          @click="selectCharacter(character.id)"
        >
          <div class="relative mr-4">
            <div class="relative">
              <img
                :src="character.avatar"
                class="w-12 h-12 rounded-full object-cover transition-all duration-300 shadow-sm group-hover:shadow-md"
                :class="[
                  character.id === currentCharacterId 
                    ? 'ring-2 ring-violet-500 ring-offset-2 ring-offset-white/80 scale-105' 
                    : 'group-hover:scale-110'
                ]"
                alt="Avatar"
              />
              <!-- 在线状态点 -->
              <div class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white rounded-full shadow-sm"></div>
            </div>
            
            <div v-if="character.unread > 0" class="absolute -top-1 -right-1 bg-gradient-to-r from-red-500 to-pink-500 text-white text-[10px] font-bold rounded-full min-w-[18px] h-[18px] flex items-center justify-center border-2 border-white shadow-sm animate-bounce">
              {{ character.unread > 99 ? '99+' : character.unread }}
            </div>
          </div>
          
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between mb-0.5">
              <div class="text-sm font-bold text-gray-800 truncate transition-colors duration-200" 
                   :class="{ 'text-violet-700': character.id === currentCharacterId }">
                {{ character.name }}
              </div>
              <div v-if="character.id === currentCharacterId" class="text-xs text-violet-500 font-medium">
                Chatting
              </div>
            </div>
            <div class="text-xs text-gray-500 truncate transition-colors duration-200 group-hover:text-gray-600" 
                 :class="{ 'text-violet-500/80': character.id === currentCharacterId }">
              {{ character.description }}
            </div>
          </div>
          
          <!-- 选中指示器 -->
          <div v-if="character.id === currentCharacterId" class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-gradient-to-b from-violet-500 to-fuchsia-500 rounded-r-full shadow-[0_0_8px_rgba(139,92,246,0.4)]"></div>
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
