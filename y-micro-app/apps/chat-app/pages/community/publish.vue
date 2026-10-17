<script setup>
import { ref } from 'vue'
import { createPost } from '@/api/forum'
import { uploadImage } from '@/api/upload'

/**
 * 发布帖子
 *
 * 字段与网页端 creationCenter/publish 保持一致：标题 / 正文 / 封面 / 可见性。
 * 后端 CreatePostDTO 未开放版块选择（boardId 已移除），因此不提供版块选择。
 */
const title = ref('')
const content = ref('')
const coverImage = ref('')
const visibility = ref(1)

const uploading = ref(false)
const submitting = ref(false)

const visibilitys = [
	{ id: 1, name: '公开' },
	{ id: 2, name: '好友可见' },
	{ id: 3, name: '私密' },
]

function toast(text) {
	uni.showToast({ title: text, icon: 'none' })
}

function handleBack() {
	uni.navigateBack()
}

/** 选择并上传封面 */
function handleChooseCover() {
	if (uploading.value) {
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

			uploading.value = true

			try {
				const response = await uploadImage(filePath)

				if (response.code === 200) {
					coverImage.value = response.data
				}
			} finally {
				uploading.value = false
			}
		},
	})
}

async function handleSubmit() {
	if (!title.value.trim()) {
		toast('请输入标题')
		return
	}

	if (title.value.trim().length < 5) {
		toast('标题至少 5 个字符')
		return
	}

	if (!content.value.trim()) {
		toast('请输入正文')
		return
	}

	if (submitting.value) {
		return
	}

	submitting.value = true

	try {
		const response = await createPost({
			title: title.value.trim(),
			content: content.value,
			coverImage: coverImage.value,
			visibility: visibility.value,
		})

		if (response.code === 200) {
			toast('发布成功')
			// 让社区列表 onShow 时刷新出这条新帖
			setTimeout(() => uni.navigateBack(), 600)
		}
	} finally {
		submitting.value = false
	}
}
</script>

<template>
	<view class="publish">
		<wd-navbar title="发布" left-arrow @click-left="handleBack">
			<template #right>
				<view class="publish__submit" @click="handleSubmit">
					{{ submitting ? '发布中' : '发布' }}
				</view>
			</template>
		</wd-navbar>

		<scroll-view class="publish__scroll" scroll-y>
			<view class="publish__card">
				<wd-input
					v-model="title"
					no-border
					placeholder="请输入标题"
					:maxlength="100"
					show-word-limit
				/>
				<view class="publish__divider"></view>
				<wd-textarea
					v-model="content"
					class="publish__content"
					no-border
					placeholder="分享你的想法..."
					:maxlength="50000"
					show-word-limit
				/>
			</view>

			<view class="publish__card">
				<text class="publish__label">封面</text>

				<view v-if="coverImage" class="publish__cover">
					<image class="publish__cover-img" :src="coverImage" mode="aspectFill" />
					<view class="publish__cover-remove" @click="coverImage = ''">×</view>
				</view>
				<view v-else class="publish__cover-add" @click="handleChooseCover">
					<wd-icon name="add" size="48rpx" color="#bfbfbf" />
					<text>{{ uploading ? '上传中...' : '上传封面' }}</text>
				</view>
			</view>

			<view class="publish__card">
				<text class="publish__label">谁可以看</text>

				<view class="publish__visibility">
					<view
						v-for="item in visibilitys"
						:key="item.id"
						class="visibility-item"
						:class="{ 'is-active': visibility === item.id }"
						@click="visibility = item.id"
					>
						{{ item.name }}
					</view>
				</view>
			</view>

			<view style="height: 60rpx;"></view>
		</scroll-view>
	</view>
</template>

<style scoped lang="scss">
.publish {
	width: 100vw;
	height: 100vh;
	display: flex;
	flex-direction: column;
	overflow: hidden;

	&__submit {
		font-size: 28rpx;
		font-weight: 600;
		color: #1890ff;
	}

	&__scroll {
		flex: 1;
		overflow: hidden;
	}

	&__card {
		margin: 24rpx;
		padding: 8rpx 32rpx;
		background: #ffffff;
		border-radius: 20rpx;
	}

	&__divider {
		height: 1rpx;
		background: rgba(0, 0, 0, 0.06);
	}

	&__content {
		padding: 0;
	}

	&__label {
		display: block;
		padding: 24rpx 0;
		font-size: 28rpx;
		font-weight: 600;
		color: #1f1f1f;
	}

	&__cover {
		position: relative;
		width: 100%;
		height: 320rpx;
		margin-bottom: 24rpx;
	}

	&__cover-img {
		width: 100%;
		height: 100%;
		border-radius: 16rpx;
	}

	&__cover-remove {
		position: absolute;
		top: 16rpx;
		right: 16rpx;
		width: 48rpx;
		height: 48rpx;
		line-height: 44rpx;
		background: rgba(0, 0, 0, 0.5);
		border-radius: 50%;
		font-size: 32rpx;
		color: #ffffff;
		text-align: center;
	}

	&__cover-add {
		width: 100%;
		height: 240rpx;
		margin-bottom: 24rpx;
		background: #fafafa;
		border: 2rpx dashed #e5e5e5;
		border-radius: 16rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;

		text {
			margin-top: 12rpx;
			font-size: 24rpx;
			color: #bfbfbf;
		}
	}

	&__visibility {
		display: flex;
		padding-bottom: 32rpx;
	}

	:deep(.wd-textarea__inner) {
		height: 420rpx;
	}
}

.visibility-item {
	height: 60rpx;
	line-height: 60rpx;
	padding: 0 32rpx;
	margin-right: 16rpx;
	background: #f5f5f5;
	border-radius: 30rpx;
	font-size: 26rpx;
	color: #595959;

	&.is-active {
		background: #e6f4ff;
		color: #1890ff;
		font-weight: 600;
	}
}
</style>
