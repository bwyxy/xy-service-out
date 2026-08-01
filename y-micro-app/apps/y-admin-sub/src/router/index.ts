import { createWebHashHistory, createRouter, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'
import usePermissionStore from '@/store/modules/permission'

// 静态路由（仿若依：只包含登录和 404，根路径 / 由后端动态路由生成）
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
    hidden: true,
  } as RouteRecordRaw,
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    hidden: true,
  } as RouteRecordRaw,
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

const whiteList = ['/login']

router.beforeEach(async (to, _from, next) => {
  const token = getToken()

  if (token) {
    if (to.path === '/login') {
      // 已登录，跳转到首页
      next({ path: '/' })
      return
    }

    const userStore = useUserStore()
    const permissionStore = usePermissionStore()

    // 如果用户信息不存在，获取用户信息并生成路由
    if (!userStore.userInfo || Object.keys(userStore.userInfo).length === 0) {
      try {
        // 1. 获取用户信息
        await userStore.getInfo()

        // 2. 生成动态路由
        const accessRoutes = await permissionStore.generateRoutes()

        // 3. 动态添加路由（仿若依：直接 addRoute 到根级别）
        accessRoutes.forEach((route) => {
          router.addRoute(route)
        })

        // 4. 重新导航到目标路径（replace: true 避免产生历史记录）
        next({ ...to, replace: true })
      } catch (error) {
        // 获取用户信息失败，清除 token 并跳转登录页
        console.error('获取用户信息失败:', error)
        await userStore.logout()
        next(`/login?redirect=${to.fullPath}`)
      }
    } else {
      // 用户信息已存在，直接放行
      next()
    }
  } else {
    // 未登录
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`)
    }
  }
})

export default router