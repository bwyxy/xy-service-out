import { reactive } from 'vue'
import { getToken, setToken, removeToken } from './auth'
import { wechatLogin, getUserInfo, logout as logoutApi } from '@/api/auth'
import { setUnauthorizedHandler } from './request'

/**
 * 登录会话管理
 *
 * 默认不登录：启动时只在本地已有 token 的情况下静默恢复用户信息，不主动发起微信登录。
 * 需要登录的动作（点击个人中心等）由 requireLogin() 拉起登录半弹框；
 * 提交类接口被后端判定未登录（401/4011）时，请求层调用同一个入口，
 * 登录成功后自动重放那次请求，不会丢掉用户这一次提交。
 * 后端用户主键是 u_id，Jackson 会按 JavaBean 规范把它序列化成 uid，
 * 登录接口返回的字段又名为 userId，这里统一归一化。
 */

/** 当前登录用户（来源 t_user） */
export const user = reactive({
	uId: null,
	nickname: '',
	avatar: '',
	sex: '',
	logged: false
})

/** 等待登录结果的会话，登录或取消时统一结算 */
let pendingLogin = null

/** 本次启动是否已尝试恢复登录态 */
let initialized = false

/** 把后端返回的用户对象归一化写入 user */
export function setUser(data) {
	if (!data) {
		return
	}

	const rawId = data.uId != null ? data.uId : data.uid != null ? data.uid : data.userId

	user.uId = rawId != null ? rawId : null
	user.nickname = data.nickname || ''
	user.avatar = data.avatar || ''
	user.sex = data.sex || ''
	user.logged = user.uId != null
}

/** 清除本地登录态 */
export function clearSession() {
	removeToken()
	setUser(null)
	user.uId = null
	user.logged = false
}

/** 本地是否已有登录态（用户信息可能仍在补全中） */
export function isLoggedIn() {
	return !!getToken()
}

/** 通过 uni.login 获取微信 code */
function getWxCode() {
	return new Promise((resolve, reject) => {
		// #ifdef MP-WEIXIN
		uni.login({
			provider: 'weixin',
			success: (res) => {
				if (res.code) {
					resolve(res.code)
					return
				}

				reject(new Error('未获取到微信 code'))
			},
			fail: reject
		})
		// #endif

		// #ifndef MP-WEIXIN
		reject(new Error('当前平台暂不支持微信登录'))
		// #endif
	})
}

/** 拉取当前登录用户信息 */
export async function fetchUserInfo() {
	const res = await getUserInfo()

	if (res.code !== 200) {
		return null
	}

	setUser(res.data)

	return res.data
}

/** 微信登录：code 换 token → 存 token → 拉完整资料 */
export async function wxLogin() {
	const code = await getWxCode()
	const res = await wechatLogin(code)

	if (res.code !== 200 || !res.data) {
		throw new Error(res.msg || '登录失败')
	}

	setToken(res.data.token)
	setUser(res.data)

	try {
		await fetchUserInfo()
	} catch (error) {
		// 资料拉取失败不影响登录态本身
		console.error('获取用户信息失败', error)
	}

	return res.data
}

/**
 * 拉起登录半弹框并等待结果
 *
 * 已登录直接返回；并发调用共用同一次登录，弹框只会被拉起一次。
 * 登录成功 resolve 当前用户，用户取消则 reject。
 */
export function requireLogin() {
	if (isLoggedIn()) {
		return Promise.resolve(user)
	}

	if (pendingLogin) {
		return pendingLogin.promise
	}

	let resolveLogin = null
	let rejectLogin = null
	const promise = new Promise((resolve, reject) => {
		resolveLogin = resolve
		rejectLogin = reject
	})

	pendingLogin = { promise, resolve: resolveLogin, reject: rejectLogin }
	uni.$emit('login-required')

	return promise
}

/** 登录半弹框点击「登录」时调用：登录成功后唤醒所有等待中的请求 */
export async function submitLogin() {
	let data = null

	try {
		data = await wxLogin()
	} catch (error) {
		// 登录失败也必须结算等待中的请求，否则它们会一直挂起；弹框由调用方提示并收起
		settleLogin(error)
		uni.$emit('login-sheet-close')

		throw error
	}

	settleLogin(null)
	uni.$emit('login-sheet-close')

	return data
}

/** 登录半弹框关闭或用户取消时调用：按取消结算等待中的请求 */
export function cancelLogin() {
	settleLogin(new Error('用户取消登录'))
	uni.$emit('login-sheet-close')
}

/** 结算等待中的登录会话，error 为 null 表示登录成功 */
function settleLogin(error) {
	if (!pendingLogin) {
		return
	}

	const current = pendingLogin

	pendingLogin = null

	if (error) {
		current.reject(error)

		return
	}

	current.resolve(user)
}

/** 启动恢复登录态：仅本地已有 token 时静默拉一次用户信息，不做自动登录 */
export async function initSession() {
	if (initialized || !getToken()) {
		return user
	}

	initialized = true

	try {
		await fetchUserInfo()
	} catch (error) {
		clearSession()
	}

	return user
}

/** 退出登录：先让服务端作废 token，再清本地登录态 */
export async function doLogout() {
	try {
		await logoutApi()
	} catch (error) {
		console.error('退出登录失败', error)
	}

	clearSession()
}

// 提交接口被判未登录（401/4011）时拉起登录半弹框，登录成功后由请求层重放该请求
setUnauthorizedHandler(requireLogin)
