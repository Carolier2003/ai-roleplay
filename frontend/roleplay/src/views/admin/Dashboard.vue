<template>
  <div class="space-y-6">
    <!-- Welcome Section -->
    <div class="relative overflow-hidden rounded-2xl bg-gradient-to-r from-purple-600 to-blue-600 p-8 text-white shadow-lg">
      <div class="relative z-10">
        <h2 class="text-3xl font-bold mb-2">欢迎回来, 管理员</h2>
        <p class="text-purple-100 text-lg opacity-90">这里是系统的控制中心，您可以管理用户、角色和知识库。</p>
      </div>
      <!-- Decorative circles -->
      <div class="absolute top-0 right-0 -mr-16 -mt-16 w-64 h-64 rounded-full bg-white opacity-10 blur-3xl"></div>
      <div class="absolute bottom-0 left-0 -ml-16 -mb-16 w-48 h-48 rounded-full bg-white opacity-10 blur-2xl"></div>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- Users Stat -->
      <div class="bg-white/80 backdrop-blur-xl rounded-2xl p-6 border border-white/20 shadow-sm hover:shadow-md transition-all duration-300 group">
        <div class="flex justify-between items-start">
          <div>
            <p class="text-sm font-medium text-gray-500">总用户数</p>
            <h3 class="text-3xl font-bold text-gray-900 mt-2 group-hover:text-purple-600 transition-colors">
              {{ stats.userCount }}
            </h3>
          </div>
          <div class="p-3 bg-purple-50 rounded-xl group-hover:bg-purple-100 transition-colors">
            <svg class="w-8 h-8 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Characters Stat -->
      <div class="bg-white/80 backdrop-blur-xl rounded-2xl p-6 border border-white/20 shadow-sm hover:shadow-md transition-all duration-300 group">
        <div class="flex justify-between items-start">
          <div>
            <p class="text-sm font-medium text-gray-500">AI 角色</p>
            <h3 class="text-3xl font-bold text-gray-900 mt-2 group-hover:text-blue-600 transition-colors">
              {{ stats.characterCount }}
            </h3>
          </div>
          <div class="p-3 bg-blue-50 rounded-xl group-hover:bg-blue-100 transition-colors">
            <svg class="w-8 h-8 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Knowledge Stat -->
      <div class="bg-white/80 backdrop-blur-xl rounded-2xl p-6 border border-white/20 shadow-sm hover:shadow-md transition-all duration-300 group">
        <div class="flex justify-between items-start">
          <div>
            <p class="text-sm font-medium text-gray-500">知识库条目</p>
            <h3 class="text-3xl font-bold text-gray-900 mt-2 group-hover:text-indigo-600 transition-colors">
              {{ stats.knowledgeCount }}
            </h3>
          </div>
          <div class="p-3 bg-indigo-50 rounded-xl group-hover:bg-indigo-100 transition-colors">
            <svg class="w-8 h-8 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- Charts & Quick Actions -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Knowledge Distribution Chart -->
      <div class="lg:col-span-2 bg-white/80 backdrop-blur-xl rounded-2xl shadow-sm border border-white/20 p-6">
        <h3 class="text-lg font-bold text-gray-900 mb-4 flex items-center">
          <span class="w-1 h-6 bg-indigo-500 rounded-full mr-3"></span>
          知识库分布
        </h3>
        <div ref="chartRef" class="w-full h-[300px]"></div>
      </div>

      <!-- Quick Links -->
      <div class="bg-white/80 backdrop-blur-xl rounded-2xl shadow-sm border border-white/20 p-6">
        <h3 class="text-lg font-bold text-gray-900 mb-4 flex items-center">
          <span class="w-1 h-6 bg-blue-500 rounded-full mr-3"></span>
          快速操作
        </h3>
        <div class="grid grid-cols-1 gap-4">
          <router-link 
            to="/admin/users"
            class="flex items-center p-4 bg-gray-50 rounded-xl hover:bg-purple-50 hover:text-purple-600 transition-all duration-300 group border border-transparent hover:border-purple-200"
          >
            <div class="mr-4 p-2 bg-white rounded-lg shadow-sm group-hover:shadow-md transition-all">
              <svg class="w-6 h-6 text-purple-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </div>
            <span class="font-medium">用户管理</span>
            <span class="ml-auto text-gray-400 group-hover:text-purple-400">→</span>
          </router-link>
          
          <router-link 
            to="/admin/characters"
            class="flex items-center p-4 bg-gray-50 rounded-xl hover:bg-blue-50 hover:text-blue-600 transition-all duration-300 group border border-transparent hover:border-blue-200"
          >
            <div class="mr-4 p-2 bg-white rounded-lg shadow-sm group-hover:shadow-md transition-all">
              <svg class="w-6 h-6 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <span class="font-medium">角色管理</span>
            <span class="ml-auto text-gray-400 group-hover:text-blue-400">→</span>
          </router-link>
          
          <router-link 
            to="/admin/knowledge"
            class="flex items-center p-4 bg-gray-50 rounded-xl hover:bg-indigo-50 hover:text-indigo-600 transition-all duration-300 group border border-transparent hover:border-indigo-200"
          >
            <div class="mr-4 p-2 bg-white rounded-lg shadow-sm group-hover:shadow-md transition-all">
              <svg class="w-6 h-6 text-indigo-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <span class="font-medium">知识库管理</span>
            <span class="ml-auto text-gray-400 group-hover:text-indigo-400">→</span>
          </router-link>

          <a 
            href="/"
            class="flex items-center p-4 bg-gray-50 rounded-xl hover:bg-green-50 hover:text-green-600 transition-all duration-300 group border border-transparent hover:border-green-200"
          >
            <div class="mr-4 p-2 bg-white rounded-lg shadow-sm group-hover:shadow-md transition-all">
              <svg class="w-6 h-6 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <span class="font-medium">返回聊天</span>
            <span class="ml-auto text-gray-400 group-hover:text-green-400">→</span>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { adminApi, type AdminStatsResponse } from '@/api/admin'
import * as echarts from 'echarts'

const stats = ref<AdminStatsResponse>({
  userCount: 0,
  characterCount: 0,
  knowledgeCount: 0,
  knowledgeDistribution: []
})

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const data = stats.value.knowledgeDistribution.map(item => ({
    name: item.characterName,
    value: item.count
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      type: 'scroll'
    },
    series: [
      {
        name: '知识库分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  }

  chartInstance.setOption(option)
}

const fetchStats = async () => {
  try {
    const res = await adminApi.getStats()
    if (res.data.code === 200) {
      stats.value = res.data.data
      initChart()
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>
