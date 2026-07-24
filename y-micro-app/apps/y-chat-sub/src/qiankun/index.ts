import { MicroAppCommunication } from '@xy/qiankun-communication'

export const microComm = new MicroAppCommunication()

/** 注册子应用全局监听事件 */
export function registerGlobalListeners(props :any) {
    if (!props) {
        console.warn('[MicroApp] 未挂载，全局监听事件注册失败')
        return
    }

    // 当前运行在基座里
    microComm.mount(props)

    console.log('init micro comm props', microComm.getProps())
    const microCommProp :any = microComm.getProps()

    if (microCommProp.globalState) {

    }
}
