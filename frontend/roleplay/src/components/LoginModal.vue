<template>
  <Teleport to="body">
    <div v-if="loginModalVisible" class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto overflow-x-hidden bg-black/50 backdrop-blur-sm p-4 sm:p-0" @click.self="handleModalClose(false)">
      <div class="relative w-full max-w-md transform rounded-2xl bg-white/80 backdrop-blur-xl p-8 text-left shadow-2xl transition-all border border-white/20 sm:my-8">
        <!-- Close button -->
        <button @click="handleModalClose(false)" class="absolute top-4 right-4 text-gray-400 hover:text-gray-600 focus:outline-none">
          <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <div class="text-center mb-8">
          <h3 class="text-2xl font-bold leading-6 text-gray-900">{{ isLogin ? '欢迎回来' : '创建账号' }}</h3>
          <p class="mt-2 text-sm text-gray-500">{{ isLogin ? '请登录您的账号以继续' : '注册一个新账号开始体验' }}</p>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div>
            <label for="account" class="block text-sm font-medium text-gray-700">账号</label>
            <div class="mt-1">
              <input
                id="account"
                v-model="form.userAccount"
                type="text"
                required
                placeholder="请输入6-20位字母数字账号"
                class="block w-full rounded-lg border border-gray-300 bg-white/50 px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm transition-all duration-200"
                :disabled="loading"
              />
            </div>
          </div>

          <div>
            <label for="password" class="block text-sm font-medium text-gray-700">密码</label>
            <div class="mt-1">
              <input
                id="password"
                v-model="form.userPassword"
                type="password"
                required
                placeholder="请输入8-20位密码"
                class="block w-full rounded-lg border border-gray-300 bg-white/50 px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm transition-all duration-200"
                :disabled="loading"
              />
            </div>
          </div>

          <div v-if="!isLogin">
            <label for="confirmPassword" class="block text-sm font-medium text-gray-700">确认密码</label>
            <div class="mt-1">
              <input
                id="confirmPassword"
                v-model="form.confirmPassword"
                type="password"
                required
                placeholder="请再次输入密码"
                class="block w-full rounded-lg border border-gray-300 bg-white/50 px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm transition-all duration-200"
                :disabled="loading"
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              :disabled="loading || !isFormValid || (cooldown > 0 && !isLogin)"
              class="flex w-full justify-center rounded-lg bg-indigo-600 px-4 py-3 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 transform active:scale-[0.98]"
            >
              <svg v-if="loading" class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ getSubmitButtonText() }}
            </button>
          </div>
        </form>

        <div class="mt-6 text-center text-sm">
          <span class="text-gray-500">
            {{ isLogin ? '还没有账号？' : '已有账号？' }}
          </span>
          <button @click="switchMode" class="font-semibold text-indigo-600 hover:text-indigo-500 ml-1 focus:outline-none">
            {{ isLogin ? '立即注册' : '去登录' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import type { RegisterRequest, LoginRequest } from '@/api/auth'

import { useToast } from '@/composables/useToast'

const authStore = useAuthStore()
const chatStore = useChatStore()
const toast = useToast()

// Simple replacement for useMessage
const message = {
  success: (msg: string) => {
    console.log('Success:', msg)
    toast.success(msg)
  },
  error: (msg: string) => {
    console.error('Error:', msg)
    toast.error(msg)
  },
  info: (msg: string) => {
    console.log('Info:', msg)
    toast.info(msg)
  }
}

// 状态
const isLogin = ref(true)
const loading = ref(false)
const cooldown = ref(0)

// 表单数据
const form = ref({
  userAccount: '',
  userPassword: '',
  confirmPassword: ''
})

// 计算属性
const loginModalVisible = computed({
  get: () => authStore.loginModalVisible,
  set: (value) => {
    if (!value) {
      authStore.hideLoginModal()
    }
  }
})

const isFormValid = computed(() => {
  if (isLogin.value) {
    return form.value.userAccount.length >= 6 && form.value.userPassword.length >= 8
  } else {
    return form.value.userAccount.length >= 6 && 
           form.value.userPassword.length >= 8 && 
           form.value.confirmPassword === form.value.userPassword
  }
})

// 方法
const switchMode = () => {
  isLogin.value = !isLogin.value
  // 清空表单
  form.value = {
    userAccount: '',
    userPassword: '',
    confirmPassword: ''
  }
}

// 处理模态框关闭
const handleModalClose = (visible: boolean) => {
  if (!visible) {
    console.log('[LoginModal] 用户点击遮罩或ESC键关闭弹窗')
    
    // 清空表单数据
    form.value = {
      userAccount: '',
      userPassword: '',
      confirmPassword: ''
    }
    
    // 重置到登录模式
    isLogin.value = true
    
    // 清除加载状态
    loading.value = false
    
    // 清除冷却时间
    cooldown.value = 0
    
    // 调用store方法隐藏弹窗
    authStore.hideLoginModal()
  }
}

const getSubmitButtonText = () => {
  if (loading.value) {
    return isLogin.value ? '登录中...' : '注册中...'
  }
  if (cooldown.value > 0 && !isLogin.value) {
    return `${cooldown.value}s后可重试`
  }
  return isLogin.value ? '登录' : '注册'
}

const startCooldown = () => {
  cooldown.value = 60
}

const handleSubmit = async () => {
  if (loading.value || (cooldown.value > 0 && !isLogin.value)) return
  
  try {
    loading.value = true
    
    if (isLogin.value) {
      // 登录
      const loginData: LoginRequest = {
        userAccount: form.value.userAccount,
        userPassword: form.value.userPassword
      }
      
      await authStore.login(loginData)
      
      console.log('[LoginModal] 登录成功')
      message.success('登录成功！')
      
      // 登录成功后检查是否有待发送消息
      if (chatStore.pendingMessage) {
        console.log('[LoginModal] 检测到待发送消息，准备自动发送')
        message.info('正在发送您的消息...')
        // 这里会在Chat.vue中监听登录状态变化来处理
      }
      
    } else {
      // 注册
      const registerData: RegisterRequest = {
        userAccount: form.value.userAccount,
        userPassword: form.value.userPassword,
        confirmPassword: form.value.confirmPassword
      }
      
      await authStore.register(registerData)
      
      console.log('[LoginModal] 注册成功，切换到登录模式')
      message.success('注册成功！请登录您的账号')
      
      // 注册成功后自动切换到登录模式并填充账号
      const registeredAccount = form.value.userAccount
      
      isLogin.value = true
      
      // 保留账号，清空密码让用户重新输入（安全考虑）
      form.value = {
        userAccount: registeredAccount,
        userPassword: '', // 清空密码，让用户重新输入
        confirmPassword: ''
      }
      
      // 提示用户
      console.log('[LoginModal] 注册成功！请输入密码进行登录')
      
      // 启动冷却时间
      startCooldown()
    }
    
  } catch (error: any) {
    console.error('[LoginModal] 操作失败:', error)
    
    // 使用错误处理器获取用户友好的错误消息
    const { ErrorHandler } = await import('@/utils/errorHandler')
    const userMessage = ErrorHandler.getUserMessage(error)
    
    // 显示错误消息
    message.error(userMessage || (isLogin.value ? '登录失败' : '注册失败'))
    
    // 如果是注册失败，启动冷却时间
    if (!isLogin.value) {
      startCooldown()
    }
  } finally {
    loading.value = false
  }
}

// 冷却时间倒计时
let cooldownTimer: number | null = null

const startCooldownTimer = () => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
  
  cooldownTimer = window.setInterval(() => {
    if (cooldown.value > 0) {
      cooldown.value--
    } else {
      clearInterval(cooldownTimer!)
      cooldownTimer = null
    }
  }, 1000)
}

// 监听冷却时间变化
watch(cooldown, (newValue) => {
  if (newValue > 0 && !cooldownTimer) {
    startCooldownTimer()
  }
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
})

// 监听弹窗显示状态，重置表单
watch(loginModalVisible, (visible) => {
  if (visible) {
    // 弹窗打开时重置表单
    form.value = {
      userAccount: '',
      userPassword: '',
      confirmPassword: ''
    }
    isLogin.value = true
    cooldown.value = 0
  }
})
</script>

<style scoped>
/* Tailwind classes handle styling */
</style>