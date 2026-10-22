<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

	/**
	 * 清单 · 清单首页
	 *
	 * 路由：pages/list/index（主包）
	 * 职责：清单 tab 主页，给事情一个归处——4 个清单分类卡、全部/待完成/已完成筛选与任务列表及空态。
	 * 目录：状态（activeCategoryId、activeFilter、tasks、visibleTasks）→ 方法（selectCategory、toggleTask、formatDue、addTask）→ 样式（.list、.categories、.category、.segment、.task）
	 */

/** 周标签，索引 0 为周一 */
const WEEK_CHARS = ['一', '二', '三', '四', '五', '六', '日']

/** 标记图标 */
const FLAG_SRC = {
	important: '/static/task_important.png',
	ordinary: '/static/task_ordinary.png'
}

/** 清单分类 */
const categories = [
	{ id: 'work', name: '工作', icon: 'briefcase' },
	{ id: 'life', name: '生活', icon: 'coffee' },
	{ id: 'read', name: '阅读', icon: 'book' },
	{ id: 'growth', name: '个人成长', icon: 'sprout' }
]

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 当前选中的清单 */
const activeCategoryId = ref('work')

/** 当前任务筛选：all / pending / done */
const activeFilter = ref('all')

/** 取相对今天 offset 天的某一时刻，便于 mock 数据始终呈现「今天/明天/昨天」 */
function atDay(offset, hour = 0, minute = 0) {
	const now = new Date()

	return new Date(now.getFullYear(), now.getMonth(), now.getDate() + offset, hour, minute).getTime()
}

/** 待办数据（暂用本地数据，后续接入接口） */
const tasks = ref([
	{
		id: 1,
		category: 'work',
		title: '整理产品改版思路',
		dueTime: atDay(0, 10, 0),
		flag: 'important',
		done: false
	},
	{
		id: 2,
		category: 'work',
		title: '和设计团队对齐首页方案',
		dueTime: atDay(1, 14, 0),
		flag: 'ordinary',
		done: false
	},
	{
		id: 3,
		category: 'work',
		title: '汇总本周用户反馈',
		dueTime: atDay(2),
		flag: '',
		done: false
	},
	{
		id: 4,
		category: 'work',
		title: '更新项目进度文档',
		dueTime: null,
		flag: '',
		done: false
	},
	{
		id: 5,
		category: 'work',
		title: '发送会议纪要',
		dueTime: atDay(-1),
		flag: '',
		done: true
	},
	{
		id: 6,
		category: 'life',
		title: '去超市买晚餐食材',
		dueTime: atDay(0, 18, 30),
		flag: '',
		done: false
	},
	{
		id: 7,
		category: 'life',
		title: '预约周末体检',
		dueTime: atDay(2),
		flag: '',
		done: false
	},
	{
		id: 8,
		category: 'life',
		title: '整理换季衣物',
		dueTime: null,
		flag: '',
		done: false
	},
	{
		id: 9,
		category: 'read',
		title: '读《也许你该找个人聊聊》',
		dueTime: atDay(0, 21, 0),
		flag: 'ordinary',
		done: false
	},
	{
		id: 10,
		category: 'read',
		title: '整理读书笔记',
		dueTime: null,
		flag: '',
		done: false
	},
	{
		id: 11,
		category: 'growth',
		title: '学习 LiveKit 部署文档',
		dueTime: atDay(1),
		flag: '',
		done: false
	},
	{
		id: 12,
		category: 'growth',
		title: '复盘本周工作',
		dueTime: null,
		flag: '',
		done: false
	}
])

/** 当前清单信息 */
const activeCategory = computed(
	() => categories.find((item) => item.id === activeCategoryId.value) || categories[0]
)

/** 当前清单下的待办，未完成的按时间升序（未设置日期排最后），已完成的排末尾 */
const categoryTasks = computed(() => {
	const list = tasks.value.filter((task) => task.category === activeCategoryId.value)
	const pending = list.filter((task) => !task.done)
	const done = list.filter((task) => task.done)

	pending.sort((a, b) => {
		if (!a.dueTime) {
			return 1
		}

		if (!b.dueTime) {
			return -1
		}

		return a.dueTime - b.dueTime
	})

	return [...pending, ...done]
})

/** 当前清单已完成的条数 */
const doneCount = computed(() => categoryTasks.value.filter((task) => task.done).length)

/** 当前清单待完成的条数 */
const pendingCount = computed(() => categoryTasks.value.filter((task) => !task.done).length)

/** 筛选后的任务列表 */
const visibleTasks = computed(() => {
	if (activeFilter.value === 'pending') {
		return categoryTasks.value.filter((task) => !task.done)
	}

	if (activeFilter.value === 'done') {
		return categoryTasks.value.filter((task) => task.done)
	}

	return categoryTasks.value
})

/** 筛选分段配置 */
const filters = computed(() => [
	{ key: 'all', label: '全部', count: categoryTasks.value.length },
	{ key: 'pending', label: '待完成', count: pendingCount.value },
	{ key: 'done', label: '已完成', count: doneCount.value }
])

/** 某个清单下的待完成条数 */
function countOf(categoryId) {
	return tasks.value.filter((task) => task.category === categoryId && !task.done).length
}

/** 自然日差值，今天为 0 */
function dayDiff(date) {
	const now = new Date()
	const target = new Date(date.getFullYear(), date.getMonth(), date.getDate())
	const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

	return Math.round((target - today) / 86400000)
}

/** 到期时间展示文案 */
function formatDue(task) {
	if (!task.dueTime) {
		return '未设置日期'
	}

	const date = new Date(task.dueTime)
	const diff = dayDiff(date)
	const dayLabel = `${date.getMonth() + 1}月${date.getDate()}日`
	const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`

	if (task.done) {
		if (diff === 0) {
			return '今天已完成'
		}

		if (diff === -1) {
			return '昨天已完成'
		}

		return `${dayLabel}已完成`
	}

	if (diff === 0) {
		return `今天 · ${time}`
	}

	if (diff === 1) {
		return `明天 · ${time}`
	}

	return `${dayLabel} · 周${WEEK_CHARS[(date.getDay() + 6) % 7]}`
}

/** 切换清单 */
function selectCategory(categoryId) {
	activeCategoryId.value = categoryId
	activeFilter.value = 'all'
}

/** 勾选 / 取消勾选任务 */
function toggleTask(task) {
	task.done = !task.done
}

/** 新建待办 */
function addTask() {
	uni.navigateTo({ url: '/subpages/common/create/index' })
}

/** 清单更多操作（暂作搜索入口，待接入菜单） */
function handleMore() {
	uni.navigateTo({ url: '/subpages/common/search/index' })
}

/** 隐藏原生 tabBar，改用自定义导航栏 */
onShow(() => {
	uni.hideTabBar()
})
</script>

<template>
	<view class="list">
		<view :style="{ height: statusBarHeight + 'px' }" />

		<brand-header @more="handleMore" />

		<view class="list__hero">
			<view class="list__hero-text">
				<text class="list__title">给事情一个归处</text>
				<text class="list__subtitle">{{ categories.length }} 个清单，让日常井井有条</text>
			</view>
			<view class="list__add" @click="addTask">＋</view>
		</view>

		<view class="categories">
			<view
				v-for="item in categories"
				:key="item.id"
				class="category"
				:class="{ 'category--active': item.id === activeCategoryId }"
				@click="selectCategory(item.id)"
			>
				<y-icon :name="item.icon" size="48rpx" />

				<view class="category__text">
					<text class="category__name">{{ item.name }}</text>
					<text class="category__count">{{ countOf(item.id) }} 件待办</text>
				</view>
			</view>
		</view>

		<view class="section">
			<view class="section__head">
				<view class="section__title-wrap">
					<text class="section__title">{{ activeCategory.name }}</text>
					<text class="section__progress">已完成 {{ doneCount }} / {{ categoryTasks.length }}</text>
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
					@click="activeFilter = item.key"
				>
					<text class="segment__label">{{ item.label }} {{ item.count }}</text>
				</view>
			</view>

			<view v-if="visibleTasks.length" class="task-list">
				<view
					v-for="task in visibleTasks"
					:key="task.id"
					class="task"
					@click="toggleTask(task)"
				>
					<view class="task__check" :class="{ 'task__check--done': task.done }">
						<text v-if="task.done" class="task__check-icon">✓</text>
					</view>

					<view class="task__main">
						<text class="task__title" :class="{ 'task__title--done': task.done }">{{ task.title }}</text>
						<text class="task__due">{{ formatDue(task) }}</text>
					</view>

					<image
						v-if="task.flag"
						class="task__flag"
						:src="FLAG_SRC[task.flag]"
						mode="aspectFit"
					/>
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
	</view>
</template>

<style scoped lang="scss">
.list {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 180rpx;
	box-sizing: border-box;
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
	font-size: 52rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.list__subtitle {
	margin-top: 16rpx;
	font-size: 26rpx;
	color: $y-text-sub;
}

.list__add {
	width: 60rpx;
	margin-top: 12rpx;
	font-size: 52rpx;
	font-weight: 300;
	line-height: 1;
	text-align: center;
	color: $y-green-primary;
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
	border: 2rpx solid #ecece5;
	background-color: $y-card-bg;
}

.category--active {
	border-color: #d8e2cb;
	background-color: $y-green-soft;
}

.category__text {
	display: flex;
	flex-direction: column;
	margin-left: 20rpx;
}

.category__name {
	font-size: 30rpx;
	font-weight: 500;
	color: $y-text-main;
}

.category__count {
	margin-top: 10rpx;
	font-size: 22rpx;
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
	font-size: 36rpx;
	font-weight: 600;
	color: $y-text-main;
}

.section__progress {
	margin-left: 16rpx;
	font-size: 24rpx;
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
	border: 3rpx solid #ccd1c3;
}

.task__check--done {
	border-color: $y-green-done;
	background-color: $y-green-done;
}

.task__check-icon {
	font-size: 24rpx;
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
	font-size: 30rpx;
	color: $y-text-main;
}

.task__title--done {
	color: $y-text-muted;
}

.task__due {
	margin-top: 14rpx;
	font-size: 24rpx;
	color: $y-text-muted;
}

.task__flag {
	flex-shrink: 0;
	width: 36rpx;
	height: 36rpx;
	margin-left: 20rpx;
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
	font-size: 34rpx;
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 16rpx;
	font-size: 26rpx;
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
	font-size: 34rpx;
	line-height: 1;
	color: #ffffff;
}

.empty__button-text {
	font-size: 30rpx;
	color: #ffffff;
}
</style>
