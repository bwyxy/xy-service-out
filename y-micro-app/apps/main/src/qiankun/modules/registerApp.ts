/**
 * 注册微应用
 * */
import { initialState } from './globalState.ts'

import env from '@/config/env.ts'

// 定义子应用的配置类
interface MicroAppConfig {
    /** 微应用名称 - 具有唯一性 */
    name: string;
    /** entry: 微应用入口 - 通过该地址加载微应用，这里我们使用 config 配置 */
    entry: string;
    /** container: 微应用挂载节点 - 微应用加载完成后将挂载在该节点上 */
    container?: string;
    /** activeRule: 微应用触发的路由规则 - 触发路由规则后将加载该微应用 */
    activeRule: any;
    props?: Record<string, any>;
}

const { adminMicroApp, chatMicroApp } = env

export const microApps : MicroAppConfig[] = [
    {
        name: 'admin',
        entry: adminMicroApp,
        activeRule: '/admin'
    },
    {
        name: 'chat',
        entry: chatMicroApp,
        activeRule: '/chat'
    }
]

export const filterApps = () => {
    microApps.forEach(microApp => {
        microApp.container = '#subapp-container'
        microApp.props = {
            globalState: initialState
        }
    })

    return microApps
}

/* qiankun 全局声明周期钩子 */
export const microConfig = {
    beforeLoad: (app :any) => {
        console.log("%c before load", "background:#3a5ab0 ; padding: 1px; border-radius: 3px;  color: #fff", app);
    }, // 预加载
    beforeMount: (app :any) => {
        console.log("%c before mount", "background:#7d9553 ; padding: 1px; border-radius: 3px;  color: #fff", app);
    }, // 挂载前回调
    afterMount: (app :any) => {
        console.log("%c after mount", "background:#7d7453 ; padding: 1px; border-radius: 3px;  color: #fff", app);
    }, // 挂载后回调
    beforeUnmount: (app :any) => {
        console.log("%c before unmount", "background:#7dd253 ; padding: 1px; border-radius: 3px;  color: #fff", app);
    }, // 卸载前回调
    afterUnmount: (app :any) => {
        console.log("%c after unmount", "background:#d2525c ; padding: 1px; border-radius: 3px;  color: #fff", app);
    } // 卸载后回调
}
