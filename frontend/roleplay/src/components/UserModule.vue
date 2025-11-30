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
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
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
    <!-- 设置面板 (使用 Grid 动画实现丝滑展开) -->
    <div 
      v-if="authStore.isLoggedIn"
      class="grid transition-[grid-template-rows,opacity,padding] duration-300 ease-[cubic-bezier(0.4,0,0.2,1)]"
      :class="showSettings ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'"
    >
      <div class="overflow-hidden min-h-0">
        <div class="mt-4 pt-4 border-t border-gray-100">
          <div class="text-sm font-semibold text-gray-700 mb-3">
            设置选项
          </div>
          
          <div class="flex flex-col gap-2">
            <div class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors" @click="editProfile">
              <div class="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center flex-shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </div>
              <span class="flex-1 text-sm font-medium text-gray-700">个人资料</span>
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
            
            <div class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors group" @click="logout">
              <div class="w-8 h-8 rounded-lg bg-red-50 text-red-500 group-hover:bg-red-100 group-hover:text-red-600 transition-colors flex items-center justify-center flex-shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                  <polyline points="16 17 21 12 16 7"></polyline>
                  <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
              </div>
              <span class="flex-1 text-sm font-medium text-gray-700 group-hover:text-gray-900">退出登录</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    
    <!-- Kimi风格个人中心弹窗 -->
    <KimiProfileCenter
      v-model:visible="showKimiProfile"
      @success="onKimiProfileSuccess"
    />
    
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import KimiProfileCenter from './KimiProfileCenter.vue'

const authStore = useAuthStore()
const chatStore = useChatStore()
const toast = useToast()
const confirm = useConfirm()

// Simple replacement for useMessage/useDialog
const message = {
  success: (msg: string) => {
    console.log('Success:', msg)
    toast.success(msg)
  },
  error: (msg: string) => {
    console.error('Error:', msg)
    toast.error(msg)
  },
  warning: (msg: string) => {
    console.warn('Warning:', msg)
    toast.warning(msg)
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
  showKimiProfile.value = true
  showSettings.value = false // 关闭设置面板
}

const logout = async () => {
  const confirmed = await confirm.warning('确定要退出登录吗？')
  if (!confirmed) return

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
