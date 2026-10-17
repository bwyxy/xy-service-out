<script setup>
import { onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { ref } from 'vue'
import { getConversation } from '@/api/chat'
import useChatStore from '@/store/chat'
import useNotificationStore from '@/store/notification'
import useUserStore from '@/store/user'
import { decodeBase64FromJava, extractMessageContent } from '@/utils/wk'

const chatStore = useChatStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { contacts, isRefreshConversations } = storeToRefs(chatStore)
const { unreadCount } = storeToRefs(notificationStore)

const loading = ref(false)

/** 解析会话最后一条消息内容（payload 为 Java 侧 Base64 编码的 JSON） */
function resolveLatestMessageContent(recents) {
	if (!recents || !recents.length) {
		return ''
	}

	try {
		const decoded = decodeBase64FromJava(recents[0].payload || '')

		return extractMessageContent(JSON.parse(decoded))
	} catch (error) {
		console.warn('[Index] 解析最后一条消息失败', error)
		return ''
	}
}

/** 将后端会话列表合并进 chat store 的 contacts */
function syncToChatStore(list) {
	for (const item of list) {
		const exists = contacts.value.find(
			(c) => String(c.uid) === String(item.channel_id) && c.channelType === item.channel_type,
		)
		const lastMessage = resolveLatestMessageContent(item.recents)

		if (exists) {
			exists.name = item.name || exists.name
			exists.avatar = item.avatar || exists.avatar
			exists.lastMessage = lastMessage || exists.lastMessage
		} else {
			contacts.value.push({
				uid: String(item.channel_id),
				name: item.name,
				type: item.channel_type === 2 ? 'group' : 'person',
				channelType: item.channel_type,
				unreadCount: item.unread_count || 0,
				lastMessage,
				lastMessageTime: Date.now(),
				avatar: item.avatar,
			})
		}
	}

	contacts.value.sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))
}

/** 拉取会话列表 */
async function fetchConversations() {
	loading.value = true
	try {
		const response = await getConversation({ msgCount: 1 })

		if (response.code === 200) {
			syncToChatStore(response.data || [])
		}
	} catch (error) {
		console.error('[Index] 获取会话列表失败', error)
	} finally {
		loading.value = false
	}
}

/** 进入聊天页 */
function handleSelectConversation(item) {
	userStore.currentChannel = {
		channelId: item.uid,
		type: item.channelType,
	}

	uni.navigateTo({
		url:
			`/pages/chat/index?channelId=${encodeURIComponent(item.uid)}` +
			`&channelType=${item.channelType}` +
			`&name=${encodeURIComponent(item.name || '')}` +
			`&avatar=${encodeURIComponent(item.avatar || '')}`,
	})
}

/** 打开通知中心 */
function handleNotice() {
	uni.navigateTo({ url: '/pages/notice/index' })
}

onShow(() => {
	fetchConversations()
	notificationStore.fetchUnreadCount()

	if (isRefreshConversations.value) {
		isRefreshConversations.value = false
	}
})
</script>

<template>
	<page-layout>
		<view class="app-container">
			<wd-navbar>
				<template #left>
					<user-info></user-info>
				</template>
				<template #right>
					<quick-menu></quick-menu>
				</template>
			</wd-navbar>

			<view class="top-search">
				<view class="search-box">
					<image src="@/static/search.png" mode="scaleToFill"></image>
					<text>搜索</text>
				</view>
			</view>

			<list-main>
				<wd-swipe-action
					v-for="item in contacts"
					:key="`${item.uid}_${item.channelType}`"
				>
					<conversation-item
						:item="item"
						class="index__item"
						@click="handleSelectConversation"
					 />
					 <template #right>
						 <view class="conversation-menu">
						 	<view
								class="conversation-menu-item"
								style="background: #e00f00;"
							>
								删除
							</view>
							<view
								class="conversation-menu-item"
								style="background: #1890ff;"
							>
								置顶
							</view>
						 </view>
					 </template>
				</wd-swipe-action>

				<view style="height: 124rpx;"></view>
			</list-main>
		</view>
	</page-layout>
</template>

<style scoped lang="scss">
.index {
	&__notice {
		position: relative;
		display: flex;
		align-items: center;
	}

	&__notice-text {
		font-size: 28rpx;
		color: #3a7bfd;
	}

	&__badge {
		position: absolute;
		top: -12rpx;
		right: -28rpx;
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

	&__list {
		padding-bottom: 24rpx;
	}

	&__item {
		border-bottom: 1px solid rgba(0, 0, 0, 0.06);

		&:last-child {
			border-bottom: none;
		}
	}

	&__hint {
		padding: 200rpx 0;
		font-size: 28rpx;
		color: #999999;
		text-align: center;
	}
}

.conversation-menu {
	height: 100%;
	display: flex;
	&-item {
		height: 100%;
		padding: 0 48rpx;
		color: #fff;
		font-size: 32rpx;
		letter-spacing: 1px;
		display: flex;
		align-items: center;
	}
}

.top-search {
	padding: 24rpx;
	background: #fff;
	.search-box {
		width: 100%;
		height: 64rpx;
		background: #f0f0f0;
		border-radius:13rpx;
		box-sizing: border-box;
		padding: 0 32rpx;
		display: flex;
		align-items: center;
		image {
			width: 28rpx;
			height: 28rpx;
			margin-right: 14rpx;
		}
		text {
			color: #8c8c8c;
			font-size: 28rpx;
			letter-spacing: 1px;
		}
	}
}
</style>
