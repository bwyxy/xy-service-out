<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getPostDetail, setPostLike } from '@/api/forum'

/**
 * 帖子详情
 *
 * 正文按纯文本渲染（contentType 为 Markdown 时不做富文本解析）。
 * 评论 / 收藏 / 分享后端尚未实现，入口保留并提示「敬请期待」。
 */
const postId = ref(0)
const post = ref(null)
const loading = ref(true)

function toast(title) {
	uni.showToast({ title, icon: 'none' })
}

function notImplemented() {
	toast('敬请期待')
}

function handleBack() {
	uni.navigateBack()
}

async function fetchDetail() {
	try {
		const response = await getPostDetail(postId.value)

		if (response.code === 200) {
			post.value = response.data
		}
	} catch (error) {
		console.error('[Community] 获取帖子详情失败', error)
	} finally {
		loading.value = false
	}
}

/** 点赞 / 取消点赞：以服务端返回的状态为准增减计数 */
async function handleLike() {
	if (!post.value) {
		return
	}

	const response = await setPostLike(post.value.postId)

	if (response.code === 200) {
		post.value.liked = response.data
		post.value.likeCount = Math.max(
			0,
			Number(post.value.likeCount || 0) + (response.data ? 1 : -1),
		)
	}
}

onLoad((options) => {
	postId.value = Number(options.postId || 0)

	if (!postId.value) {
		loading.value = false
		return
	}

	fetchDetail()
})
</script>

<template>
	<view class="post-detail">
		<wd-navbar title="帖子详情" left-arrow @click-left="handleBack"></wd-navbar>

		<scroll-view class="post-detail__scroll" scroll-y>
			<view v-if="post" class="post-detail__body">
				<view class="author">
					<image
						class="author__avatar"
						:src="post.avatar || '/static/logo.png'"
						mode="aspectFill"
					/>
					<view class="author__info">
						<text class="author__name">{{ post.nickname || '匿名' }}</text>
						<text class="author__meta">
							{{ post.publishedTime || post.createTime || '' }}
						</text>
					</view>
					<view class="author__follow" @click="notImplemented">关注</view>
				</view>

				<text class="post-title">{{ post.title }}</text>

				<image
					v-if="post.coverImage"
					class="post-cover"
					:src="post.coverImage"
					mode="widthFix"
				/>

				<text class="post-content">{{ post.content }}</text>

				<view class="post-meta">
					<text>{{ post.viewCount || 0 }} 浏览</text>
					<text v-if="post.ipAddr">{{ post.ipAddr }}</text>
				</view>
			</view>

			<view v-else-if="!loading" class="post-detail__empty">帖子不存在或已被删除</view>

			<view class="comment-block">
				<text class="comment-block__title">评论</text>
				<view class="comment-block__empty" @click="notImplemented">
					还没有评论，快来抢沙发
				</view>
			</view>

			<view style="height: 160rpx;"></view>
		</scroll-view>

		<view class="post-bar">
			<view class="post-bar__input" @click="notImplemented">
				<text>说点什么...</text>
			</view>

			<view class="post-bar__action" @click="handleLike">
				<wd-icon
					name="thumb-up"
					size="40rpx"
					:color="post && post.liked ? '#1890ff' : '#8c8c8c'"
				/>
				<text class="post-bar__text" :class="{ 'is-active': post && post.liked }">
					{{ post ? post.likeCount || 0 : 0 }}
				</text>
			</view>

			<view class="post-bar__action" @click="notImplemented">
				<wd-icon name="star" size="40rpx" color="#8c8c8c" />
				<text class="post-bar__text">收藏</text>
			</view>

			<view class="post-bar__action" @click="notImplemented">
				<wd-icon name="share" size="40rpx" color="#8c8c8c" />
				<text class="post-bar__text">分享</text>
			</view>
		</view>
	</view>
</template>

<style scoped lang="scss">
.post-detail {
	width: 100vw;
	height: 100vh;
	display: flex;
	flex-direction: column;
	overflow: hidden;

	&__scroll {
		flex: 1;
		overflow: hidden;
	}

	&__body {
		margin: 24rpx;
		padding: 32rpx;
		background: #ffffff;
		border-radius: 20rpx;
	}

	&__empty {
		padding: 200rpx 0;
		font-size: 28rpx;
		color: #999999;
		text-align: center;
	}
}

.author {
	display: flex;
	align-items: center;

	&__avatar {
		width: 80rpx;
		height: 80rpx;
		border-radius: 50%;
		flex-shrink: 0;
	}

	&__info {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
		display: flex;
		flex-direction: column;
	}

	&__name {
		font-size: 28rpx;
		font-weight: 600;
		color: #1f1f1f;
	}

	&__meta {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #bfbfbf;
	}

	&__follow {
		height: 56rpx;
		line-height: 56rpx;
		padding: 0 32rpx;
		border: 1rpx solid #1890ff;
		border-radius: 28rpx;
		font-size: 24rpx;
		color: #1890ff;
	}
}

.post-title {
	margin-top: 32rpx;
	font-size: 36rpx;
	font-weight: 600;
	line-height: 52rpx;
	letter-spacing: 1rpx;
	color: #1f1f1f;
}

.post-cover {
	width: 100%;
	margin-top: 24rpx;
	border-radius: 16rpx;
}

.post-content {
	margin-top: 24rpx;
	font-size: 30rpx;
	line-height: 50rpx;
	letter-spacing: 1rpx;
	color: #434343;
	white-space: pre-wrap;
	word-break: break-all;
}

.post-meta {
	display: flex;
	justify-content: space-between;
	margin-top: 40rpx;
	font-size: 22rpx;
	color: #bfbfbf;
}

.comment-block {
	margin: 24rpx;
	padding: 32rpx;
	background: #ffffff;
	border-radius: 20rpx;

	&__title {
		font-size: 30rpx;
		font-weight: 600;
		color: #1f1f1f;
	}

	&__empty {
		padding: 60rpx 0;
		font-size: 26rpx;
		color: #bfbfbf;
		text-align: center;
	}
}

.post-bar {
	width: 100%;
	box-sizing: border-box;
	padding: 16rpx 24rpx calc(16rpx + constant(safe-area-inset-bottom));
	padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
	background: #ffffff;
	border-top: 1rpx solid rgba(0, 0, 0, 0.06);
	display: flex;
	align-items: center;

	&__input {
		flex: 1;
		height: 68rpx;
		line-height: 68rpx;
		padding: 0 32rpx;
		background: #f0f0f0;
		border-radius: 34rpx;
		font-size: 26rpx;
		color: #8c8c8c;
		box-sizing: border-box;
	}

	&__action {
		display: flex;
		align-items: center;
		margin-left: 28rpx;
	}

	&__text {
		margin-left: 8rpx;
		font-size: 24rpx;
		color: #8c8c8c;

		&.is-active {
			color: #1890ff;
		}
	}
}
</style>
