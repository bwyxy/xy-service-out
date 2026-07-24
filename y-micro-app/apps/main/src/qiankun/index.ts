import { registerMicroApps } from 'qiankun'

import { microApps, filterApps } from './modules/registerApp.ts'

export { baseComm, qiankunActions } from './modules/globalState.ts'

// 微应用注册
export const registerApps = () => {
    // @ts-ignore
    registerMicroApps(microApps, filterApps())

    // start({
    //     prefetch: "all", // 可选，是否开启预加载，默认为 true。
    //     sandbox: {
    //         // strictStyleIsolation: false,
    //         experimentalStyleIsolation: true
    //     }, // 可选，是否开启沙箱，默认为 true。// 从而确保微应用的样式不会对全局造成影响。
    //     singular: true, // 可选，是否为单实例场景，单实例指的是同一时间只会渲染一个微应用。默认为 true。
    // })
}
