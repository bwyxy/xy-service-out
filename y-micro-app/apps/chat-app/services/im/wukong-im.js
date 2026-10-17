import { WKIM, WKIMChannelType, WKIMEvent, WKIMDeviceFlag } from '@/libs/easyjssdk'
import { getDeviceId } from '@/utils/device'

/**
 * 悟空 IM 客户端服务
 *
 * 基于 easyjssdk 对 WKIM 进行封装，提供单例管理、事件订阅、消息收发等能力。
 * easyjssdk 内置 uni-app 适配器（uni.connectSocket），可直接运行在 App 端。
 */
export class WukongIMService {
  constructor() {
    this.client = null
    this.messageHandlers = []
    this.conversationHandlers = []
    this.connectHandlers = []
    this.disconnectHandlers = []
    this.errorHandlers = []
  }

  /** 获取单例实例 */
  static getInstance() {
    if (!WukongIMService.instance) {
      WukongIMService.instance = new WukongIMService()
    }
    return WukongIMService.instance
  }

  /**
   * 初始化并连接悟空 IM 服务
   *
   * @param config { serverUrl, userInfo: { uid, token } }
   */
  connect(config) {
    if (this.client) {
      console.warn('[WukongIM] 客户端已初始化，跳过重复连接')
      return
    }

    if (!config.serverUrl || !config.userInfo.uid) {
      console.error('[WukongIM] 连接参数不完整，请检查 serverUrl 与 uid')
      return
    }

    this.client = WKIM.init(
      config.serverUrl,
      {
        uid: config.userInfo.uid,
        token: config.userInfo.token,
        deviceId: getDeviceId(),
        // 移动端固定为 App 设备标识，与后端多端登录管控保持一致
        deviceFlag: WKIMDeviceFlag.App,
      },
      { singleton: true },
    )

    this.bindClientEvents()

    this.client
      .connect()
      .then(() => {
        console.log('[WukongIM] 连接已启动，等待 Connect 事件')
      })
      .catch((error) => {
        console.error('[WukongIM] 连接启动失败:', error)
      })
  }

  /** 断开与悟空 IM 服务的连接 */
  disconnect() {
    if (!this.client) {
      return
    }

    try {
      if (typeof this.client.disconnect === 'function') {
        this.client.disconnect()
      }
    } catch (error) {
      console.error('[WukongIM] 断开连接时发生错误:', error)
    } finally {
      this.client = null
    }
  }

  /** 当前连接是否已就绪 */
  isConnected() {
    return !!this.client && this.client.isConnected === true
  }

  /** 获取底层 IM 客户端实例 */
  getClient() {
    return this.client
  }

  /** 订阅消息到达事件 */
  onMessage(handler) {
    this.messageHandlers.push(handler)
  }

  /** 订阅会话列表更新事件 */
  onConversationUpdate(handler) {
    this.conversationHandlers.push(handler)
  }

  /** 订阅连接成功事件 */
  onConnect(handler) {
    this.connectHandlers.push(handler)
  }

  /** 订阅连接断开事件 */
  onDisconnect(handler) {
    this.disconnectHandlers.push(handler)
  }

  /** 订阅错误事件 */
  onError(handler) {
    this.errorHandlers.push(handler)
  }

  /** 发送个人消息 */
  sendPersonalMessage(channelId, payload) {
    return this.sendMessage(channelId, WKIMChannelType.Person, payload)
  }

  /** 发送群消息 */
  sendGroupMessage(channelId, payload) {
    return this.sendMessage(channelId, WKIMChannelType.Group, payload)
  }

  async sendMessage(channelId, channelType, payload) {
    if (!this.client) {
      throw new Error('[WukongIM] 客户端尚未初始化，请先调用 connect')
    }

    return this.client.send(channelId, channelType, payload)
  }

  bindClientEvents() {
    if (!this.client) {
      return
    }

    this.client.on(WKIMEvent.Connect, (result) => {
      console.log('[WukongIM] 连接成功:', result)
      this.connectHandlers.forEach((handler) => handler(result))
    })

    this.client.on(WKIMEvent.Disconnect, (info) => {
      console.log('[WukongIM] 连接断开:', info)
      this.disconnectHandlers.forEach((handler) => handler(info))
    })

    this.client.on(WKIMEvent.Message, (message) => {
      console.log('[WukongIM] 收到消息:', message)
      this.messageHandlers.forEach((handler) => handler(message))
    })

    this.client.on(WKIMEvent.Error, (error) => {
      console.error('[WukongIM] 连接错误:', error)
      this.errorHandlers.forEach((handler) => handler(error))
    })
  }
}

/** 全局唯一的悟空 IM 服务实例 */
export const wukongIMService = WukongIMService.getInstance()
