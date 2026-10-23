/**
 * 隐私协议与授权
 *
 * 小程序在公众平台配置《用户隐私保护指引》后，manifest 里开启 __usePrivacyCheck__，
 * 微信会在调用需授权接口（头像、图片、位置等）前先来问一次同意。
 * 这里统一接管这件事：同意结果存在本地，首次启动由页面里的 privacy-sheet 弹一次，
 * 微信主动来问时也用同一个弹框应答，避免各处各写一套、也避免弹两次。
 */

/** 本地缓存的同意标记 */
const PRIVACY_AGREED_KEY = 'y_privacy_agreed'

/** 请求弹出同意弹框（页面里的 privacy-sheet 监听） */
export const PRIVACY_REQUIRED_EVENT = 'privacy-required'

/** 关闭同意弹框 */
export const PRIVACY_CLOSE_EVENT = 'privacy-sheet-close'

/** 微信那次询问的放行函数，等用户点完再回调；为空表示当前没人在等 */
let privacyResolve = null

/** 是否已经同意过隐私协议 */
export function hasAgreedPrivacy() {
	return !!uni.getStorageSync(PRIVACY_AGREED_KEY)
}

/** 还没同意过就弹出同意弹框（页面挂载时调用；已同意则什么都不做） */
export function requirePrivacyAgreement() {
	if (hasAgreedPrivacy()) {
		return
	}

	uni.$emit(PRIVACY_REQUIRED_EVENT)
}

/**
 * 用户点了「同意并继续」
 *
 * 同意结果只记在本地：微信侧那份《用户隐私保护指引》由公众平台维护，
 * 这里要回应的是微信 onNeedPrivacyAuthorization 的那次询问（如果它正在等）。
 */
export function agreePrivacy() {
	uni.setStorageSync(PRIVACY_AGREED_KEY, 1)
	uni.$emit(PRIVACY_CLOSE_EVENT)
	resolvePrivacy('agree')
}

/** 用户点了「暂不同意」：不记本地，下次进来还会再问一次 */
export function rejectPrivacy() {
	uni.$emit(PRIVACY_CLOSE_EVENT)
	resolvePrivacy('disagree')
}

/**
 * 启动时挂上微信的隐私授权监听
 *
 * 开启 __usePrivacyCheck__ 后，微信调用需授权接口前会走到这里：
 * 没同意过就把这次询问转成自己的弹框，用户同意了再放行。
 */
export function initPrivacyListener() {
	// #ifdef MP-WEIXIN
	if (typeof wx !== 'undefined' && typeof wx.onNeedPrivacyAuthorization === 'function') {
		wx.onNeedPrivacyAuthorization((resolve) => {
			// 已经同意过就直接放行，不再打扰
			if (hasAgreedPrivacy()) {
				resolve({ event: 'agree' })

				return
			}

			privacyResolve = resolve
			uni.$emit(PRIVACY_REQUIRED_EVENT)
		})
	}
	// #endif
}

/** 把结果回给微信；没人在等就忽略 */
function resolvePrivacy(event) {
	if (!privacyResolve) {
		return
	}

	privacyResolve({ event })
	privacyResolve = null
}
