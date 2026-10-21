/// <reference types="vite/client" />
declare module "*.vue" {
  import type { DefineComponent } from "vue";
  // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/ban-types
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

// src/types/wangeditor.d.ts
declare module '@wangeditor/editor-for-vue' {
  import { DefineComponent } from 'vue'
  const Editor: DefineComponent
  const Toolbar: DefineComponent
  export { Editor, Toolbar }
}

declare module '@wangeditor/editor-for-vue'

interface ImportMetaEnv {
  VITE_NODE_ENV: string;
  VITE_BASE_API_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module '@xy/qiankun-communication'

declare module 'js-cookie'

declare module '@xy/utils'

declare module '@wangeditor/editor-for-vue' {
  import type { Component } from 'vue'
  export const Editor: Component
  export const Toolbar: Component
}

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
