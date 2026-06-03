/// <reference types="vite/client" />

interface R<T> {
    code: number
    data: T
    msg: string
    ok: boolean
    timestamp: number
}

declare module '*.vue' {
    import type { DefineComponent } from 'vue'
    const component: DefineComponent<{}, {}, any>
    export default component
}

declare module 'postcss-px-to-viewport'

declare module 'vue3-starry-sky'