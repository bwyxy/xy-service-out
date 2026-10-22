<script setup>
import { computed, ref } from 'vue'

	/**
	 * 我的 · 已完成与归档
	 *
	 * 路由：subpages/me/archive/index（分包 subpages/me，按需加载）
	 * 职责：已完成与归档页，做过的事都留在这里——已完成/已归档分组、搜索、归档与删除。
	 * 目录：状态（records、tab、keyword、list）→ 方法（toggleArchive、remove、clearDone）→ 样式（.search、.segment、.record、.empty）
	 */

/** 记录（暂用本地数据，后续接入接口） */
const records = ref([
	{ id: 1, title: '发送会议纪要', time: '昨天 17:40 完成', archived: false },
	{ id: 2, title: '晨间拉伸 15 分钟', time: '今天 08:15 完成', archived: false },
	{ id: 3, title: '预约周末体检', time: '10月20日 10:00 完成', archived: false },
	{ id: 4, title: '去超市买晚餐食材', time: '10月19日 18:30 完成', archived: false },
	{ id: 5, title: '整理旧的项目文档', time: '9月12日 归档', archived: true },
	{ id: 6, title: '第一版原型评审', time: '8月28日 归档', archived: true }
])

/** 当前分组：done / archived */
const tab = ref('done')

/** 搜索关键词 */
const keyword = ref('')

/** 分组配置 */
const TAB_ITEMS = [
	{ key: 'done', label: '已完成' },
	{ key: 'archived', label: '已归档' }
]

/** 当前分组下的记录 */
const list = computed(() => {
	const archived = tab.value === 'archived'
	const word = keyword.value.trim()

	return records.value.filter((item) => {
		if (item.archived !== archived) {
			return false
		}

		return !word || item.title.includes(word)
	})
})

/** 各分组条数 */
function countOf(archived) {
	return records.value.filter((item) => item.archived === archived).length
}

/** 归档 / 恢复 */
function toggleArchive(item) {
	item.archived = !item.archived
	uni.showToast({ title: item.archived ? '已归档' : '已恢复到已完成', icon: 'none' })
}

/** 删除单条 */
function remove(item) {
	records.value = records.value.filter((record) => record.id !== item.id)
	uni.showToast({ title: '已删除', icon: 'none' })
}

/** 清空已完成 */
function clearDone() {
	if (!countOf(false)) {
		return
	}

	uni.showModal({
		title: '清空已完成',
		content: '清空后这些记录将不再显示。',
		confirmColor: '#4a6b3e',
		success: (res) => {
			if (!res.confirm) {
				return
			}

			records.value = records.value.filter((item) => item.archived)
			uni.showToast({ title: '已清空', icon: 'none' })
		}
	})
}
</script>

<template>
	<sub-page title="已完成与归档" subtitle="做过的事，都留在这里。">
		<view class="search">
			<view class="search__field">
				<view class="search__icon">
					<y-icon name="search" size="30rpx" color="#8e8e85" />
				</view>
				<input
					v-model="keyword"
					class="search__input"
					type="text"
					placeholder="搜索已完成的任务"
					placeholder-style="color:#a9a9a0;font-size:26rpx"
				/>
				<view v-if="keyword" class="search__clear" @click="keyword = ''">
					<y-icon name="close-circle" size="34rpx" color="#ccd1c4" />
				</view>
			</view>
		</view>

		<view class="segment">
			<view
				v-for="item in TAB_ITEMS"
				:key="item.key"
				class="segment__item"
				:class="{ 'segment__item--active': tab === item.key }"
				@click="tab = item.key"
			>
				<text class="segment__label">
					{{ item.label }} {{ countOf(item.key === 'archived') }}
				</text>
			</view>
		</view>

		<view v-if="list.length" class="list">
			<view v-for="item in list" :key="item.id" class="record">
				<view class="record__main">
					<text class="record__title">{{ item.title }}</text>
					<text class="record__time">{{ item.time }}</text>
				</view>

				<view class="record__action" @click="toggleArchive(item)">
					<y-icon :name="item.archived ? 'rotate' : 'archive'" size="34rpx" color="#8e8e85" />
				</view>

				<view class="record__action" @click="remove(item)">
					<y-icon name="trash" size="34rpx" color="#c2c2b9" />
				</view>
			</view>
		</view>

		<view v-else class="empty">
			<y-icon :name="tab === 'archived' ? 'archive' : 'check'" size="72rpx" color="#c4d2b4" />
			<text class="empty__title">
				{{ tab === 'archived' ? '还没有归档的任务' : '还没有完成的任务' }}
			</text>
			<text class="empty__desc">完成一件，这里就会多一行。</text>
		</view>

		<view v-if="tab === 'done' && countOf(false)" class="clear" @click="clearDone">
			<y-icon name="trash" size="30rpx" color="#8e8e85" />
			<text class="clear__text">清空已完成</text>
		</view>
	</sub-page>
</template>

<style scoped lang="scss">
/* 搜索框 */
.search {
	margin-top: 32rpx;
}

.search__field {
	display: flex;
	align-items: center;
	height: 84rpx;
	padding: 0 24rpx;
	box-sizing: border-box;
	border-radius: 20rpx;
	border: 2rpx solid #e6e6de;
	background-color: $y-card-bg;
}

.search__icon {
	display: flex;
	align-items: center;
	flex-shrink: 0;
	margin-right: 16rpx;
}

.search__input {
	flex: 1;
	height: 84rpx;
	font-size: 28rpx;
	color: $y-text-main;
}

.search__clear {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	margin-left: 12rpx;
}

/* 分组 */
.segment {
	display: flex;
	margin-top: 24rpx;
	padding: 6rpx;
	border-radius: 18rpx;
	background-color: #edede6;
}

.segment__item {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 60rpx;
	border-radius: 14rpx;
}

.segment__item--active {
	background-color: $y-card-bg;
}

.segment__label {
	font-size: 24rpx;
	color: $y-text-sub;
}

.segment__item--active .segment__label {
	font-weight: 500;
	color: $y-text-main;
}

/* 记录 */
.list {
	margin-top: 8rpx;
}

.record {
	display: flex;
	align-items: center;
	padding: 30rpx 0;
	border-bottom: 2rpx solid $y-divider;
}

.record__main {
	flex: 1;
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.record__title {
	font-size: 30rpx;
	color: $y-text-main;
}

.record__time {
	margin-top: 12rpx;
	font-size: 22rpx;
	color: $y-text-muted;
}

.record__action {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 64rpx;
	height: 64rpx;
	margin-left: 8rpx;
}

/* 空状态 */
.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 100rpx 0 40rpx;
}

.empty__title {
	margin-top: 28rpx;
	font-size: 32rpx;
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 14rpx;
	font-size: 24rpx;
	color: $y-text-sub;
}

/* 清空 */
.clear {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	margin-top: 48rpx;
	border-radius: 999rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;
}

.clear__text {
	margin-left: 10rpx;
	font-size: 28rpx;
	color: $y-text-sub;
}
</style>
