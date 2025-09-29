<template>
  <n-modal 
    v-model:show="loginModalVisible" 
    :closable="true" 
    :mask-closable="true"
    class="login-modal"
    @update:show="handleModalClose"
  >
    <n-card 
      :title="isLogin ? '登录' : '注册'" 
      size="huge"
      class="login-card"
      :style="{ width: themeConfig.login.width }"
    >
      <n-form 
        ref="formRef" 
        :model="form" 
        :rules="rules"
        @keyup.enter="handleSubmit"
      >
        <n-form-item label="账号" path="userAccount">
          <n-input 
            v-model:value="form.userAccount" 
            placeholder="请输入6-20位字母数字账号"
            :style="{ height: '36px' }"
            :disabled="loading"
          />
        </n-form-item>
        
        <n-form-item label="密码" path="userPassword">
          <n-input 
            v-model:value="form.userPassword" 
            type="password"
            placeholder="请输入8-20位密码"
            :style="{ height: '36px' }"
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
            :style="{ height: '36px' }"
            :disabled="loading"
            show-password-on="click"
          />
        </n-form-item>
      </n-form>
      
      <template #footer>
        <div class="login-footer">
          <n-button 
            type="primary" 
            size="large" 
            block 
            @click="handleSubmit"
            :loading="loading"
            :disabled="!isFormValid || (cooldown > 0 && !isLogin)"
            :style="{ height: themeConfig.login.btnHeight }"
          >
            {{ getSubmitButtonText() }}
          </n-button>
          
          <div class="switch-tip">
            {{ isLogin ? '还没有账号？' : '已有账号？' }}
            <a @click="switchMode" class="switch-link">
              {{ isLogin ? '立即注册' : '去登录' }}
            </a>
          </div>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { NModal, NCard, NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useTheme } from '@/composables/useTheme'
import type { RegisterRequest, LoginRequest } from '@/api/auth'

const { themeConfig } = useTheme()
const authStore = useAuthStore()
const chatStore = useChatStore()
const message = useMessage()

// 表单引用
const formRef = ref<FormInst>()

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

// 表单验证规则
const rules: FormRules = {
  userAccount: [
    { required: true, message: '请输入账号' },
    { min: 6, max: 20, message: '账号长度必须在6-20位之间' },
    { pattern: /^[a-zA-Z0-9]{6,20}$/, message: '账号必须是6-20位字母数字' }
  ],
  userPassword: [
    { required: true, message: '请输入密码' },
    { min: 8, max: 20, message: '密码长度必须在8-20位之间' }
  ],
  confirmPassword: [
    { 
      required: true, 
      message: '请确认密码',
      trigger: ['blur', 'input']
    },
    {
      validator: (rule, value) => {
        if (value !== form.value.userPassword) {
          return new Error('两次输入的密码不一致')
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ]
}

// 方法
const switchMode = () => {
  isLogin.value = !isLogin.value
  // 清空表单
  form.value = {
    userAccount: '',
    userPassword: '',
    confirmPassword: ''
  }
  // 重置验证
  formRef.value?.restoreValidation()
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
    
    // 重置验证状态
    formRef.value?.restoreValidation()
    
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
    // 表单验证
    await formRef.value?.validate()
    
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
      const registeredPassword = form.value.userPassword
      
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
  
  cooldownTimer = setInterval(() => {
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
    formRef.value?.restoreValidation()
  }
})
</script>

<style scoped>
.login-modal :deep(.n-card) {
  border-radius: v-bind('themeConfig.login.radius');
}

.login-card {
  max-width: 90vw;
}

.login-footer {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.switch-tip {
  text-align: center;
  font-size: 14px;
  color: var(--gray-600, #6b7280);
}

.switch-link {
  color: var(--primary-500, #1677ff);
  cursor: pointer;
  text-decoration: none;
  margin-left: 4px;
}

.switch-link:hover {
  text-decoration: underline;
}

/* 输入框样式 */
:deep(.n-input) {
  border-radius: 8px;
}

:deep(.n-input .n-input__input-el) {
  background-color: transparent !important;
  padding: 8px 12px !important;
  line-height: 1.4 !important;
}

:deep(.n-input:not(.n-input--disabled):hover .n-input__border) {
  border-color: #d1d5db;
}

:deep(.n-input:not(.n-input--disabled).n-input--focus .n-input__border) {
  border-color: #d1d5db;
  box-shadow: none;
}

/* 按钮样式 */
:deep(.n-button--primary-type) {
  border-radius: 8px;
  font-weight: 500;
}
</style>