/**
 * 基座侧通信 API
 * 在基座应用中初始化，统一管理全局状态的收发
 */
import { MicroAppStateActions } from 'qiankun'
import type { AppMessage, MessageHandler, Unsubscribe } from './types'

export class BaseAppCommunication {
  private actions: MicroAppStateActions
  private listeners: Map<string, Set<MessageHandler>> = new Map()
  /** 缓存每种类型的最新消息，用于 on() 注册时回放 */
  private stateCache: Map<string, AppMessage> = new Map()

  constructor(actions: MicroAppStateActions) {
    this.actions = actions
  }

  /**
   * 分发收到的状态变更给已注册的监听器
   * 由外部 onGlobalStateChange 回调中调用
   */
  dispatch(state: Record<string, any>): void {
    console.log('[BaseApp] dispatch, state:', JSON.stringify(state))
    if (!state || !state.type) {
      console.log('[BaseApp] state 无 type 字段，跳过')
      return
    }
    // 缓存最新消息
    this.stateCache.set(state.type, state as AppMessage)
    const handlers = this.listeners.get(state.type)
    console.log(`[BaseApp] type=${state.type}, 已注册监听器数量:`, handlers?.size ?? 0)
    if (handlers) {
      handlers.forEach(fn => fn(state as AppMessage))
    }
  }

  /**
   * 发送消息给所有子应用
   */
  send(msg: AppMessage): void {
    console.log(this.actions)
    this.actions.setGlobalState({
      ...msg,
      timestamp: msg.timestamp ?? Date.now()
    })
  }

  /**
   * 订阅子应用发来的消息
   * 如果该类型的消息已被 dispatch 缓存，则立即回放
   * @returns 取消订阅函数
   */
  on(type: string, handler: MessageHandler): Unsubscribe {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type)!.add(handler)

    // 回放已缓存的消息
    const cached = this.stateCache.get(type)
    if (cached) {
      handler(cached)
    }

    return () => {
      this.listeners.get(type)?.delete(handler)
    }
  }

  /**
   * 清除某个类型的所有监听器
   */
  off(type: string): void {
    this.listeners.delete(type)
  }

  /**
   * 清除所有监听器
   */
  clear(): void {
    this.listeners.clear()
  }

  /**
   * 获取 qiankun actions 实例（用于注册子应用时传递）
   */
  getActions(): MicroAppStateActions {
    return this.actions
  }
}
