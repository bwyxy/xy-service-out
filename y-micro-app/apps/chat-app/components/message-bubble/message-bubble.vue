<script setup>
import { storeToRefs } from 'pinia'
import useUserStore from '@/store/user'
import { MessageType } from '@/services/im/constants'

/**
 * 消息气泡
 *
 * 替代网页端 components/MessageBox/MessageBox.vue：
 * - 文本消息使用 text 组件（原 v-html 由纯文本替代）
 * - 图片消息点击可预览（替代 el-image 的 preview-src-list）
 * - 通话记录（210）居中灰底展示
 */
const props = defineProps({
	/** 消息对象 */
	item: { type: Object, required: true },
	/** 对方的头像地址 */
	avatar: { type: String, default: '' },
})

const { userInfo } = storeToRefs(useUserStore())

function previewImage() {
	uni.previewImage({
		urls: [props.item.content],
		current: props.item.content,
	})
}
</script>

<template>
	<!-- 通话记录：灰色居中提示，不区分左右 -->
	<view v-if="item.type === MessageType.RTC_RECORD" class="message-bubble message-bubble--tip">
		<text class="message-bubble__record">{{ item.content }}</text>
	</view>

	<view v-else class="message-bubble" :class="{ 'message-bubble--self': item.isSelf }">
		<view class="message-bubble__avatar">
			<image
				v-if="item.isSelf ? userInfo.avatar : avatar"
				class="message-bubble__avatar-img"
				:src="item.isSelf ? userInfo.avatar : avatar"
				mode="aspectFill"
			/>
			<image v-else class="message-bubble__avatar-img" src="/static/logo.png" mode="aspectFill" />
		</view>

		<view class="message-bubble__body">
			<image
				v-if="item.type === MessageType.IMAGE"
				class="message-bubble__image"
				:src="item.content"
				mode="widthFix"
				@click="previewImage"
			/>
			<view v-else class="message-bubble__content">
				<text class="message-bubble__text" selectable>{{ item.content }}</text>
			</view>
		</view>

		<text v-if="item.isError" class="message-bubble__error">!</text>
	</view>
</template>

<style scoped lang="scss">
.message-bubble {
	display: flex;
	align-items: flex-start;
	width: 100%;
	margin-bottom: 32rpx;

	&--tip {
		justify-content: center;
	}

	&--self {
		flex-direction: row-reverse;

		.message-bubble__avatar {
			margin-right: 0;
			margin-left: 20rpx;
		}

		.message-bubble__content {
			background: #3a7bfd;

			.message-bubble__text {
				color: #ffffff;
			}
		}

		.message-bubble__error {
			margin-left: 0;
			margin-right: 12rpx;
		}
	}

	&__record {
		padding: 6rpx 20rpx;
		background: #f2f2f2;
		border-radius: 8rpx;
		font-size: 24rpx;
		color: #999999;
	}

	&__avatar {
		width: 80rpx;
		height: 80rpx;
		flex-shrink: 0;
		margin-right: 20rpx;
	}

	&__avatar-img {
		width: 100%;
		height: 100%;
		border-radius: 50%;
	}

	&__body {
		max-width: 460rpx;
	}

	&__content {
		padding: 20rpx 24rpx;
		background: #ffffff;
		border-radius: 12rpx;
		box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.06);
	}

	&__text {
		font-size: 30rpx;
		line-height: 1.5;
		color: #333333;
		word-break: break-all;
	}

	&__image {
		max-width: 400rpx;
		border-radius: 12rpx;
	}

	&__error {
		align-self: center;
		margin-left: 12rpx;
		font-size: 32rpx;
		font-weight: bold;
		color: #f5222d;
	}
}
</style>
