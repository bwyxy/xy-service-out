import request from '@/utils/request.ts'

/**
 * RTC 通话 API
 *
 * 控制面走后端 REST（房间状态机），媒体面信令（offer/answer/candidate）
 * 复用 api/chat 的 sendMessage 经悟空 IM 传输。
 * */

export interface CallResult {
  /** 房间 ID */
  roomId: string
  /** 媒体类型 1-语音 2-视频 */
  mediaType: number
}

export interface IceServer {
  urls: string
  username?: string
  credential?: string
}

/**
 * 发起通话
 * @param calleeUid 被叫用户 ID
 * @param mediaType 媒体类型 1-语音 2-视频
 * */
export const rtcCall = (calleeUid: string | number, mediaType: number) => {
  return request({
    url: '/rtc/call',
    method: 'post',
    data: { calleeUid, mediaType },
  })
}

/**
 * 接听
 * */
export const rtcAccept = (roomId: string) => {
  return request({
    url: '/rtc/accept',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 拒绝
 * */
export const rtcReject = (roomId: string) => {
  return request({
    url: '/rtc/reject',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 挂断
 * */
export const rtcHangup = (roomId: string) => {
  return request({
    url: '/rtc/hangup',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 主叫取消
 * */
export const rtcCancel = (roomId: string) => {
  return request({
    url: '/rtc/cancel',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 获取 ICE 服务器配置（STUN/TURN）
 * */
export const getIceServers = () => {
  return request({
    url: '/rtc/ice-servers',
    method: 'get',
  })
}
