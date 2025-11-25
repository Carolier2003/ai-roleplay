<template>
  <div class="px-6 py-4 border-t border-gray-100 bg-gray-50/50 flex flex-col sm:flex-row justify-between items-center gap-4">
    <!-- Left: Info -->
    <div class="text-sm text-gray-500">
      显示第 <span class="font-medium text-gray-900">{{ total > 0 ? (current - 1) * pageSize + 1 : 0 }}</span> 到 <span class="font-medium text-gray-900">{{ Math.min(current * pageSize, total) }}</span> 条，共 <span class="font-medium text-gray-900">{{ total }}</span> 条记录
    </div>

    <!-- Right: Controls -->
    <div class="flex items-center gap-2">
      <!-- Previous -->
      <button 
        :disabled="current === 1"
        @click="changePage(current - 1)"
        class="p-2 rounded-lg border border-gray-200 bg-white text-gray-500 hover:bg-gray-50 hover:text-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm"
        title="上一页"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>

      <!-- Page Numbers -->
      <div class="hidden sm:flex items-center gap-1">
        <template v-for="(page, index) in displayedPages" :key="index">
          <!-- Ellipsis -->
          <span 
            v-if="page === '...'" 
            class="px-2 text-gray-400 select-none"
          >...</span>
          
          <!-- Page Number -->
          <button 
            v-else
            @click="changePage(page as number)"
            class="min-w-[32px] h-8 px-2 flex items-center justify-center rounded-lg text-sm font-medium transition-all duration-200"
            :class="current === page 
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/30 ring-2 ring-indigo-600 ring-offset-2' 
              : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50 hover:text-indigo-600 hover:border-indigo-200'"
          >
            {{ page }}
          </button>
        </template>
      </div>

      <!-- Mobile Page Indicator (Simple) -->
      <div class="sm:hidden flex items-center gap-1 px-2">
        <span class="text-sm font-medium text-indigo-600">{{ current }}</span>
        <span class="text-sm text-gray-400">/</span>
        <span class="text-sm text-gray-600">{{ totalPages }}</span>
      </div>

      <!-- Next -->
      <button 
        :disabled="current === totalPages || total === 0"
        @click="changePage(current + 1)"
        class="p-2 rounded-lg border border-gray-200 bg-white text-gray-500 hover:bg-gray-50 hover:text-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm"
        title="下一页"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </button>

      <!-- Page Size Selector (Optional improvement) -->
      <select 
        :value="pageSize"
        @change="changePageSize(($event.target as HTMLSelectElement).value)"
        class="ml-2 h-9 pl-2 pr-6 bg-white border border-gray-200 text-gray-600 text-sm rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all cursor-pointer"
      >
        <option :value="10">10条/页</option>
        <option :value="20">20条/页</option>
        <option :value="50">50条/页</option>
      </select>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  current: number
  pageSize: number
  total: number
}

const props = withDefaults(defineProps<Props>(), {
  current: 1,
  pageSize: 10,
  total: 0
})

const emit = defineEmits<{
  (e: 'update:current', page: number): void
  (e: 'update:pageSize', size: number): void
  (e: 'change', page: number): void
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

// Logic to generate page numbers with ellipsis
const displayedPages = computed(() => {
  const total = totalPages.value
  const current = props.current
  const delta = 2 // Number of pages to show around current page
  
  const range: (number | string)[] = []
  
  // Always show first page
  range.push(1)
  
  const left = current - delta
  const right = current + delta
  
  let l: number | null = null
  
  for (let i = 2; i < total; i++) {
    if (i >= left && i <= right) {
      // Add ellipsis before the range if needed
      if (l && i - l !== 1) {
        range.push('...')
      }
      range.push(i)
      l = i
    }
  }
  
  // Add ellipsis before last page if needed
  if (l && total - l !== 1 && total > 1) {
    range.push('...')
  }
  
  // Always show last page if it's not the first page
  if (total > 1) {
    range.push(total)
  }
  
  return range
})

const changePage = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  emit('update:current', page)
  emit('change', page)
}

const changePageSize = (sizeStr: string) => {
  const size = parseInt(sizeStr)
  emit('update:pageSize', size)
  // Reset to page 1 when changing page size
  emit('update:current', 1)
  emit('change', 1)
}
</script>
