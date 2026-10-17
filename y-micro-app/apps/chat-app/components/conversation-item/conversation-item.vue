<script setup>
import { computed } from 'vue'

/**
 * 会话 / 联系人条目
 *
 * 展示头像、昵称、最后一条消息与未读数，点击后由父级处理跳转。
 */
const props = defineProps({
	/** 联系人数据 { uid, name, avatar, lastMessage, unreadCount, channelType } */
	item: { type: Object, required: true },
	/** 是否展示右侧箭头 */
	showArrow: { type: Boolean, default: false },
})

defineEmits(['click'])

/** 图片消息在会话列表中的展示文案 */
const IMAGE_URL_RE = /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i

const displayMessage = computed(() => {
	const message = props.item.lastMessage || ''

	if (IMAGE_URL_RE.test(message)) {
		return '[图片消息]'
	}

	return message || '暂无消息'
})

const badgeText = computed(() => {
	const count = props.item.unreadCount || 0

	return count > 99 ? '99+' : String(count)
})
</script>

<template>
	<view class="conversation-item" @click="$emit('click', item)">
		<view class="conversation-item__avatar">
			<image v-if="item.avatar" class="conversation-item__avatar-img" :src="item.avatar" mode="aspectFill" />
			<image v-else class="conversation-item__avatar-img" src="/static/logo.png" mode="aspectFill" />

			<view v-if="item.unreadCount > 0" class="conversation-item__badge">{{ badgeText }}</view>
		</view>

		<view class="conversation-item__content">
			<view class="conversation-item__title">
				<text class="conversation-item__name">{{ item.name || item.uid }}</text>
			</view>
			<text class="conversation-item__message">{{ displayMessage }}</text>
		</view>

		<text v-if="showArrow" class="conversation-item__arrow">›</text>
	</view>
</template>

<style scoped lang="scss">
@mixin textEllipsis($num) {
	word-break: break-all;
	word-wrap:break-word;
	overflow: hidden;
	text-overflow: ellipsis;
	display: -webkit-box;
	-webkit-line-clamp: $num;
	-webkit-box-orient: vertical;
}

.conversation-item {
	display: flex;
	align-items: center;
	padding: 24rpx;
	background: #ffffff;

	&:active {
		background: #f5f5f5;
	}

	&__avatar {
		position: relative;
		width: 96rpx;
		height: 96rpx;
		flex-shrink: 0;
	}

	&__avatar-img {
		width: 100%;
		height: 100%;
		border-radius: 16rpx;
	}

	&__badge {
		position: absolute;
		top: -8rpx;
		right: -8rpx;
		min-width: 32rpx;
		height: 32rpx;
		padding: 0 6rpx;
		background: #f56c6c;
		border-radius: 16rpx;
		font-size: 20rpx;
		line-height: 32rpx;
		color: #ffffff;
		text-align: center;
	}

	&__content {
		flex: 1;
		min-width: 0;
		margin-left: 24rpx;
		display: flex;
		flex-direction: column;
	}

	&__title {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	&__name {
		font-size: 32rpx;
		font-weight: 500;
		color: #1f1f1f;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	&__message {
		margin-top: 12rpx;
		font-size: 26rpx;
		color: #999999;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		@include textEllipsis(1);
	}

	&__arrow {
		margin-left: 16rpx;
		font-size: 40rpx;
		color: #c8c7cc;
	}
}
</style>
