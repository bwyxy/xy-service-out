import request from '@/utils/request.ts'

/**
 * RTC 通话 API
 *
 * 控制面走后端 REST（房间状态机 + LiveKit token 签发），
 * 媒体面由 LiveKit SFU 承载，呼叫控制信令（200-205）经悟空 IM 个人频道传输。
 * */

/** 通话参与者（DB 视角，含未接入的受邀者） */
export interface RtcMember {
  uid: number | string
  nickname?: string
  avatar?: string
  /** 群昵称（群聊时优先展示） */
  groupNickname?: string
  /** 角色 0-普通 1-主持人 */
  role?: number
  /** 0-呼叫中 1-已加入 2-已离开 3-已拒绝 4-忙线 */
  status?: number
  joinTime?: string | null
}

/** 通话会话信息（call / accept / token 统一返回体） */
export interface CallSession {
  roomId: string
  /** 媒体类型 1-语音 2-视频 */
  mediaType: number
  /** 频道类型 1-单聊 2-群聊 */
  channelType: number
  /** 单聊：对端 uid；群聊：g_{groupId} */
  channelId: string
  /** LiveKit 接入地址（ws/wss） */
  livekitUrl: string
  /** LiveKit 接入 token */
  token: string
  maxParticipants: number
  participants: RtcMember[]
}

/** 房间详情 */
export interface RoomDetail {
  roomId: string
  mediaType: number
  channelType: number
  channelId: string
  hostUid: number
  /** 0-呼叫中 1-通话中 2-已结束 */
  status: number
  maxParticipants: number
  livekitUrl: string
  startTime: string | null
  participants: RtcMember[]
}

/** 发起通话参数 */
export interface CallPayload {
  /** 单聊：被叫 uid */
  calleeUid?: string | number
  /** 群聊：g_{groupId} */
  channelId?: string
  /** 1-单聊（默认）2-群聊 */
  channelType?: number
  /** 群聊：被邀请成员（不含自己） */
  inviteeUids?: Array<string | number>
  /** 媒体类型 1-语音 2-视频 */
  mediaType: number
}

/**
 * 发起通话（单聊/群聊）
 * */
export const rtcCall = (payload: CallPayload) => {
  return request({
    url: '/rtc/call',
    method: 'post',
    data: payload,
  })
}

/**
 * 接听 / 重新接入（幂等，返回接入 token）
 * */
export const rtcAccept = (roomId: string) => {
  return request({
    url: '/rtc/accept',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 拒绝（单聊结束房间，群聊仅本人退出邀请）
 * */
export const rtcReject = (roomId: string) => {
  return request({
    url: '/rtc/reject',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 挂断 / 离开（单聊结束房间；群聊仅本人离开）
 * */
export const rtcHangup = (roomId: string) => {
  return request({
    url: '/rtc/hangup',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 主叫取消（仅呼叫中阶段、仅发起人）
 * */
export const rtcCancel = (roomId: string) => {
  return request({
    url: '/rtc/cancel',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 群通话中追加邀请成员
 * */
export const rtcInvite = (roomId: string, inviteeUids: Array<string | number>) => {
  return request({
    url: '/rtc/invite',
    method: 'post',
    data: { roomId, inviteeUids },
  })
}

/**
 * 结束整场通话（发起人或群主/管理员，仅群聊）
 * */
export const rtcEnd = (roomId: string) => {
  return request({
    url: '/rtc/end',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 重新签发接入 token（断线重连 / 刷新页面后重新入房）
 * */
export const rtcToken = (roomId: string) => {
  return request({
    url: '/rtc/token',
    method: 'post',
    data: { roomId },
  })
}

/**
 * 查询房间详情
 * */
export const getRoomDetail = (roomId: string) => {
  return request({
    url: `/rtc/room/${roomId}`,
    method: 'get',
  })
}

/**
 * RTC 服务健康检查（媒体面 LiveKit 连通性）
 * */
export const getRtcHealth = () => {
  return request({
    url: '/rtc/health',
    method: 'get',
  })
}
