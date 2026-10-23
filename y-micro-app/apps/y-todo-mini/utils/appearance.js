import { computed, reactive } from 'vue'

/**
 * 外观与显示设置
 *
 * 主题与字号都要立刻生效，因此颜色统一走 CSS 变量（--y-*）：
 * 页面根节点挂上 themeClass 后，页面内的组件靠样式继承自动跟随，不必逐个改组件；
 * 字号则由 --y-font-scale 缩放（各文件写成 calc(Nrpx * var(--y-font-scale, 1))）。
 * 设置只存本地，不同步后端。
 */

/** 本地缓存键 */
const STORAGE_KEY = 'y-appearance'

export const MINI_VERSION= 'y-mini-version'

/** 可持久化的设置项与默认值 */
const DEFAULTS = {
	/** system-跟随系统，light-浅色，dark-深色 */
	theme: 'system',
	/** small-小，normal-标准，large-大 */
	fontSize: 'normal',
	/** 首页是否显示「已完成」分区 */
	showDone: true,
	/** 首页是否显示今日进度卡 */
	showProgress: true,
	/** 周视图是否从周一开始 */
	weekStart: true
}

/** 主题可选值 */
const THEMES = ['system', 'light', 'dark']

/** 字号可选值 */
const FONT_SIZES = ['small', 'normal', 'large']

/**
 * 当前外观设置
 *
 * systemTheme 不是设置项，而是「跟随系统」时读到的系统主题，
 * 放在同一个 reactive 里是为了让 themeClass 能跟着它一起刷新。
 */
export const appearance = reactive({ ...DEFAULTS, systemTheme: 'light' })

/** 当前实际主题：跟随系统时取系统主题 */
export function resolveTheme() {
	return appearance.theme === 'system' ? appearance.systemTheme : appearance.theme
}

/** 页面根节点要挂的类名：主题一套颜色变量、字号一档缩放比例，定义在 App.vue */
export const themeClass = computed(() => `theme-${resolveTheme()} size-${appearance.fontSize}`)

/** 把设置项写回本地 */
function persist() {
	const data = {}

	Object.keys(DEFAULTS).forEach((key) => {
		data[key] = appearance[key]
	})

	uni.setStorageSync(STORAGE_KEY, data)
}

/**
 * 同步系统主题
 *
 * 只有「跟随系统」时才用得上；平台不支持 onThemeChange 时静默跳过，保持浅色。
 */
function watchSystemTheme() {
	try {
		appearance.systemTheme = uni.getSystemInfoSync().theme === 'dark' ? 'dark' : 'light'
	} catch (error) {
		appearance.systemTheme = 'light'
	}

	if (typeof uni.onThemeChange !== 'function') {
		return
	}

	uni.onThemeChange(({ theme }) => {
		appearance.systemTheme = theme === 'dark' ? 'dark' : 'light'
		applyPageStyle()
	})
}

/**
 * 页面与导航栏底色跟随主题
 *
 * 小程序原生底色不参与 CSS 变量，只能主动设置；
 * 页面内容区已由各页根节点的背景铺满，这里主要管下拉回弹露出的一截。
 */
export function applyPageStyle() {
	// #ifdef MP-WEIXIN
	const backgroundColor = resolveTheme() === 'dark' ? '#191917' : '#f7f7f2'

	uni.setBackgroundColor({
		backgroundColor,
		backgroundColorTop: backgroundColor,
		backgroundColorBottom: backgroundColor
	})
	// #endif
}

/** 切换主题 */
export function setTheme(theme) {
	if (!THEMES.includes(theme) || appearance.theme === theme) {
		return
	}

	appearance.theme = theme
	persist()
	applyPageStyle()
}

/** 切换字号 */
export function setFontSize(fontSize) {
	if (!FONT_SIZES.includes(fontSize) || appearance.fontSize === fontSize) {
		return
	}

	appearance.fontSize = fontSize
	persist()
}

/** 切换显示偏好（showDone / showProgress / weekStart） */
export function setPreference(key, value) {
	if (typeof DEFAULTS[key] !== 'boolean') {
		return
	}

	appearance[key] = !!value
	persist()
}

/** 启动时读本地缓存并开始跟随系统主题 */
export function initAppearance() {
	let saved = null

	try {
		saved = uni.getStorageSync(STORAGE_KEY)
	} catch (error) {
		saved = null
	}

	if (saved && typeof saved === 'object') {
		Object.keys(DEFAULTS).forEach((key) => {
			const value = saved[key]

			// 单选类字段要挡掉脏值，开关类字段统一收敛成布尔
			if (key === 'theme' && !THEMES.includes(value)) {
				return
			}

			if (key === 'fontSize' && !FONT_SIZES.includes(value)) {
				return
			}

			if (value !== undefined) {
				appearance[key] = typeof DEFAULTS[key] === 'boolean' ? !!value : value
			}
		})
	}

	watchSystemTheme()
	applyPageStyle()
}

/** 启动时获取小程序版本号 */
export function initGetMiniVersion() {
	const { miniProgram: { envVersion, version } } =	uni.getAccountInfoSync()

	const env = envVersion=='develop'?'开发版':envVersion=='trial'?'体验版':version
	uni.setStorageSync(MINI_VERSION, env)
}
