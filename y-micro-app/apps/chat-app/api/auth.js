import request from '@/utils/request'

/**
 * 登录
 * */
export function login(data) {
  return request({
    url: '/chat/login',
    method: 'POST',
    data,
  })
}

/**
 * 注册
 * */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'POST',
    data,
  })
}

/**
 * 发送邮箱验证码
 * */
export function sendEmailCode(email) {
  return request.post(`/send/code?email=${email}`)
}

/**
 * 获取用户信息
 * */
export function getUserInfo() {
  return request.post('/auth/userinfo')
}

/**
 * 登出（结束当前设备会话，token 立即失效）
 * */
export function logout() {
  return request.post('/auth/logout')
}

/**
 * 更新用户信息
 * */
export function updateUserInfo(data) {
  return request({
    url: '/update/info',
    method: 'PUT',
    data,
  })
}

/**
 * 修改密码
 * */
export function editPassword(data) {
  return request({
    url: '/edit/password',
    method: 'POST',
    data,
  })
}
