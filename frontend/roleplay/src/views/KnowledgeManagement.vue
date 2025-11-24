<template>
  <div class="min-h-screen bg-gray-50 py-8 px-4">
    <div class="max-w-4xl mx-auto">
      <!-- 页面标题 -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">📚 知识库管理</h1>
        <p class="text-gray-600">上传JSON文件导入RAG知识库，无需Python脚本</p>
      </div>

      <!-- 角色选择卡片 -->
      <div class="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4">选择角色</h2>
        <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
          <button
            v-for="char in characters"
            :key="char.id"
            @click="selectedCharacterId = char.id"
            :class="[
              'px-4 py-3 rounded-lg border-2 transition-all',
              selectedCharacterId === char.id
                ? 'border-blue-500 bg-blue-50 text-blue-700'
                : 'border-gray-200 hover:border-gray-300'
            ]"
          >
            <div class="font-medium">{{ char.name }}</div>
            <div class="text-xs text-gray-500 mt-1">ID: {{ char.id }}</div>
          </button>
        </div>
      </div>

      <!-- 文件上传区域 -->
      <div class="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4">上传JSON文件</h2>
        
        <div
          @drop.prevent="handleDrop"
          @dragover.prevent
          @dragenter.prevent
          :class="[
            'border-2 border-dashed rounded-lg p-8 text-center transition-all',
            isDragging
              ? 'border-blue-500 bg-blue-50'
              : 'border-gray-300 hover:border-gray-400'
          ]"
        >
          <input
            ref="fileInput"
            type="file"
            accept=".json"
            @change="handleFileSelect"
            class="hidden"
          />
          
          <div class="mb-4">
            <svg
              class="mx-auto h-12 w-12 text-gray-400"
              stroke="currentColor"
              fill="none"
              viewBox="0 0 48 48"
            >
              <path
                d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          
          <p class="text-gray-600 mb-2">
            拖拽JSON文件到此处，或
            <button
              @click="$refs.fileInput.click()"
              class="text-blue-600 hover:text-blue-700 underline"
            >
              点击选择文件
            </button>
          </p>
          
          <p class="text-sm text-gray-500">
            支持标准JSON数组格式或单个JSON对象
          </p>
          
          <div v-if="selectedFile" class="mt-4 p-3 bg-gray-50 rounded">
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <svg class="h-5 w-5 text-green-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                </svg>
                <span class="font-medium">{{ selectedFile.name }}</span>
                <span class="text-gray-500 ml-2">({{ formatFileSize(selectedFile.size) }})</span>
              </div>
              <button
                @click="selectedFile = null"
                class="text-red-500 hover:text-red-700"
              >
                移除
              </button>
            </div>
          </div>
        </div>

        <!-- 导入按钮 -->
        <div class="mt-6 flex justify-end">
          <button
            @click="handleImport"
            :disabled="!selectedFile || !selectedCharacterId || isImporting"
            :class="[
              'px-6 py-2 rounded-lg font-medium transition-all',
              !selectedFile || !selectedCharacterId || isImporting
                ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                : 'bg-blue-600 text-white hover:bg-blue-700'
            ]"
          >
            <span v-if="isImporting" class="flex items-center">
              <svg class="animate-spin -ml-1 mr-3 h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              导入中...
            </span>
            <span v-else>🚀 开始导入</span>
          </button>
        </div>
      </div>

      <!-- 导入结果 -->
      <div v-if="importResult" class="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4">导入结果</h2>
        <div
          :class="[
            'p-4 rounded-lg',
            importResult.success ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'
          ]"
        >
          <div class="flex items-start">
            <svg
              v-if="importResult.success"
              class="h-6 w-6 text-green-500 mr-3 mt-0.5"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
            </svg>
            <svg
              v-else
              class="h-6 w-6 text-red-500 mr-3 mt-0.5"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
            </svg>
            <div class="flex-1">
              <p :class="['font-medium', importResult.success ? 'text-green-800' : 'text-red-800']">
                {{ importResult.message }}
              </p>
              <div v-if="importResult.success" class="mt-2 text-sm text-green-700">
                <p>✅ 成功导入 {{ importResult.imported_count }} 条知识</p>
                <p v-if="importResult.filename">📄 文件: {{ importResult.filename }}</p>
                <p v-if="importResult.character_id">👤 角色ID: {{ importResult.character_id }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 使用说明 -->
      <div class="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 class="text-lg font-semibold text-blue-900 mb-3">📖 使用说明</h3>
        <ul class="space-y-2 text-blue-800 text-sm">
          <li>✅ 支持标准JSON数组格式：<code class="bg-blue-100 px-1 rounded">[{...}, {...}]</code></li>
          <li>✅ 支持单个JSON对象格式：<code class="bg-blue-100 px-1 rounded">{...}</code></li>
          <li>✅ 自动识别常见字段：title、content、name、description等</li>
          <li>✅ 支持泰拉瑞亚特殊格式：武器名、工具名、NPC名称等</li>
          <li>✅ 文件大小限制：建议不超过10MB</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { importKnowledgeFromFile, type KnowledgeImportResult } from '@/api/knowledge'
import { useToast } from '@/composables/useToast'

const toast = useToast()

// 角色列表
const characters = [
  { id: 1, name: '哈利·波特' },
  { id: 2, name: '苏格拉底' },
  { id: 3, name: '爱因斯坦' },
  { id: 4, name: '江户川柯南' },
  { id: 5, name: '泰拉瑞亚向导' }
]

// 状态管理
const selectedCharacterId = ref<number | null>(null)
const selectedFile = ref<File | null>(null)
const isDragging = ref(false)
const isImporting = ref(false)
const importResult = ref<KnowledgeImportResult | null>(null)

// 文件选择处理
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    selectedFile.value = target.files[0]
  }
}

// 拖拽处理
const handleDrop = (event: DragEvent) => {
  isDragging.value = false
  if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
    const file = event.dataTransfer.files[0]
    if (file.name.toLowerCase().endsWith('.json')) {
            selectedFile.value = file
    } else {
      toast.error('只支持JSON文件格式')
    }
  }
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 导入处理
const handleImport = async () => {
  if (!selectedFile.value || !selectedCharacterId.value) {
    toast.error('请选择文件和角色')
    return
  }

  isImporting.value = true
  importResult.value = null

  try {
    const result = await importKnowledgeFromFile(selectedFile.value, selectedCharacterId.value)
    importResult.value = result

    if (result.success) {
      toast.success(`成功导入 ${result.imported_count} 条知识`)
      // 清空文件选择
      selectedFile.value = null
      if (document.querySelector('input[type="file"]')) {
        (document.querySelector('input[type="file"]') as HTMLInputElement).value = ''
      }
    } else {
      toast.error(result.message || '导入失败')
    }
  } catch (error: any) {
    console.error('导入失败:', error)
    importResult.value = {
      success: false,
      message: error.response?.data?.message || error.message || '导入失败，请检查网络连接'
    }
    toast.error('导入失败: ' + (error.response?.data?.message || error.message))
  } finally {
    isImporting.value = false
  }
}
</script>
