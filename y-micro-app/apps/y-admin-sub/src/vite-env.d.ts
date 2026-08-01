/// <reference types="vite/client" />


declare module '@xy/qiankun-communication'

declare module 'js-cookie'

declare module '@xy/utils'

// 扩展 vue-router 类型，支持仿若依的自定义路由属性
import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    noCache?: boolean
    link?: string | null
    hidden?: boolean
    alwaysShow?: boolean
  }
}
