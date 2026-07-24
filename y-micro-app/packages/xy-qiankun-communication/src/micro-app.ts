/**
 * 子应用侧通信 API
 * 在子应用的 mount 生命周期中初始化，提供统一的收发接口
 */
import type { AppMessage, MessageHandler, Unsubscribe } from './types'

/** qiankun 传递给子应用的 props 类型 */
export interface QiankunProps {
  onGlobalStateChange: (
    callback: (state: any, prevState: any) => void,
    fireImmediately?: boolean
  ) => void
  setGlobalState: (state: any) => boolean
  [key: string]: any
}

export class MicroAppCommunication {
  private props: QiankunProps | null = null
  private listeners: Map<string, Set<MessageHandler>> = new Map()
  /** 缓存每种类型的最新消息，用于 on() 注册时回放 */
  private stateCache: Map<string, AppMessage> = new Map()
  private mounted = false

  /**
   * 子应用 mount 时调用，注入 qiankun props
   * @param props qiankun 传递的 props
   * @param fireImmediately 是否立即触发一次 onGlobalStateChange（默认 true）
   */
  mount(props: QiankunProps, fireImmediately = true): void {
    this.props = props
    this.mounted = true
    console.log('[MicroApp] mount 完成, setGlobalState 可用:', typeof props.setGlobalState === 'function')

    props.onGlobalStateChange((state: Record<string, any>, _prevState: any) => {
      if (!state || !state.type) return
      // 缓存最新消息
      this.stateCache.set(state.type, state as AppMessage)
      const handlers = this.listeners.get(state.type)
      if (handlers) {
        handlers.forEach(fn => fn(state as AppMessage))
      }
    }, fireImmediately)
  }

  /**
   * 子应用 unmount 时调用，清理资源
   */
  unmount(): void {
    this.props = null
    this.mounted = false
    this.listeners.clear()
    this.stateCache.clear()
  }

  /**
   * 发送消息给基座
   */
  send(msg: AppMessage): void {
    if (!this.props) {
      console.warn('[MicroApp] 未挂载，消息发送失败')
      return
    }
    console.log(this.props)
    console.log('[MicroApp] send 调用, msg:', JSON.stringify(msg))
    const result = this.props.setGlobalState({
      ...msg,
      timestamp: msg.timestamp ?? Date.now()
    })
    console.log('[MicroApp] setGlobalState 返回值:', result)
  }

  /**
   * 订阅基座发来的消息
   * 如果该类型的消息在 mount 时已通过 onGlobalStateChange 缓存，则立即回放
   * @returns 取消订阅函数
   */
  on(type: string, handler: MessageHandler): Unsubscribe {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type)!.add(handler)

    // 回放已缓存的消息（解决 mount 时 fireImmediately 先于 on() 执行导致的消息丢失）
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
   * 获取 qiankun 原始 props（用于访问 container 等）
   */
  getProps(): QiankunProps | null {
    return this.props
  }

  /**
   * 是否已挂载
   */
  isMounted(): boolean {
    return this.mounted
  }
}
