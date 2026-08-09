import request from '@/utils/request.ts'

/**
 * 获取会话列表
 * */
export const getConversation = (data: any) => {
  return request({
    url: '/conversation/list',
    method: 'post',
    data,
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
}

export const sendMessage = (data: SendMessageParams) => {
  return request({
    url: '/message/send',
    method: 'post',
    data,
  })
}
