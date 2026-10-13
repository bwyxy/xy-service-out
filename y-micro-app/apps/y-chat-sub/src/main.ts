import { createApp, type App } from 'vue'
import AppDom from './App.vue'
import router from './router'
import '@/assets/styles/index.scss'
import 'animate.css';
import 'element-plus/dist/index.css'
/** qiankun */
import { renderWithQiankun, qiankunWindow } from 'vite-plugin-qiankun/dist/helper'
import { registerGlobalListeners, microComm } from '@/qiankun/index.ts'
import { setupStore } from '@/store/index.ts'
import { isTauriMainWindow } from '@/utils/tauri.ts'
import { setToken, setCookies } from '@/utils/auth.ts'

let app :App | null = null

/**
 * 桌面主窗口登录桥接：
 * 1. 监听登录成功事件（登录窗口经 Rust 转发），写入凭据后进入主界面；
 * 2. 应用重启时从 Rust 拉取持久化凭据回填（webview 会话 cookie 不跨重启），
 *    实现免登录直接进入主窗口。
 */
const setupDesktopLoginBridge = () => {
    if (!isTauriMainWindow()) return

    import('@tauri-apps/api/event').then(async ({ listen }) => {
        await listen<{ token: string; wssAddr: string; wssKey: string }>('login-success', ({ payload }) => {
            setToken(payload.token)
            setCookies(payload.wssKey, payload.wssAddr)
            // 重新触发受保护路由的导航
            router.push('/')
        })

        // 应用重启恢复：拉取 Rust 持久化的凭据
        try {
            const { invoke } = await import('@tauri-apps/api/core')
            const stored = await invoke<{ loggedIn: boolean; token: string; wssAddr: string; wssKey: string }>('get_stored_credentials')
            if (stored.loggedIn && stored.token) {
                setToken(stored.token)
                setCookies(stored.wssKey, stored.wssAddr)
                router.push('/')
            }
            // 未登录场景由 Rust 启动时打开登录小窗口，主窗口保持隐藏，无需处理
        } catch (e) {
            console.error('恢复登录状态失败', e)
        }
    })
}

// 渲染函数
const render = (props: any) => {
    // @ts-ignore
    const { container } = props;

    const mountRoot = container ? container.querySelector('#app') : document.getElementById('app');
    app = createApp(AppDom);

    app.use(router)

    setupStore(app)

    app.mount(mountRoot);

    setupDesktopLoginBridge()
}

const renderApp = (props :any) => {
    render(props);
}

// 初始化 qiankun 生命周期
renderWithQiankun({
    bootstrap() {
        console.log('bootstrap');
    },
    mount(props :any) {
        renderApp(props);
        // 注册子应用全局事件监听
        registerGlobalListeners(props)
    },
    unmount() {
        console.log('unmount');
        microComm.unmount()
        if (app) {
            app.unmount();
            app = null;
        }
    },
} as any);

// 独立运行时直接渲染
// @ts-ignore
if (!qiankunWindow.__POWERED_BY_QIANKUN__) {
    renderApp({})
}
