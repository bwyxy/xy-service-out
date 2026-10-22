<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { user, initSession, requireLogin } from '@/utils/session'

	/**
	 * 我的 · 我的
	 *
	 * 路由：pages/me/index（主包）
	 * 职责：我的 tab 主页，照顾好每一个自己——展示用户卡、统计与设置分组；默认不登录，点击用户卡拉起登录半弹框。
	 * 目录：状态（statusBarHeight、profile、stats、settings）→ 方法（openSetting、handleHeader、handleProfile）→ 样式（.me、.profile、.stats、.section、.row）
	 */

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 用户卡展示信息：昵称与头像取自登录态，未登录时给出引导文案 */
const profile = computed(() => ({
	name: user.nickname || '微信用户',
	desc: user.logged ? '和忆一起，慢慢成长' : '登录后同步你的清单与专注记录',
	avatar: user.avatar
}))

/** 统计（暂用本地数据） */
const stats = [
	{ value: 128, label: '累计完成' },
	{ value: 12, label: '连续记录 / 天' },
	{ value: 4, label: '我的清单' }
]

/** 设置项分组 */
const settings = [
	[
		{ key: 'bell', label: '提醒设置', value: '已开启', path: '/subpages/me/reminder/index' },
		{ key: 'contrast', label: '外观与显示', value: '跟随系统', path: '/subpages/me/appearance/index' },
		{ key: 'refresh', label: '数据同步与备份', value: '已同步', path: '/subpages/me/sync/index' }
	],
	[
		{ key: 'archive', label: '已完成与归档', value: '', path: '/subpages/me/archive/index' },
		{ key: 'help', label: '帮助与反馈', value: '', path: '/subpages/me/help/index' },
		{ key: 'info', label: '关于忆待办', value: 'v1.0.0', path: '/subpages/me/about/index' }
	]
]

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
		// 用户取消登录不额外提示，弹框自身已给反馈
		requireLogin().catch(() => {})

		return
	}

	uni.navigateTo({ url: '/subpages/me/profile/index' })
}

/** 隐藏原生 tabBar，改用自定义导航栏；启动时若本地已有 token，静默恢复登录态 */
onShow(() => {
	uni.hideTabBar()
	initSession()
})
</script>

<template>
	<view class="me">
		<view :style="{ height: statusBarHeight + 'px' }" />

		<brand-header @more="handleHeader('更多菜单')" @theme="handleHeader('主题切换')" />

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
	</view>
</template>

<style scoped lang="scss">
.me {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 180rpx;
	box-sizing: border-box;
}

/* 标题 */
.me__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.me__title {
	font-size: 52rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.me__subtitle {
	margin-top: 16rpx;
	font-size: 26rpx;
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
	background-color: rgba(255, 255, 255, 0.72);
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
	font-size: 36rpx;
	font-weight: 600;
	color: $y-text-main;
}

.profile__desc {
	margin-top: 12rpx;
	font-size: 24rpx;
	color: #6f8064;
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
		border-left: 2rpx solid #d3ddc6;
	}
}

.stats__value {
	font-size: 48rpx;
	font-weight: 600;
	color: $y-text-main;
}

.stats__label {
	margin-top: 10rpx;
	font-size: 22rpx;
	color: #6f8064;
}

/* 设置 */
.section {
	margin-top: 48rpx;
}

.section__title {
	font-size: 32rpx;
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
	font-size: 30rpx;
	color: $y-text-main;
}

.row__value {
	margin-right: 12rpx;
	font-size: 26rpx;
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
	font-size: 24rpx;
	color: $y-text-sub;
}

.me__foot-brand {
	margin-top: 12rpx;
	font-size: 22rpx;
	color: $y-text-muted;
}
</style>
