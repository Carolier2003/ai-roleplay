<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'

const { injectCSSVariables } = useTheme()
const authStore = useAuthStore()
const chatStore = useChatStore()

onMounted(() => {
  console.log('[App.vue] App组件已挂载')
  injectCSSVariables()
  // 初始化认证状态
  authStore.initAuth()
  // ✅ 先检查用户是否已登录，登录后再加载角色列表
  if (authStore.isLoggedIn) {
    chatStore.loadCharacters()
  }
})
</script>

<template>
  <div class="min-h-screen bg-gray-50 font-sans text-gray-900 relative overflow-hidden">
    <!-- Global Animated Background -->
    <div class="fixed inset-0 z-0 pointer-events-none">
      <div class="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-purple-400/30 blur-[100px] animate-blob"></div>
      <div class="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-indigo-400/30 blur-[100px] animate-blob animation-delay-2000"></div>
      <div class="absolute bottom-[-10%] left-[20%] w-[40%] h-[40%] rounded-full bg-pink-400/30 blur-[100px] animate-blob animation-delay-4000"></div>
    </div>

    <!-- Main Content -->
    <div class="relative z-10">
      <router-view></router-view>
    </div>
  </div>
</template>

<style>
/* 滚动条全局样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  @apply bg-gray-100;
}

::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full;
}

::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400;
}
</style>