<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h1>AI 角色扮演</h1>
        <p>请登录您的账户</p>
      </div>
      
      <n-form 
        ref="formRef" 
        :model="form" 
        :rules="rules"
        @keyup.enter="handleSubmit"
        class="login-form"
      >
        <n-form-item label="账号" path="userAccount">
          <n-input 
            v-model:value="form.userAccount" 
            placeholder="请输入6-20位字母数字账号"
            size="large"
            :disabled="loading"
          />
        </n-form-item>
        
        <n-form-item label="密码" path="userPassword">
          <n-input 
            v-model:value="form.userPassword" 
            type="password"
            placeholder="请输入8-20位密码"
            size="large"
            :disabled="loading"
            show-password-on="click"
          />
        </n-form-item>
        
        <n-form-item 
          v-if="!isLogin" 
          label="确认密码" 
          path="confirmPassword"
        >
          <n-input 
            v-model:value="form.confirmPassword" 
            type="password"
            placeholder="请再次输入密码"
            size="large"
            :disabled="loading"
            show-password-on="click"
          />
        </n-form-item>
        
        <n-button 
          type="primary" 
          size="large" 
          block 
          @click="handleSubmit"
          :loading="loading"
          :disabled="!isFormValid"
          class="submit-btn"
        >
          {{ isLogin ? '登录' : '注册' }}
        </n-button>
        
        <div class="switch-tip">
          {{ isLogin ? '还没有账号？' : '已有账号？' }}
          <a @click="switchMode" class="switch-link">
            {{ isLogin ? '立即注册' : '去登录' }}
          </a>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest, RegisterRequest } from '@/api/auth'

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()

// 表单引用
const formRef = ref<FormInst>()

// 状态
const isLogin = ref(true)
const loading = computed(() => authStore.loading)

// 表单数据
const form = ref({
  userAccount: '',
  userPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const rules: FormRules = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 6, max: 20, message: '账号长度为6-20位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '账号只能包含字母和数字', trigger: 'blur' }
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度为8-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { 
      required: true, 
      message: '请确认密码', 
      trigger: 'blur',
      validator: (rule, value) => {
        if (!isLogin.value && value !== form.value.userPassword) {
          return new Error('两次输入的密码不一致')
        }
        return true
      }
    }
  ]
}

// 计算属性
const isFormValid = computed(() => {
  const { userAccount, userPassword, confirmPassword } = form.value
  
  if (!userAccount || !userPassword) return false
  if (userAccount.length < 6 || userAccount.length > 20) return false
  if (userPassword.length < 8 || userPassword.length > 20) return false
  if (!isLogin.value && (!confirmPassword || confirmPassword !== userPassword)) return false
  
  return true
})

// 方法
const switchMode = () => {
  isLogin.value = !isLogin.value
  form.value.confirmPassword = ''
}

const handleSubmit = async () => {
  if (!formRef.value || !isFormValid.value) return
  
  try {
    await formRef.value.validate()
    
    if (isLogin.value) {
      // 登录
      const loginData: LoginRequest = {
        userAccount: form.value.userAccount,
        userPassword: form.value.userPassword
      }
      
      await authStore.login(loginData)
      message.success('登录成功')
      
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
      message.success('注册成功，请登录')
      
      // 切换到登录模式
      isLogin.value = true
      form.value.userPassword = ''
      form.value.confirmPassword = ''
    }
  } catch (error: any) {
    message.error(error.message || (isLogin.value ? '登录失败' : '注册失败'))
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.login-header p {
  color: #6b7280;
  font-size: 16px;
}

.login-form {
  margin-bottom: 0;
}

.submit-btn {
  margin-top: 24px;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
}

.switch-tip {
  text-align: center;
  margin-top: 24px;
  color: #6b7280;
  font-size: 14px;
}

.switch-link {
  color: #1677ff;
  cursor: pointer;
  text-decoration: none;
  margin-left: 4px;
}

.switch-link:hover {
  text-decoration: underline;
}
</style>
