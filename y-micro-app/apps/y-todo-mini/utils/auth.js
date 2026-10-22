import { TOKEN_KEY } from './config'

/**
 * 登录态存储
 *
 * 小程序无 cookie，统一使用 uni 本地存储；
 * 函数命名与 chat-app 的 utils/auth.js 保持一致，便于两端共享心智。
 */

export function getToken() {
	return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token) {
	uni.setStorageSync(TOKEN_KEY, token || '')
}

export function removeToken() {
	uni.removeStorageSync(TOKEN_KEY)
}
