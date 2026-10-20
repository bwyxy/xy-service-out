import request from '@/utils/request'

// 查询客户端用户列表
export function listChatUser(query?: any) {
  return request({
    url: '/system/chat-user/list',
    method: 'post',
    data: query
  })
}

// 搜索用户（用于黑名单选择目标用户）
export function searchChatUser(params?: any) {
  return request({
    url: '/system/chat-user/search',
    method: 'get',
    params
  })
}

// 查询用户详情
export function getChatUser(uId: number | string) {
  return request({
    url: '/system/chat-user/' + uId,
    method: 'get'
  })
}

// 修改用户状态
export function updateChatUserStatus(uId: number | string, status: string) {
  return request({
    url: '/system/chat-user/status',
    method: 'put',
    data: { uId, status }
  })
}

// 查询用户登录设备
export function listChatSession(uId: number | string) {
  return request({
    url: `/system/chat-user/${uId}/sessions`,
    method: 'get'
  })
}

// 强制下线指定设备
export function forceLogoutSession(token: string) {
  return request({
    url: '/system/chat-user/session/' + encodeURIComponent(token),
    method: 'delete'
  })
}

// 强制下线用户全部设备
export function forceLogoutAllSessions(uId: number | string) {
  return request({
    url: `/system/chat-user/${uId}/sessions`,
    method: 'delete'
  })
}

// 查询用户黑名单
export function listChatBlacklist(uId: number | string, params?: any) {
  return request({
    url: `/system/chat-user/${uId}/blacklist`,
    method: 'get',
    params
  })
}

// 新增黑名单
export function addChatBlacklist(data: any) {
  return request({
    url: '/system/chat-user/blacklist',
    method: 'post',
    data
  })
}

// 移除黑名单
export function removeChatBlacklist(id: number | string) {
  return request({
    url: '/system/chat-user/blacklist/' + id,
    method: 'delete'
  })
}
