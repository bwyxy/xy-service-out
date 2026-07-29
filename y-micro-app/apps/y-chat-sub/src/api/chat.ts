const BASE_URL = import.meta.env.VITE_BASE_API_URL || 'http://localhost:9050'

async function request(url: string, options: RequestInit = {}) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }

  // 携带 JWT token
  const token = localStorage.getItem('chat_token')
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(`${BASE_URL}${url}`, {
    headers,
    ...options,
  })
  return res.json()
}

/** 登录（手机号/邮箱 + 密码） */
export function login(account: string, password: string) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ account, password }),
  })
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return request('/user/info')
}

/** 注册 */
export function register(data: {
  nickname?: string
  phone: string
  email: string
  password: string
  confirmPassword: string
  emailCode: string
  sex?: string
}) {
  return request('/auth/register', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

/** 发送验证码 */
export function sendCode(email: string) {
  return request(`/send/code?email=${encodeURIComponent(email)}`, {
    method: 'POST',
  })
}

/** 更新悟空IM Token */
export function updateToken(uid: string, token: string, deviceFlag = 1, deviceLevel = 1) {
  return request('/api/chat/update/token', {
    method: 'POST',
    body: JSON.stringify({ uid, token, deviceFlag, deviceLevel }),
  })
}

/** 创建群聊 */
export function createGroup(groupId: string, groupName: string) {
  return request('/api/chat/group/create', {
    method: 'POST',
    body: JSON.stringify({ groupId, groupName }),
  })
}

/** 加入群聊 */
export function joinGroup(groupId: string, uid: string) {
  return request('/api/chat/group/join', {
    method: 'POST',
    body: JSON.stringify({ groupId, uid }),
  })
}

/** 同步消息 */
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