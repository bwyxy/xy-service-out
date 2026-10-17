import request from '@/utils/request'

/**
 * 创建群聊
 * */
export function createGroup(data) {
  return request({
    url: '/group/create',
    method: 'post',
    data,
  })
}
