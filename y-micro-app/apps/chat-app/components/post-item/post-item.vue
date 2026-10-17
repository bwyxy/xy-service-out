<script setup>
import { computed } from 'vue'

/**
 * 社区帖子卡片
 *
 * 参考手机端 QQ 空间信息流：封面大图 + 两行标题 + 作者行 + 点赞/评论。
 * 点赞与评论由父级处理，卡片只负责展示与派发事件。
 */
const props = defineProps({
	/** 帖子数据（ForumPostVO） */
	item: { type: Object, required: true },
})

const emit = defineEmits(['click', 'like', 'comment'])

/** 超过 9999 的计数统一显示为 9999+ */
function formatCount(count) {
	const value = Number(count || 0)

	return value >= 9999 ? '9999+' : String(value)
}

const likeText = computed(() => formatCount(props.item.likeCount))
const commentText = computed(() => formatCount(props.item.commentCount))

/** 状态角标：非已发布（status !== 2）时展示审核状态 */
const statusBadge = computed(() => {
	const map = { 0: '草稿', 1: '审核中', 3: '未通过', 4: '已下架' }

	return Number(props.item.status) === 2 ? '' : map[props.item.status] || ''
})

/** 已发布且可见性为私密的帖子展示「私密」角标 */
const isPrivate = computed(
	() => Number(props.item.status) === 2 && Number(props.item.visibility) === 3,
)
</script>

<template>
	<view class="post-item" @click="emit('click', item)">
		<view v-if="item.coverImage" class="post-item__cover">
			<image class="post-item__cover-img" :src="item.coverImage" mode="aspectFill" />

			<view v-if="statusBadge" class="post-item__badge">{{ statusBadge }}</view>
			<view v-else-if="isPrivate" class="post-item__badge post-item__badge--private">私密</view>
		</view>

		<view class="post-item__body">
			<text class="post-item__title">{{ item.title }}</text>
			<text v-if="item.summary" class="post-item__summary">{{ item.summary }}</text>

			<view class="post-item__footer">
				<image
					class="post-item__avatar"
					:src="item.avatar || '/static/logo.png'"
					mode="aspectFill"
				/>
				<text class="post-item__nickname">{{ item.nickname || '匿名' }}</text>

				<view class="post-item__action" @click.stop="emit('like', item)">
					<wd-icon
						name="thumb-up"
						size="32rpx"
						:color="item.liked ? '#1890ff' : '#bfbfbf'"
					/>
					<text class="post-item__action-text" :class="{ 'is-active': item.liked }">
						{{ likeText }}
					</text>
				</view>

				<view class="post-item__action" @click.stop="emit('comment', item)">
					<wd-icon name="chat" size="32rpx" color="#bfbfbf" />
					<text class="post-item__action-text">{{ commentText }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<style scoped lang="scss">
.post-item {
	margin: 0 24rpx 24rpx;
	background: #ffffff;
	border-radius: 20rpx;
	overflow: hidden;

	&:active {
		opacity: 0.9;
	}

	&__cover {
		position: relative;
		width: 100%;
		height: 320rpx;
	}

	&__cover-img {
		width: 100%;
		height: 100%;
	}

	&__badge {
		position: absolute;
		top: 20rpx;
		right: 20rpx;
		padding: 4rpx 16rpx;
		background: rgba(0, 0, 0, 0.5);
		border-radius: 24rpx;
		font-size: 22rpx;
		color: #ffffff;

		&--private {
			background: rgba(24, 144, 255, 0.85);
		}
	}

	&__body {
		padding: 24rpx;
	}

	&__title {
		font-size: 32rpx;
		font-weight: 600;
		line-height: 46rpx;
		letter-spacing: 1rpx;
		color: #1f1f1f;
		overflow: hidden;
		text-overflow: ellipsis;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
	}

	&__summary {
		margin-top: 12rpx;
		font-size: 26rpx;
		line-height: 38rpx;
		color: #8c8c8c;
		overflow: hidden;
		text-overflow: ellipsis;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
	}

	&__footer {
		display: flex;
		align-items: center;
		margin-top: 24rpx;
	}

	&__avatar {
		width: 44rpx;
		height: 44rpx;
		border-radius: 50%;
		flex-shrink: 0;
	}

	&__nickname {
		flex: 1;
		min-width: 0;
		margin-left: 14rpx;
		font-size: 24rpx;
		color: #595959;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	&__action {
		display: flex;
		align-items: center;
		margin-left: 36rpx;
	}

	&__action-text {
		margin-left: 8rpx;
		font-size: 24rpx;
		color: #bfbfbf;

		&.is-active {
			color: #1890ff;
		}
	}
}
</style>
