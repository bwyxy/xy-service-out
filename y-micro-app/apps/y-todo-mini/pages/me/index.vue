<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getTaskStats } from '@/api/task'
import { user, isLoggedIn, initSession, requireLogin } from '@/utils/session'
import { appearance, themeClass, MINI_VERSION } from '@/utils/appearance'

	/**
 * 我的 · 我的
 *
 * 路由：pages/me/index（主包）
 * 职责：我的 tab 主页，照顾好每一个自己——展示用户卡、统计与设置分组；默认不登录，点击用户卡拉起登录半弹框。
 * 数据：统计走 GET /task/stats（累计完成 / 连续记录天数 / 清单数），未登录时不请求，只显示占位符。
 * 目录：状态（statusBarHeight、summary、profile、stats、settings）→ 方法（refreshStats、openSetting、handleHeader、handleProfile）→ 样式（.me、.profile、.stats、.section、.row）
 */

const version = computed(() => {
	return uni.getStorageSync(MINI_VERSION) || '1.0.0'
})

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 统计原始值：null 表示还没拿到（未登录或请求未回来），展示成占位符 */
const summary = ref({ doneTotal: null, streakDays: null, tagCount: null })

/** 没有数字时显示占位符，别把「未登录」显示成 0 完成 */
function showNumber(value) {
	return value == null ? '—' : value
}

/** 统计：三项都来自 GET /task/stats */
const stats = computed(() => [
	{ value: showNumber(summary.value.doneTotal), label: '累计完成' },
	{ value: showNumber(summary.value.streakDays), label: '连续记录 / 天' },
	{ value: showNumber(summary.value.tagCount), label: '我的清单' }
])

/** 拉统计数据：未登录不发请求，免得一进「我的」就被拉起登录半弹框 */
async function refreshStats() {
	if (!isLoggedIn()) {
		return
	}

	const res = await getTaskStats()

	if (res.code === 200 && res.data) {
		summary.value = res.data
	}
}

/** 用户卡展示信息：昵称与头像取自登录态，未登录时给出引导文案 */
const profile = computed(() => ({
	name: user.nickname || '微信用户',
	desc: user.logged ? '和忆一起，慢慢成长' : '登录后同步你的清单与专注记录',
	avatar: user.avatar
}))

/** 进入清单管理（新建 / 改名换图标 / 删除） */
function manageTags() {
	uni.navigateTo({ url: '/subpages/common/tag/index' })
}

/** 外观与显示的现值摘要：主题 + 字号 */
const appearanceLabel = computed(() => {
	const themeLabel = { system: '跟随系统', light: '浅色', dark: '深色' }[appearance.theme]
	const sizeLabel = { small: '小字', normal: '标准字号', large: '大字' }[appearance.fontSize]

	return `${themeLabel || '跟随系统'} · ${sizeLabel || '标准字号'}`
})

/** 设置项分组：外观与显示那行的值跟着实际设置走 */
const settings = computed(() => [
	[
		{ key: 'list', label: '清单列表', value: '', path: '/subpages/common/tag/index' },
		{ key: 'bell', label: '提醒设置', value: '已开启', path: '/subpages/me/reminder/index' },
		{ key: 'contrast', label: '外观与显示', value: appearanceLabel.value, path: '/subpages/me/appearance/index' },
		// { key: 'refresh', label: '数据同步与备份', value: '已同步', path: '/subpages/me/sync/index' }
	],
	[
		{ key: 'archive', label: '已完成与归档', value: '', path: '/subpages/me/archive/index' },
		{ key: 'help', label: '帮助与反馈', value: '', path: '/subpages/me/help/index' },
		{ key: 'info', label: '关于忆待办', value: `v${version.value}`, path: '/subpages/me/about/index' }
	]
])

/** 打开设置项 */
function openSetting(item) {
	if (item.path) {
		uni.navigateTo({ url: item.path })

		return
	}

	uni.showToast({ title: `${item.label}（待开发）`, icon: 'none' })
}

/** 顶栏按钮（待接入菜单与主题） */
function handleHeader(label) {
	uni.showToast({ title: `${label}（待开发）`, icon: 'none' })
}

/** 点击用户卡：未登录先拉起登录半弹框，已登录进入个人资料 */
function handleProfile() {
	if (!user.logged) {
		// 用户取消登录不额外提示，弹框自身已给反馈；登录成功后把统计补上
		requireLogin().then(refreshStats).catch(() => {})

		return
	}

	uni.navigateTo({ url: '/subpages/me/profile/index' })
}

/** 隐藏原生 tabBar，改用自定义导航栏；启动时若本地已有 token，静默恢复登录态并拉统计 */
onShow(() => {
	uni.hideTabBar()
	initSession().then(refreshStats)
})
</script>

<template>
	<view class="me" :class="themeClass">
		<view class="me__top">
			<view :style="{ height: statusBarHeight + 'px' }" />

			<brand-header @more="handleHeader('更多菜单')" @theme="handleHeader('主题切换')" />
		</view>

		<view class="me__hero">
			<text class="me__title">照顾好，每一个自己</text>
			<text class="me__subtitle">把日子过成喜欢的样子。</text>
		</view>

		<view class="profile" @click="handleProfile">
			<view class="profile__avatar">
				<image
					v-if="profile.avatar"
					class="profile__avatar-img"
					:src="profile.avatar"
					mode="aspectFill"
				/>
				<y-icon v-else name="sprout" size="48rpx" />
			</view>

			<view class="profile__text">
				<text class="profile__name">{{ profile.name }}</text>
				<text class="profile__desc">{{ profile.desc }}</text>
			</view>

			<y-icon name="chevron-right" size="32rpx" color="#8fa37f" />
		</view>

		<view class="stats">
			<view v-for="item in stats" :key="item.label" class="stats__item">
				<text class="stats__value">{{ item.value }}</text>
				<text class="stats__label">{{ item.label }}</text>
			</view>
		</view>

		<view class="section">
			<text class="section__title">我的设置</text>

			<view v-for="(group, index) in settings" :key="index" class="group">
				<view
					v-for="item in group"
					:key="item.key"
					class="row"
					@click="openSetting(item)"
				>
					<y-icon :name="item.key" size="38rpx" />
					<text class="row__label">{{ item.label }}</text>
					<text v-if="item.value" class="row__value">{{ item.value }}</text>
					<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
				</view>
			</view>
		</view>

		<view class="me__foot">
			<text class="me__foot-text">每一件小事，都算数。</text>
			<text class="me__foot-brand">忆 · 待办</text>
		</view>

		<custom-tabbar active="pages/me/index" />

		<login-sheet />
		<privacy-sheet />
	</view>
</template>

<style scoped lang="scss">
.me {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 180rpx;
	box-sizing: border-box;
}

/* 顶栏吸顶：状态栏留白与品牌顶栏一起钉在页面顶部，背景铺页面底色避免内容透出 */
.me__top {
	position: sticky;
	top: 0;
	z-index: 100;
	background-color: $y-page-bg;
}

/* 标题 */
.me__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.me__title {
	font-size: calc(52rpx * var(--y-font-scale, 1));
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.me__subtitle {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 用户卡片 */
.profile {
	display: flex;
	align-items: center;
	margin-top: 32rpx;
	padding: 32rpx;
	border-radius: 32rpx;
	background-color: $y-green-soft;
}

.profile__avatar {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 108rpx;
	height: 108rpx;
	margin-right: 24rpx;
	border-radius: 50%;
	background-color: $y-glass-bg;
	overflow: hidden;
}

.profile__avatar-img {
	width: 100%;
	height: 100%;
}

.profile__text {
	flex: 1;
	display: flex;
	flex-direction: column;
}

.profile__name {
	font-size: calc(36rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.profile__desc {
	margin-top: 12rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

/* 统计 */
.stats {
	display: flex;
	align-items: center;
	margin-top: 24rpx;
	padding: 36rpx 0;
	border-radius: 32rpx;
	background-color: $y-green-soft;
}

.stats__item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;

	& + & {
		border-left: 2rpx solid $y-green-border;
	}
}

.stats__value {
	font-size: calc(48rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.stats__label {
	margin-top: 10rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

/* 设置 */
.section {
	margin-top: 48rpx;
}

.section__title {
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.group {
	margin-top: 24rpx;
	padding: 8rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.row {
	display: flex;
	align-items: center;
	padding: 30rpx 0;

	& + & {
		border-top: 2rpx solid $y-divider;
	}
}

.row__label {
	flex: 1;
	margin-left: 24rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__value {
	margin-right: 12rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 页脚 */
.me__foot {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 60rpx;
}

.me__foot-text {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.me__foot-brand {
	margin-top: 12rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}
</style>
