<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { user, fetchUserInfo, setUser, requireLogin } from '@/utils/session'
import { updateProfile, uploadImage } from '@/api/user'

	/**
	 * 我的 · 个人资料
	 *
	 * 路由：subpages/me/profile/index（分包 subpages/me，按需加载）
	 * 职责：编辑 t_user 里允许用户自己改的三项——头像、昵称、性别，其余字段这里不维护。
	 * 数据：进入时拉 GET /wechat/getInfo 回显，保存走 PUT /profile，头像先传 POST /upload/image 换成地址。
	 * 目录：状态（form、uploading、saving）→ 方法（loadProfile、onChooseAvatar、chooseFromAlbum、applyAvatar、pickSex、save）→ 样式（.avatar、.group、.row、.submit）
	 */

/** 性别候选项，取值与 t_user.sex 的 0-未知 1-男 2-女 对齐 */
const SEX_OPTIONS = [
	{ label: '不公开', value: '0' },
	{ label: '男', value: '1' },
	{ label: '女', value: '2' }
]

/** 表单：只放 t_user 里真实存在的可改字段 */
const form = ref({ nickname: '', avatar: '', sex: '' })

/** 头像上传中 */
const uploading = ref(false)

/** 保存中，防连点 */
const saving = ref(false)

/** 性别展示文案（空的按未设置展示，不落库成空串） */
const sexLabel = computed(() => {
	const matched = SEX_OPTIONS.find((item) => item.value === form.value.sex)

	return matched ? matched.label : '未设置'
})

/** 回显资料：未登录先拉起登录半弹框，登录后再拉一次后端的最新值 */
async function loadProfile() {
	if (!user.logged) {
		try {
			await requireLogin()
		} catch (error) {
			// 用户取消登录：保持空表单，保存时请求层会再要求一次登录
			return
		}
	}

	form.value.nickname = user.nickname || ''
	form.value.avatar = user.avatar || ''
	form.value.sex = user.sex || ''

	try {
		const data = await fetchUserInfo()

		if (data) {
			form.value.nickname = data.nickname || ''
			form.value.avatar = data.avatar || ''
			form.value.sex = data.sex || ''
		}
	} catch (error) {
		// 拉取失败就用登录态里的值兜底，不打断编辑
	}
}

/** 微信头像选择（button open-type="chooseAvatar"） */
function onChooseAvatar(event) {
	const avatarUrl = event && event.detail ? event.detail.avatarUrl : ''

	if (avatarUrl) {
		applyAvatar(avatarUrl)
	}
}

/** 非微信端从相册或相机选一张 */
function chooseFromAlbum() {
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		success: (res) => {
			const filePath = res.tempFilePaths && res.tempFilePaths[0]

			if (filePath) {
				applyAvatar(filePath)
			}
		}
	})
}

/** 上传头像：拿到地址只更新表单，点保存时才写库 */
async function applyAvatar(tempFilePath) {
	if (uploading.value) {
		return
	}

	uploading.value = true
	uni.showLoading({ title: '上传中', mask: true })

	try {
		const res = await uploadImage(tempFilePath)

		if (res && res.code === 200 && res.data) {
			form.value.avatar = res.data
			await updateProfile({
				avatar: res.data
			})

			return
		}

		uni.showToast({ title: (res && res.msg) || '头像上传失败', icon: 'none' })
	} catch (error) {
		uni.showToast({ title: '头像上传失败', icon: 'none' })
	} finally {
		uni.hideLoading()
		uploading.value = false
	}
}

/** 选择性别 */
function pickSex() {
	uni.showActionSheet({
		itemList: SEX_OPTIONS.map((item) => item.label),
		success: (res) => {
			const picked = SEX_OPTIONS[res.tapIndex]

			if (picked) {
				form.value.sex = picked.value
			}
		}
	})
}

/** 保存：只提交有值的字段，成功后同步登录态，页面其它地方立即可见新昵称与头像 */
async function save() {
	const nickname = form.value.nickname.trim()

	if (!nickname) {
		uni.showToast({ title: '昵称不能为空', icon: 'none' })

		return
	}

	if (saving.value) {
		return
	}

	saving.value = true

	try {
		const payload = { nickname }

		// 头像和性别可能本来就空着，空值不提交，避免把「没填」当成非法入参
		if (form.value.avatar) {
			payload.avatar = form.value.avatar
		}

		if (form.value.sex) {
			payload.sex = form.value.sex
		}

		const res = await updateProfile(payload)

		if (res.code !== 200) {
			return
		}

		setUser({ ...user, nickname, avatar: form.value.avatar, sex: form.value.sex })
		uni.showToast({ title: '已保存', icon: 'none' })
	} finally {
		saving.value = false
	}
}

/** 进入即回显一次 */
onShow(() => {
	loadProfile()
})
</script>

<template>
	<sub-page title="个人资料" subtitle="让忆记住你的样子。" fixed-footer>
		<view class="avatar">
			<!-- #ifdef MP-WEIXIN -->
			<button class="avatar__picker" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
				<image v-if="form.avatar" class="avatar__img" :src="form.avatar" mode="aspectFill" />
				<y-icon v-else name="sprout" size="72rpx" />
			</button>
			<!-- #endif -->

			<!-- #ifndef MP-WEIXIN -->
			<view class="avatar__picker" @click="chooseFromAlbum">
				<image v-if="form.avatar" class="avatar__img" :src="form.avatar" mode="aspectFill" />
				<y-icon v-else name="sprout" size="72rpx" />
			</view>
			<!-- #endif -->

			<view class="avatar__edit">
				<y-icon name="camera" size="28rpx" color="#62804f" />
				<text class="avatar__edit-text">{{ uploading ? '上传中…' : '更换头像' }}</text>
			</view>
		</view>

		<view class="group">
			<view class="row row--input">
				<text class="row__label">昵称</text>
				<input
					class="row__input"
					v-model="form.nickname"
					type="nickname"
					maxlength="20"
					placeholder="怎么称呼你"
					placeholder-style="color:#a9a9a0"
				/>
			</view>
		</view>

		<view class="group">
			<view class="row" @click="pickSex">
				<text class="row__label">性别</text>
				<text class="row__value">{{ sexLabel }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>
		</view>

		<text class="foot">这些资料只存在你自己的账号里。</text>

		<template #footer>
			<view class="submit">
				<view class="submit__button" :class="{ 'submit__button--busy': saving }" @click="save">
					<text class="submit__text">{{ saving ? '保存中…' : '保存' }}</text>
				</view>
			</view>
		</template>
	</sub-page>
</template>

<style scoped lang="scss">
/* 头像 */
.avatar {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 44rpx;
}

/* 微信端是 button，非微信端是 view，这里统一成同一个圆形外观 */
.avatar__picker {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 176rpx;
	height: 176rpx;
	padding: 0;
	margin: 0;
	border: none;
	border-radius: 50%;
	background-color: $y-green-soft;
	overflow: hidden;

	&::after {
		border: none;
	}
}

.avatar__img {
	width: 100%;
	height: 100%;
}

.avatar__edit {
	display: flex;
	align-items: center;
	margin-top: 20rpx;
}

.avatar__edit-text {
	margin-left: 8rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-green-tag-text;
}

/* 表单分组 */
.group {
	margin-top: 32rpx;
	padding: 4rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.avatar + .group {
	margin-top: 48rpx;
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
	flex-shrink: 0;
	width: 160rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__input {
	flex: 1;
	height: 50rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
	text-align: right;
}

.row__value {
	flex: 1;
	margin-right: 12rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
	text-align: right;
}

/* 页脚 */
.foot {
	display: block;
	margin-top: 48rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
	text-align: center;
}

/* 底部按钮 */
.submit {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 24rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
	background-color: $y-page-bg;
}

.submit__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	border-radius: 24rpx;
	background-color: $y-green-primary;
}

.submit__button--busy {
	opacity: 0.6;
}

.submit__text {
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: #ffffff;
}
</style>
