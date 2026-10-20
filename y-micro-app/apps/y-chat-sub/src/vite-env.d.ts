/// <reference types="vite/client" />


declare module '@xy/qiankun-communication'

declare module '@xy/utils'

declare module 'js-cookie'

declare module '@tauri-apps/plugin-opener'

/** 构建时由 vite define 注入：当前前端版本号，来源 package.json */
declare const __APP_VERSION__: string

/** 构建时由 vite define 注入：当前前端版本码，由版本号换算（1.2.3 -> 10203） */
declare const __APP_VERSION_CODE__: number
