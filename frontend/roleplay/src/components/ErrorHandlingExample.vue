<!--
  错误处理示例组件
  展示如何在组件中使用统一的错误处理机制
-->
<template>
  <div class="p-6 border border-gray-200 rounded-lg my-5 bg-white shadow-sm">
    <h3 class="text-lg font-bold text-gray-900 mb-4">错误处理示例</h3>
    
    <div class="flex flex-wrap gap-3 mb-5">
      <button 
        @click="testUserNotFound" 
        class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors font-medium text-sm"
      >
        测试用户不存在错误
      </button>
      
      <button 
        @click="testNetworkError" 
        class="px-4 py-2 bg-yellow-500 text-white rounded-md hover:bg-yellow-600 transition-colors font-medium text-sm"
      >
        测试网络错误
      </button>
      
      <button 
        @click="testAuthError" 
        class="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors font-medium text-sm"
      >
        测试认证错误
      </button>
      
      <button 
        @click="testBusinessError" 
        class="px-4 py-2 bg-cyan-500 text-white rounded-md hover:bg-cyan-600 transition-colors font-medium text-sm"
      >
        测试业务错误
      </button>
    </div>
    
    <div v-if="errorHandler.loading.value" class="flex items-center gap-2 my-5 text-gray-600">
      <div class="w-4 h-4 border-2 border-gray-600 border-t-transparent rounded-full animate-spin"></div>
      <span class="text-sm">处理中...</span>
    </div>
    
    <div v-if="errorHandler.error.value" class="my-5 p-4 bg-gray-50 rounded-md border border-gray-200">
      <h4 class="text-red-600 font-medium mb-2 m-0">当前错误信息：</h4>
      <pre class="bg-white p-3 rounded border border-gray-200 overflow-x-auto text-xs my-2 font-mono">{{ JSON.stringify(errorHandler.error.value, null, 2) }}</pre>
      <button 
        @click="errorHandler.clearError" 
        class="px-3 py-1 bg-white border border-gray-300 text-gray-700 rounded hover:bg-gray-50 text-xs transition-colors"
      >
        清除错误
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
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
