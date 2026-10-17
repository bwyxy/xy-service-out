import { TOKEN_KEY } from './config'

/**
 * 登录态存储
 *
 * 网页端使用 js-cookie，App 端无 cookie，统一改用 uni 本地存储。
 * 函数命名与网页端保持一致，便于 api / store 层平滑迁移。
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

export function setCookies(key, value) {
  uni.setStorageSync(key, value || '')
}

export function getCookies(key) {
  return uni.getStorageSync(key) || ''
}
