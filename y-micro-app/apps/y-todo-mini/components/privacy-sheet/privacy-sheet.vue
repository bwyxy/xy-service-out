<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

import {
	agreePrivacy,
	PRIVACY_CLOSE_EVENT,
	PRIVACY_REQUIRED_EVENT,
	rejectPrivacy,
	requirePrivacyAgreement
} from '@/utils/privacy'
import { themeClass } from '@/utils/appearance'

	/**
	 * 隐私同意弹框
	 *
	 * 首次启动（本地没有同意记录）时弹出，同意结果记在 utils/privacy.js；
	 * 微信调用需授权接口前来问的那次，也由这里应答，两处共用一套弹框。
	 * 挂在各个页面里的实例都会监听同一个事件，用户点完一起关闭。
	 *
	 * 这里自绘遮罩与面板，没有走 model-popup：弹框要在页面首帧就出现，
	 * 而 page-container 在首帧同批创建、show 初始即为 true 时面板往往渲染不出来，
	 * 外面那层全屏容器却照样拦住触摸，结果是「看不见弹框、页面还滑不动」。
	 * 自绘则整层随 visible 挂载与移除，弹框关闭后不会留下拦截触摸的空层。
	 */

/** 弹框是否显示 */
const visible = ref(false)

/** 需要问一次同意 */
function onRequired() {
	visible.value = true
}

/** 其它页面里的同一份实例已经处理过了，这里跟着关掉 */
function onClose() {
	visible.value = false
}

/** 同意并继续 */
function handleAgree() {
	agreePrivacy()
}

/** 暂不同意：不记结果，下次进来还会再问 */
function handleReject() {
	rejectPrivacy()
}

/** 看协议：跳协议页，弹框留着，看完回来还能直接同意 */
function openAgreement(tab) {
	uni.navigateTo({ url: `/subpages/me/agreement/index?tab=${tab}` })
}

onMounted(() => {
	uni.$on(PRIVACY_REQUIRED_EVENT, onRequired)
	uni.$on(PRIVACY_CLOSE_EVENT, onClose)

	// 挂载即检查一次：首次启动没有同意记录时，弹框就是从这一页出来的
	requirePrivacyAgreement()
})

onUnmounted(() => {
	uni.$off(PRIVACY_REQUIRED_EVENT, onRequired)
	uni.$off(PRIVACY_CLOSE_EVENT, onClose)
})
</script>

<template>
	<view v-if="visible" class="privacy-sheet" :class="themeClass">
		<!-- 遮罩：点空白处等同「暂不同意」 -->
		<view class="privacy-sheet__mask" @click="handleReject" />

		<view class="privacy-sheet__panel">
			<view class="privacy-sheet__head">
				<text class="privacy-sheet__title">欢迎使用忆 · 待办</text>

				<text class="privacy-sheet__desc">
					为了帮你把清单、提醒与专注记录好好存下来，我们会在你同意后使用微信登录凭证与昵称头像。请阅读并同意
					<text class="privacy-sheet__link" @click="openAgreement('privacy')">《隐私政策》</text>
					与
					<text class="privacy-sheet__link" @click="openAgreement('terms')">《用户协议》</text>
					。
				</text>
			</view>

			<view class="privacy-sheet__button" @click="handleAgree">
				<text class="privacy-sheet__button-text">同意并继续</text>
			</view>

			<view class="privacy-sheet__cancel" @click="handleReject">
				<text class="privacy-sheet__cancel-text">暂不同意</text>
			</view>
		</view>
	</view>
</template>

<style scoped lang="scss">
/* 铺满视口的一层，关闭时整个节点移除 */
.privacy-sheet {
	position: fixed;
	left: 0;
	top: 0;
	right: 0;
	bottom: 0;
	z-index: 9999;
}

.privacy-sheet__mask {
	position: absolute;
	left: 0;
	top: 0;
	right: 0;
	bottom: 0;
	background-color: $uni-bg-color-mask;
}

/* 面板贴底：面板本身是这一层里唯一可点的区域，所以不用再单独写高度 */
.privacy-sheet__panel {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	flex-direction: column;
	padding: 48rpx 48rpx calc(40rpx + env(safe-area-inset-bottom));
	border-radius: 40rpx 40rpx 0 0;
	background-color: $y-card-bg;
}

.privacy-sheet__head {
	display: flex;
	flex-direction: column;
}

.privacy-sheet__title {
	font-size: calc(40rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.privacy-sheet__desc {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	line-height: 1.7;
	color: $y-text-sub;
}

.privacy-sheet__link {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-green-primary;
}

.privacy-sheet__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	margin-top: 48rpx;
	border-radius: 48rpx;
	background-color: $y-green-primary;
}

.privacy-sheet__button-text {
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: #ffffff;
}

.privacy-sheet__cancel {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	margin-top: 16rpx;
}

.privacy-sheet__cancel-text {
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}
</style>
