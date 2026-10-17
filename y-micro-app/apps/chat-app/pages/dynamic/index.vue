<script setup>
/**
 * 动态（tab）—— 本期占位页
 *
 * 朋友圈 / 动态流等模块不在本期范围内，后续按需接入。
 */
</script>

<template>
	<page-layout title="动态">
		<view class="placeholder">
			<text class="placeholder__title">动态</text>
			<text class="placeholder__desc">动态功能开发中，敬请期待</text>
		</view>
	</page-layout>
</template>

<style scoped lang="scss">
.placeholder {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 240rpx;

	&__title {
		font-size: 36rpx;
		font-weight: 600;
		color: #1f1f1f;
	}

	&__desc {
		margin-top: 16rpx;
		font-size: 28rpx;
		color: #999999;
	}
}
</style>
