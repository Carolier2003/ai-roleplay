<template>
  <div class="space-y-6">
    <!-- Header Card -->
    <div class="bg-white/80 backdrop-blur-xl rounded-2xl shadow-sm border border-white/20 p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-all duration-300 hover:shadow-md">
      <div class="flex items-center gap-4">
        <div class="p-2 bg-indigo-50 rounded-lg">
          <svg class="w-6 h-6 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
        <h3 class="text-xl font-bold text-gray-900">知识库管理</h3>
        <div class="h-6 w-px bg-gray-200 mx-2"></div>
        <div class="relative">
          <select 
            v-model="selectedCharacterId" 
            @change="handleCharacterChange"
            class="appearance-none pl-4 pr-10 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm font-medium text-gray-700 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all cursor-pointer hover:bg-white hover:shadow-sm"
          >
            <option :value="undefined">📚 所有角色</option>
            <option v-for="char in characters" :key="char.id" :value="char.id">
              {{ char.name }}
            </option>
          </select>
          <div class="absolute inset-y-0 right-0 flex items-center px-2 pointer-events-none text-gray-500">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
            </svg>
          </div>
        </div>
      </div>
      
      <div class="flex items-center gap-3">
        <div class="relative group">
          <input 
            v-model="keyword" 
            @keyup.enter="fetchKnowledge"
            type="text" 
            placeholder="搜索知识..." 
            class="pl-10 pr-4 py-2 w-64 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
          />
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg class="w-5 h-5 text-gray-400 group-focus-within:text-indigo-500 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
        </div>
        
        <button 
          @click="fetchKnowledge"
          class="px-4 py-2 bg-white border border-gray-200 text-gray-700 rounded-xl hover:bg-gray-50 hover:border-gray-300 transition-all text-sm font-medium shadow-sm"
        >
          搜索
        </button>
        
        <button 
          @click="openCreateModal"
          class="px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-all shadow-sm hover:shadow-indigo-500/30 text-sm font-medium flex items-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          添加知识
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
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">标题</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">类型</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">重要性</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">所属角色</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="item in knowledgeList" :key="item.id" class="group hover:bg-indigo-50/30 transition-colors duration-200">
              <td class="px-6 py-4 font-medium text-gray-900 max-w-xs truncate" :title="item.title">
                {{ item.title }}
              </td>
              <td class="px-6 py-4">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-600/20">
                  {{ item.knowledgeType }}
                </span>
              </td>
              <td class="px-6 py-4">
                <div class="flex items-center gap-1">
                  <span class="text-yellow-400">★</span>
                  <span class="text-sm text-gray-600">{{ item.importanceScore }}</span>
                </div>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">
                {{ getCharacterName(item.characterId) }}
              </td>
              <td class="px-6 py-4">
                <span 
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ring-1 ring-inset"
                  :class="item.vectorId ? 'bg-green-50 text-green-700 ring-green-600/20' : 'bg-gray-50 text-gray-700 ring-gray-600/20'"
                >
                  <span class="w-1.5 h-1.5 rounded-full mr-1.5" :class="item.vectorId ? 'bg-green-600' : 'bg-gray-500'"></span>
                  {{ item.vectorId ? '已向量化' : '未向量化' }}
                </span>
              </td>
              <td class="px-6 py-4 text-right">
                <div class="flex justify-end items-center gap-2 opacity-0 group-hover:opacity-100 transition-all duration-200 transform translate-x-2 group-hover:translate-x-0">
                  <button 
                    @click="editKnowledge(item)"
                    class="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    title="编辑"
                  >
                    <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button 
                    @click="deleteKnowledge(item)"
                    class="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    title="删除"
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

      <!-- Pagination -->
      <div class="px-6 py-4 border-t border-gray-100 bg-gray-50/50 flex justify-between items-center">
        <div class="text-sm text-gray-500">
          共 <span class="font-medium text-gray-900">{{ total }}</span> 条记录
        </div>
        <div class="flex gap-2">
          <button 
            :disabled="currentPage === 1"
            @click="changePage(currentPage - 1)"
            class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 hover:text-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm"
          >
            上一页
          </button>
          <div class="flex items-center gap-1 px-2">
            <span class="text-sm font-medium text-indigo-600 bg-indigo-50 px-3 py-1 rounded-md">{{ currentPage }}</span>
            <span class="text-sm text-gray-400">/</span>
            <span class="text-sm text-gray-600 px-2">{{ totalPages }}</span>
          </div>
          <button 
            :disabled="currentPage === totalPages"
            @click="changePage(currentPage + 1)"
            class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 hover:text-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- Edit/Create Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white p-6 rounded-lg w-[600px] max-h-[90vh] overflow-y-auto">
        <h3 class="text-lg font-bold mb-4">{{ isEditing ? '编辑知识' : '添加知识' }}</h3>
        
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">所属角色</label>
            <select 
              v-model="form.characterId" 
              class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-purple-500"
              :disabled="isEditing"
            >
              <option v-for="char in characters" :key="char.id" :value="char.id">
                {{ char.name }}
              </option>
            </select>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">标题</label>
            <input 
              v-model="form.title" 
              type="text" 
              class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-purple-500"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">内容</label>
            <textarea 
              v-model="form.content" 
              rows="6"
              class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-purple-500"
            ></textarea>
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">类型</label>
              <select 
                v-model="form.knowledgeType" 
                class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-purple-500"
              >
                <option value="PERSONALITY">性格特征</option>
                <option value="BASIC_INFO">基本信息</option>
                <option value="KNOWLEDGE">专业知识</option>
                <option value="EVENTS">重要事件</option>
                <option value="RELATIONSHIPS">人际关系</option>
                <option value="ABILITIES">能力技能</option>
                <option value="QUOTES">经典语录</option>
              </select>
            </div>
            
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">重要性 (1-10)</label>
              <input 
                v-model.number="form.importanceScore" 
                type="number" 
                min="1" 
                max="10"
                class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-purple-500"
              />
            </div>
          </div>
        </div>
        
        <div class="flex justify-end gap-3 mt-6">
          <button 
            @click="showModal = false"
            class="px-4 py-2 bg-gray-200 rounded hover:bg-gray-300"
          >
            取消
          </button>
          <button 
            @click="saveKnowledge"
            class="px-4 py-2 bg-purple-600 text-white rounded hover:bg-purple-700"
          >
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getCharacterList, type Character } from '@/api/chat'
import { adminApi } from '@/api/admin'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const characters = ref<Character[]>([])
const knowledgeList = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const keyword = ref('')
const selectedCharacterId = ref<number | undefined>(undefined)

const showModal = ref(false)
const isEditing = ref(false)
const form = ref({
  id: undefined as number | undefined,
  characterId: undefined as number | undefined,
  title: '',
  content: '',
  knowledgeType: 'KNOWLEDGE',
  importanceScore: 5
})

const fetchCharacters = async () => {
  try {
    characters.value = await getCharacterList()
    // 如果没有选中的角色，默认选中第一个
    if (selectedCharacterId.value === undefined && characters.value.length > 0) {
      selectedCharacterId.value = characters.value[0].id
      fetchKnowledge()
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

const fetchKnowledge = async () => {
  try {
    const res = await adminApi.getKnowledgeList({
      characterId: selectedCharacterId.value,
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value
    })
    // 后端返回的是 Map { success: true, data: ... } 而不是标准 ApiResponse
    if (res.data.code === 200 || res.data.success) {
      const data = res.data.data
      // 兼容 MyBatis-Plus Page 对象结构
      if (data.records) {
        knowledgeList.value = data.records
        total.value = data.total
        totalPages.value = data.pages
      } else {
        // 兼容直接返回列表的情况 (虽然这里应该是 Page)
        knowledgeList.value = Array.isArray(data) ? data : []
        total.value = knowledgeList.value.length
        totalPages.value = 1
      }
    }
  } catch (error) {
    console.error('获取知识列表失败:', error)
    toast.error('获取知识列表失败')
  }
}

const handleCharacterChange = () => {
  currentPage.value = 1
  fetchKnowledge()
}

const changePage = (page: number) => {
  currentPage.value = page
  fetchKnowledge()
}

const getCharacterName = (id: number) => {
  const char = characters.value.find(c => c.id === id)
  return char ? char.name : `ID:${id}`
}

const openCreateModal = () => {
  isEditing.value = false
  form.value = {
    id: undefined,
    characterId: selectedCharacterId.value || (characters.value.length > 0 ? characters.value[0].id : undefined),
    title: '',
    content: '',
    knowledgeType: 'KNOWLEDGE',
    importanceScore: 5
  }
  showModal.value = true
}

const editKnowledge = (item: any) => {
  isEditing.value = true
  form.value = {
    id: item.id,
    characterId: item.characterId,
    title: item.title,
    content: item.content,
    knowledgeType: item.knowledgeType,
    importanceScore: item.importanceScore
  }
  showModal.value = true
}

const saveKnowledge = async () => {
  if (!form.value.title || !form.value.content) {
    toast.warning('请填写标题和内容')
    return
  }
  
  try {
    if (isEditing.value) {
      const res = await adminApi.updateKnowledge(form.value.id!, form.value)
      if (res.data.code === 200) {
        toast.success('更新成功')
        showModal.value = false
        fetchKnowledge()
      }
    } else {
      const res = await adminApi.addKnowledge({
        characterId: form.value.characterId!,
        knowledge: form.value
      })
      if (res.data.code === 200) {
        toast.success('添加成功')
        showModal.value = false
        fetchKnowledge()
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    toast.error('保存失败')
  }
}

const deleteKnowledge = async (item: any) => {
  if (!confirm(`确定要删除知识 "${item.title}" 吗？`)) return
  try {
    const res = await adminApi.deleteKnowledge(item.id)
    if (res.data.code === 200) {
      toast.success('删除成功')
      fetchKnowledge()
    }
  } catch (error) {
    toast.error('删除失败')
  }
}

onMounted(() => {
  fetchCharacters()
})
</script>
