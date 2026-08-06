import request from '@/utils/request.ts'

/**
 * 获取会话列表
 * */
export const getConversation = (data :any) => {
  return request({
    url: '/conversation/list',
    method: 'post',
    data
  })
}

/**
 * 获取历史消息
 * */
export const getMessageList = (data :any) => {
  return request({
    url: '/message/list',
    method: 'post',
    data
  })
}