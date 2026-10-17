<script setup>
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import { sendEmailCode } from '@/api/auth'
import useChatStore from '@/store/chat'
import useUserStore from '@/store/user'
import { getToken } from '@/utils/auth'
import { isCnMobile, isEmail } from '@/utils/verify'

/**
 * 登录 / 注册页（应用启动页）
 *
 * 迁移自网页端 views/login（Login.vue + Register.vue 合并为单页切换）：
 * - Element Plus 表单 → wot-design-uni 输入框 + 手动校验
 * - 验证码 120s 倒计时逻辑保持一致
 */
const userStore = useUserStore()
const chatStore = useChatStore()

/** 当前模式：login 登录 / register 注册 */
const mode = ref('login')
const isLoading = ref(false)

const loginForm = reactive({
	account: '',
	password: '',
})

const registerForm = reactive({
	phone: '',
	email: '',
	password: '',
	confirmPassword: '',
	emailCode: '',
	sex: '0',
})

/** 邮箱验证码倒计时状态 */
const smsCode = reactive({
	text: '发送验证码',
	loading: false,
	cooling: false,
	remaining: 120,
	timer: null,
})

function toast(title) {
	uni.showToast({ title, icon: 'none' })
}

/** 已登录则直接进入首页 */
function redirectHome() {
	uni.switchTab({ url: '/pages/index/index' })
}

function switchMode(target) {
	mode.value = target
	isLoading.value = false
}

/** 登录 */
async function handleLogin() {
	if (isLoading.value) {
		return
	}
	if (!loginForm.account) {
		toast('请输入手机号或者邮箱')
		return
	}
	if (!loginForm.password) {
		toast('请输入密码')
		return
	}

	isLoading.value = true
	const result = await userStore.userLogin({
		account: loginForm.account,
		password: loginForm.password,
	})

	if (!result) {
		isLoading.value = false
		return
	}

	// 登录成功后拉取用户信息并建立 IM 连接
	try {
		await userStore.setUserInfo()
		await chatStore.initWKIM()
	} catch (error) {
		console.error('[Login] 初始化用户信息失败', error)
		isLoading.value = false
		return
	}

	isLoading.value = false
	redirectHome()
}

/** 校验注册表单 */
function validateRegisterForm() {
	if (!isCnMobile(registerForm.phone)) {
		return '请输入正确的手机号'
	}
	if (!isEmail(registerForm.email)) {
		return '请输入正确的邮箱'
	}
	if (!registerForm.emailCode) {
		return '请输入邮箱验证码'
	}
	if (!registerForm.password) {
		return '请输入密码'
	}
	if (registerForm.password !== registerForm.confirmPassword) {
		return '两次输入的密码不一致'
	}
	return ''
}

/** 注册 */
async function handleRegister() {
	if (isLoading.value) {
		return
	}

	const errorMessage = validateRegisterForm()
	if (errorMessage) {
		toast(errorMessage)
		return
	}

	isLoading.value = true
	const isSuccess = await userStore.userRegister({ ...registerForm })
	isLoading.value = false

	if (isSuccess) {
		uni.showToast({ title: '注册成功', icon: 'success' })
		switchMode('login')
	}
}

/** 发送邮箱验证码（120s 倒计时） */
async function handleSendCode() {
	if (smsCode.cooling || smsCode.loading) {
		toast('发送太过于频繁了')
		return
	}
	if (!isEmail(registerForm.email)) {
		toast('请输入正确的邮箱')
		return
	}

	smsCode.loading = true
	const result = await sendEmailCode(registerForm.email)
	smsCode.loading = false

	if (result.code !== 200) {
		return
	}

	smsCode.cooling = true
	smsCode.timer = setInterval(() => {
		if (smsCode.remaining <= 0) {
			clearInterval(smsCode.timer)
			smsCode.timer = null
			smsCode.cooling = false
			smsCode.text = '重新发送'
			smsCode.remaining = 120
		} else {
			smsCode.remaining--
		}
	}, 1000)
}

onLoad(() => {
	// 冷启动时若本地已有登录态，直接进入首页（IM 连接由 App.vue 负责恢复）
	if (getToken()) {
		redirectHome()
	}
})

onUnload(() => {
	if (smsCode.timer) {
		clearInterval(smsCode.timer)
		smsCode.timer = null
	}
})
</script>

<template>
	<view class="login">
		<view class="login__header">
			<image class="login__logo" src="/static/logo.png" mode="aspectFit" />
			<text class="login__slogan">{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</text>
		</view>

		<view class="login__panel">
			<!-- 登录 -->
			<template v-if="mode === 'login'">
				<wd-input
					v-model="loginForm.account"
					class="login__input"
					placeholder="请输入手机号或者邮箱"
					no-border
					clearable
				/>
				<wd-input
					v-model="loginForm.password"
					class="login__input"
					placeholder="请输入密码"
					no-border
					show-password
				/>

				<wd-button
					class="login__submit"
					type="primary"
					block
					:loading="isLoading"
					@click="handleLogin"
				>登录</wd-button>

				<view class="login__switch" @click="switchMode('register')">
					<text class="login__switch-text">没有账号，注册用户 &gt;&gt;&gt;</text>
				</view>
			</template>

			<!-- 注册 -->
			<template v-else>
				<wd-input
					v-model="registerForm.phone"
					class="login__input"
					placeholder="请输入手机号"
					type="number"
					no-border
					clearable
				/>
				<wd-input
					v-model="registerForm.email"
					class="login__input"
					placeholder="请输入邮箱"
					no-border
					clearable
				/>
				<view class="login__sms">
					<wd-input
						v-model="registerForm.emailCode"
						class="login__input login__input--sms"
						placeholder="请输入邮箱验证码"
						no-border
					/>
					<wd-button
						class="login__sms-btn"
						type="primary"
						plain
						size="small"
						:loading="smsCode.loading"
						@click="handleSendCode"
					>
						{{ smsCode.cooling ? `${smsCode.remaining}后重新发送` : smsCode.text }}
					</wd-button>
				</view>
				<wd-input
					v-model="registerForm.password"
					class="login__input"
					placeholder="请输入密码"
					no-border
					show-password
				/>
				<wd-input
					v-model="registerForm.confirmPassword"
					class="login__input"
					placeholder="请确认密码"
					no-border
					show-password
				/>

				<wd-button
					class="login__submit"
					type="primary"
					block
					:loading="isLoading"
					@click="handleRegister"
				>提交</wd-button>

				<view class="login__switch" @click="switchMode('login')">
					<text class="login__switch-text">前往登录 &gt;&gt;&gt;</text>
				</view>
			</template>
		</view>
	</view>
</template>

<style scoped lang="scss">
.login {
	min-height: 100vh;
	padding: 120rpx 48rpx;
	box-sizing: border-box;
	background: #ffffff;

	&__header {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-bottom: 60rpx;
	}

	&__logo {
		width: 160rpx;
		height: 160rpx;
		border-radius: 32rpx;
	}

	&__slogan {
		margin-top: 24rpx;
		font-size: 40rpx;
		font-weight: 600;
		color: #1f1f1f;
		letter-spacing: 2rpx;
	}

	&__panel {
		display: flex;
		flex-direction: column;
	}

	&__input {
		margin-bottom: 24rpx;
		padding: 0 24rpx;
		background: #f7f7f7;
		border-radius: 12rpx;

		&--sms {
			flex: 1;
			margin-bottom: 0;
		}
	}

	&__sms {
		display: flex;
		align-items: center;
		margin-bottom: 24rpx;
	}

	&__sms-btn {
		margin-left: 16rpx;
		flex-shrink: 0;
	}

	&__submit {
		margin-top: 24rpx;
	}

	&__switch {
		display: flex;
		justify-content: center;
		margin-top: 32rpx;
	}

	&__switch-text {
		font-size: 26rpx;
		color: #3a7bfd;
	}
}
</style>
