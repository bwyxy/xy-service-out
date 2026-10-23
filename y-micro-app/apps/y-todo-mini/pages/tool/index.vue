<script setup>
import { onShow } from '@dcloudio/uni-app'
import { themeClass } from '@/utils/appearance'

	/**
	 * 工具 · 工具首页
	 *
	 * 路由：pages/tool/index（主包）
	 * 职责：工具 tab 主页，让专注，轻一点——含番茄专注卡、日常好帮手 2×2 与清单模板入口。
	 * 目录：状态（helpers、focusMinutes、FOCUS_TOOL、TEMPLATE_TOOL）→ 方法（openTool、handleHeader）→ 样式（.tool、.focus、.helper、.template）
	 */

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 日常好帮手 */
const helpers = [
	{ key: 'calendar', name: '日历视图', desc: '按日期安排每一件事', path: '/subpages/tool/calendar/index' },
	{ key: 'grid', name: '四象限', desc: '先做真正重要的事', path: '/subpages/tool/matrix/index' },
	{ key: 'sprout', name: '习惯打卡', desc: '让小坚持慢慢生长', path: '/subpages/tool/habit/index' },
	{ key: 'chart', name: '待办统计', desc: '看见每一步的积累', path: '/subpages/tool/stats/index' }
]

/** 番茄专注时长（分钟） */
const focusMinutes = '25:00'

/** 番茄专注与清单模板入口 */
const FOCUS_TOOL = { path: '/subpages/tool/focus/index', name: '番茄专注' }
const TEMPLATE_TOOL = { path: '/subpages/tool/template/index', name: '清单模板' }

/** 打开工具 */
function openTool(item) {
	if (item.path) {
		uni.navigateTo({ url: item.path })

		return
	}

	uni.showToast({ title: `${item.name}（待开发）`, icon: 'none' })
}

/** 顶栏按钮（待接入菜单与主题） */
function handleHeader(label) {
	uni.showToast({ title: `${label}（待开发）`, icon: 'none' })
}

/** 隐藏原生 tabBar，改用自定义导航栏 */
onShow(() => {
	uni.hideTabBar()
})
</script>

<template>
	<view class="tool" :class="themeClass">
		<view class="tool__top">
			<view :style="{ height: statusBarHeight + 'px' }" />

			<brand-header @more="handleHeader('更多菜单')" @theme="handleHeader('主题切换')" />
		</view>

		<view class="tool__hero">
			<text class="tool__title">让专注，轻一点</text>
			<text class="tool__subtitle">一些小工具，陪你把日常安排好。</text>
		</view>

		<view class="focus">
			<view class="focus__main">
				<text class="focus__name">番茄专注</text>
				<text class="focus__desc">留出 25 分钟，只做一件事。</text>
				<view class="focus__start" @click="openTool(FOCUS_TOOL)">
					<text class="focus__start-text">开始专注</text>
					<y-icon name="arrow-right" size="28rpx" />
				</view>
			</view>

			<view class="focus__timer">
				<text class="focus__time">{{ focusMinutes }}</text>
				<text class="focus__hint">慢慢来，更专注</text>
			</view>
		</view>

		<view class="section">
			<text class="section__title">日常好帮手</text>

			<view class="helpers">
				<view
					v-for="item in helpers"
					:key="item.key"
					class="helper"
					@click="openTool(item)"
				>
					<y-icon :name="item.key" size="44rpx" />
					<text class="helper__name">{{ item.name }}</text>
					<text class="helper__desc">{{ item.desc }}</text>
				</view>
			</view>
		</view>

		<view class="template" @click="openTool(TEMPLATE_TOOL)">
			<view class="template__icon">
				<y-icon name="file-text" size="44rpx" />
			</view>

			<view class="template__text">
				<text class="template__name">清单模板</text>
				<text class="template__desc">晨间计划、旅行准备，从容开始</text>
			</view>

			<y-icon name="chevron-right" size="32rpx" color="#b7b7ae" />
		</view>

		<text class="tool__foot">工具不必很多，顺手就好。</text>

		<custom-tabbar active="pages/tool/index" />

		<login-sheet />
		<privacy-sheet />
	</view>
</template>

<style scoped lang="scss">
.tool {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 180rpx;
	box-sizing: border-box;
}

/* 顶栏吸顶：状态栏留白与品牌顶栏一起钉在页面顶部，背景铺页面底色避免内容透出 */
.tool__top {
	position: sticky;
	top: 0;
	z-index: 100;
	background-color: $y-page-bg;
}

/* 标题 */
.tool__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.tool__title {
	font-size: calc(52rpx * var(--y-font-scale, 1));
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.tool__subtitle {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 番茄专注 */
.focus {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 32rpx;
	padding: 40rpx;
	border-radius: 32rpx;
	background-color: $y-green-soft;
}

.focus__main {
	flex: 1;
	display: flex;
	flex-direction: column;
}

.focus__name {
	font-size: calc(34rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.focus__desc {
	margin-top: 16rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

.focus__start {
	display: flex;
	align-items: center;
	margin-top: 24rpx;
}

.focus__start-text {
	margin-right: 8rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-green-primary;
}

.focus__timer {
	flex-shrink: 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	width: 190rpx;
	height: 190rpx;
	margin-left: 24rpx;
	border-radius: 50%;
	border: 2rpx solid $y-green-border;
	background-color: $y-glass-bg;
}

.focus__time {
	font-size: calc(40rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.focus__hint {
	margin-top: 8rpx;
	font-size: calc(20rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

/* 日常好帮手 */
.section {
	margin-top: 48rpx;
}

.section__title {
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.helpers {
	display: flex;
	flex-wrap: wrap;
	margin-top: 24rpx;
}

.helper {
	display: flex;
	flex-direction: column;
	width: calc(50% - 12rpx);
	margin-bottom: 20rpx;
	padding: 32rpx 28rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
	box-sizing: border-box;

	&:nth-child(2n) {
		margin-left: 20rpx;
	}
}

.helper__name {
	margin-top: 20rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-text-main;
}

.helper__desc {
	margin-top: 12rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 清单模板 */
.template {
	display: flex;
	align-items: center;
	margin-top: 12rpx;
	padding: 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.template__icon {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 80rpx;
	height: 80rpx;
	margin-right: 24rpx;
	border-radius: 24rpx;
	background-color: $y-green-tag-bg;
}

.template__text {
	flex: 1;
	display: flex;
	flex-direction: column;
}

.template__name {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-text-main;
}

.template__desc {
	margin-top: 10rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 页脚 */
.tool__foot {
	display: block;
	margin-top: 56rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
	text-align: center;
}
</style>
