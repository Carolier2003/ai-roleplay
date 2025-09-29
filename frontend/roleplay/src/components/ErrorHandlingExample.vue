<!--
  错误处理示例组件
  展示如何在组件中使用统一的错误处理机制
-->
<template>
  <div class="error-handling-example">
    <h3>错误处理示例</h3>
    
    <div class="button-group">
      <n-button @click="testUserNotFound" type="primary">
        测试用户不存在错误
      </n-button>
      
      <n-button @click="testNetworkError" type="warning">
        测试网络错误
      </n-button>
      
      <n-button @click="testAuthError" type="error">
        测试认证错误
      </n-button>
      
      <n-button @click="testBusinessError" type="info">
        测试业务错误
      </n-button>
    </div>
    
    <div v-if="errorHandler.loading.value" class="loading">
      <n-spin size="small" />
      处理中...
    </div>
    
    <div v-if="errorHandler.error.value" class="error-info">
      <h4>当前错误信息：</h4>
      <pre>{{ JSON.stringify(errorHandler.error.value, null, 2) }}</pre>
      <n-button @click="errorHandler.clearError" size="small">
        清除错误
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NButton, NSpin } from 'naive-ui'
import { useErrorHandler } from '@/composables/useErrorHandler'

const errorHandler = useErrorHandler()

// 模拟用户不存在错误
const testUserNotFound = async () => {
  await errorHandler.withErrorHandling(
    async () => {
      // 模拟后端返回用户不存在错误
      const mockError = {
        response: {
          status: 404,
          data: {
            timestamp: new Date().toISOString(),
            status: 404,
            error: 'USER_NOT_FOUND',
            message: '用户不存在',
            path: '/api/user/123'
          }
        }
      }
      throw mockError
    },
    '获取用户信息',
    { showLoading: true }
  )
}

// 模拟网络错误
const testNetworkError = async () => {
  await errorHandler.withErrorHandling(
    async () => {
      // 模拟网络连接失败
      const mockError = {
        message: 'Network Error',
        code: 'NETWORK_ERROR'
      }
      throw mockError
    },
    '网络请求',
    { showLoading: true }
  )
}

// 模拟认证错误
const testAuthError = async () => {
  await errorHandler.withErrorHandling(
    async () => {
      // 模拟token过期错误
      const mockError = {
        response: {
          status: 401,
          data: {
            timestamp: new Date().toISOString(),
            status: 401,
            error: 'TOKEN_EXPIRED',
            message: '令牌已过期',
            path: '/api/auth/me'
          }
        }
      }
      throw mockError
    },
    '获取用户信息',
    { showLoading: true }
  )
}

// 模拟业务错误
const testBusinessError = async () => {
  await errorHandler.withErrorHandling(
    async () => {
      // 模拟角色不存在错误
      const mockError = {
        response: {
          status: 400,
          data: {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'CHAT_CHARACTER_NOT_FOUND',
            message: '角色不存在',
            path: '/api/chat/character/999'
          }
        }
      }
      throw mockError
    },
    '获取角色信息',
    { showLoading: true }
  )
}
</script>

<style scoped>
.error-handling-example {
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin: 20px 0;
}

.button-group {
  display: flex;
  gap: 10px;
  margin: 20px 0;
  flex-wrap: wrap;
}

.loading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 20px 0;
  color: #666;
}

.error-info {
  margin: 20px 0;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 4px;
}

.error-info h4 {
  margin: 0 0 10px 0;
  color: #d32f2f;
}

.error-info pre {
  background: white;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
  margin: 10px 0;
}
</style>
