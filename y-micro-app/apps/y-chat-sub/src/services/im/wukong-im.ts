import { WKIM, WKIMChannelType, WKIMEvent } from 'easyjssdk'
import type {
  WukongIMConfig,
  WukongIMConversation,
  WukongIMEventHandler,
  WukongIMMessage,
  WukongIMMessagePayload,
} from './types'

/**
 * 悟空 IM 客户端服务
 *
 * 基于 easyjssdk 对 WKIM 进行封装，提供单例管理、事件订阅、消息收发等能力。
 * 避免将 IM 连接逻辑分散在多个 store 中，提升可维护性。
 */
export class WukongIMService {
  private static instance: WukongIMService | null = null

  private client: any = null

  private readonly messageHandlers: WukongIMEventHandler<WukongIMMessage>[] = []
  private readonly conversationHandlers: WukongIMEventHandler<WukongIMConversation[]>[] = []
  private readonly connectHandlers: WukongIMEventHandler<unknown>[] = []
  private readonly disconnectHandlers: WukongIMEventHandler<unknown>[] = []
  private readonly errorHandlers: WukongIMEventHandler<unknown>[] = []

  /**
   * 获取单例实例
   */
  static getInstance(): WukongIMService {
    if (!WukongIMService.instance) {
      WukongIMService.instance = new WukongIMService()
    }
    return WukongIMService.instance
  }

  /**
   * 初始化并连接悟空 IM 服务
   * @param config 连接配置
   */
  connect(config: WukongIMConfig): void {
    if (this.client) {
      console.warn('[WukongIM] 客户端已初始化，跳过重复连接')
      return
    }

    if (!config.serverUrl || !config.userInfo.uid) {
      console.error('[WukongIM] 连接参数不完整，请检查 serverUrl 与 uid')
      return
    }

    this.client = WKIM.init(config.serverUrl, {
      uid: config.userInfo.uid,
      token: config.userInfo.token,
    })

    this.bindClientEvents()

    this.client
      .connect()
      .then(() => {
        console.log('[WukongIM] 连接已启动，等待 Connect 事件')
      })
      .catch((error: unknown) => {
        console.error('[WukongIM] 连接启动失败:', error)
      })
  }

  /**
   * 断开与悟空 IM 服务的连接
   */
  disconnect(): void {
    if (!this.client) {
      return
    }

    try {
      // easyjssdk 未暴露 disconnect 时静默处理
      if (typeof this.client.disconnect === 'function') {
        this.client.disconnect()
      }
    } catch (error) {
      console.error('[WukongIM] 断开连接时发生错误:', error)
    } finally {
      this.client = null
    }
  }

  /**
   * 获取底层 IM 客户端实例
   */
  getClient(): any {
    return this.client
  }

  /**
   * 当前是否已初始化客户端
   */
  isInitialized(): boolean {
    return this.client !== null
  }

  /**
   * 订阅消息到达事件
   */
  onMessage(handler: WukongIMEventHandler<WukongIMMessage>): void {
    this.messageHandlers.push(handler)
  }

  /**
   * 订阅会话列表更新事件
   * @remarks easyjssdk 未直接提供该事件，当前通过消息事件触发，后续可扩展
   */
  onConversationUpdate(handler: WukongIMEventHandler<WukongIMConversation[]>): void {
    this.conversationHandlers.push(handler)
  }

  /**
   * 订阅连接成功事件
   */
  onConnect(handler: WukongIMEventHandler<unknown>): void {
    this.connectHandlers.push(handler)
  }

  /**
   * 订阅连接断开事件
   */
  onDisconnect(handler: WukongIMEventHandler<unknown>): void {
    this.disconnectHandlers.push(handler)
  }

  /**
   * 订阅错误事件
   */
  onError(handler: WukongIMEventHandler<unknown>): void {
    this.errorHandlers.push(handler)
  }

  /**
   * 发送个人消息
   * @param channelId 目标用户 ID（个人频道 ID 即为对方 UID）
   * @param payload 消息负载
   */
  async sendPersonalMessage(channelId: string, payload: WukongIMMessagePayload): Promise<unknown> {
    return this.sendMessage(channelId, WKIMChannelType.Person, payload)
  }

  /**
   * 发送群消息
   * @param channelId 群频道 ID
   * @param payload 消息负载
   */
  async sendGroupMessage(channelId: string, payload: WukongIMMessagePayload): Promise<unknown> {
    return this.sendMessage(channelId, WKIMChannelType.Group, payload)
  }

  /**
   * 同步频道历史消息
   * @remarks easyjssdk 未直接提供本地同步接口，需通过后端 REST API 获取
   */
  syncMessage(_channelId: string, _channelType: number, _startMessageSeq: number, _limit: number): void {
    console.warn('[WukongIM] syncMessage 在 easyjssdk 中未实现，请调用后端消息同步接口')
  }

  private bindClientEvents(): void {
    if (!this.client) {
      return
    }

    this.client.on(WKIMEvent.Connect, (result: unknown) => {
      console.log('[WukongIM] 连接成功:', result)
      this.connectHandlers.forEach((handler) => handler(result))
    })

    this.client.on(WKIMEvent.Disconnect, (info: unknown) => {
      console.log('[WukongIM] 连接断开:', info)
      this.disconnectHandlers.forEach((handler) => handler(info))
    })

    this.client.on(WKIMEvent.Message, (message: WukongIMMessage) => {
      console.log('[WukongIM] 收到消息:', message)
      this.messageHandlers.forEach((handler) => handler(message))
    })

    this.client.on(WKIMEvent.Error, (error: unknown) => {
      console.error('[WukongIM] 连接错误:', error)
      this.errorHandlers.forEach((handler) => handler(error))
    })
  }

  private async sendMessage(
    channelId: string,
    channelType: number,
    payload: WukongIMMessagePayload,
  ): Promise<unknown> {
    if (!this.client) {
      throw new Error('[WukongIM] 客户端尚未初始化，请先调用 connect')
    }

    return this.client.send(channelId, channelType, payload)
  }
}

/**
 * 全局唯一的悟空 IM 服务实例
 */
export const wukongIMService = WukongIMService.getInstance()
