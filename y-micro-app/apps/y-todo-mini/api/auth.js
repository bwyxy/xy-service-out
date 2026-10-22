import request from '@/utils/request'

/**
 * 微信登录
 *
 * code 由 uni.login 获取，后端接口为 @RequestParam，因此走 query string 传参。
 * */
export function wechatLogin(code) {
	return request({ url: '/wechat/login', method: 'POST', params: { code } })
}

/**
 * 获取登录用户信息（t_user）
 * */
export function getUserInfo() {
	return request({ url: '/wechat/getInfo', method: 'GET' })
}

/**
 * 退出登录（当前 token 加入黑名单，立即失效）
 * */
export function logout() {
	return request({ url: '/wechat/logout', method: 'POST' })
}
