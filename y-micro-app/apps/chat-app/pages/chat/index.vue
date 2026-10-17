<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { computed, nextTick, ref, watch } from 'vue'
import { getUserInfo, getSystemUserInfo } from '@/api/user'
import { uploadImage } from '@/api/upload'
import { MessageType } from '@/services/im/constants'
import useChatStore from '@/store/chat'
import useUserStore from '@/store/user'

/**
 * 聊天页
 *
 * 迁移自网页端 ChatWindow + RichInput：
 * - DOM 滚动容器 → scroll-view + scroll-into-view
 * - contenteditable 富输入 + input[type=file] → textarea + uni.chooseImage + uni.uploadFile
 * - 语音/视频通话（本期不做音视频）相关入口已剔除
 */
const chatStore = useChatStore()
const userStore = useUserStore()
const { activeContact, messages } = storeToRefs(chatStore)

const sending = ref(false)
const inputText = ref('')
const loadingMore = ref(false)
const scrollIntoViewId = ref('')
const channelInfo = ref({})

/** 当前会话的消息列表 */
const currentMessages = computed(() => {
	if (!activeContact.value) {
		return []
	}

	return messages.value.filter(
		(m) =>
			m.channelId === activeContact.value.uid &&
			m.channelType === activeContact.value.channelType,
	)
})

/** 导航栏标题 */
const channelTitle = computed(
	() => channelInfo.value.nickname || activeContact.value?.name || '聊天',
)

/** 滚动到底部 */
function scrollToBottom() {
	const list = currentMessages.value
	if (!list.length) {
		return
	}

	const target = `msg-${list[list.length - 1].id}`
	scrollIntoViewId.value = ''
	nextTick(() => {
		setTimeout(() => {
			scrollIntoViewId.value = target
		}, 50)
	})
}

/** 拉取频道信息（昵称 / 头像），数字 UID 为普通用户，非数字为系统用户 */
async function loadChannelInfo() {
	if (!activeContact.value) {
		channelInfo.value = {}
		return
	}

	if (activeContact.value.channelType !== 1) {
		channelInfo.value = { nickname: activeContact.value.name }
		return
	}

	const response = !isNaN(Number(activeContact.value.uid))
		? await getUserInfo(Number(activeContact.value.uid))
		: await getSystemUserInfo(activeContact.value.uid)

	if (response && response.code === 200) {
		channelInfo.value = response.data || {}
		activeContact.value.name = response.data?.nickname || activeContact.value.name
		activeContact.value.avatar = response.data?.avatar || activeContact.value.avatar
	} else {
		channelInfo.value = { nickname: activeContact.value.name }
	}
}

/** 发送文本消息 */
async function handleSendText() {
	const text = inputText.value.trim()

	if (!text || sending.value || !activeContact.value) {
		return
	}

	sending.value = true
	try {
		await chatStore.sendMessage({ type: MessageType.TEXT, content: text })
		inputText.value = ''
		scrollToBottom()
	} finally {
		sending.value = false
	}
}

/** 选择并发送图片消息 */
function handleChooseImage() {
	if (sending.value || !activeContact.value) {
		return
	}

	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		success: async (res) => {
			const filePath = res.tempFilePaths && res.tempFilePaths[0]
			if (!filePath) {
				return
			}

			sending.value = true
			try {
				const uploadRes = await uploadImage(filePath)
				if (uploadRes.code === 200 && uploadRes.data) {
					await chatStore.sendMessage({ type: MessageType.IMAGE, content: uploadRes.data })
					scrollToBottom()
				}
			} catch (error) {
				console.error('[Chat] 发送图片失败', error)
			} finally {
				sending.value = false
			}
		},
	})
}

/** 上拉加载更早的历史消息 */
async function handleLoadMore() {
	if (loadingMore.value || !activeContact.value || !currentMessages.value.length) {
		return
	}

	const oldest = currentMessages.value[0]
	const oldestSeq = Number(String(oldest.id).split('_')[0]) || 0

	if (!oldestSeq) {
		return
	}

	loadingMore.value = true
	try {
		await chatStore.loadHistoryMessages(activeContact.value, oldestSeq, 20)
	} finally {
		loadingMore.value = false
	}
}

watch(
	() => currentMessages.value.length,
	() => {
		scrollToBottom()
	},
)

onLoad(async (query) => {
	const channelId = decodeURIComponent(query.channelId || '')
	const channelType = Number(query.channelType || 1)
	const name = decodeURIComponent(query.name || '')
	const avatar = decodeURIComponent(query.avatar || '')

	if (!channelId) {
		return
	}

	let contact = chatStore.contacts.find(
		(c) => c.uid === channelId && c.channelType === channelType,
	)

	if (!contact) {
		contact = {
			uid: channelId,
			name: name || channelId,
			type: channelType === 2 ? 'group' : 'person',
			channelType,
			unreadCount: 0,
			avatar,
		}
		chatStore.contacts.push(contact)
	}

	userStore.currentChannel = { channelId, type: channelType }

	await chatStore.selectContact(contact)
	await loadChannelInfo()
	scrollToBottom()
})
</script>

<template>
	<view class="chat">
		<wd-navbar :title="activeContact.name" left-arrow>

</wd-navbar>

		<scroll-view
			class="chat__scroll"
			scroll-y
			:scroll-into-view="scrollIntoViewId"
			:scroll-with-animation="true"
			@scrolltoupper="handleLoadMore"
		>
			<view class="chat__list">
				<view v-if="loadingMore" class="chat__loading">
					<text class="chat__loading-text">加载中...</text>
				</view>

				<message-bubble
					v-for="item in currentMessages"
					:id="`msg-${item.id}`"
					:key="item.id"
					:item="item"
					:avatar="activeContact?.avatar || ''"
				/>

				<view v-if="currentMessages.length === 0" class="chat__empty">
					<text class="chat__empty-text">暂无消息，发送一条消息开始聊天吧</text>
				</view>
			</view>
		</scroll-view>

		<view class="chat__footer">
			<textarea
				v-model="inputText"
				class="chat__input"
				placeholder="请输入消息"
				auto-height
				:maxlength="-1"
				confirm-type="send"
				:show-confirm-bar="false"
				@confirm="handleSendText"
			/>
			<view class="chat__action" @click="handleChooseImage">
				<text class="chat__action-icon">＋</text>
			</view>
			<wd-button
				class="chat__send"
				type="primary"
				size="small"
				:loading="sending"
				@click="handleSendText"
			>发送</wd-button>
		</view>
	</view>
</template>

<style scoped lang="scss">
.chat {
	width: 100vw;
	height: 100vh;
	display: flex;
	flex-direction: column;
	overflow: hidden;
	&__scroll {
		flex: 1;
		overflow: hidden;
		// height: 100%;
	}

	&__list {
		padding: 24rpx;
	}

	&__loading {
		display: flex;
		justify-content: center;
		padding-bottom: 16rpx;
	}

	&__loading-text {
		font-size: 24rpx;
		color: #999999;
	}

	&__empty {
		display: flex;
		justify-content: center;
		padding-top: 160rpx;
	}

	&__empty-text {
		font-size: 28rpx;
		color: #999999;
	}

	&__footer {
		display: flex;
		align-items: flex-end;
		padding: 16rpx 24rpx;
		background: #ffffff;
		border-top: 1px solid rgba(0, 0, 0, 0.06);
	}

	&__input {
		flex: 1;
		max-height: 200rpx;
		padding: 16rpx 20rpx;
		background: #f7f7f7;
		border-radius: 12rpx;
		font-size: 30rpx;
		line-height: 1.4;
	}

	&__action {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 64rpx;
		height: 64rpx;
		margin: 0 16rpx;
		border-radius: 50%;
		background: #f7f7f7;
	}

	&__action-icon {
		font-size: 40rpx;
		color: #666666;
	}

	&__send {
		flex-shrink: 0;
	}
}
</style>
