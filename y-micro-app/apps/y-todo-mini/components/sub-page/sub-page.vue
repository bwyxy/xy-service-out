<script setup>
/**
 * 子页面通用布局
 *
 * 统一「状态栏留白 + 返回顶栏 + 标题区 + 内容插槽」结构，
 * 各子页面只需传入 title / subtitle 并填充默认插槽。
 * 状态栏留白与顶栏滚动时吸顶，标题区随内容滚走。
 * 需要固定底部操作栏时，填充 footer 插槽并开启 fixedFooter。
 * 主题与字号类挂在最外层，子页面内的组件靠继承自动跟随主题。
 */
import { themeClass } from '@/utils/appearance'

const props = defineProps({
	/** 页面主标题 */
	title: { type: String, default: '' },
	/** 页面副标题 */
	subtitle: { type: String, default: '' },
	/** 是否存在固定底部栏，开启后内容区留出避让空间 */
	fixedFooter: { type: Boolean, default: false }
})

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 返回上一页，无历史栈时回到首页 */
function handleBack() {
	if (getCurrentPages().length > 1) {
		uni.navigateBack()

		return
	}

	uni.switchTab({ url: '/pages/index/index' })
}

/** 顶栏通用按钮（待接入菜单与主题） */
function handlePlaceholder(label) {
	uni.showToast({ title: `${label}（待开发）`, icon: 'none' })
}
</script>

<template>
	<view class="sub-page" :class="[themeClass, { 'sub-page--fixed-footer': fixedFooter }]">
		<view class="sub-page__top">
			<view :style="{ height: statusBarHeight + 'px' }" />

			<brand-header
				show-back
				@back="handleBack"
				@more="handlePlaceholder('更多菜单')"
				@theme="handlePlaceholder('主题切换')"
			/>
		</view>

		<view class="sub-page__hero">
			<text class="sub-page__title">{{ title }}</text>
			<text class="sub-page__subtitle">{{ subtitle }}</text>
		</view>

		<slot />

		<slot name="footer" />

		<login-sheet />
		<privacy-sheet />
	</view>
</template>

<style scoped lang="scss">
.sub-page {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 80rpx;
	box-sizing: border-box;
}

.sub-page--fixed-footer {
	padding-bottom: 240rpx;
}

/*
 * 顶栏吸顶
 *
 * 状态栏留白与品牌顶栏一起钉在页面顶部：页面内容滚到下方时仍能返回和切换主题，
 * 背景用页面底色铺满，避免下方内容从顶栏透出来。
 */
.sub-page__top {
	position: sticky;
	top: 0;
	z-index: 100;
	background-color: $y-page-bg;
}

/* 标题 */
.sub-page__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.sub-page__title {
	font-size: calc(52rpx * var(--y-font-scale, 1));
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.sub-page__subtitle {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}
</style>
