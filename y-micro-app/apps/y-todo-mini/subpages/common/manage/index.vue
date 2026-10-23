<script setup>
import { computed, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'

import { completeTask, listTasks, removeTask } from '@/api/task'
import { listTags } from '@/api/tag'
import { formatDueLabel, toTimestamp } from '@/utils/date'

	/**
	 * 通用 · 任务管理
	 *
	 * 路由：subpages/common/manage/index（分包 subpages/common，按需加载）
	 * 职责：跨清单管理所有任务——按清单与状态筛选、勾选完成、编辑、删除。
	 * 数据：清单来自 GET /tag/list；任务来自 POST /task/list（不传 tagId 即跨清单汇总）；
	 *       完成走 POST /task/complete，删除走 DELETE /task/{taskId}，编辑跳创建页的编辑态。
	 * 分页：按页累积、上拉加载下一页，排序与三个计数一律以后端返回为准。
	 * 目录：状态（tags、activeTagId、activeStatus、tasks、pageNum、hasMore、loadingTasks、togglingIds、counts）
	 *       → 方法（loadTags、loadTasks、resetTaskList、selectTag、selectStatus、toggleTask、applyTaskStatus、
	 *         openActions、editTask、deleteTask、tagNameOf、addTask）
	 *       → 样式（.segment、.chips、.chip、.task、.task-foot、.empty）
	 */

/** 任务列表每页条数 */
const PAGE_SIZE = 20

/** 标记图标 */
const FLAG_SRC = {
	important: '/static/task_important.png',
	ordinary: '/static/task_ordinary.png'
}

/** 后端 priority → 列表页小旗子 */
const FLAG_BY_PRIORITY = {
	1: 'ordinary',
	2: 'important'
}

/** 清单筛选的第一项：tagId 为 null 表示不限清单 */
const ALL_TAG = { tagId: null, tagName: '全部清单' }

/** 清单列表，第一项固定是「全部清单」 */
const tags = ref([{ ...ALL_TAG }])

/** 当前选中的清单 Id，null 表示全部 */
const activeTagId = ref(null)

/** 当前状态筛选：all / pending / done */
const activeStatus = ref('all')

/** 当前已加载的任务 */
const tasks = ref([])

/** 已加载到的页码 */
const pageNum = ref(1)

/** 是否还有下一页 */
const hasMore = ref(false)

/** 加载中标记，挡住上拉时连续触发的重复请求 */
const loadingTasks = ref(false)

/** 正在提交完成状态的任务 Id，挡住连点 */
const togglingIds = ref([])

/** 三个筛选下的条数（后端随分页返回，分页后本地算不出来） */
const counts = ref({ all: 0, pending: 0, done: 0 })

/** 请求序号：切清单或状态后，用来丢弃过期响应 */
let requestSeq = 0

/** 状态分段配置，计数取自后端 */
const statusFilters = computed(() => [
	{ key: 'all', label: '全部', count: counts.value.all },
	{ key: 'pending', label: '待完成', count: counts.value.pending },
	{ key: 'done', label: '已完成', count: counts.value.done }
])

/** 列表页脚文案：还有下一页时提示上拉 */
const footText = computed(() => {
	if (!hasMore.value) {
		return '没有更多了'
	}

	return loadingTasks.value ? '正在加载…' : '上拉加载更多'
})

/** 清单 Id → 清单名，任务行上标出所属清单用 */
function tagNameOf(tagId) {
	const tag = tags.value.find((item) => item.tagId === tagId)

	return tag ? tag.tagName : ''
}

/** 拉取清单（未登录时请求层会先拉起登录半弹框，登录成功后自动重放） */
async function loadTags() {
	const res = await listTags()

	if (res.code !== 200 || !Array.isArray(res.data)) {
		return
	}

	tags.value = [ALL_TAG, ...res.data.map((item) => ({ tagId: item.tagId, tagName: item.tagName }))]

	// 选中的清单被删掉了，回落到「全部清单」
	if (activeTagId.value != null && !tags.value.some((item) => item.tagId === activeTagId.value)) {
		activeTagId.value = null
	}
}

/** 后端任务 → 列表项 */
function toTaskItem(item) {
	return {
		id: item.taskId,
		title: item.title,
		description: item.description,
		tagId: item.tagId,
		priority: item.priority,
		urgencyLevel: item.urgencyLevel,
		// 原始字符串留着，编辑时要拆回日期与时间
		dueDate: item.dueDate,
		dueTime: toTimestamp(item.dueDate),
		repeatRule: item.repeatRule,
		remindBefore: item.remindBefore,
		done: item.completed === 1
	}
}

/** 清空列表并复位分页（切换筛选、或拉到过期数据时用） */
function resetTaskList() {
	requestSeq += 1
	tasks.value = []
	pageNum.value = 1
	hasMore.value = false
	loadingTasks.value = false
	counts.value = { all: 0, pending: 0, done: 0 }
}

/**
 * 拉取任务列表
 *
 * @param {boolean} [reset] true-从第 1 页覆盖列表；false-追加下一页（上拉加载）
 */
async function loadTasks(reset = true) {
	// 上拉时 onReachBottom 会被连续触发，挡掉重复追加
	if (!reset && loadingTasks.value) {
		return
	}

	const nextPage = reset ? 1 : pageNum.value + 1
	const seq = (requestSeq += 1)

	loadingTasks.value = true

	try {
		const res = await listTasks({
			tagId: activeTagId.value,
			status: activeStatus.value,
			pageNum: nextPage,
			pageSize: PAGE_SIZE
		})

		// 期间切了清单或状态，这一页已是旧数据，直接丢弃
		if (seq !== requestSeq || res.code !== 200 || !res.data) {
			return
		}

		const page = res.data
		const list = Array.isArray(page.list) ? page.list.map(toTaskItem) : []

		tasks.value = reset ? list : tasks.value.concat(list)
		pageNum.value = Number(page.pageNum) || nextPage
		hasMore.value = Boolean(page.hasMore)
		counts.value = {
			all: Number(page.allCount) || 0,
			pending: Number(page.pendingCount) || 0,
			done: Number(page.doneCount) || 0
		}
	} finally {
		if (seq === requestSeq) {
			loadingTasks.value = false
		}
	}
}

/** 切换清单筛选：列表从第 1 页重新拉 */
function selectTag(tagId) {
	if (tagId === activeTagId.value) {
		return
	}

	activeTagId.value = tagId
	loadTasks()
}

/** 切换「全部/待完成/已完成」：列表从第 1 页重新拉 */
function selectStatus(key) {
	if (key === activeStatus.value) {
		return
	}

	activeStatus.value = key
	loadTasks()
}

/** 勾选 / 取消勾选任务：把目标状态提交给后端，成功后再落到本地列表 */
async function toggleTask(task) {
	if (togglingIds.value.includes(task.id)) {
		return
	}

	const target = task.done ? 0 : 1

	togglingIds.value = togglingIds.value.concat(task.id)

	try {
		// 失败提示已由 request 统一 toast，这里失败就不动本地状态
		const res = await completeTask({ taskId: task.id, completed: target })

		if (res.code === 200) {
			applyTaskStatus(task, target)
		}
	} catch (error) {
		console.warn('[manage] 标记任务失败', error)
	} finally {
		togglingIds.value = togglingIds.value.filter((id) => id !== task.id)
	}
}

/**
 * 把完成状态落到本地列表与计数上
 *
 * 只翻转一条任务的状态，所以三个计数可以在本地精确维护，不必整列表重拉
 * （重拉会把已经上拉加载出来的页都丢掉）。
 * 但位置得跟着动：「全部」下后端是未完成在前、已完成在后，这条要挪到分组边界上。
 */
function applyTaskStatus(task, target) {
	const done = target === 1

	if (task.done === done) {
		return
	}

	task.done = done
	counts.value = {
		all: counts.value.all,
		pending: counts.value.pending + (done ? -1 : 1),
		done: counts.value.done + (done ? 1 : -1)
	}

	// 当前筛选下这条已经不符合条件了，直接从列表移掉
	if ((activeStatus.value === 'pending' && done) || (activeStatus.value === 'done' && !done)) {
		tasks.value = tasks.value.filter((item) => item.id !== task.id)

		// 一页都清空了但后面还有数据，补拉一页，别停在一个假的空态上
		if (!tasks.value.length && hasMore.value) {
			loadTasks()
		}

		return
	}

	const rest = tasks.value.filter((item) => item.id !== task.id)
	const boundary = rest.findIndex((item) => item.done)

	// boundary 就是第一个已完成项的位置：完成时插在它前面（成为已完成区的第一条），
	// 取消完成时插在它前面（落到未完成区的末尾），两种情况位置一致
	tasks.value =
		boundary === -1 ? [...rest, task] : [...rest.slice(0, boundary), task, ...rest.slice(boundary)]
}

/** 任务行的「⋯」：编辑 / 删除 */
function openActions(task) {
	uni.showActionSheet({
		itemList: ['编辑任务', '删除任务'],
		success: ({ tapIndex }) => {
			if (tapIndex === 0) {
				editTask(task)

				return
			}

			deleteTask(task)
		}
	})
}

/**
 * 编辑任务
 *
 * 跳创建页的编辑态，任务对象用 eventChannel 带过去，省掉一个详情接口
 * （列表返回的字段已经够填满表单）。
 */
function editTask(task) {
	uni.navigateTo({
		url: '/subpages/common/create/index?mode=edit',
		success: (res) => {
			res.eventChannel.emit('task', {
				taskId: task.id,
				title: task.title,
				description: task.description,
				tagId: task.tagId,
				tagName: tagNameOf(task.tagId),
				priority: task.priority,
				dueDate: task.dueDate,
				repeatRule: task.repeatRule,
				remindBefore: task.remindBefore
			})
		}
	})
}

/** 删除任务（二次确认，删掉就不再出现在任何列表里） */
async function deleteTask(task) {
	const confirmed = await new Promise((resolve) => {
		uni.showModal({
			title: '删除任务',
			content: `「${task.title}」会被删除，之后不再出现在任何列表里。`,
			confirmText: '删除',
			confirmColor: '#b5543f',
			success: (res) => resolve(!!res.confirm),
			fail: () => resolve(false)
		})
	})

	if (!confirmed) {
		return
	}

	try {
		const res = await removeTask(task.id)

		if (res.code !== 200) {
			return
		}

		uni.showToast({ title: '已删除', icon: 'none' })
		// 删掉之后条数、位置、计数都会变，从第 1 页重拉最省心
		await loadTasks()
	} catch (error) {
		console.warn('[manage] 删除任务失败', error)
	}
}

/** 新建任务 */
function addTask() {
	uni.navigateTo({ url: '/subpages/common/create/index' })
}

/** 上拉到底：还有下一页就追加加载 */
onReachBottom(() => {
	if (hasMore.value) {
		loadTasks(false)
	}
})

/** 回到本页时刷新清单与任务（登录后、编辑或新建回来后都能拿到最新数据） */
onShow(() => {
	loadTags().then(() => loadTasks())
})
</script>

<template>
	<sub-page title="任务管理" subtitle="所有清单的事，都收在这里。">
		<view class="segment">
			<view
				v-for="item in statusFilters"
				:key="item.key"
				class="segment__item"
				:class="{ 'segment__item--active': activeStatus === item.key }"
				@click="selectStatus(item.key)"
			>
				<text class="segment__label">{{ item.label }} {{ item.count }}</text>
			</view>
		</view>

		<scroll-view class="chips" scroll-x :show-scrollbar="false">
			<view class="chips__inner">
				<view
					v-for="item in tags"
					:key="item.tagId == null ? 'all' : item.tagId"
					class="chip"
					:class="{ 'chip--active': item.tagId === activeTagId }"
					@click="selectTag(item.tagId)"
				>
					<text class="chip__label">{{ item.tagName }}</text>
				</view>
			</view>
		</scroll-view>

		<view v-if="tasks.length" class="task-list">
			<view v-for="task in tasks" :key="task.id" class="task">
				<view
					class="task__check"
					:class="{ 'task__check--done': task.done }"
					@click="toggleTask(task)"
				>
					<text v-if="task.done" class="task__check-icon">✓</text>
				</view>

				<view class="task__main">
					<text class="task__title" :class="{ 'task__title--done': task.done }">{{ task.title }}</text>

					<view class="task__meta">
						<text v-if="tagNameOf(task.tagId)" class="task__tag">{{ tagNameOf(task.tagId) }}</text>
						<text class="task__due">{{ formatDueLabel(task.dueTime, task.done) }}</text>
					</view>
				</view>

				<image
					v-if="FLAG_BY_PRIORITY[task.priority]"
					class="task__flag"
					:src="FLAG_SRC[FLAG_BY_PRIORITY[task.priority]]"
					mode="aspectFit"
				/>

				<view class="task__more" @click="openActions(task)">
					<view class="task__dot" />
					<view class="task__dot" />
					<view class="task__dot" />
				</view>
			</view>

			<view class="task-foot">
				<text class="task-foot__text">{{ footText }}</text>
			</view>
		</view>

		<view v-else class="empty">
			<image class="empty__image" src="/static/today_task_null.png" mode="aspectFit" />
			<text class="empty__title">这里还没有任务</text>
			<text class="empty__desc">换个清单或筛选看看，也可以新建一件。</text>
			<view class="empty__button" @click="addTask">
				<text class="empty__button-plus">＋</text>
				<text class="empty__button-text">新建任务</text>
			</view>
		</view>
	</sub-page>
</template>

<style scoped lang="scss">
/* 状态分段 */
.segment {
	display: flex;
	margin-top: 32rpx;
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

/* 清单筛选 */
.chips {
	margin-top: 24rpx;
	white-space: nowrap;
}

.chips__inner {
	display: inline-flex;
	align-items: center;
}

.chip {
	display: flex;
	align-items: center;
	height: 60rpx;
	margin-right: 16rpx;
	padding: 0 28rpx;
	box-sizing: border-box;
	border-radius: 999rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;
}

.chip--active {
	border-color: $y-green-border;
	background-color: $y-green-soft;
}

.chip__label {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.chip--active .chip__label {
	color: $y-text-main;
}

/* 任务行 */
.task {
	display: flex;
	align-items: flex-start;
	padding: 32rpx 0;
	border-bottom: 2rpx solid $y-divider;
}

.task__check {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 40rpx;
	height: 40rpx;
	margin-top: 4rpx;
	margin-right: 24rpx;
	box-sizing: border-box;
	border-radius: 50%;
	border: 3rpx solid $y-check-border;
}

.task__check--done {
	border-color: $y-green-done;
	background-color: $y-green-done;
}

.task__check-icon {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 1;
	color: #ffffff;
}

.task__main {
	flex: 1;
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.task__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.task__title--done {
	color: $y-text-muted;
}

.task__meta {
	display: flex;
	align-items: center;
	margin-top: 14rpx;
}

.task__tag {
	margin-right: 16rpx;
	padding: 4rpx 16rpx;
	border-radius: 999rpx;
	background-color: $y-sunken-bg;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.task__due {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.task__flag {
	flex-shrink: 0;
	width: 36rpx;
	height: 36rpx;
	margin-top: 4rpx;
	margin-left: 20rpx;
}

.task__more {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	margin-top: 8rpx;
	padding: 8rpx 0 8rpx 24rpx;
}

.task__dot {
	width: 8rpx;
	height: 8rpx;
	border-radius: 50%;
	background-color: $y-text-muted;

	& + & {
		margin-left: 6rpx;
	}
}

/* 分页页脚 */
.task-foot {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 32rpx 0 8rpx;
}

.task-foot__text {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 空状态 */
.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 60rpx 0 40rpx;
}

.empty__image {
	width: 240rpx;
	height: 240rpx;
}

.empty__title {
	margin-top: 12rpx;
	font-size: calc(34rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.empty__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	margin-top: 44rpx;
	padding: 0 44rpx;
	border-radius: 999rpx;
	background-color: $y-green-primary;
}

.empty__button-plus {
	margin-right: 10rpx;
	font-size: calc(34rpx * var(--y-font-scale, 1));
	line-height: 1;
	color: #ffffff;
}

.empty__button-text {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: #ffffff;
}
</style>
