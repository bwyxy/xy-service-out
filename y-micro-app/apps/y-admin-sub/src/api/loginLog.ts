import request from '@/utils/request'

// 查询客户端登录日志列表
export function listChatLoginLog(query?: any) {
  return request({
    url: '/system/login-log/list',
    method: 'post',
    data: query
  })
}

// 删除客户端登录日志
export function delChatLoginLog(id: number | string) {
  return request({
    url: '/system/login-log/' + id,
    method: 'delete'
  })
}
