<script setup>
import { ref } from 'vue'

	/**
	 * 我的 · 个人资料
	 *
	 * 路由：subpages/me/profile/index（分包 subpages/me，按需加载）
	 * 职责：个人资料页，让忆记住你的样子——编辑昵称、签名、性别、生日与所在地并保存。
	 * 目录：状态（form）→ 方法（changeAvatar、pickField、save）→ 样式（.avatar、.group、.row、.submit）
	 */

/** 表单（暂用本地数据，后续接入接口） */
const form = ref({
	nickname: '小禾',
	signature: '慢慢来，比较快。',
	gender: '女',
	birthday: '未设置',
	city: '浙江 · 杭州'
})

/** 更换头像（待接入上传） */
function changeAvatar() {
	uni.showToast({ title: '更换头像（待开发）', icon: 'none' })
}

/** 修改其他字段（待接入选择器） */
function pickField(label) {
	uni.showToast({ title: `修改${label}（待开发）`, icon: 'none' })
}

/** 保存资料（待接入接口） */
function save() {
	if (!form.value.nickname.trim()) {
		uni.showToast({ title: '昵称不能为空', icon: 'none' })

		return
	}

	uni.showToast({ title: '已保存', icon: 'none' })
}
</script>

<template>
	<sub-page title="个人资料" subtitle="让忆记住你的样子。" fixed-footer>
		<view class="avatar" @click="changeAvatar">
			<view class="avatar__circle">
				<y-icon name="sprout" size="72rpx" />
			</view>

			<view class="avatar__edit">
				<y-icon name="camera" size="28rpx" color="#62804f" />
				<text class="avatar__edit-text">更换头像</text>
			</view>
		</view>

		<view class="group">
			<view class="row row--input">
				<text class="row__label">昵称</text>
				<input class="row__input" v-model="form.nickname" placeholder="怎么称呼你" placeholder-style="color:#a9a9a0" />
			</view>

			<view class="row row--input">
				<text class="row__label">个性签名</text>
				<input class="row__input" v-model="form.signature" placeholder="写一句喜欢的话" placeholder-style="color:#a9a9a0" />
			</view>
		</view>

		<view class="group">
			<view class="row" @click="pickField('性别')">
				<text class="row__label">性别</text>
				<text class="row__value">{{ form.gender }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row" @click="pickField('生日')">
				<text class="row__label">生日</text>
				<text class="row__value">{{ form.birthday }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row" @click="pickField('所在地')">
				<text class="row__label">所在地</text>
				<text class="row__value">{{ form.city }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>
		</view>

		<text class="foot">这些信息只存在你自己的数据里。</text>

		<template #footer>
			<view class="submit">
				<view class="submit__button" @click="save">
					<text class="submit__text">保存</text>
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

.avatar__circle {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 176rpx;
	height: 176rpx;
	border-radius: 50%;
	background-color: $y-green-soft;
}

.avatar__edit {
	display: flex;
	align-items: center;
	margin-top: 20rpx;
}

.avatar__edit-text {
	margin-left: 8rpx;
	font-size: 26rpx;
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
	font-size: 30rpx;
	color: $y-text-main;
}

.row--input .row__label {
	width: 160rpx;
}

.row__input {
	flex: 1;
	height: 50rpx;
	font-size: 30rpx;
	color: $y-text-main;
	text-align: right;
}

.row__value {
	flex: 1;
	margin-right: 12rpx;
	font-size: 28rpx;
	color: $y-text-sub;
	text-align: right;
}

/* 页脚 */
.foot {
	display: block;
	margin-top: 48rpx;
	font-size: 24rpx;
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

.submit__text {
	font-size: 32rpx;
	font-weight: 500;
	color: #ffffff;
}
</style>
