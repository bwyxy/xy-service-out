import { sendMessage } from '@/api/chat.ts'
import { messageHandlerService } from '@/services/im/message-handler.ts'
import { MessageType } from '@/services/im/constants'
import type { WukongIMMessage, WukongIMMessagePayload } from '@/services/im/types'

/**
 * RTC 信令服务
 *
 * 媒体面信令（offer/answer/ICE candidate）复用现有 /message/send 接口
 * 经悟空 IM 个人频道传输（后端对 200-208 类型消息自动设置 noPersist=1 不入历史）。
 *
 * 接收侧由 message-handler 的 RTC 分流（onRtcSignal）回调本服务的订阅者。
 * */

/** RTC 信令数据（content JSON 解析结果） */
export interface RtcSignalData {
  roomId?: string
  mediaType?: number
  caller?: { uid: number; nickname: string; avatar: string }
  /** WebRTC SDP */
  sdp?: string
  /** ICE candidate */
  candidate?: RTCIceCandidateInit
}

/** 信令处理器：fromUid 为对端用户，type 为信令类型 */
export type RtcSignalListener = (fromUid: string, type: number, data: RtcSignalData) => void

class RtcSignalingService {
  private listeners: RtcSignalListener[] = []
  private initialized = false

  /** 注册 IM 信令监听（幂等） */
  init(): void {
    if (this.initialized) {
      return
    }
    messageHandlerService.onRtcSignal(this.handleSignal)
    this.initialized = true
  }

  /** 订阅信令 */
  onSignal(listener: RtcSignalListener): void {
    this.listeners.push(listener)
  }

  /** 取消订阅 */
  offSignal(listener: RtcSignalListener): void {
    const index = this.listeners.indexOf(listener)
    if (index > -1) {
      this.listeners.splice(index, 1)
    }
  }

  /**
   * 发送 RTC 信令给对端（经 /message/send 走悟空 IM）
   * @param peerUid 对端用户 ID
   * @param type 信令类型（206 offer / 207 answer / 208 candidate）
   * @param data 信令数据（JSON 序列化后作为消息 content）
   * */
  async send(peerUid: string, type: MessageType.RTC_OFFER | MessageType.RTC_ANSWER | MessageType.RTC_CANDIDATE, data: Record<string, unknown>): Promise<void> {
    await sendMessage({
      channelId: peerUid,
      channelType: 1,
      type,
      content: JSON.stringify(data),
      clientMsgNo: `rtc_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    })
  }

  private handleSignal = (message: WukongIMMessage, payload: WukongIMMessagePayload): void => {
    let data: RtcSignalData = {}
    try {
      const content = payload.content ?? payload.message ?? ''
      if (content) {
        data = JSON.parse(content) as RtcSignalData
      }
    } catch {
      console.warn('[RtcSignaling] 信令 content 解析失败', payload.content)
      return
    }
    this.listeners.forEach((listener) => listener(String(message.fromUid), payload.type, data))
  }
}

export const rtcSignalingService = new RtcSignalingService()
