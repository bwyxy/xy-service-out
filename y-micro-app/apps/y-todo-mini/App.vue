<script>
	import { initSession } from '@/utils/session'

	export default {
		onLaunch: function() {
			// 默认不登录：只在本地已有 token 时静默恢复登录态，不主动发起微信登录
			initSession()
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	/*每个页面公共css */
</style>
