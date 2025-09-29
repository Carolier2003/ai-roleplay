<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { NConfigProvider, NMessageProvider, NDialogProvider } from 'naive-ui'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'

const { yuanbaoTheme, injectCSSVariables } = useTheme()
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
  <NConfigProvider :theme="yuanbaoTheme">
    <NMessageProvider>
      <NDialogProvider>
        <RouterView />
      </NDialogProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: var(--gray-50, #f9fafb);
}

#app {
  height: 100%;
}

/* 滚动条全局样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: var(--gray-100, #f3f4f6);
}

::-webkit-scrollbar-thumb {
  background: var(--gray-300, #d1d5db);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--gray-400, #9ca3af);
}
</style>