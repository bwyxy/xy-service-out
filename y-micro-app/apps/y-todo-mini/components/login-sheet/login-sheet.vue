<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { submitLogin, cancelLogin } from '@/utils/session'
import { themeClass } from '@/utils/appearance'
import { agreePrivacy, hasAgreedPrivacy } from '@/utils/privacy'

const visible = ref(false)

const loading = ref(false)

/** 是否勾选同意协议：已经同意过隐私协议的老用户默认勾上，不必每次都点一次 */
const agreed = ref(false)

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

/** 切换协议勾选 */
function toggleAgree() {
	agreed.value = !agreed.value
}

/** 看协议：勾选状态留着，看完回来接着登录 */
function openAgreement(tab) {
	uni.navigateTo({ url: `/subpages/me/agreement/index?tab=${tab}` })
}

onMounted(() => {
	uni.$on('login-required', onRequired)
	uni.$on('login-sheet-close', onClose)
})

onUnmounted(() => {
	uni.$off('login-required', onRequired)
	uni.$off('login-sheet-close', onClose)
})

/** 登录：必须先勾选协议，未获同意不拉起微信授权 */
async function handleLogin() {
	if (loading.value) {
		return
	}

	if (!agreed.value) {
		uni.showToast({ title: '请先阅读并勾选同意协议', icon: 'none' })

		return
	}

	loading.value = true

	try {
		await submitLogin()
		// 勾选即表示同意，登录成功后落到本地，首次启动的隐私弹框也不再重复打扰
		agreePrivacy()
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
		<!-- 弹层走原生 page-container，脱离页面节点树，主题变量要在这里再挂一次 -->
		<view class="login-sheet__mask" :class="themeClass" @click="cancelLogin" />
			<view class="login-sheet__panel" :class="themeClass">
			<view />

			<view class="login-sheet__head">
				<text class="login-sheet__title">忆待办</text>
				<text class="login-sheet__desc">登录即可在多端同步你的清单、专注与习惯记录。</text>
			</view>

			<view class="login-sheet__agree" @click="toggleAgree">
				<view class="login-sheet__checkbox" :class="{ 'login-sheet__checkbox--checked': agreed }">
					<y-icon v-if="agreed" name="check" size="24rpx" color="#ffffff" />
				</view>

				<text class="login-sheet__agree-text">
					我已阅读并同意
					<text class="login-sheet__link" @click.stop="openAgreement('privacy')">《隐私政策》</text>
					与
					<text class="login-sheet__link" @click.stop="openAgreement('terms')">《用户协议》</text>
				</text>
			</view>

			<view
				class="login-sheet__button"
				:class="{ 'login-sheet__button--disabled': !agreed }"
				@click="handleLogin"
			>
				<text class="login-sheet__button-text">{{ loading ? '登录中…' : '同意并登录' }}</text>
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
	background-color: $y-card-bg;
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
	font-size: calc(40rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.login-sheet__desc {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	line-height: 1.6;
	color: $y-text-sub;
}

/* 协议勾选 */
.login-sheet__agree {
	display: flex;
	align-items: flex-start;
	margin-top: 32rpx;
}

.login-sheet__checkbox {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 34rpx;
	height: 34rpx;
	margin-top: 3rpx;
	margin-right: 12rpx;
	border: 2rpx solid $y-check-border;
	border-radius: 50%;
	box-sizing: border-box;
}

.login-sheet__checkbox--checked {
	border-color: $y-green-primary;
	background-color: $y-green-primary;
}

.login-sheet__agree-text {
	flex: 1;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 40rpx;
	color: $y-text-muted;
}

.login-sheet__link {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-green-primary;
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

.login-sheet__button--disabled {
	background-color: $y-green-done;
}

.login-sheet__button-text {
	font-size: calc(32rpx * var(--y-font-scale, 1));
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}
</style>
