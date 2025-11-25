<template>
  <div class="space-y-6">
    <!-- Header Card -->
    <div class="bg-white/80 backdrop-blur-xl rounded-2xl shadow-sm border border-white/20 p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-all duration-300 hover:shadow-md">
      <div class="flex items-center gap-4">
        <div class="p-2 bg-purple-50 rounded-lg">
          <svg class="w-6 h-6 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
        </div>
        <div>
          <h3 class="text-xl font-bold text-gray-900">用户管理</h3>
          <p class="text-gray-500 text-sm mt-1">管理系统中的所有注册用户</p>
        </div>
      </div>
      
      <div class="flex items-center gap-3">
        <div class="relative group">
          <input 
            v-model="keyword" 
            @keyup.enter="fetchUsers"
            type="text" 
            placeholder="搜索账号、昵称或邮箱..." 
            class="pl-10 pr-4 py-2 w-64 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all"
          />
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg class="w-5 h-5 text-gray-400 group-focus-within:text-purple-500 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
        </div>
        
        <button 
          @click="fetchUsers"
          class="px-4 py-2 bg-white border border-gray-200 text-gray-700 rounded-xl hover:bg-gray-50 hover:border-gray-300 transition-all text-sm font-medium shadow-sm"
        >
          搜索
        </button>
      </div>
    </div>

    <!-- Content Card -->
    <div class="bg-white/80 backdrop-blur-xl rounded-2xl shadow-sm border border-white/20 overflow-hidden flex flex-col min-h-[600px]">
      <!-- Table -->
      <div class="overflow-x-auto flex-1">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">用户</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">角色</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">注册时间</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">最后登录</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <template v-if="users.length > 0">
              <tr v-for="user in users" :key="user.userId" class="group hover:bg-purple-50/30 transition-colors duration-200">
                <td class="px-6 py-4">
                  <div class="flex items-center gap-4">
                    <div class="relative">
                      <div class="absolute -inset-0.5 bg-gradient-to-r from-purple-600 to-blue-600 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-200 blur-[2px]"></div>
                      <img 
                        :src="user.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${user.userAccount}`" 
                        class="relative w-10 h-10 rounded-full bg-white object-cover ring-2 ring-white"
                        alt="Avatar"
                      />
                    </div>
                    <div>
                      <div class="font-medium text-gray-900 group-hover:text-purple-700 transition-colors">{{ user.displayName }}</div>
                      <div class="text-xs text-gray-500 font-mono mt-0.5">{{ user.userAccount }}</div>
                      <div v-if="user.email" class="text-xs text-gray-400 mt-0.5">{{ user.email }}</div>
                    </div>
                  </div>
                </td>
                <td class="px-6 py-4">
                  <span 
                    class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ring-1 ring-inset transition-colors"
                    :class="user.role === 'ADMIN' 
                      ? 'bg-purple-50 text-purple-700 ring-purple-600/20' 
                      : 'bg-blue-50 text-blue-700 ring-blue-600/20'"
                  >
                    <span class="w-1.5 h-1.5 rounded-full mr-1.5" :class="user.role === 'ADMIN' ? 'bg-purple-600' : 'bg-blue-600'"></span>
                    {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  <span 
                    class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ring-1 ring-inset transition-colors"
                    :class="user.status === 1 
                      ? 'bg-green-50 text-green-700 ring-green-600/20' 
                      : 'bg-red-50 text-red-700 ring-red-600/20'"
                  >
                    <span class="w-1.5 h-1.5 rounded-full mr-1.5" :class="user.status === 1 ? 'bg-green-600' : 'bg-red-600'"></span>
                    {{ user.status === 1 ? '正常' : '已禁用' }}
                  </span>
                </td>
                <td class="px-6 py-4 text-sm text-gray-500 tabular-nums">
                  {{ formatDate(user.createdAt) }}
                </td>
                <td class="px-6 py-4 text-sm text-gray-500 tabular-nums">
                  {{ formatDate(user.lastLoginAt) }}
                </td>
                <td class="px-6 py-4 text-right">
                  <div class="flex justify-end items-center gap-2 opacity-0 group-hover:opacity-100 transition-all duration-200 transform translate-x-2 group-hover:translate-x-0">
                    <button 
                      v-if="user.role !== 'ADMIN'"
                      @click="promoteUser(user)"
                      class="p-1.5 text-gray-400 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition-colors"
                      title="设为管理员"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                    </button>
                    <button 
                      v-else
                      @click="demoteUser(user)"
                      class="p-1.5 text-purple-600 hover:text-gray-600 hover:bg-gray-50 rounded-lg transition-colors"
                      title="取消管理员"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                      </svg>
                    </button>
                    
                    <div class="w-px h-4 bg-gray-200 mx-1"></div>

                    <button 
                      v-if="user.status === 1"
                      @click="banUser(user)"
                      class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                      title="禁用用户"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                      </svg>
                    </button>
                    <button 
                      v-else
                      @click="unbanUser(user)"
                      class="p-1.5 text-red-600 hover:text-green-600 hover:bg-green-50 rounded-lg transition-colors"
                      title="启用用户"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
            <template v-else>
              <tr>
                <td colspan="6" class="px-6 py-24 text-center">
                  <div class="flex flex-col items-center justify-center">
                    <div class="w-24 h-24 bg-gray-50 rounded-full flex items-center justify-center mb-4">
                      <svg class="w-12 h-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                      </svg>
                    </div>
                    <h3 class="text-lg font-medium text-gray-900">未找到用户</h3>
                    <p class="text-gray-500 mt-1">尝试调整搜索关键词或清除筛选条件</p>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <Pagination 
        v-model:current="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        @change="fetchUsers"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi, type User } from '@/api/admin'
import { useToast } from '@/composables/useToast'
import Pagination from '@/components/common/Pagination.vue'

const toast = useToast()
const users = ref<User[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const keyword = ref('')

const fetchUsers = async () => {
  try {
    const res = await adminApi.getUsers({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value
    })
    if (res.data.code === 200) {
      users.value = res.data.data.records
      total.value = res.data.data.total
      totalPages.value = res.data.data.pages
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    toast.error('获取用户列表失败')
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const promoteUser = async (user: User) => {
  if (!confirm(`确定要将用户 ${user.displayName} 设为管理员吗？`)) return
  try {
    const res = await adminApi.updateUserRole(user.userId, 'ADMIN')
    if (res.data.code === 200) {
      toast.success('操作成功')
      fetchUsers()
    }
  } catch (error) {
    toast.error('操作失败')
  }
}

const demoteUser = async (user: User) => {
  if (!confirm(`确定要取消用户 ${user.displayName} 的管理员权限吗？`)) return
  try {
    const res = await adminApi.updateUserRole(user.userId, 'USER')
    if (res.data.code === 200) {
      toast.success('操作成功')
      fetchUsers()
    }
  } catch (error) {
    toast.error('操作失败')
  }
}

const banUser = async (user: User) => {
  if (!confirm(`确定要禁用用户 ${user.displayName} 吗？`)) return
  try {
    const res = await adminApi.updateUserStatus(user.userId, 0)
    if (res.data.code === 200) {
      toast.success('操作成功')
      fetchUsers()
    }
  } catch (error) {
    toast.error('操作失败')
  }
}

const unbanUser = async (user: User) => {
  try {
    const res = await adminApi.updateUserStatus(user.userId, 1)
    if (res.data.code === 200) {
      toast.success('操作成功')
      fetchUsers()
    }
  } catch (error) {
    toast.error('操作失败')
  }
}

onMounted(() => {
  fetchUsers()
})
</script>
