<script setup>
import { computed } from 'vue'
import { MINI_VERSION } from '@/utils/appearance.js'

	/**
	 * 我的 · 关于忆
	 *
	 * 路由：subpages/me/about/index（分包 subpages/me，按需加载）
	 * 职责：关于忆页，让每一件小事都被记住——品牌信息、简介与协议条目。
	 * 目录：状态（version、items）→ 方法（openItem）→ 样式（.brand、.intro、.group、.foot）
	 */

/** 版本号 */
const version = computed(() => {
	return uni.getStorageSync(MINI_VERSION) || '1.0.0'
})

/** 关于信息 */
const items = [
	{ key: 'update', label: '检查更新', value: `已是最新版本 ${version.value}` },
	{ key: 'terms', label: '用户协议', value: '' },
	{ key: 'privacy', label: '隐私政策', value: '' }
]

/** 打开条目：协议跳协议页对应分页，检查更新只提示当前版本 */
function openItem(item) {
	if (item.key === 'privacy' || item.key === 'terms') {
		uni.navigateTo({ url: `/subpages/me/agreement/index?tab=${item.key}` })

		return
	}

	uni.showToast({ title: `当前已是最新版本 ${version.value}`, icon: 'none' })
}
</script>

<template>
	<sub-page title="关于忆待办" subtitle="让每一件小事，都被记住。">
		<view class="brand">
			<view class="brand__logo">
				<image class="brand__image" src="/static/top_icon.png" mode="aspectFit" />
			</view>

			<text class="brand__name">忆 · 待办</text>
			<text class="brand__version">{{ version }}</text>
		</view>

		<view class="intro">
			<text class="intro__text">
				忆待办是一款安静的待办小工具：把要做的事写下来，一件一件慢慢做完，回头看时，日子是有痕迹的。
			</text>
		</view>

		<view class="group">
			<view v-for="item in items" :key="item.key" class="row" @click="openItem(item)">
				<text class="row__label">{{ item.label }}</text>
				<text v-if="item.value" class="row__value">{{ item.value }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>
		</view>

		<view class="foot">
			<text class="foot__brand">忆 · 待办</text>
			<text class="foot__copy">© 2026 忆 · 待办</text>
		</view>
	</sub-page>
</template>

<style scoped lang="scss">
/* 品牌 */
.brand {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 56rpx;
}

.brand__logo {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 160rpx;
	height: 160rpx;
	border-radius: 44rpx;
	background-color: $y-green-soft;
}

.brand__image {
	width: 80rpx;
	height: 80rpx;
}

.brand__name {
	margin-top: 28rpx;
	font-size: calc(36rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.brand__version {
	margin-top: 12rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 简介 */
.intro {
	margin-top: 40rpx;
	padding: 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.intro__text {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	line-height: 44rpx;
	color: $y-text-sub;
}

/* 条目 */
.group {
	margin-top: 24rpx;
	padding: 4rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.row {
	display: flex;
	align-items: center;
	padding: 30rpx 0;

	& + .row {
		border-top: 2rpx solid $y-divider;
	}
}

.row__label {
	flex: 1;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__value {
	margin-right: 12rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 页脚 */
.foot {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 64rpx;
}

.foot__brand {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.foot__copy {
	margin-top: 12rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}
</style>
