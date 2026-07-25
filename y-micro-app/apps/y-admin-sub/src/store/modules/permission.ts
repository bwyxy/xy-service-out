import { defineStore } from 'pinia'
import { getRouters } from '@/api/login'
import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'

// 后端组件路径 -> 前端组件映射
const componentMap: Record<string, any> = {
  'Layout': () => import('@/layout/index.vue'),
  'ParentView': () => import('@/layout/index.vue'),
  'system/menu/index': () => import('@/views/system/menu/index.vue'),
}

// 将后端 RouterVo 转为 Vue Router 路由
function filterAsyncRouter(routers: any[]): RouteRecordRaw[] {
  const res: RouteRecordRaw[] = []
  routers.forEach((router) => {
    const route: any = {
      path: router.path,
      name: router.name,
      hidden: router.hidden,
      meta: {
        title: router.meta?.title || '',
        icon: router.meta?.icon || '',
        noCache: router.meta?.noCache || false,
        link: router.meta?.link || null,
      },
    }

    if (router.component) {
      const componentKey = router.component
      if (componentKey === 'Layout') {
        route.component = Layout
      } else if (componentMap[componentKey]) {
        route.component = componentMap[componentKey]
      }
    }

    if (router.redirect) {
      route.redirect = router.redirect
    }
    if (router.alwaysShow) {
      route.meta.alwaysShow = true
    }

    if (router.children && router.children.length > 0) {
      route.children = filterAsyncRouter(router.children)
    }

    res.push(route as RouteRecordRaw)
  })
  return res
}

const usePermissionStore = defineStore('permission', {
  state() {
    return {
      routes: [] as RouteRecordRaw[],
      addRoutes: [] as RouteRecordRaw[],
    }
  },
  actions: {
    async generateRoutes() {
      const res = await getRouters()
      const routers = res.data || []
      const accessedRoutes = filterAsyncRouter(routers)
      this.addRoutes = accessedRoutes
      this.routes = [
        ...accessedRoutes,
      ]
      return accessedRoutes
    },
  },
})

export default usePermissionStore