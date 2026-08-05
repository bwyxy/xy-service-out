import { defineStore } from 'pinia'
import { getRouters } from '@/api/login'
import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'
// @ts-ignore
import ParentView from '@/layout/ParentView.vue'

// 后端组件路径 -> 前端组件映射（显式覆盖）
const componentMap: Record<string, any> = {
  'system/menu/index': () => import('@/views/system/menu/index.vue'),
}

// 使用 Vite 的 import.meta.glob 预构建所有视图模块，避免运行时动态 import 失败
// 注意：key 是相对于当前文件的路径，如 ../../views/dashboard/index.vue
const modules = import.meta.glob('./../../views/**/*.vue')

/**
 * 加载组件（仿若依 loadView）
 * @param componentPath 后端返回的组件路径，如 "system/menu/index"
 */
const loadView = (componentPath: string) => {
  // 1. 先查显式映射
  if (componentMap[componentPath]) {
    return componentMap[componentPath]
  }
  // 2. 通过 import.meta.glob 查找：按 views/ 截取路径匹配
  for (const path in modules) {
    const dir = path.split('views/')[1]?.split('.vue')[0]
    if (dir === componentPath) {
      return modules[path]
    }
  }
  return undefined
}

/**
 * 将后端 RouterVo 转为 Vue Router 路由（仿若依 filterAsyncRouter）
 */
function filterAsyncRouter(routerList: any[]): RouteRecordRaw[] {
  const res: RouteRecordRaw[] = []

  routerList.forEach((router) => {
    const route: any = {
      path: router.path,
      name: router.name,
      hidden: router.hidden,
      meta: {
        title: router.meta?.title || '',
        icon: router.meta?.icon || '',
        noCache: router.meta?.noCache || false,
        link: router.meta?.link || null,
        hidden: router.hidden,
      },
    }

    // 处理 redirect：noRedirect 表示不跳转，设置为 undefined
    if (router.redirect && router.redirect !== 'noRedirect') {
      route.redirect = router.redirect
    }

    // 处理组件
    if (router.component) {
      if (router.component === 'Layout') {
        route.component = Layout
      } else if (router.component === 'ParentView') {
        route.component = ParentView
      } else if (router.component === 'InnerLink') {
        // @ts-ignore
        route.component = () => import('@/layout/InnerLink.vue')
      } else {
        route.component = loadView(router.component)
      }
    }

    // 递归处理子路由
    if (router.children && router.children.length > 0) {
      route.children = filterAsyncRouter(router.children)
    }

    // alwaysShow
    if (router.alwaysShow) {
      route.meta.alwaysShow = true
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
      this.routes = [...accessedRoutes]
      return accessedRoutes
    },
  },
})

export default usePermissionStore