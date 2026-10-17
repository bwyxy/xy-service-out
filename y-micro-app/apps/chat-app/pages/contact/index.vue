<script setup>
import { onShow, onUnload } from '@dcloudio/uni-app'
import { ref } from 'vue'
import {
	getFriendList,
	getReceivedRequests,
	handleFriendRequest,
	searchUsers,
	sendFriendRequest,
} from '@/api/friend'
import useChatStore from '@/store/chat'
import useUserStore from '@/store/user'

/**
 * 联系人页（tab）
 *
 * 迁移自网页端 views/friend（好友列表）与 SearchFun 下的
 * SearchUserDialog（搜索用户）/ FriendRequestDialog（好友请求）：
 * 三块内容合并为一个页面，通过顶部搜索框切换「搜索结果」与「好友视图」。
 */
const chatStore = useChatStore()
const userStore = useUserStore()

const keyword = ref('')
const friends = ref([])
const requests = ref([])
const searchResult = ref([])
const searching = ref(false)
const processingMap = ref({})

let searchTimer = null

/** 拉取好友列表 */
async function fetchFriends() {
	const response = await getFriendList()

	if (response.code === 200) {
		friends.value = response.data || []
	}
}

/** 拉取待处理的好友请求 */
async function fetchRequests() {
	const response = await getReceivedRequests(0)

	if (response.code === 200) {
		requests.value = response.data || []
	}
}

/** 搜索用户 */
async function doSearch(kw) {
	if (!kw) {
		searchResult.value = []
		return
	}

	searching.value = true
	try {
		const response = await searchUsers(kw)
		if (response.code === 200) {
			searchResult.value = response.data || []
		}
	} finally {
		searching.value = false
	}
}

function handleKeywordChange(event) {
	const value = (event && event.value) || ''

	if (searchTimer) {
		clearTimeout(searchTimer)
	}

	searchTimer = setTimeout(() => {
		doSearch(value ? value.trim() : '')
	}, 300)
}

/** 发送好友请求 */
async function handleAddFriend(user) {
	processingMap.value[user.uId] = true
	try {
		const response = await sendFriendRequest({ toUid: user.uId, applyMsg: '' })
		if (response.code === 200) {
			uni.showToast({ title: '好友请求已发送', icon: 'none' })
		}
	} finally {
		processingMap.value[user.uId] = false
	}
}

/** 处理收到的好友请求：action 1=同意 2=拒绝 */
async function handleRequestAction(request, action) {
	const response = await handleFriendRequest({
		requestId: request.id,
		action,
		replyMsg: '',
	})

	if (response.code === 200) {
		uni.showToast({ title: action === 1 ? '已同意' : '已拒绝', icon: 'none' })
		fetchRequests()
		if (action === 1) {
			fetchFriends()
		}
	}
}

/** 与好友聊天 */
function handleChat(friend) {
	const uid = String(friend.uid ?? friend.friendId)
	const nickname = friend.nickname || uid
	const avatar = friend.avatar || ''

	chatStore.addContact(uid, nickname, avatar)
	userStore.currentChannel = { channelId: uid, type: 1 }

	uni.navigateTo({
		url:
			`/pages/chat/index?channelId=${encodeURIComponent(uid)}` +
			`&channelType=1` +
			`&name=${encodeURIComponent(nickname)}` +
			`&avatar=${encodeURIComponent(avatar)}`,
	})
}

onShow(() => {
	fetchFriends()
	fetchRequests()
})

onUnload(() => {
	if (searchTimer) {
		clearTimeout(searchTimer)
		searchTimer = null
	}
})
</script>

<template>
	<page-layout title="联系人" :scrollable="false">
		<view class="contact__search">
			<wd-search
				v-model="keyword"
				placeholder="搜索用户"
				hide-cancel
				@change="handleKeywordChange"
				@search="handleKeywordChange"
			/>
		</view>

		<scroll-view class="contact__scroll" scroll-y>
			<!-- 搜索结果 -->
			<template v-if="keyword">
				<view class="contact__section-title">搜索结果</view>

				<view v-if="searching" class="contact__hint">搜索中...</view>
				<view v-else-if="searchResult.length === 0" class="contact__hint">未搜索到用户</view>

				<view
					v-for="user in searchResult"
					:key="user.uId"
					class="contact__row"
				>
					<image
						v-if="user.avatar"
						class="contact__avatar"
						:src="user.avatar"
						mode="aspectFill"
					/>
					<image v-else class="contact__avatar" src="/static/logo.png" mode="aspectFill" />

					<view class="contact__info">
						<text class="contact__name">{{ user.nickname || '用户' + user.uId }}</text>
						<text class="contact__sub">{{ user.phone || user.email || '' }}</text>
					</view>

					<wd-button
						type="primary"
						size="small"
						:loading="processingMap[user.uId]"
						@click="handleAddFriend(user)"
					>添加</wd-button>
				</view>
			</template>

			<!-- 好友视图 -->
			<template v-else>
				<view v-if="requests.length > 0">
					<view class="contact__section-title">新的朋友</view>

					<view v-for="request in requests" :key="request.id" class="contact__row">
						<image
							v-if="request.fromAvatar"
							class="contact__avatar"
							:src="request.fromAvatar"
							mode="aspectFill"
						/>
						<image v-else class="contact__avatar" src="/static/logo.png" mode="aspectFill" />

						<view class="contact__info">
							<text class="contact__name">
								{{ request.fromNickname || '用户' + request.fromUid }}
							</text>
							<text class="contact__sub">{{ request.applyMsg || '请求添加您为好友' }}</text>
						</view>

						<view class="contact__actions">
							<wd-button size="small" type="primary" @click="handleRequestAction(request, 1)">
								同意
							</wd-button>
							<wd-button size="small" plain @click="handleRequestAction(request, 2)">
								拒绝
							</wd-button>
						</view>
					</view>
				</view>

				<view class="contact__section-title">好友列表</view>

				<view v-if="friends.length === 0" class="contact__hint">还没有好友，去搜索添加吧</view>

				<view
					v-for="friend in friends"
					:key="friend.id || friend.friendId"
					class="contact__row"
					@click="handleChat(friend)"
				>
					<image
						v-if="friend.avatar"
						class="contact__avatar"
						:src="friend.avatar"
						mode="aspectFill"
					/>
					<image v-else class="contact__avatar" src="/static/logo.png" mode="aspectFill" />

					<view class="contact__info">
						<text class="contact__name">{{ friend.nickname || friend.friendId }}</text>
					</view>
				</view>
			</template>
		</scroll-view>
	</page-layout>
</template>

<style scoped lang="scss">
.contact {
	&__search {
		padding: 16rpx 24rpx;
		background: #ffffff;
	}

	&__scroll {
		flex: 1;
		height: 100%;
	}

	&__section-title {
		padding: 24rpx 24rpx 12rpx;
		font-size: 26rpx;
		color: #999999;
	}

	&__hint {
		padding: 60rpx 0;
		font-size: 28rpx;
		color: #999999;
		text-align: center;
	}

	&__row {
		display: flex;
		align-items: center;
		padding: 24rpx;
		background: #ffffff;
		border-bottom: 1px solid rgba(0, 0, 0, 0.06);

		&:active {
			background: #f5f5f5;
		}
	}

	&__avatar {
		width: 88rpx;
		height: 88rpx;
		flex-shrink: 0;
		border-radius: 50%;
	}

	&__info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		margin-left: 24rpx;
	}

	&__name {
		font-size: 30rpx;
		color: #1f1f1f;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	&__sub {
		margin-top: 8rpx;
		font-size: 24rpx;
		color: #999999;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	&__actions {
		display: flex;
		align-items: center;
		gap: 16rpx;
	}
}
</style>
