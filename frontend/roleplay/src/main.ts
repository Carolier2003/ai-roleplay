import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// 通用字体和样式
import './style.css'

console.log('[main.ts] 开始创建Vue应用')

const app = createApp(App)

console.log('[main.ts] Vue应用创建成功')

const pinia = createPinia()
app.use(pinia)
console.log('[main.ts] Pinia已安装')

app.use(router)
console.log('[main.ts] Router已安装')

// 初始化 stores
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'

// 初始化认证状态
const authStore = useAuthStore()
authStore.initAuth()
console.log('[main.ts] 认证状态已初始化')

// 初始化聊天状态
const chatStore = useChatStore()
chatStore.initLastCharacter()
console.log('[main.ts] 聊天状态已初始化')

// 加载角色列表
chatStore.loadCharacters().then(() => {
  console.log('[main.ts] 角色列表加载完成')
}).catch((error) => {
  console.error('[main.ts] 角色列表加载失败:', error)
})
console.log('[main.ts] 开始加载角色列表')

console.log('[main.ts] 准备挂载应用到 #app')
app.mount('#app')
console.log('[main.ts] 应用挂载完成')
