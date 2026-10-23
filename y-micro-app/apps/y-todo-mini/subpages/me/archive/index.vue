<script setup>
import { computed, reactive, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'

import { archiveTask, clearDoneTasks, listTasks, removeTask } from '@/api/task'
import { formatHistoryLabel } from '@/utils/date'

	/**
	 * 我的 · 已完成与归档
	 *
	 * 路由：subpages/me/archive/index（分包 subpages/me，按需加载）
	 * 职责：已完成与归档页，做过的事都留在这里——已完成/已归档分组、搜索、归档与删除。
	 * 数据：两组都走 POST /task/list——「已完成」传 status=done（后端只查未归档），
	 *       「已归档」传 archived=1；归档与恢复走 POST /task/archive，清空走 POST /task/clear-done。
	 * 目录：状态（tab、keyword、lists、requestSeq）→ 方法（loadTab、refresh、toggleArchive、remove、clearDone）
	 *       → 样式（.search、.segment、.record、.empty、.clear）
	 */

/** 每页条数 */
const PAGE_SIZE = 20

/** 分组配置：key 同时是 lists 的键 */
const TAB_ITEMS = [
	{ key: 'done', label: '已完成' },
	{ key: 'archived', label: '已归档' }
]

/** 当前分组：done / archived */
const tab = ref('done')

/** 搜索关键词 */
const keyword = ref('')

/**
 * 两组各自的分页状态
 *
 * 两个分组的条数要同时显示在分段标签上，所以进来就把两组第一页都拉一次；
 * 之后切换分组直接用缓存，只有上拉加载和搜索才会再发请求。
 */
const lists = reactive({
	done: { items: [], total: 0, pageNum: 1, hasMore: false, loading: false },
	archived: { items: [], total: 0, pageNum: 1, hasMore: false, loading: false }
})

/** 每组的请求序号：连续改关键词时用来丢弃先发后到的过期响应 */
const requestSeq = { done: 0, archived: 0 }

/** 当前分组的数据 */
const current = computed(() => lists[tab.value])

/** 去掉首尾空格的关键词，空串当没搜 */
const searchWord = computed(() => keyword.value.trim())

/** 列表页脚文案 */
const footText = computed(() => {
	if (!current.value.hasMore) {
		return '没有更多了'
	}

	return current.value.loading ? '正在加载…' : '上拉加载更多'
})

/** 空状态文案：搜过就报关键词，没搜过按分组给引导 */
const emptyText = computed(() => {
	if (searchWord.value) {
		return `没有找到「${searchWord.value}」`
	}

	return tab.value === 'archived' ? '还没有归档的任务' : '还没有完成的任务'
})

/** 后端任务 → 记录行 */
function toRecord(item, key) {
	const archived = key === 'archived'
	// 已归档的看归档时间，刚完成的看完成时间
	const value = archived ? item.archiveTime || item.completionTime : item.completionTime
	const stamp = formatHistoryLabel(value)

	return {
		id: item.taskId,
		title: item.title,
		archived,
		time: stamp ? `${stamp} ${archived ? '归档' : '完成'}` : (archived ? '已归档' : '已完成')
	}
}

/**
 * 拉取某一组的数据
 *
 * @param {string} key  done / archived
 * @param {boolean} [reset] true-从第 1 页覆盖；false-追加下一页（上拉加载）
 */
async function loadTab(key, reset = true) {
	const target = lists[key]

	if (!reset && target.loading) {
		return
	}

	const nextPage = reset ? 1 : target.pageNum + 1
	const seq = (requestSeq[key] += 1)

	target.loading = true

	try {
		// 失败提示由 request 统一 toast；未登录时它也会先拉起登录再重放这次请求
		const res = await listTasks({
			status: key === 'done' ? 'done' : undefined,
			archived: key === 'archived' ? 1 : undefined,
			keyword: searchWord.value || undefined,
			pageNum: nextPage,
			pageSize: PAGE_SIZE
		})

		// 期间又改了关键词，这一页已是旧数据，直接丢弃
		if (seq !== requestSeq[key] || res.code !== 200 || !res.data) {
			return
		}

		const page = res.data
		const items = Array.isArray(page.list) ? page.list.map((item) => toRecord(item, key)) : []

		target.items = reset ? items : target.items.concat(items)
		target.total = Number(page.total) || 0
		target.pageNum = Number(page.pageNum) || nextPage
		target.hasMore = Boolean(page.hasMore)
	} finally {
		if (seq === requestSeq[key]) {
			target.loading = false
		}
	}
}

/** 重拉两组的第一页（进入页面、改关键词时用） */
function refresh() {
	loadTab('done')
	loadTab('archived')
}

/** 从某一组里本地移除一条并更新条数 */
function dropFromTab(key, taskId) {
	const target = lists[key]

	target.items = target.items.filter((record) => record.id !== taskId)

	if (target.total > 0) {
		target.total -= 1
	}
}

/** 归档 / 恢复到已完成 */
async function toggleArchive(item) {
	const from = tab.value
	const to = from === 'archived' ? 'done' : 'archived'
	const target = item.archived ? 0 : 1

	const res = await archiveTask({ taskId: item.id, archived: target })

	if (res.code !== 200) {
		return
	}

	uni.showToast({ title: target === 1 ? '已归档' : '已恢复到已完成', icon: 'none' })

	// 当前分组先本地挪走，另一组重拉第一页：两边条数与列表都对得上，且不动用户正在看的这页
	dropFromTab(from, item.id)
	loadTab(to)
}

/** 打开任务详情：点行进详情，右侧的归档、删除按钮各自独立响应 */
function enterDetail(item) {
	uni.navigateTo({ url: `/subpages/common/detail/index?taskId=${item.id}` })
}

/** 删除单条 */
function remove(item) {
	uni.showModal({
		title: '删除记录',
		content: `删除后「${item.title}」将不再出现在任何列表里。`,
		confirmColor: '#4a6b3e',
		success: async (res) => {
			if (!res.confirm) {
				return
			}

			const result = await removeTask(item.id)

			if (result.code !== 200) {
				return
			}

			dropFromTab(tab.value, item.id)
			uni.showToast({ title: '已删除', icon: 'none' })
		}
	})
}

/** 清空已完成（已归档的不受影响） */
function clearDone() {
	if (!lists.done.total) {
		return
	}

	uni.showModal({
		title: '清空已完成',
		content: '清空后这些记录将不再显示，已归档的不受影响。',
		confirmColor: '#4a6b3e',
		success: async (res) => {
			if (!res.confirm) {
				return
			}

			const result = await clearDoneTasks()

			if (result.code !== 200) {
				return
			}

			uni.showToast({ title: '已清空', icon: 'none' })
			loadTab('done')
		}
	})
}

/** 清空关键词并重新查两组 */
function clearKeyword() {
	keyword.value = ''
	refresh()
}

/** 上拉到底：当前分组还有下一页就追加加载 */
onReachBottom(() => {
	if (current.value.hasMore) {
		loadTab(tab.value, false)
	}
})

/** 每次展示都重拉一次，别的地方刚完成的任务也能及时出现在这里 */
onShow(refresh)
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
					confirm-type="search"
					placeholder="搜索已完成的任务"
					placeholder-style="color:#a9a9a0;font-size:26rpx"
					@confirm="refresh"
				/>
				<view v-if="keyword" class="search__clear" @click="clearKeyword">
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
					{{ item.label }} {{ lists[item.key].total }}
				</text>
			</view>
		</view>

		<view v-if="current.items.length" class="list">
			<view v-for="item in current.items" :key="item.id" class="record" @click="enterDetail(item)">
				<view class="record__main">
					<text class="record__title">{{ item.title }}</text>
					<text class="record__time">{{ item.time }}</text>
				</view>

				<view class="record__action" @click.stop="toggleArchive(item)">
					<y-icon :name="item.archived ? 'rotate' : 'archive'" size="34rpx" color="#8e8e85" />
				</view>

				<view class="record__action" @click.stop="remove(item)">
					<y-icon name="trash" size="34rpx" color="#c2c2b9" />
				</view>
			</view>

			<view class="list-foot">
				<text class="list-foot__text">{{ footText }}</text>
			</view>
		</view>

		<view v-else class="empty">
			<y-icon :name="tab === 'archived' ? 'archive' : 'check'" size="72rpx" color="#c4d2b4" />
			<text class="empty__title">{{ emptyText }}</text>
			<text class="empty__desc">完成一件，这里就会多一行。</text>
		</view>

		<view v-if="tab === 'done' && lists.done.total" class="clear" @click="clearDone">
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
	border: 2rpx solid $y-divider;
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
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
	background-color: $y-sunken-bg;
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.record__time {
	margin-top: 12rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
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

/* 分页页脚 */
.list-foot {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 32rpx 0 8rpx;
}

.list-foot__text {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
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
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}
</style>
