import { BASE_API_URL, REQUEST_TIMEOUT } from './config'
import { getToken } from './auth'

/**
 * 统一请求封装
 *
 * xy-app 的 jwt 配置为 header=authorization、prefix=Bearer
 * （见 xy-system/src/main/resources/application-dev.yml），
 * 因此登录态用 Authorization 头传递，而非 chat-app 的 token 头。
 * 始终 resolve 出后端信封 { code, msg, data }，与后端 R 的契约一致。
 * 默认不登录：接口被判未登录时拉起登录半弹框，登录成功后自动重放该请求。
 */

/**
 * 登录态失效错误码
 * - 401：未登录 / token 已过期
 * - 4011：会话被同类型设备顶下线或已登出
 */
const SESSION_EXPIRED_CODES = [401, 4011]

/** 未登录处理器，由 utils/session.js 注册为 requireLogin（拉起登录半弹框并等待登录结果） */
let unauthorizedHandler = null

/** 注册未登录处理器 */
export function setUnauthorizedHandler(handler) {
	unauthorizedHandler = handler
}

/**
 * 接口被判未登录时的统一处理：拉起登录并重放本次请求
 *
 * 登录成功 → 带 _retried 标记重发原请求，用户这一次提交不会丢；
 * 用户取消登录，或重放后仍被判未登录（_retried 已置位）→ 直接返回原始响应，避免死循环。
 */
function handleUnauthorized(options, body, resolve) {
	if (options._retried || !unauthorizedHandler) {
		resolve(body)

		return
	}

	unauthorizedHandler().then(
		() => resolve(request({ ...options, _retried: true })),
		() => resolve(body)
	)
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
	const token = getToken()

	return new Promise((resolve, reject) => {
		uni.request({
			url: resolveUrl(buildQuery(url, params)),
			method: String(method).toUpperCase(),
			data,
			timeout: timeout || REQUEST_TIMEOUT,
			header: {
				'Content-Type': 'application/json',
				...(token ? { token: `${token}` } : {}),
				...(header || {})
			},
			success: (res) => {
				const body = res.data || {}

				if (SESSION_EXPIRED_CODES.includes(body.code)) {
					handleUnauthorized(options, body, resolve)
					return
				}

				if (body.code !== 200) {
					uni.showToast({
						title: body.msg || '请求失败',
						icon: 'none'
					})
				}

				resolve(body)
			},
			fail: (error) => {
				uni.showToast({
					title: '网络异常，请稍后重试',
					icon: 'none'
				})
				reject(error)
			}
		})
	})
}

request.get = function get(url, params) {
	return request({ url, method: 'GET', params })
}

request.post = function post(url, data) {
	return request({ url, method: 'POST', data })
}

export default request
