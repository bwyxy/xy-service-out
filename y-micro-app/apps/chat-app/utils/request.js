import { BASE_API_URL, REQUEST_TIMEOUT } from './config'
import { getToken } from './auth'

/**
 * 统一请求封装
 *
 * 替代网页端的 @xy/utils createAxios（依赖浏览器 XMLHttpRequest）。
 * 保持与原响应拦截器一致的契约：resolve 出后端信封 { code, msg, data }，
 * 因此 api 层的调用写法（res.code / res.data / res.data.data）可 1:1 迁移。
 */

/**
 * 登录态失效错误码
 * - 401：未登录 / token 已过期
 * - 4011：会话被同类型设备顶下线或已登出
 */
const SESSION_EXPIRED_CODES = [401, 4011]

/** 派发全局登录态失效事件，由 App.vue 统一消费（清登录态 + 跳登录页） */
function notifySessionExpired(msg) {
  uni.$emit('session-expired', { reason: msg })
}

/** 将对象拼接为 query string */
function buildQuery(url, params) {
  if (!params) {
    return url
  }

  const parts = Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== null)
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)

  if (!parts.length) {
    return url
  }

  return `${url}${url.includes('?') ? '&' : '?'}${parts.join('&')}`
}

/** 相对路径拼接为绝对地址 */
function resolveUrl(url) {
  if (/^https?:/i.test(url)) {
    return url
  }
  return `${BASE_API_URL.replace(/\/+$/, '')}/${String(url).replace(/^\/+/, '')}`
}

export function request(options) {
  const { url, method = 'POST', data, params, header, timeout } = options

  return new Promise((resolve, reject) => {
    uni.request({
      url: resolveUrl(buildQuery(url, params)),
      method: String(method).toUpperCase(),
      data,
      timeout: timeout || REQUEST_TIMEOUT,
      header: {
        'Content-Type': 'application/json',
        token: getToken(),
        ...(header || {}),
      },
      success: (res) => {
        const body = res.data || {}

        if (SESSION_EXPIRED_CODES.includes(body.code)) {
          notifySessionExpired(body.msg)
          resolve(body)
          return
        }

        if (body.code !== 200) {
          uni.showToast({
            title: body.msg || '请求失败',
            icon: 'none',
          })
        }

        resolve(body)
      },
      fail: (error) => {
        uni.showToast({
          title: '网络异常，请稍后重试',
          icon: 'none',
        })
        reject(error)
      },
    })
  })
}

request.get = function get(url, params) {
  return request({ url: buildQuery(url, params), method: 'GET' })
}

request.post = function post(url, data) {
  return request({ url, method: 'POST', data })
}

/**
 * 上传文件（图片 / 头像）
 *
 * 后端 /upload/image、/upload/avatar 为 multipart 表单，字段名固定为 file。
 */
export function uploadFile(options) {
  const { url, filePath, name = 'file', formData } = options

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: resolveUrl(url),
      filePath,
      name,
      formData,
      timeout: REQUEST_TIMEOUT,
      header: {
        token: getToken(),
      },
      success: (res) => {
        let body
        try {
          body = JSON.parse(res.data)
        } catch {
          body = {}
        }

        if (SESSION_EXPIRED_CODES.includes(body.code)) {
          notifySessionExpired(body.msg)
        } else if (body.code !== 200) {
          uni.showToast({
            title: body.msg || '上传失败',
            icon: 'none',
          })
        }

        resolve(body)
      },
      fail: (error) => {
        uni.showToast({
          title: '上传失败，请稍后重试',
          icon: 'none',
        })
        reject(error)
      },
    })
  })
}

export default request
