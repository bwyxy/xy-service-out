import { messageHandlerService } from '@/services/im/message-handler.ts'
import type { WukongIMMessage, WukongIMMessagePayload } from '@/services/im/types'

/**
 * RTC 呼叫控制信令服务
 *
 * 呼叫控制（200 邀请 / 201 取消 / 202 接听 / 203 拒绝 / 204 挂断离开 / 205 忙线）
 * 由后端经悟空 IM 个人频道推送；媒体面（SDP/ICE）已迁移至 LiveKit，
 * 前端只做接收，不再发送任何媒体信令。
 *
 * 接收侧由 message-handler 的 RTC 分流（onRtcSignal）回调本服务的订阅者。
 * */

/** 呼叫信令数据（content JSON 解析结果） */
export interface RtcSignalData {
  roomId?: string
  /** 媒体类型 1-语音 2-视频 */
  mediaType?: number
  /** 频道类型 1-单聊 2-群聊 */
  channelType?: number
  /** 单聊：对端 uid；群聊：g_{groupId} */
  channelId?: string
  /** 群聊：群名称 */
  groupName?: string
  /** 群聊：通话主持人 uid */
  hostUid?: number | string
  /** 群聊：受邀人数（用于「N 人被邀请」提示） */
  inviteeCount?: number
  /** 群聊：通话人数上限 */
  maxParticipants?: number
  /** 群聊挂断：离开的成员 uid */
  hangupUid?: number | string
  /** 邀请者信息（200 邀请） */
  caller?: { uid: number; nickname: string; avatar: string }
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
