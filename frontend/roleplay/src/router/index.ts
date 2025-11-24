import { createRouter, createWebHistory } from 'vue-router'
import Chat from '../views/Chat.vue'
import Login from '../views/Login.vue'
import KnowledgeManagement from '../views/KnowledgeManagement.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: () => {
        const lastCharacterId = localStorage.getItem('LAST_CHARACTER_ID')
        return `/chat/${lastCharacterId || '1'}`
      }
    },
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { requiresAuth: false }
    },
    {
      path: '/chat/:characterId',
      name: 'chat',
      component: Chat,
      props: route => ({ characterId: Number(route.params.characterId) }),  // ✅ 转换为数字
      meta: { requiresAuth: false } // ✅ 允许游客访问聊天页面
    },
    {
      path: '/knowledge',
      name: 'knowledge-management',
      component: KnowledgeManagement,
      meta: { requiresAuth: false }
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('../views/admin/Dashboard.vue')
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/UserManagement.vue')
        },
        {
          path: 'characters',
          name: 'admin-characters',
          component: () => import('../views/admin/CharacterManagement.vue')
        },
        {
          path: 'knowledge',
          name: 'admin-knowledge',
          component: () => import('../views/admin/KnowledgeManagement.vue')
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: () => {
        const lastCharacterId = localStorage.getItem('LAST_CHARACTER_ID')
        return `/chat/${lastCharacterId || '1'}`
      }
    }
  ],
})

// 获取最后访问的角色ID
const getLastCharacterId = (): number => {
  const lastCharacterId = localStorage.getItem('LAST_CHARACTER_ID')
  return lastCharacterId ? Number(lastCharacterId) : 1
}

// 保存最后访问的角色ID
const saveLastCharacterId = (characterId: number) => {
  localStorage.setItem('LAST_CHARACTER_ID', characterId.toString())
}

// 路由守卫：保存角色访问记录
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('ACCESS_TOKEN')
  const userInfo = localStorage.getItem('USER_INFO')
  const isAuthenticated = !!(token && userInfo)

  console.log('[router] 路由守卫检查:', {
    to: to.path,
    from: from.path,
    isAuthenticated
  })

  // 如果用户已登录但访问登录页面，重定向到最后访问的聊天页面
  if (to.path === '/login' && isAuthenticated) {
    const lastCharacterId = getLastCharacterId()
    console.log('[router] 用户已登录，从登录页面重定向到聊天页面:', lastCharacterId)
    next(`/chat/${lastCharacterId}`)
    return
  }

  // 保存当前访问的角色ID
  if (to.name === 'chat' && to.params.characterId) {
    const characterId = Number(to.params.characterId)
    if (!isNaN(characterId)) {
      saveLastCharacterId(characterId)
      console.log('[router] 保存最后访问的角色ID:', characterId)
    }
  }

  next()
})

export default router
