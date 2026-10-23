<script>
	import { initSession } from '@/utils/session'
	import { initAppearance, initGetMiniVersion } from '@/utils/appearance'
	import { initPrivacyListener } from '@/utils/privacy'

	export default {
		onLaunch: function() {
			// 主题、字号与显示偏好来自本地缓存，越早恢复越好，避免启动时闪一下浅色
			initAppearance()
			// 默认不登录：只在本地已有 token 时静默恢复登录态，不主动发起微信登录
			initSession()

			initGetMiniVersion()
			// 接管微信隐私授权：未同意时交由 privacy-sheet 弹框，同意后直接放行
			initPrivacyListener()
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

	/* ===== 外观与显示：主题颜色变量 =====
	 *
	 * 页面根节点挂上 theme-light / theme-dark（由 utils/appearance.js 的 themeClass 给出），
	 * 页面内所有组件都从根节点继承这套变量，因此切换主题不需要逐个组件处理。
	 * 这里的值必须与 uni.scss 里同名变量后面的浅色兜底值保持一致。
	 */
	.theme-light {
		--y-green-primary: #4a6b3e;
		--y-green-soft: #e5ebd9;
		--y-green-tag-bg: #e9efe0;
		--y-green-tag-text: #62804f;
		--y-green-track: #dde5d2;
		--y-green-done: #9aae8c;

		--y-text-main: #2c2c27;
		--y-text-sub: #8e8e85;
		--y-text-muted: #a9a9a0;

		--y-page-bg: #f7f7f2;
		--y-card-bg: #ffffff;
		--y-divider: #ebebe4;

		--y-sunken-bg: #f2f2ec;
		--y-header-bg: #efefe9;
		--y-green-text: #6f8064;
		--y-green-border: #d8e2cb;
		--y-check-border: #ccd1c3;
		--y-contrast-chip: #3a3a34;
		--y-text-body: #5c5c55;
		--y-glass-bg: rgba(255, 255, 255, 0.72);
		--y-tabbar-bg: rgba(247, 247, 242, 0.94);
		--y-warm-soft-bg: #f8ebdf;
		--y-warm-border: #e8c7a8;
		--y-accent-warm: #a05a3f;
		--y-flame: #c2793c;
		--y-danger: #b5543f;
	}

	/* 深色模式：绿与暖色都提亮一档，保证在近黑底上仍有对比度 */
	.theme-dark {
		--y-green-primary: #93b878;
		--y-green-soft: #2b3325;
		--y-green-tag-bg: #2d3527;
		--y-green-tag-text: #a6c489;
		--y-green-track: #3b4533;
		--y-green-done: #6f8a5e;

		--y-text-main: #e9e9e1;
		--y-text-sub: #a0a098;
		--y-text-muted: #79796f;

		--y-page-bg: #191917;
		--y-card-bg: #24241f;
		--y-divider: #333330;

		--y-sunken-bg: #202019;
		--y-header-bg: #232320;
		--y-green-text: #a3c186;
		--y-green-border: #3f4b36;
		--y-check-border: #4c5744;
		--y-contrast-chip: #d5d5cc;
		--y-text-body: #c8c8c0;
		--y-glass-bg: rgba(36, 36, 31, 0.72);
		--y-tabbar-bg: rgba(25, 25, 23, 0.94);
		--y-warm-soft-bg: #3a2b22;
		--y-warm-border: #6b4c34;
		--y-accent-warm: #dda184;
		--y-flame: #e0a262;
		--y-danger: #e0836f;
	}

	/* 字号档位：只给出缩放比例，具体字号由各文件里的 calc(Nrpx * var(--y-font-scale)) 算出 */
	.size-small {
		--y-font-scale: 0.9;
	}

	.size-normal {
		--y-font-scale: 1;
	}

	.size-large {
		--y-font-scale: 1.15;
	}
</style>
