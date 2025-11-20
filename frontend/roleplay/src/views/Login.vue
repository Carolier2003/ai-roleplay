<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 p-4">
    <div class="w-full max-w-md bg-white/90 backdrop-blur-lg rounded-2xl shadow-2xl p-8 transform transition-all hover:scale-[1.01]">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-800 mb-2">AI 角色扮演</h1>
        <p class="text-gray-500">请登录您的账户</p>
      </div>
      
      <form @submit.prevent="handleSubmit" class="space-y-6">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">账号</label>
          <input 
            v-model="form.userAccount" 
            type="text"
            placeholder="请输入6-20位字母数字账号"
            class="w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition-all bg-white/50"
            :disabled="loading"
          />
          <p v-if="errors.userAccount" class="mt-1 text-xs text-red-500">{{ errors.userAccount }}</p>
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">密码</label>
          <input 
            v-model="form.userPassword" 
            type="password"
            placeholder="请输入8-20位密码"
            class="w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition-all bg-white/50"
            :disabled="loading"
          />
          <p v-if="errors.userPassword" class="mt-1 text-xs text-red-500">{{ errors.userPassword }}</p>
        </div>
        
        <div v-if="!isLogin" class="animate-fade-in">
          <label class="block text-sm font-medium text-gray-700 mb-1">确认密码</label>
          <input 
            v-model="form.confirmPassword" 
            type="password"
            placeholder="请再次输入密码"
            class="w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition-all bg-white/50"
            :disabled="loading"
          />
          <p v-if="errors.confirmPassword" class="mt-1 text-xs text-red-500">{{ errors.confirmPassword }}</p>
        </div>
        
        <button 
          type="submit" 
          class="w-full py-3 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-md hover:shadow-lg transform transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed flex justify-center items-center"
          :disabled="loading || !isFormValid"
        >
          <svg v-if="loading" class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          {{ isLogin ? '登录' : '注册' }}
        </button>
        
        <div class="text-center text-sm text-gray-500 mt-6">
          {{ isLogin ? '还没有账号？' : '已有账号？' }}
          <a @click="switchMode" class="text-indigo-600 hover:text-indigo-800 font-medium cursor-pointer hover:underline transition-colors">
            {{ isLogin ? '立即注册' : '去登录' }}
          </a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest, RegisterRequest } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()

// 状态
const isLogin = ref(true)
const loading = computed(() => authStore.loading)

// 表单数据
const form = ref({
  userAccount: '',
  userPassword: '',
  confirmPassword: ''
})

// 错误信息
const errors = reactive({
  userAccount: '',
  userPassword: '',
  confirmPassword: ''
})

// 验证逻辑
const validate = () => {
  let isValid = true
  errors.userAccount = ''
  errors.userPassword = ''
  errors.confirmPassword = ''

  const { userAccount, userPassword, confirmPassword } = form.value

  if (!userAccount) {
    errors.userAccount = '请输入账号'
    isValid = false
  } else if (userAccount.length < 6 || userAccount.length > 20) {
    errors.userAccount = '账号长度为6-20位'
    isValid = false
  } else if (!/^[a-zA-Z0-9]+$/.test(userAccount)) {
    errors.userAccount = '账号只能包含字母和数字'
    isValid = false
  }

  if (!userPassword) {
    errors.userPassword = '请输入密码'
    isValid = false
  } else if (userPassword.length < 8 || userPassword.length > 20) {
    errors.userPassword = '密码长度为8-20位'
    isValid = false
  }

  if (!isLogin.value) {
    if (!confirmPassword) {
      errors.confirmPassword = '请确认密码'
      isValid = false
    } else if (confirmPassword !== userPassword) {
      errors.confirmPassword = '两次输入的密码不一致'
      isValid = false
    }
  }

  return isValid
}

// 计算属性
const isFormValid = computed(() => {
  // 简单的预验证，用于禁用按钮
  const { userAccount, userPassword, confirmPassword } = form.value
  if (!userAccount || !userPassword) return false
  if (!isLogin.value && !confirmPassword) return false
  return true
})

// 方法
const switchMode = () => {
  isLogin.value = !isLogin.value
  form.value.confirmPassword = ''
  // 清除错误
  errors.userAccount = ''
  errors.userPassword = ''
  errors.confirmPassword = ''
}

const handleSubmit = async () => {
  if (!validate()) return
  
  try {
    if (isLogin.value) {
      // 登录
      const loginData: LoginRequest = {
        userAccount: form.value.userAccount,
        userPassword: form.value.userPassword
      }
      
      await authStore.login(loginData)
      // 简单的 alert 替代 message
      // alert('登录成功')
      
      // 跳转到聊天页面
      router.push('/chat/yuanbao')
    } else {
      // 注册
      const registerData: RegisterRequest = {
        userAccount: form.value.userAccount,
        userPassword: form.value.userPassword,
        confirmPassword: form.value.confirmPassword
      }
      
      await authStore.register(registerData)
      alert('注册成功，请登录')
      
      // 切换到登录模式
      isLogin.value = true
      form.value.userPassword = ''
      form.value.confirmPassword = ''
    }
  } catch (error: any) {
    alert(error.message || (isLogin.value ? '登录失败' : '注册失败'))
  }
}
</script>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
