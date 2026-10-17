<script setup>
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { reactive, ref } from 'vue'
import { editPassword, logout, sendEmailCode, updateUserInfo } from '@/api/auth'
import { uploadAvatar } from '@/api/upload'
import useUserStore from '@/store/user'
import { isEmail } from '@/utils/verify'

/**
 * 我的（个人信息 / 编辑资料 / 修改密码 / 退出登录）
 *
 * 迁移自网页端 views/user（index + EditInfo + EditPassword + UploadAvatar）：
 * - 头像裁剪组件（cropper-next-vue，依赖 DOM）→ uni.chooseImage 选择后直接上传
 * - Element Plus 表单 → wot-design-uni 输入框 + 手动校验
 */
const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)

const profileForm = reactive({
	nickname: '',
	sex: '1',
})

const passwordForm = reactive({
	email: '',
	emailCode: '',
	oldPassword: '',
	newPassword: '',
	confirmNewPassword: '',
})

const savingProfile = ref(false)
const savingPassword = ref(false)

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

/** 拉取用户信息并回填表单 */
async function loadUserInfo() {
	if (!userInfo.value || !userInfo.value.uid) {
		try {
			await userStore.setUserInfo()
		} catch (error) {
			console.error('[User] 获取用户信息失败', error)
			return
		}
	}

	profileForm.nickname = userInfo.value.nickname || ''
	profileForm.sex = String(userInfo.value.sex || '1')
	passwordForm.email = userInfo.value.email || ''
}

/** 更换头像：选择后直接上传，由服务端落库 */
function handleChooseAvatar() {
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		success: async (res) => {
			const filePath = res.tempFilePaths && res.tempFilePaths[0]
			if (!filePath) {
				return
			}

			const response = await uploadAvatar(filePath)
			if (response.code === 200) {
				uni.showToast({ title: '更新头像成功', icon: 'none' })
				await userStore.setUserInfo()
			}
		},
	})
}

/** 保存资料 */
async function handleSaveProfile() {
	if (!profileForm.nickname) {
		toast('请输入昵称')
		return
	}
	if (savingProfile.value) {
		return
	}

	savingProfile.value = true
	const response = await updateUserInfo({ ...profileForm })
	savingProfile.value = false

	if (response.code === 200) {
		toast('更新成功')
		await userStore.setUserInfo()
	}
}

/** 发送邮箱验证码（120s 倒计时） */
async function handleSendCode() {
	if (smsCode.cooling || smsCode.loading) {
		toast('发送太过于频繁了')
		return
	}
	if (!isEmail(passwordForm.email)) {
		toast('请输入正确的邮箱')
		return
	}

	smsCode.loading = true
	const result = await sendEmailCode(passwordForm.email)
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

/** 修改密码 */
async function handleChangePassword() {
	if (!isEmail(passwordForm.email)) {
		toast('请输入正确的邮箱')
		return
	}
	if (!passwordForm.emailCode) {
		toast('请输入邮箱验证码')
		return
	}
	if (!passwordForm.oldPassword) {
		toast('请输入旧密码')
		return
	}
	if (!passwordForm.newPassword) {
		toast('请输入新密码')
		return
	}
	if (passwordForm.newPassword !== passwordForm.confirmNewPassword) {
		toast('两次输入的新密码不一致')
		return
	}
	if (savingPassword.value) {
		return
	}

	savingPassword.value = true
	const response = await editPassword({ ...passwordForm })
	savingPassword.value = false

	if (response.code === 200) {
		toast('修改密码成功')
		passwordForm.emailCode = ''
		passwordForm.oldPassword = ''
		passwordForm.newPassword = ''
		passwordForm.confirmNewPassword = ''
	}
}

/** 退出登录 */
function handleLogout() {
	uni.showModal({
		title: '提示',
		content: '确定要退出登录吗？',
		success: async (res) => {
			if (!res.confirm) {
				return
			}

			try {
				await logout()
			} catch (error) {
				console.error('[User] 登出接口调用失败', error)
			}

			userStore.outLogin()
			uni.reLaunch({ url: '/pages/login/index' })
		},
	})
}

onLoad(() => {
	loadUserInfo()
})

onUnload(() => {
	if (smsCode.timer) {
		clearInterval(smsCode.timer)
		smsCode.timer = null
	}
})
</script>

<template>
	<view class="user">
		<wd-navbar></wd-navbar>

		<view class="user-content">
			<view class="user-info-box">
				<wd-img
					width="100rpx"
					height="100rpx"
					round
					:src="userInfo.avatar"
				></wd-img>
				<view class="user-info-box_rt">
					<text class="user-name">{{ userInfo.nickname }}</text>
					<text class="user-sex">{{ userInfo.sex === '1' ? '男' : '女' }}</text>
				</view>
			</view>

			<wd-cell-group>
				<wd-cell title="个人资料" suffix-icon="right"></wd-cell>
				<wd-cell title="修改密码" border suffix-icon="right"></wd-cell>
			</wd-cell-group>
		</view>
		<!-- <view class="user__card">
			<view class="user__avatar" @click="handleChooseAvatar">
				<image
					v-if="userInfo.avatar"
					class="user__avatar-img"
					:src="userInfo.avatar"
					mode="aspectFill"
				/>
				<image v-else class="user__avatar-img" src="/static/logo.png" mode="aspectFill" />
				<text class="user__avatar-tip">点击更换</text>
			</view>

			<view class="user__row">
				<text class="user__label">昵称</text>
				<text class="user__value">{{ userInfo.nickname || '-' }}</text>
			</view>
			<view class="user__row">
				<text class="user__label">性别</text>
				<text class="user__value">
					{{ userInfo.sex === '1' ? '男' : userInfo.sex === '2' ? '女' : '未知' }}
				</text>
			</view>
			<view class="user__row">
				<text class="user__label">手机号</text>
				<text class="user__value">{{ userInfo.phone || '-' }}</text>
			</view>
			<view class="user__row">
				<text class="user__label">注册邮箱</text>
				<text class="user__value">{{ userInfo.email || '-' }}</text>
			</view>
		</view>

		<view class="user__card">
			<text class="user__section-title">编辑资料</text>

			<view class="user__row">
				<text class="user__label">昵称</text>
				<wd-input v-model="profileForm.nickname" class="user__input" no-border placeholder="请输入昵称" />
			</view>

			<view class="user__row">
				<text class="user__label">性别</text>
				<view class="user__sex">
					<view
						class="user__sex-item"
						:class="{ 'user__sex-item--active': profileForm.sex === '1' }"
						@click="profileForm.sex = '1'"
					>男</view>
					<view
						class="user__sex-item"
						:class="{ 'user__sex-item--active': profileForm.sex === '2' }"
						@click="profileForm.sex = '2'"
					>女</view>
				</view>
			</view>

			<wd-button type="primary" block :loading="savingProfile" @click="handleSaveProfile">保存</wd-button>
		</view>

		<view class="user__card">
			<text class="user__section-title">修改密码</text>

			<wd-input
				v-model="passwordForm.email"
				class="user__field"
				no-border
				placeholder="请输入邮箱"
				clearable
			/>

			<view class="user__sms">
				<wd-input
					v-model="passwordForm.emailCode"
					class="user__field user__field--sms"
					no-border
					placeholder="请输入邮箱验证码"
				/>
				<wd-button
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
				v-model="passwordForm.oldPassword"
				class="user__field"
				no-border
				placeholder="请输入旧密码"
				show-password
			/>
			<wd-input
				v-model="passwordForm.newPassword"
				class="user__field"
				no-border
				placeholder="请输入新密码"
				show-password
			/>
			<wd-input
				v-model="passwordForm.confirmNewPassword"
				class="user__field"
				no-border
				placeholder="请确认新密码"
				show-password
			/>

			<wd-button type="primary" block :loading="savingPassword" @click="handleChangePassword">
				确认
			</wd-button>
		</view>

		<view class="user__logout">
			<wd-button type="error" block plain @click="handleLogout">退出登录</wd-button>
		</view> -->
	</view>
</template>

<style scoped lang="scss">
.user {
	width: 100vw;
	height: 100vh;
	display: flex;
	flex-direction: column;
	overflow: hidden;

	&-content {
		width: 100%;
		height: calc(100% - 320rpx);
		background: #fff;
		border-radius: 36rpx 36rpx 0 0;
		box-sizing: border-box;
		padding: 70rpx 38rpx 32rpx;
		position: fixed;
		left: 0;
		bottom: 0;
	}

	.user-info-box {
		width: calc(100% - 76rpx);
		height: 120rpx;
		display: flex;
		align-items: center;
		position: absolute;
		left: 38rpx;
		top: -50rpx;
		&_rt {
			flex: 1;
			height: 100%;
			box-sizing: border-box;
			padding-top: 24rpx;
			margin-left: 14rpx;
			display: flex;
			flex-direction: column;
			.user-name {
				color: #1f1f1f;
				font-size: 28rpx;
				font-weight: 600;
				letter-spacing: 1px;
				margin-bottom: 18rpx;
			}
			.user-sex {
				color: #8c8c8c;
				font-size: 20rpx;
			}
		}
	}
	&__card {
		margin: 24rpx;
		padding: 24rpx;
		background: #ffffff;
		border-radius: 16rpx;
	}

	&__avatar {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 24rpx 0 40rpx;
	}

	&__avatar-img {
		width: 160rpx;
		height: 160rpx;
		border-radius: 50%;
	}

	&__avatar-tip {
		margin-top: 16rpx;
		font-size: 24rpx;
		color: #999999;
	}

	&__row {
		display: flex;
		align-items: center;
		min-height: 88rpx;
		border-bottom: 1px solid rgba(0, 0, 0, 0.06);
	}

	&__label {
		width: 160rpx;
		flex-shrink: 0;
		font-size: 30rpx;
		color: #1f1f1f;
	}

	&__value {
		flex: 1;
		font-size: 30rpx;
		color: #8c8c8c;
		text-align: right;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	&__input {
		flex: 1;
	}

	&__sex {
		flex: 1;
		display: flex;
		justify-content: flex-end;
	}

	&__sex-item {
		padding: 10rpx 32rpx;
		margin-left: 16rpx;
		background: #f7f7f7;
		border-radius: 8rpx;
		font-size: 28rpx;
		color: #666666;

		&--active {
			background: #3a7bfd;
			color: #ffffff;
		}
	}

	&__section-title {
		font-size: 30rpx;
		font-weight: 500;
		color: #1f1f1f;
	}

	&__field {
		margin-top: 24rpx;
		padding: 0 24rpx;
		background: #f7f7f7;
		border-radius: 12rpx;

		&--sms {
			flex: 1;
			margin-top: 0;
		}
	}

	&__sms {
		display: flex;
		align-items: center;
		margin-top: 24rpx;
	}

	&__logout {
		margin: 24rpx 24rpx 60rpx;
	}
}
</style>
