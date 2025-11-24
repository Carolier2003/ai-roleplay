<template>
  <div class="flex h-screen bg-gray-100">
    <!-- Sidebar -->
    <div class="w-64 bg-white shadow-lg flex flex-col z-20">
      <div class="p-6 border-b flex items-center gap-3">
        <div class="w-8 h-8 bg-gradient-to-br from-purple-600 to-blue-600 rounded-lg flex items-center justify-center shadow-md">
          <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        <h1 class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-gray-800 to-gray-600">
          管理后台
        </h1>
      </div>
      
      <nav class="flex-1 p-4 space-y-2">
        <router-link 
          to="/admin" 
          class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-500 hover:bg-purple-50 hover:text-purple-600 transition-all duration-200 group"
          :class="{ 'bg-purple-50 text-purple-600 font-medium shadow-sm': $route.name === 'admin-dashboard' }"
        >
          <svg class="w-5 h-5 transition-colors" :class="$route.name === 'admin-dashboard' ? 'text-purple-600' : 'text-gray-400 group-hover:text-purple-600'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
          </svg>
          <span>仪表盘</span>
        </router-link>

        <router-link 
          to="/admin/users" 
          class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-500 hover:bg-purple-50 hover:text-purple-600 transition-all duration-200 group"
          active-class="bg-purple-50 text-purple-600 font-medium shadow-sm"
        >
          <svg class="w-5 h-5 transition-colors" :class="$route.name === 'admin-users' ? 'text-purple-600' : 'text-gray-400 group-hover:text-purple-600'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <span>用户管理</span>
        </router-link>
        
        <router-link 
          to="/admin/characters" 
          class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-500 hover:bg-purple-50 hover:text-purple-600 transition-all duration-200 group"
          active-class="bg-purple-50 text-purple-600 font-medium shadow-sm"
        >
          <svg class="w-5 h-5 transition-colors" :class="$route.name === 'admin-characters' ? 'text-purple-600' : 'text-gray-400 group-hover:text-purple-600'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>角色管理</span>
        </router-link>
        
        <router-link 
          to="/admin/knowledge" 
          class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-500 hover:bg-purple-50 hover:text-purple-600 transition-all duration-200 group"
          active-class="bg-purple-50 text-purple-600 font-medium shadow-sm"
        >
          <svg class="w-5 h-5 transition-colors" :class="$route.name === 'admin-knowledge' ? 'text-purple-600' : 'text-gray-400 group-hover:text-purple-600'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
          <span>知识库管理</span>
        </router-link>
      </nav>
      
      <div class="p-4 border-t">
        <router-link 
          to="/" 
          class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-500 hover:bg-gray-50 hover:text-gray-700 transition-all duration-200 group"
        >
          <svg class="w-5 h-5 text-gray-400 group-hover:text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          <span>返回前台</span>
        </router-link>
      </div>
    </div>

    <!-- Main Content -->
    <!-- Main Content -->
    <div class="flex-1 flex flex-col min-w-0">
      <header class="bg-white shadow-sm px-8 py-4 flex justify-between items-center z-10 relative">
        <h2 class="text-lg font-medium text-gray-700">{{ currentRouteName }}</h2>
        <div class="flex items-center gap-4">
          <span class="text-sm text-gray-500">管理员: {{ currentUser?.displayName || 'Admin' }}</span>
          <img 
            :src="currentUser?.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Admin'" 
            class="w-8 h-8 rounded-full bg-gray-200"
            alt="Avatar"
          />
        </div>
      </header>
      
      <div class="flex-1 overflow-auto">
        <main class="p-8 max-w-7xl mx-auto w-full">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const currentUser = computed(() => authStore.userInfo)

const currentRouteName = computed(() => {
  switch (route.name) {
    case 'admin-dashboard': return '仪表盘'
    case 'admin-users': return '用户管理'
    case 'admin-characters': return '角色管理'
    case 'admin-knowledge': return '知识库管理'
    default: return '管理后台'
  }
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
