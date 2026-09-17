import { createWebHashHistory, createRouter, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'
import usePermissionStore from '@/store/modules/permission'
import { isHttp } from '@/utils/validate.ts'
import Layout from '@/layout/index.vue'

// 静态路由（只包含登录，根路径 / 与 404 由后端动态路由生成后追加）
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: Layout,
    children: [
      {
        path: '/',
        name: 'Index',
        component: () => import('@/views/home.vue'),
        meta: { title: '仪表盘', icon: 'ChromeFilled', affix: true },
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
    hidden: true,
  } as RouteRecordRaw,
  {
    path: '/index',
    redirect: '/',
    hidden: true,
  } as RouteRecordRaw,
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = []

// 404 兜底路由，必须在动态路由添加后再注册
const notFoundRoute: RouteRecordRaw = {
  path: '/:pathMatch(.*)*',
  name: 'NotFound',
  component: () => import('@/views/error/404.vue'),
  hidden: true,
} as RouteRecordRaw

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

const whiteList = ['/login']

router.beforeEach(async (to, _from) => {
  const token = getToken()

  if (token) {
    if (to.path === '/login') {
      // 已登录，跳转到首页
      return { path: '/' }
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
        // @ts-ignore
        accessRoutes.forEach((route) => {
          if (!isHttp(route.path)) {
            router.addRoute(route)
          }
        })

        // 4. 注册 404 兜底路由，必须放在动态路由之后
        router.addRoute(notFoundRoute)

        console.log(accessRoutes)

        // 5. 重新导航到目标路径（replace: true 避免产生历史记录）
        return { ...to, replace: true }
      } catch (error) {
        // 获取用户信息失败，清除 token 并跳转登录页
        console.error('获取用户信息失败:', error)
        await userStore.logout()
        return `/login?redirect=${to.fullPath}`
      }
    } else {
      // 用户信息已存在，直接放行
      return
    }
  } else {
    // 未登录
    if (whiteList.includes(to.path)) {
      return
    } else {
      return `/login?redirect=${to.fullPath}`
    }
  }
})

export default router