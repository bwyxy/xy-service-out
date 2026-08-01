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