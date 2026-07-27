const BASE_URL = import.meta.env.VITE_BASE_API_URL || 'http://localhost:9050'
// const BASE_URL = '/api'

async function request(url: string, options: RequestInit = {}) {
  const res = await fetch(`${BASE_URL}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  return res.json()
}

// 聊天登录（获取悟空IM连接凭据）
export function chatLogin(uid: string, name: string) {
  return request('/api/chat/update/token', {
    method: 'POST',
    body: JSON.stringify({
      uid: uid,
      token: name,
      deviceFlag: 1,
      deviceLevel: 1
    }),
  })
}

// 获取 Token
export function getToken(uid: string) {
  return request('/api/chat/token', {
    method: 'POST',
    body: JSON.stringify({ uid }),
  })
}

// 创建群聊
export function createGroup(groupId: string, groupName: string) {
  return request('/api/chat/group/create', {
    method: 'POST',
    body: JSON.stringify({ groupId, groupName }),
  })
}

// 加入群聊
export function joinGroup(groupId: string, uid: string) {
  return request('/api/chat/group/join', {
    method: 'POST',
    body: JSON.stringify({ groupId, uid }),
  })
}

// 同步消息
export function syncMessages(params: {
  uid: string
  channelId: string
  channelType: number
  startSeq?: number
  endSeq?: number
  limit?: number
}) {
  return request('/api/chat/messages/sync', {
    method: 'POST',
    body: JSON.stringify(params),
  })
}