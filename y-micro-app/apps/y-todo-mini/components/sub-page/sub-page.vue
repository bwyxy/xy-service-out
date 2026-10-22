<script setup>
/**
 * 子页面通用布局
 *
 * 统一「状态栏留白 + 返回顶栏 + 标题区 + 内容插槽」结构，
 * 各子页面只需传入 title / subtitle 并填充默认插槽。
 * 需要固定底部操作栏时，填充 footer 插槽并开启 fixedFooter。
 */
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
	<view class="sub-page" :class="{ 'sub-page--fixed-footer': fixedFooter }">
		<view :style="{ height: statusBarHeight + 'px' }" />

		<brand-header
			show-back
			@back="handleBack"
			@more="handlePlaceholder('更多菜单')"
			@theme="handlePlaceholder('主题切换')"
		/>

		<view class="sub-page__hero">
			<text class="sub-page__title">{{ title }}</text>
			<text class="sub-page__subtitle">{{ subtitle }}</text>
		</view>

		<slot />

		<slot name="footer" />

		<login-sheet />
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

/* 标题 */
.sub-page__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.sub-page__title {
	font-size: 52rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.sub-page__subtitle {
	margin-top: 16rpx;
	font-size: 26rpx;
	color: $y-text-sub;
}
</style>
