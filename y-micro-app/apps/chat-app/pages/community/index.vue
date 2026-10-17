<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getBoards, getPosts, setPostLike } from '@/api/forum'

/**
 * 社区（tab）
 *
 * 版块胶囊 + 帖子信息流，交互与样式参考手机端 QQ 空间。
 * 评论 / 关注等后端尚未实现的入口统一提示「敬请期待」。
 */
const PAGE_SIZE = 10

const boards = ref([{ boardId: 0, name: '全部' }])
const currentBoardId = ref(0)
const boardsLoaded = ref(false)

const posts = ref([])
const pageNum = ref(1)
const loading = ref(false)
/** wd-loadmore 状态：'' | 'loading' | 'error' | 'finished' */
const loadState = ref('')

function toast(title) {
	uni.showToast({ title, icon: 'none' })
}

function notImplemented() {
	toast('敬请期待')
}

/** 拉取顶级版块，头部固定插入「全部」（boardId=0 后端不过滤） */
async function fetchBoards() {
	const response = await getBoards({})

	if (response.code === 200) {
		boards.value = response.data || []
		boardsLoaded.value = true
	}
}

/** 拉取帖子列表，append 为 true 时追加下一页 */
async function fetchPosts(append = false) {
	if (loading.value) {
		return
	}

	loading.value = true
	loadState.value = 'loading'

	try {
		const response = await getPosts({
			pageNum: pageNum.value,
			pageSize: PAGE_SIZE,
			boardId: currentBoardId.value,
		})

		if (response.code === 200) {
			const data = response.data || {}
			const list = data.list || []

			posts.value = append ? posts.value.concat(list) : list
			loadState.value = pageNum.value >= Number(data.pages || 0) ? 'finished' : ''
		} else {
			loadState.value = 'error'
		}
	} catch (error) {
		console.error('[Community] 获取帖子列表失败', error)

		if (append) {
			pageNum.value -= 1
		}
		loadState.value = 'error'
	} finally {
		loading.value = false
	}
}

/** 回到首屏 */
function refresh() {
	pageNum.value = 1
	loadState.value = ''
	fetchPosts()
}

function handleLoadMore() {
	if (loading.value || loadState.value === 'finished') {
		return
	}

	pageNum.value += 1
	fetchPosts(true)
}

function handleSwitchBoard(boardId) {
	if (boardId === currentBoardId.value) {
		return
	}

	currentBoardId.value = boardId
	refresh()
}

/** 点赞 / 取消点赞：以服务端返回的状态为准增减计数 */
async function handleLike(post) {
	const response = await setPostLike(post.postId)

	if (response.code === 200) {
		post.liked = response.data
		post.likeCount = Math.max(0, Number(post.likeCount || 0) + (response.data ? 1 : -1))
	}
}

function handleDetail(post) {
	uni.navigateTo({ url: `/pages/community/detail?postId=${post.postId}` })
}

function handlePublish() {
	uni.navigateTo({ url: '/pages/community/publish' })
}

onShow(() => {
	if (!boardsLoaded.value) {
		fetchBoards()
	}

	refresh()
})
</script>

<template>
	<page-layout>
		<view class="app-container">
			<wd-navbar>
				<template #title>
					<view
						v-for="board in boards"
						:key="board.boardId"
						class="board-item"
						:class="{ 'is-active': board.boardId === currentBoardId }"
						@click="handleSwitchBoard(board.boardId)"
					>
						{{ board.name }}
					</view>
				</template>
				<template #right>
					<view class="community-publish" @click="handlePublish">
						<image src="@/static/plus.png" mode="scaleToFill"></image>
					</view>
				</template>
			</wd-navbar>

			<view class="community-top">

				<!-- <scroll-view class="board-tabs" scroll-x>
					<view
						v-for="board in boards"
						:key="board.boardId"
						class="board-item"
						:class="{ 'is-active': board.boardId === currentBoardId }"
						@click="handleSwitchBoard(board.boardId)"
					>
						{{ board.name }}
					</view>
				</scroll-view> -->
			</view>

			<list-main @scrolltolower="handleLoadMore">
				<view class="post-list">
					<post-item
						v-for="post in posts"
						:key="post.postId"
						:item="post"
						@click="handleDetail"
						@like="handleLike"
						@comment="notImplemented"
					/>
				</view>

				<view v-if="!posts.length && !loading" class="post-empty">
					<text class="post-empty__title">这里还没有内容</text>
					<text class="post-empty__desc">点击右上角发布你的第一条帖子</text>
				</view>

				<wd-loadmore :state="loadState" />

				<view style="height: 124rpx;"></view>
			</list-main>
		</view>
	</page-layout>
</template>

<style scoped lang="scss">
.community-publish {
	display: flex;
	align-items: center;

	image {
		width: 34rpx;
		height: 34rpx;
	}
}

.community-top {
	padding: 0 24rpx;
}

.community-cover {
	height: 180rpx;
	box-sizing: border-box;
	padding: 34rpx 36rpx;
	background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
	border-radius: 20rpx;
	display: flex;
	flex-direction: column;
	justify-content: center;

	&__title {
		font-size: 40rpx;
		font-weight: 600;
		letter-spacing: 2rpx;
		color: #ffffff;
	}

	&__desc {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.85);
	}
}

.board-tabs {
	width: 100%;
	margin: 24rpx 0;
	white-space: nowrap;
}

.board-item {
	display: inline-block;
	height: 60rpx;
	line-height: 60rpx;
	padding: 0 32rpx;
	margin-right: 16rpx;
	background: #ffffff;
	border-radius: 30rpx;
	font-size: 26rpx;
	color: #595959;
	letter-spacing: 1rpx;

	&.is-active {
		background: #1890ff;
		color: #ffffff;
		font-weight: 600;
	}
}

.post-list {
	width: 100%;
}

.post-empty {
	padding: 160rpx 0;
	display: flex;
	flex-direction: column;
	align-items: center;

	&__title {
		font-size: 30rpx;
		color: #595959;
	}

	&__desc {
		margin-top: 14rpx;
		font-size: 24rpx;
		color: #bfbfbf;
	}
}
</style>
