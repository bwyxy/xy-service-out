<script>
	import { getToken } from '@/utils/auth'
	import useUserStore from '@/store/user'
	import useChatStore from '@/store/chat'

	/** 强制下线默认提示文案 */
	const DEFAULT_FORCE_LOGOUT_REASON = '您的账号已在其他同类型设备上登录，当前设备已被强制退出'

	/** 防止并发重复弹窗 */
	let loggingOut = false

	/**
	 * 兼容强制下线信令 content 为 JSON 字符串或已解析对象两种形式
	 */
	function parseContent(raw) {
		if (!raw) {
			return null
		}
		if (typeof raw === 'string') {
			try {
				return JSON.parse(raw)
			} catch {
				return null
			}
		}
		if (typeof raw === 'object') {
			return raw
		}
		return null
	}

	/**
	 * 统一触发：清理登录态 + 全局弹窗 + 回登录页
	 *
	 * App 端 App.vue 的 template 不参与渲染，因此使用 uni.showModal 替代网页端的弹窗组件。
	 */
	function triggerForceLogout(reason) {
		if (loggingOut) {
			return
		}
		loggingOut = true

		try {
			useUserStore().outLogin()
		} catch (error) {
			console.error('[App] 清理登录态失败', error)
		}

		uni.showModal({
			title: '登录已失效',
			content: reason || DEFAULT_FORCE_LOGOUT_REASON,
			showCancel: false,
			confirmText: '重新登录',
			complete: () => {
				loggingOut = false
				uni.reLaunch({ url: '/pages/login/index' })
			},
		})
	}

	/**
	 * 服务端 220 信令：同类型设备新登录导致当前设备被强制下线
	 */
	function handleForceLogout(event) {
		const parsed = parseContent(event && event.content)
		const kickedToken = parsed && parsed.kickedToken

		// token 校验：若服务端标记的会话 token 与本机持有的不一致，
		// 说明本机已是新登录的设备（旧会话被踢的是另一台），忽略该通知
		if (kickedToken && kickedToken !== getToken()) {
			return
		}

		triggerForceLogout(parsed && parsed.reason)
	}

	/**
	 * 接口返回登录态失效（HTTP 401 / 业务码 4011）时同样走强制退出流程
	 */
	function handleSessionExpired(event) {
		triggerForceLogout(event && event.reason)
	}

	/**
	 * 启动初始化
	 *
	 * 未登录直接进登录页；已登录则拉取用户信息并建立 IM 连接。
	 * 使用 bootstrapping 标记避免 onLaunch 与 onShow 并发重复初始化。
	 */
	let bootstrapping = false

	async function bootstrap() {
		if (bootstrapping) {
			return
		}

		if (!getToken()) {
			uni.reLaunch({ url: '/pages/login/index' })
			return
		}

		bootstrapping = true

		try {
			await useUserStore().setUserInfo()
			await useChatStore().initWKIM()
		} catch (error) {
			// 登录态失效时 request.js 已派发 session-expired，此处仅记录
			console.error('[App] 启动初始化失败', error)
		} finally {
			bootstrapping = false
		}
	}

	export default {
		onLaunch: function() {
			console.log('App Launch')

			uni.$on('session-expired', handleSessionExpired)
			uni.$on('force-logout', handleForceLogout)

			bootstrap()
		},
		onShow: function() {
			console.log('App Show')

			// 从后台恢复时若尚未建立 IM 连接（例如进程被回收后重建），补齐连接
			const chatStore = useChatStore()
			if (getToken() && !chatStore.connected) {
				bootstrap()
			}
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style lang="scss">
	@import 'uni.scss';

	page {
		background: #f0f5ff;
	}

	.app-container {
		width: 100vw;
		height: 100vh;
		overflow: hidden;
		display: flex;
		flex-direction: column;
	}
</style>
