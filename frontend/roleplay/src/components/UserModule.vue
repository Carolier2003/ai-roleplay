<template>
  <div class="bg-white rounded-xl p-4 mb-4 shadow-sm border border-gray-100">
    <!-- 用户信息区域 -->
    <div class="flex items-center gap-3">
      <div class="flex-shrink-0">
        <div 
          class="w-12 h-12 rounded-full border-2 border-gray-200 overflow-hidden transition-transform duration-300 hover:scale-105 hover:border-blue-500 relative"
          :class="{ 'rounded-xl': !authStore.isLoggedIn }"
        >
          <img 
            v-if="userAvatar" 
            :src="userAvatar" 
            class="w-full h-full object-cover"
            @error="handleAvatarError"
            alt="User Avatar"
          />
             <div v-else class="w-full h-full flex flex-col items-center justify-center text-white font-mono"
               :class="authStore.isLoggedIn ? 'bg-gradient-to-br from-indigo-500 to-purple-600' : 'bg-gradient-to-br from-orange-400 to-yellow-300'">
            <div v-if="!authStore.isLoggedIn" class="flex flex-col items-center">
              <div class="text-xs mb-0.5 tracking-widest">• •</div>
              <div class="text-[10px]">‿</div>
            </div>
            <div v-else class="flex flex-col items-center">
              <div class="text-xs mb-0.5 tracking-widest">◉ ◉</div>
              <div class="text-[10px]">◡</div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="flex-1 min-w-0">
        <div class="text-base font-semibold text-gray-800 mb-1 truncate">
          {{ displayName }}
        </div>
        <div class="flex items-center gap-1.5 text-xs text-gray-500">
          <span class="w-2 h-2 rounded-full flex-shrink-0" :class="authStore.isLoggedIn ? 'bg-green-500' : 'bg-gray-400'"></span>
          <span>{{ statusText }}</span>
        </div>
      </div>
      
      <div class="flex-shrink-0">
        <button
          v-if="authStore.isLoggedIn"
          @click="showSettings = !showSettings"
          class="text-gray-500 hover:text-blue-500 p-1 rounded-full hover:bg-gray-100 transition-colors"
        >
          <span class="text-xl">⚙️</span>
        </button>
        <button
          v-else
          @click="authStore.showLoginModal()"
          class="px-3 py-1 text-xs font-medium text-white bg-blue-500 rounded-full hover:bg-blue-600 transition-colors shadow-sm"
        >
          登录
        </button>
      </div>
    </div>
    
    <!-- 设置面板 -->
    <div v-if="showSettings && authStore.isLoggedIn" class="mt-4 pt-4 border-t border-gray-100">
      <div class="text-sm font-semibold text-gray-700 mb-3">
        设置选项
      </div>
      
      <div class="flex flex-col gap-2">
        <div class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors" @click="editProfile">
          <span class="w-5 text-center text-base">👤</span>
          <span class="flex-1 text-sm text-gray-700">个人资料</span>
          <span class="text-xs text-gray-500">编辑</span>
        </div>
        
        <div class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors group" @click="logout">
          <span class="w-5 text-center text-base">🚪</span>
          <span class="flex-1 text-sm text-gray-700">退出登录</span>
          <span class="text-xs text-red-500 group-hover:text-red-600">退出</span>
        </div>
      </div>
    </div>
    
    <!-- 聊呗标识栏 -->
    <div class="mt-4 pt-4 border-t border-gray-100 flex items-center gap-3">
      <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg shadow-indigo-500/30 text-white flex items-center justify-center text-2xl">
        <div class="flex flex-col items-center justify-center font-mono">
          <div class="text-[8px] mb-[1px] tracking-widest">◉ ◉</div>
          <div class="text-[8px]">◡</div>
        </div>
      </div>
      <div class="flex-1">
        <div class="text-base font-bold bg-gradient-to-br from-indigo-500 to-purple-600 bg-clip-text text-transparent">
          聊呗
        </div>
        <div class="text-xs text-gray-400">
          AI角色扮演
        </div>
      </div>
    </div>
    
    <!-- Kimi风格个人中心弹窗 (暂时禁用，等待迁移) -->
    <!-- <KimiProfileCenter
      v-model:visible="showKimiProfile"
      @success="onKimiProfileSuccess"
    /> -->
    
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
// import KimiProfileCenter from './KimiProfileCenter.vue'

const authStore = useAuthStore()
const chatStore = useChatStore()

// Simple replacement for useMessage/useDialog
const message = {
  success: (msg: string) => console.log('Success:', msg),
  error: (msg: string) => {
    console.error('Error:', msg)
    alert(msg)
  },
  warning: (msg: string) => {
    console.warn('Warning:', msg)
    alert(msg)
  }
}

const showSettings = ref(false)
const showKimiProfile = ref(false)

// 计算属性
const displayName = computed(() => {
  if (authStore.isLoggedIn && authStore.userInfo) {
    return authStore.userInfo.displayName || authStore.userInfo.userAccount || '用户'
  }
  return '游客'
})

const userAvatar = computed(() => {
  if (authStore.isLoggedIn && authStore.userInfo?.avatarUrl) {
    const avatarUrl = authStore.userInfo.avatarUrl.trim()
    // 验证URL格式
    if (avatarUrl && (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://') || avatarUrl.startsWith('data:image/'))) {
      console.log('[UserModule] 使用用户自定义头像:', avatarUrl)
      return avatarUrl
    }
  }
  // 所有用户（包括游客和没有头像的登录用户）都使用默认头像
  console.log('[UserModule] 使用默认头像 fallback')
  return undefined // 使用 fallback 模板中的头像
})

const statusText = computed(() => {
  return authStore.isLoggedIn ? '在线' : '游客模式'
})

// 方法
const editProfile = () => {
  console.log('[UserModule] 打开Kimi风格个人中心')
  // showKimiProfile.value = true
  message.warning('个人中心功能正在升级维护中，请稍后再试')
  showSettings.value = false // 关闭设置面板
}

const logout = async () => {
  if (!confirm('确定要退出登录吗？')) return

  try {
    await authStore.logout()
    showSettings.value = false
    
    // 清除聊天状态
    chatStore.clearMessages()
    
    message.success('已退出登录')
    
    // 强制更新页面状态
    await nextTick()
  } catch (error) {
    console.error('[UserModule] 退出登录失败:', error)
    message.error('退出登录失败')
  }
}

// Kimi个人中心更新成功回调
const onKimiProfileSuccess = () => {
  console.log('[UserModule] Kimi个人中心更新成功')
  message.success('个人资料更新成功')
}

// 头像加载失败处理
const handleAvatarError = (error: Event) => {
  console.error('[UserModule] 头像加载失败:', userAvatar.value, error)
  
  // 如果是阿里云OSS链接，尝试添加时间戳
  if (userAvatar.value && userAvatar.value.includes('aliyuncs.com') && !userAvatar.value.includes('?t=')) {
    const timestamp = Date.now()
    const newUrl = `${userAvatar.value}?t=${timestamp}`
    console.log('[UserModule] 尝试添加时间戳重新加载头像:', newUrl)
    
    // 更新用户信息中的头像URL
    if (authStore.userInfo && authStore.userInfo.avatarUrl === userAvatar.value) {
      authStore.userInfo.avatarUrl = newUrl
      localStorage.setItem('USER_INFO', JSON.stringify(authStore.userInfo))
    }
  }
}
</script>

<style scoped>
/* Tailwind classes handle styling */
</style>
