<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { submitLogin, cancelLogin } from '@/utils/session'

const visible = ref(false)

const loading = ref(false)

/** 拉起弹框 */
function onRequired() {
	console.log('显示')
	visible.value = true
}

/** 收起弹框 */
function onClose() {
	visible.value = false
	loading.value = false
}

onMounted(() => {
	uni.$on('login-required', onRequired)
	uni.$on('login-sheet-close', onClose)
})

onUnmounted(() => {
	uni.$off('login-required', onRequired)
	uni.$off('login-sheet-close', onClose)
})

/** 微信一键登录 */
async function handleLogin() {
	if (loading.value) {
		return
	}

	loading.value = true

	try {
		await submitLogin()
		uni.showToast({ title: '登录成功', icon: 'none' })
	} catch (error) {
		uni.showToast({ title: error.message || '登录失败，请稍后重试', icon: 'none' })
	} finally {
		loading.value = false
	}
}
</script>

<template>
	<model-popup v-model="visible" @beforeClose="cancelLogin">
		<view class="login-sheet__mask" @click="cancelLogin" />
			<view class="login-sheet__panel">
			<view />

			<view class="login-sheet__head">
				<text class="login-sheet__title">登录后，待办不丢</text>
				<text class="login-sheet__desc">登录即可在多端同步你的清单、专注与习惯记录。</text>
			</view>

			<view class="login-sheet__button" @click="handleLogin">
				<text class="login-sheet__button-text">{{ loading ? '登录中…' : '微信一键登录' }}</text>
			</view>

			<view class="login-sheet__cancel" @click="cancelLogin">
				<text class="login-sheet__cancel-text">暂不登录</text>
			</view>
		</view>
	</model-popup>
</template>

<style scoped lang="scss">
.login-sheet__mask {
	width: 100%;
	background-color: #fff;
}

.login-sheet__panel {
	display: flex;
	flex-direction: column;
	padding: 20rpx 48rpx calc(40rpx + env(safe-area-inset-bottom));
	border-radius: 40rpx 40rpx 0 0;
	background-color: $y-card-bg;
}

.login-sheet__bar {
	width: 72rpx;
	height: 8rpx;
	margin: 8rpx auto 0;
	border-radius: 4rpx;
	background-color: $y-divider;
}

.login-sheet__head {
	display: flex;
	flex-direction: column;
	margin-top: 40rpx;
}

.login-sheet__title {
	font-size: 40rpx;
	font-weight: 600;
	color: $y-text-main;
}

.login-sheet__desc {
	margin-top: 16rpx;
	font-size: 26rpx;
	line-height: 1.6;
	color: $y-text-sub;
}

.login-sheet__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	margin-top: 48rpx;
	border-radius: 48rpx;
	background-color: $y-green-primary;
}

.login-sheet__button-text {
	font-size: 32rpx;
	font-weight: 600;
	color: #ffffff;
}

.login-sheet__cancel {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	margin-top: 16rpx;
}

.login-sheet__cancel-text {
	font-size: 28rpx;
	color: $y-text-muted;
}
</style>
