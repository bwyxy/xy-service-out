<script setup>
import { ref } from 'vue'
import { submitFeedback } from '@/api/feedback'

	/**
	 * 我的 · 意见反馈
	 *
	 * 路由：subpages/me/feedback/index（分包 subpages/me，按需加载）
	 * 职责：把想说的话落库——填写反馈内容与选填联系方式，提交成功后回到上一页。
	 * 数据：提交走 POST /feedback，提交人由后端从 token 解析。
	 * 目录：状态（content、contact、submitting）→ 方法（submit）→ 样式（.card、.row、.foot、.submit）
	 */

/** 反馈内容长度上限，与后端校验保持一致 */
const CONTENT_MAX_LENGTH = 500

/** 联系方式长度上限，与 t_todo_feedback.contact 对齐 */
const CONTACT_MAX_LENGTH = 100

/** 反馈内容 */
const content = ref('')

/** 联系方式（选填） */
const contact = ref('')

/** 提交中，防连点 */
const submitting = ref(false)

/** 提交反馈 */
async function submit() {
	const text = content.value.trim()

	if (!text) {
		uni.showToast({ title: '请先写点想说的话', icon: 'none' })

		return
	}

	if (submitting.value) {
		return
	}

	submitting.value = true

	try {
		const res = await submitFeedback({ content: text, contact: contact.value.trim() })

		if (res.code !== 200) {
			return
		}

		uni.showToast({ title: '已收到，谢谢你', icon: 'none' })

		setTimeout(() => uni.navigateBack(), 800)
	} finally {
		submitting.value = false
	}
}
</script>

<template>
	<sub-page title="意见反馈" subtitle="告诉我们哪里可以更好。" fixed-footer>
		<view class="card">
			<textarea
				class="card__input"
				v-model="content"
				:maxlength="CONTENT_MAX_LENGTH"
				placeholder="想说什么都可以，我们会认真读。"
				placeholder-style="color:#a9a9a0"
			/>

			<text class="card__count">{{ content.length }}/{{ CONTENT_MAX_LENGTH }}</text>
		</view>

		<view class="group">
			<view class="row">
				<text class="row__label">联系方式</text>
				<input
					class="row__input"
					v-model="contact"
					:maxlength="CONTACT_MAX_LENGTH"
					placeholder="选填，方便我们回复你"
					placeholder-style="color:#a9a9a0"
				/>
			</view>
		</view>

		<text class="foot">你的每一条反馈，都会被读到。</text>

		<template #footer>
			<view class="submit">
				<view class="submit__button" :class="{ 'submit__button--busy': submitting }" @click="submit">
					<text class="submit__text">{{ submitting ? '提交中…' : '提交' }}</text>
				</view>
			</view>
		</template>
	</sub-page>
</template>

<style scoped lang="scss">
/* 输入区 */
.card {
	margin-top: 32rpx;
	padding: 28rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.card__input {
	width: 100%;
	height: 320rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	line-height: 44rpx;
	color: $y-text-main;
}

.card__count {
	display: block;
	margin-top: 16rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
	text-align: right;
}

/* 联系方式 */
.group {
	margin-top: 32rpx;
	padding: 4rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.row {
	display: flex;
	align-items: center;
	padding: 30rpx 0;
}

.row__label {
	flex-shrink: 0;
	width: 180rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__input {
	flex: 1;
	height: 50rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-main;
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
