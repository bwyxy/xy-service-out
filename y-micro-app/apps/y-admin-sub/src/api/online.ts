import request from '@/utils/request'

// 查询在线用户列表
export function listOnline(query?: any) {
  return request({
    url: '/system/online/list',
    method: 'get',
    params: query
  })
}

// 强退在线用户
export function forceLogout(tokenId: string) {
  return request({
    url: '/system/online/' + encodeURIComponent(tokenId),
    method: 'delete'
  })
}
