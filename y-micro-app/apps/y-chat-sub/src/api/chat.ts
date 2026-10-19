import request from '@/utils/request.ts'
import type { MentionInfo, QuoteInfo } from '@/store/chat.ts'

/**
 * 获取会话列表
 *
 * 后端 /conversation/list 从 query 参数绑定，故用 params 传递。
 * */
export const getConversation = (params: any) => {
  return request({
    url: '/conversation/list',
    method: 'post',
    params,
  })
}

/**
 * 获取历史消息
 * */
export const getMessageList = (data: any) => {
  return request({
    url: '/message/list',
    method: 'post',
    data,
  })
}

/**
 * 设置/取消会话置顶
 * */
export interface ConversationTopParams {
  channelId: string
  channelType: number
  /** 1=置顶 0=取消置顶 */
  top: number
}

export const setConversationTop = (data: ConversationTopParams) => {
  return request({
    url: '/conversation/top',
    method: 'post',
    data,
  })
}

/**
 * 会话频道参数
 * */
export interface ConversationChannelParams {
  channelId: string
  channelType: number
}

/**
 * 删除聊天记录（仅当前用户不可见，对方不受影响）
 * */
export const clearConversationMessages = (data: ConversationChannelParams) => {
  return request({
    url: '/conversation/clear',
    method: 'post',
    data,
  })
}

/**
 * 删除会话（仅当前用户不可见，对方不受影响）
 * */
export const deleteConversation = (data: ConversationChannelParams) => {
  return request({
    url: '/conversation/delete',
    method: 'post',
    data,
  })
}

/**
 * 聊天记录搜索结果项
 * */
export interface SearchMessageItem {
  messageSeq?: number
  messageId?: number
  fromUid: string
  type: number
  content: string
  timestamp: number
}

/**
 * 搜索聊天记录参数
 * */
export interface SearchMessageParams {
  channelId: string
  channelType: number
  /** 关键字（按类型/日期浏览时可不传） */
  keyword?: string
  /** 消息类型过滤，为空默认仅文本 */
  types?: number[]
  /** 是否仅查找链接 */
  linkOnly?: boolean
  /** 起始时间（毫秒，含） */
  startTime?: number
  /** 结束时间（毫秒，含） */
  endTime?: number
  limit?: number
}

/**
 * 搜索聊天记录
 * */
export const searchMessages = (data: SearchMessageParams) => {
  return request({
    url: '/message/search',
    method: 'post',
    data,
  })
}

/**
 * 发送消息（经服务端转发至悟空 IM）
 *
 * 统一走服务端接口，便于后续做内容安全校验、图片鉴黄等业务扩展。
 */
export interface SendMessageParams {
  channelId: string
  channelType: number
  type: number
  content: string
  clientMsgNo?: string
  /** 引用的消息快照 */
  quote?: QuoteInfo
  /** @提醒的成员 */
  mentions?: MentionInfo[]
}

export const sendMessage = (data: SendMessageParams) => {
  return request({
    url: '/message/send',
    method: 'post',
    data,
  })
}

/**
 * 撤回消息
 *
 * 服务端校验通过后会向频道广播撤回信令，各端据此把原消息替换为「已撤回」。
 */
export interface RecallMessageParams {
  channelId: string
  channelType: number
  messageSeq: number
  clientMsgNo?: string
}

export const recallMessage = (data: RecallMessageParams) => {
  return request({
    url: '/message/recall',
    method: 'post',
    data,
  })
}
