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

let app :App | null = null

// 渲染函数
const render = (props: any) => {
    // @ts-ignore
    const { container } = props;

    const mountRoot = container ? container.querySelector('#app') : document.getElementById('app');
    app = createApp(AppDom);

    app.use(router)

    setupStore(app)

    app.mount(mountRoot);
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
