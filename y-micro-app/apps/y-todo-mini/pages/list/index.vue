<script setup>
import { computed, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'

import { completeTask, listTasks } from '@/api/task'
import { listTags } from '@/api/tag'
import { isLoggedIn } from '@/utils/session'
import { formatDueLabel, toTimestamp } from '@/utils/date'
import { themeClass } from '@/utils/appearance'

	/**
	 * 清单 · 清单首页
	 *
	 * 路由：pages/list/index（主包）
	 * 职责：清单 tab 主页，给事情一个归处——清单分类卡、全部/待完成/已完成筛选与任务列表及空态。
	 * 数据：分类卡来自 GET /tag/list，任务列表来自 POST /task/list（未登录时均为空，展示空态，不弹登录）。
	 * 分页：任务列表按页累积，上拉加载下一页；排序由后端负责（未完成在前、快到期在前），三个筛选的计数改用后端返回的 allCount/pendingCount/doneCount。
	 * 完成：点勾选调 POST /task/complete 提交目标状态，成功后再本地改状态 + 挪位置 + 维护计数（不整列表重拉，避免丢掉已加载的页）。
	 * 目录：状态（categories、activeCategoryId、activeFilter、tasks、pageNum、hasMore、loadingTasks、togglingIds、counts）→ 方法（loadTags、loadTasks、selectCategory、selectFilter、countOf、toggleTask、applyTaskStatus、addTask、handleMore、openSearch）→ 样式（.list、.search-entry、.categories、.category、.segment、.task）
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

/** 清单分类，未登录时的占位（与后端首次登录种下的 4 个默认清单一致） */
const DEFAULT_CATEGORIES = [
	{ tagId: null, name: '工作', icon: 'briefcase' },
	{ tagId: null, name: '生活', icon: 'coffee' },
	{ tagId: null, name: '阅读', icon: 'book' },
	{ tagId: null, name: '个人成长', icon: 'sprout' }
]

/** 清单分类（清单名在同一用户下唯一，故用名称作为选中标识） */
const categories = ref(DEFAULT_CATEGORIES.map((item) => ({ ...item })))

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 当前选中的清单 */
const activeCategoryId = ref(DEFAULT_CATEGORIES[0].name)

/** 当前任务筛选：all / pending / done */
const activeFilter = ref('all')

/** 当前清单下已加载的待办（来自 POST /task/list，按页累积） */
const tasks = ref([])

/** 已加载到的页码 */
const pageNum = ref(1)

/** 是否还有下一页 */
const hasMore = ref(false)

/** 加载中标记，挡住上拉时连续触发的重复请求 */
const loadingTasks = ref(false)

/** 正在提交完成状态的任务 Id，挡住连点 */
const togglingIds = ref([])

/** 该清单在「全部/待完成/已完成」下的条数（后端随分页返回，分页后本地算不出来） */
const counts = ref({ all: 0, pending: 0, done: 0 })

/** 请求序号：切换清单或筛选后，用来丢弃过期响应 */
let requestSeq = 0

/** 当前清单信息 */
const activeCategory = computed(
	() => categories.value.find((item) => item.name === activeCategoryId.value) || categories.value[0]
)

/** 筛选分段配置，计数取自后端 */
const filters = computed(() => [
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

/** 某个清单下的待完成条数（后端 GET /tag/list 已经算好） */
function countOf(category) {
	return category.pendingCount || 0
}

/** 拉取清单列表：未登录时保留默认清单占位，不打扰用户登录 */
async function loadTags() {
	if (!isLoggedIn()) {
		return
	}

	const res = await listTags()

	if (res.code !== 200 || !Array.isArray(res.data) || !res.data.length) {
		return
	}

	categories.value = res.data.map((item) => ({
		tagId: item.tagId,
		name: item.tagName,
		icon: item.icon || 'folder',
		pendingCount: item.pendingCount || 0
	}))

	// 选中的清单被删掉时，回落到第一个清单
	if (!categories.value.some((item) => item.name === activeCategoryId.value)) {
		activeCategoryId.value = categories.value[0].name
	}
}

/** 后端任务 → 列表项 */
function toTaskItem(item) {
	return {
		id: item.taskId,
		title: item.title,
		// 后端返回 yyyy-MM-dd HH:mm:ss，统一交给 utils/date 解析（iOS 侧只认 yyyy/MM/dd HH:mm:ss）
		dueTime: toTimestamp(item.dueDate),
		flag: FLAG_BY_PRIORITY[item.priority] || '',
		done: item.completed === 1
	}
}

/** 清空列表并复位分页（未登录、清单还是占位、或切换清单/筛选时用） */
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
 *
 * 未登录或清单还没落到后端（占位）时清空列表展示空态，不弹登录。
 * 排序由后端负责（未完成在前、快到期在前），这里不再本地排序，分页下本地排序也没有意义。
 */
async function loadTasks(reset = true) {
	const category = activeCategory.value

	if (!isLoggedIn() || !category || category.tagId == null) {
		resetTaskList()

		return
	}

	// 上拉时 onReachBottom 会被连续触发，挡掉重复追加
	if (!reset && loadingTasks.value) {
		return
	}

	const nextPage = reset ? 1 : pageNum.value + 1
	const seq = (requestSeq += 1)

	loadingTasks.value = true

	try {
		const res = await listTasks({
			tagId: category.tagId,
			status: activeFilter.value,
			pageNum: nextPage,
			pageSize: PAGE_SIZE
		})

		// 期间切了清单或筛选，这一页已是旧数据，直接丢弃
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

/** 切换清单：筛选复位到「全部」，列表从第 1 页重新拉 */
function selectCategory(categoryName) {
	activeCategoryId.value = categoryName
	activeFilter.value = 'all'
	loadTasks()
}

/** 切换「全部/待完成/已完成」：列表从第 1 页重新拉 */
function selectFilter(key) {
	if (key === activeFilter.value) {
		return
	}

	activeFilter.value = key
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
		console.warn('[list] 标记任务失败', error)
	} finally {
		togglingIds.value = togglingIds.value.filter((id) => id !== task.id)
	}
}

/** 打开任务详情：点行进详情，点左侧圆圈才是勾选完成 */
function enterDetail(task) {
	uni.navigateTo({ url: `/subpages/common/detail/index?taskId=${task.id}` })
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
	if ((activeFilter.value === 'pending' && done) || (activeFilter.value === 'done' && !done)) {
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

/** 新建待办 */
function addTask() {
	uni.navigateTo({ url: '/subpages/common/create/index' })
}

/** 顶部「更多」进任务管理页（跨清单汇总、筛选、编辑、删除） */
function handleMore() {
	uni.navigateTo({ url: '/subpages/common/manage/index' })
}

/** 搜索所有清单里的任务与备注 */
function openSearch() {
	uni.navigateTo({ url: '/subpages/common/search/index' })
}

/** 上拉到底：还有下一页就追加加载（不够一屏时不会触发，先看首屏结果即可） */
onReachBottom(() => {
	if (hasMore.value) {
		loadTasks(false)
	}
})

/** 隐藏原生 tabBar，改用自定义导航栏；回到本页时刷新清单与任务（登录后 / 新建任务后都能拿到最新数据） */
onShow(() => {
	uni.hideTabBar()
	loadTags().then(() => loadTasks())
})
</script>

<template>
	<view class="list" :class="themeClass">
		<view class="list__top">
			<view :style="{ height: statusBarHeight + 'px' }" />

			<brand-header @more="handleMore" />
		</view>

		<view class="list__hero">
			<view class="list__hero-text">
				<text class="list__title">给事情一个归处</text>
				<text class="list__subtitle">{{ categories.length }} 个清单，让日常井井有条</text>
			</view>
			<view class="list__add" @click="addTask">＋</view>
		</view>

		<view class="search-entry" @click="openSearch">
			<y-icon name="search" size="30rpx" color="#8e8e85" />
			<text class="search-entry__text">搜索任务、备注</text>
		</view>

		<view class="categories">
			<view
				v-for="item in categories"
				:key="item.name"
				class="category"
				:class="{ 'category--active': item.name === activeCategoryId }"
				@click="selectCategory(item.name)"
			>
				<y-icon :name="item.icon" size="48rpx" />

				<view class="category__text">
					<text class="category__name">{{ item.name }}</text>
					<text class="category__count">{{ countOf(item) }} 件待办</text>
				</view>
			</view>
		</view>

		<view class="section">
			<view class="section__head">
				<view class="section__title-wrap">
					<text class="section__title">{{ activeCategory.name }}</text>
					<text class="section__progress">已完成 {{ counts.done }} / {{ counts.all }}</text>
				</view>

				<view class="section__more" @click="handleMore">
					<view class="section__dot" />
					<view class="section__dot" />
					<view class="section__dot" />
				</view>
			</view>

			<view class="segment">
				<view
					v-for="item in filters"
					:key="item.key"
					class="segment__item"
					:class="{ 'segment__item--active': activeFilter === item.key }"
					@click="selectFilter(item.key)"
				>
					<text class="segment__label">{{ item.label }} {{ item.count }}</text>
				</view>
			</view>

			<view v-if="tasks.length" class="task-list">
				<view
					v-for="task in tasks"
					:key="task.id"
					class="task"
					@click="enterDetail(task)"
				>
					<view class="task__check" :class="{ 'task__check--done': task.done }" @click.stop="toggleTask(task)">
						<text v-if="task.done" class="task__check-icon">✓</text>
					</view>

					<view class="task__main">
						<text class="task__title" :class="{ 'task__title--done': task.done }">{{ task.title }}</text>
						<text class="task__due">{{ formatDueLabel(task.dueTime, task.done) }}</text>
					</view>

					<image
						v-if="task.flag"
						class="task__flag"
						:src="FLAG_SRC[task.flag]"
						mode="aspectFit"
					/>
				</view>

				<view class="task-foot">
					<text class="task-foot__text">{{ footText }}</text>
				</view>
			</view>

			<view v-else class="empty">
				<image class="empty__image" src="/static/today_task_null.png" mode="aspectFit" />
				<text class="empty__title">这个清单，等你写下第一件事</text>
				<text class="empty__desc">把{{ activeCategory.name }}里的大小事放在这里，慢慢理清。</text>
				<view class="empty__button" @click="addTask">
					<text class="empty__button-plus">＋</text>
					<text class="empty__button-text">添加{{ activeCategory.name }}待办</text>
				</view>
			</view>
		</view>

		<custom-tabbar active="pages/list/index" />

		<login-sheet />
		<privacy-sheet />
	</view>
</template>

<style scoped lang="scss">
.list {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 180rpx;
	box-sizing: border-box;
}

/* 顶栏吸顶：状态栏留白与品牌顶栏一起钉在页面顶部，背景铺页面底色避免内容透出 */
.list__top {
	position: sticky;
	top: 0;
	z-index: 100;
	background-color: $y-page-bg;
}

/* 标题 */
.list__hero {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	margin-top: 24rpx;
}

.list__hero-text {
	display: flex;
	flex-direction: column;
}

.list__title {
	font-size: calc(52rpx * var(--y-font-scale, 1));
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.list__subtitle {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.list__add {
	width: 60rpx;
	margin-top: 12rpx;
	font-size: calc(52rpx * var(--y-font-scale, 1));
	font-weight: 300;
	line-height: 1;
	text-align: center;
	color: $y-green-primary;
}

/* 搜索入口 */
.search-entry {
	display: flex;
	align-items: center;
	height: 84rpx;
	margin-top: 28rpx;
	padding: 0 24rpx;
	box-sizing: border-box;
	border-radius: 20rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;
}

.search-entry__text {
	margin-left: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: #a9a9a0;
}

/* 清单卡片 */
.categories {
	display: flex;
	flex-wrap: wrap;
	justify-content: space-between;
	margin-top: 32rpx;
}

.category {
	display: flex;
	align-items: center;
	width: 320rpx;
	height: 150rpx;
	margin-bottom: 24rpx;
	padding: 0 24rpx;
	box-sizing: border-box;
	border-radius: 24rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;
}

.category--active {
	border-color: $y-green-border;
	background-color: $y-green-soft;
}

.category--manage {
	border-style: dashed;
	border-color: $y-green-border;
	background-color: $y-green-soft;
}

.category__text {
	display: flex;
	flex-direction: column;
	margin-left: 20rpx;
}

.category__name {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-text-main;
}

.category__count {
	margin-top: 10rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 分组 */
.section {
	margin-top: 32rpx;
}

.section__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.section__title-wrap {
	display: flex;
	align-items: baseline;
}

.section__title {
	font-size: calc(36rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.section__progress {
	margin-left: 16rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.section__more {
	display: flex;
	align-items: center;
	padding: 12rpx 0;
}

.section__dot {
	width: 8rpx;
	height: 8rpx;
	border-radius: 50%;
	background-color: $y-text-muted;

	& + & {
		margin-left: 6rpx;
	}
}

/* 筛选分段 */
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

.task__due {
	margin-top: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.task__flag {
	flex-shrink: 0;
	width: 36rpx;
	height: 36rpx;
	margin-left: 20rpx;
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
