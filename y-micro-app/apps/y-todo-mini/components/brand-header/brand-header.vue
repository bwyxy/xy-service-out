<script setup>
/**
 * 品牌顶栏
 *
 * 各页面统一的「忆 · 待办」顶栏，右侧为菜单与主题按钮。
 * 按钮行为由使用方通过 back / more / theme 事件自行决定。
 * showBack 为 true 时，左侧由品牌文案切换为「返回」，用于子页面。
 */
defineProps({
	/** 是否显示返回按钮（子页面使用） */
	showBack: { type: Boolean, default: false }
})

const emit = defineEmits(['back', 'more', 'theme'])
</script>

<template>
	<view class="brand-header">
		<view v-if="showBack" class="brand-header__back" @click="emit('back')">
			<y-icon name="chevron-left" size="32rpx" />
			<text class="brand-header__back-text">返回</text>
		</view>

		<view v-else class="brand-header__brand">
			<image class="brand-header__icon" src="/static/top_icon.png" mode="aspectFit" />
			<text class="brand-header__text">忆 · 待办</text>
		</view>

		<view class="brand-header__actions">
			<view class="brand-header__more" @click="emit('more')">
				<view class="brand-header__dot" />
				<view class="brand-header__dot" />
				<view class="brand-header__dot" />
			</view>
			<view class="brand-header__ring" @click="emit('theme')">
				<view class="brand-header__ring-inner" />
			</view>
		</view>
	</view>
</template>

<style scoped lang="scss">
.brand-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 88rpx;
}

.brand-header__brand {
	display: flex;
	align-items: center;
}

.brand-header__icon {
	width: 40rpx;
	height: 40rpx;
	margin-right: 12rpx;
}

.brand-header__text {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-text-main;
}

.brand-header__back {
	display: flex;
	align-items: center;
}

.brand-header__back-text {
	margin-left: 8rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.brand-header__actions {
	display: flex;
	align-items: center;
}

.brand-header__more {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 64rpx;
	height: 44rpx;
	margin-right: 16rpx;
	border-radius: 999rpx;
	background-color: $y-header-bg;
}

.brand-header__dot {
	width: 8rpx;
	height: 8rpx;
	border-radius: 50%;
	background-color: $y-text-sub;

	& + & {
		margin-left: 6rpx;
	}
}

.brand-header__ring {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 44rpx;
	height: 44rpx;
	border-radius: 50%;
	border: 2rpx solid $y-divider;
	background-color: $y-header-bg;
}

.brand-header__ring-inner {
	width: 16rpx;
	height: 16rpx;
	border-radius: 50%;
	border: 4rpx solid $y-text-muted;
}
</style>
