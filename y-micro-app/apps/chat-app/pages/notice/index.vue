<script setup>
import { onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import useNotificationStore from '@/store/notification'

/**
 * 通知列表
 *
 * 迁移自网页端通知模块，复用 notification store（数据来源 /notification/list）。
 */
const notificationStore = useNotificationStore()
const { notifications, unreadCount, loading } = storeToRefs(notificationStore)

/** 格式化后端返回的时间字符串 */
function formatTime(value) {
	if (!value) {
		return ''
	}

	return String(value).replace('T', ' ').slice(0, 16)
}

/** 点击单条：标记已读 */
async function handleRead(item) {
	if (item.isRead === 1) {
		return
	}

	await notificationStore.markRead(item.id)
}

/** 全部已读 */
async function handleReadAll() {
	if (!unreadCount.value) {
		return
	}

	await notificationStore.markAllRead()
}

onShow(() => {
	notificationStore.fetchNotifications()
})
</script>

<template>
	<page-layout title="通知" show-back>
		<template #right>
			<text v-if="unreadCount > 0" class="notice__read-all" @click="handleReadAll">全部已读</text>
		</template>

		<view class="notice">
			<view v-if="notifications.length === 0 && !loading" class="notice__empty">
				<text class="notice__empty-text">暂无通知</text>
			</view>

			<view
				v-for="item in notifications"
				:key="item.id"
				class="notice__item"
				@click="handleRead(item)"
			>
				<view class="notice__header">
					<text class="notice__title">{{ item.title || '系统通知' }}</text>
					<text class="notice__time">{{ formatTime(item.createTime) }}</text>
				</view>
				<text class="notice__content">{{ item.content }}</text>
				<view v-if="item.isRead === 0" class="notice__dot" />
			</view>
		</view>
	</page-layout>
</template>

<style scoped lang="scss">
.notice {
	&__read-all {
		font-size: 28rpx;
		color: #3a7bfd;
	}

	&__empty {
		display: flex;
		justify-content: center;
		padding-top: 200rpx;
	}

	&__empty-text {
		font-size: 28rpx;
		color: #999999;
	}

	&__item {
		position: relative;
		margin: 24rpx;
		padding: 24rpx;
		background: #ffffff;
		border-radius: 16rpx;
	}

	&__header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	&__title {
		font-size: 30rpx;
		font-weight: 500;
		color: #1f1f1f;
	}

	&__time {
		font-size: 24rpx;
		color: #999999;
	}

	&__content {
		display: block;
		margin-top: 12rpx;
		font-size: 28rpx;
		line-height: 1.5;
		color: #666666;
	}

	&__dot {
		position: absolute;
		top: 28rpx;
		right: 24rpx;
		width: 16rpx;
		height: 16rpx;
		background: #f56c6c;
		border-radius: 50%;
	}
}
</style>
