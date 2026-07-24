/**
 * 定义全局状态下发
 * */
import { initGlobalState } from 'qiankun'
import { BaseAppCommunication } from '@xy/qiankun-communication'

// 定义全局下发的数据
export const initialState = {
    // 用户信息
    userInfo: null,
    // token
    token: '',
    type: '',
    payload: null,
    wsStatus: 0,
    redirectUrl: ''
}

export const qiankunActions = initGlobalState(initialState)

// 初始化通信实例，传入 actions
export const baseComm = new BaseAppCommunication(qiankunActions)

// 注册全局状态变更监听（直接注册，不经过类封装）
qiankunActions.onGlobalStateChange((state: Record<string, any>) => {
    console.log('[主应用] onGlobalStateChange 触发, state:', JSON.stringify(state))
    baseComm.dispatch(state)
})
