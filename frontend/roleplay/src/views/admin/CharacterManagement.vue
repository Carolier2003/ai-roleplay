<template>
  <div class="space-y-6">
    <!-- Header Card -->
    <div class="bg-white/80 backdrop-blur-xl rounded-2xl shadow-sm border border-white/20 p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-all duration-300 hover:shadow-md">
      <div class="flex items-center gap-4">
        <div class="p-2 bg-blue-50 rounded-lg">
          <svg class="w-6 h-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div>
          <h3 class="text-xl font-bold text-gray-900">角色管理</h3>
          <p class="text-gray-500 text-sm mt-1">管理 AI 角色及其配置</p>
        </div>
      </div>
      
      <button 
        @click="showCreateModal = true"
        class="px-4 py-2 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-all shadow-sm hover:shadow-blue-500/30 text-sm font-medium flex items-center gap-2"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        新建角色
      </button>
    </div>

    <!-- Content Card -->
    <div class="bg-white/80 backdrop-blur-xl rounded-2xl shadow-sm border border-white/20 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">角色</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">描述</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="char in characters" :key="char.id" class="group hover:bg-blue-50/30 transition-colors duration-200">
              <td class="px-6 py-4">
                <div class="flex items-center gap-4">
                  <div class="relative">
                    <div class="absolute -inset-0.5 bg-gradient-to-r from-blue-600 to-cyan-600 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-200 blur-[2px]"></div>
                    <img 
                      :src="char.avatarUrl" 
                      class="relative w-12 h-12 rounded-full bg-gray-100 object-cover ring-2 ring-white shadow-sm"
                      alt="Avatar"
                    />
                  </div>
                  <div>
                    <div class="font-bold text-gray-900 group-hover:text-blue-700 transition-colors">{{ char.name }}</div>
                    <div class="text-xs text-gray-400 font-mono mt-0.5">ID: {{ char.id }}</div>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4">
                <p class="text-sm text-gray-600 max-w-md line-clamp-2 leading-relaxed">
                  {{ char.description || '暂无描述' }}
                </p>
              </td>
              <td class="px-6 py-4">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-50 text-green-700 ring-1 ring-inset ring-green-600/20">
                  <span class="w-1.5 h-1.5 rounded-full bg-green-600 mr-1.5"></span>
                  已启用
                </span>
              </td>
              <td class="px-6 py-4 text-right">
                <div class="flex justify-end items-center gap-2">
                  <button 
                    @click="editCharacter(char)"
                    class="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    v-tooltip="'编辑'"
                  >
                    <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button 
                    @click="deleteCharacter(char)"
                    class="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    v-tooltip="'删除'"
                  >
                    <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create/Edit Modal (Placeholder) -->
    <div v-if="showCreateModal" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 transition-all duration-300">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 transform transition-all scale-100">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-xl font-bold text-gray-900">新建角色</h3>
          <button @click="showCreateModal = false" class="text-gray-400 hover:text-gray-500 transition-colors">
            <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <div class="flex flex-col items-center justify-center py-8 text-center">
          <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mb-4">
            <svg class="w-8 h-8 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <h4 class="text-lg font-medium text-gray-900 mb-2">功能开发中</h4>
          <p class="text-gray-500 text-sm">角色创建功能正在紧张开发中，敬请期待！</p>
        </div>

        <div class="flex justify-end mt-6">
          <button 
            @click="showCreateModal = false"
            class="px-4 py-2 bg-gray-100 text-gray-700 rounded-xl hover:bg-gray-200 transition-colors font-medium text-sm"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCharacterList, type Character } from '@/api/chat'
import { adminApi } from '@/api/admin'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const characters = ref<Character[]>([])
const showCreateModal = ref(false)

const fetchCharacters = async () => {
  try {
    characters.value = await getCharacterList()
  } catch (error) {
    console.error('获取角色列表失败:', error)
    toast.error('获取角色列表失败')
  }
}

const editCharacter = (char: Character) => {
  // TODO: Implement edit logic or navigate to edit page
  toast.info(`编辑角色: ${char.name} (功能开发中)`)
}

const deleteCharacter = async (char: Character) => {
  if (!confirm(`确定要删除角色 ${char.name} 吗？此操作不可恢复！`)) return
  try {
    await adminApi.deleteCharacter(char.id)
    toast.success('删除成功')
    fetchCharacters()
  } catch (error) {
    console.error('删除角色失败:', error)
    toast.error('删除失败')
  }
}

onMounted(() => {
  fetchCharacters()
})
</script>
