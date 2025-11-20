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
  <div class="h-full w-full bg-gray-50 text-gray-900 font-sans antialiased">
    <RouterView />
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