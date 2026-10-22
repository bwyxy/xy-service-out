<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

/** 周标签，索引 0 为周一 */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 标记图标 */
const FLAG_SRC = {
	important: '/static/task_important.png',
	ordinary: '/static/task_ordinary.png'
}

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 排序方向：true 为按时间正序 */
const sortAsc = ref(true)

/** 「已完成」分组是否展开 */
const doneExpanded = ref(true)

/** 当前选中的日期 key */
const selectedKey = ref('')

/** 待办数据（暂用本地数据，后续接入接口） */
const tasks = ref([])

/** 本周日期条数据 */
const weekDays = computed(() => {
	const now = new Date()
	const offset = (now.getDay() + 6) % 7
	const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - offset)

	return WEEK_LABELS.map((label, index) => {
		const date = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + index)
		const key = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`

		return {
			key,
			week: label,
			day: date.getDate(),
			active: key === selectedKey.value
		}
	})
})

/** 标题下方日期文案 */
const dateLabel = computed(() => {
	const [year, month, day] = selectedKey.value.split('-').map(Number)
	const weekIndex = (new Date(year, month - 1, day).getDay() + 6) % 7

	return `${month}月${day}日，星期${WEEK_LABELS[weekIndex]} · 秋日晴好`
})

/** 待完成列表 */
const pendingTasks = computed(() => {
	const list = tasks.value.filter((item) => !item.done)

	list.sort((a, b) => (sortAsc.value ? 1 : -1) * a.time.localeCompare(b.time))

	return list
})

/** 已完成列表 */
const doneTasks = computed(() => tasks.value.filter((item) => item.done))

/** 全部任务数 */
const totalCount = computed(() => tasks.value.length)

/** 已完成任务数 */
const doneCount = computed(() => doneTasks.value.length)

/** 进度百分比 */
const percent = computed(() => {
	if (!totalCount.value) {
		return 0
	}

	return Math.round((doneCount.value / totalCount.value) * 100)
})

/** 进度卡片底部提示 */
const progressTip = computed(() =>
	totalCount.value ? '每完成一件，都在向前一点。' : '新的一天，从一件小事开始。'
)

/** 初始化选中今天 */
function initSelectedDate() {
	const now = new Date()
	selectedKey.value = `${now.getFullYear()}-${now.getMonth() + 1}-${now.getDate()}`
}

initSelectedDate()

/** 切换选中日期 */
function selectDate(day) {
	selectedKey.value = day.key
}

/** 切换排序方向 */
function toggleSort() {
	sortAsc.value = !sortAsc.value
}

/** 勾选 / 取消勾选任务 */
function toggleTask(task) {
	task.done = !task.done
}

/** 添加今日待办 */
function addTask() {
	uni.navigateTo({ url: '/subpages/common/create/index' })
}

/** 隐藏原生 tabBar，改用自定义导航栏 */
onShow(() => {
	uni.hideTabBar()
})
</script>

<template>
	<view class="today">
		<view :style="{ height: statusBarHeight + 'px' }" />

		<brand-header />

		<view class="today__hero">
			<text class="today__title">把今天，慢慢做好</text>
			<text class="today__subtitle">{{ dateLabel }}</text>
		</view>

		<view class="progress">
			<image class="progress__deco" src="/static/task_progress.png" mode="aspectFit" />

			<view class="progress__head">
				<text class="progress__label">今日进度</text>
				<view class="progress__count">
					<text class="progress__count-done">{{ doneCount }}</text>
					<text class="progress__count-total">/ {{ totalCount }}</text>
				</view>
			</view>

			<view class="progress__track">
				<view class="progress__fill" :style="{ width: percent + '%' }" />
			</view>

			<text class="progress__tip">{{ progressTip }}</text>
		</view>

		<view class="week">
			<view
				v-for="day in weekDays"
				:key="day.key"
				class="week__item"
				:class="{ 'week__item--active': day.active }"
				@click="selectDate(day)"
			>
				<text class="week__label">{{ day.week }}</text>
				<text class="week__date">{{ day.day }}</text>
			</view>
		</view>

		<view class="section">
			<view class="section__head">
				<view class="section__title-wrap">
					<text class="section__title">待完成</text>
					<text class="section__count">{{ pendingTasks.length }}</text>
				</view>

				<view class="section__sort" @click="toggleSort">
					<image class="section__sort-icon" src="/static/date_filter.png" mode="aspectFit" />
					<text class="section__sort-text">{{ sortAsc ? '按时间排序' : '按时间倒序' }}</text>
				</view>
			</view>

			<view v-if="pendingTasks.length" class="task-list">
				<view
					v-for="task in pendingTasks"
					:key="task.id"
					class="task"
					@click="toggleTask(task)"
				>
					<view class="task__check" />

					<view class="task__main">
						<text class="task__title">{{ task.title }}</text>
						<view class="task__meta">
							<text class="task__time">{{ task.time }}</text>
							<text v-if="task.note" class="task__note">{{ task.note }}</text>
							<text v-if="task.tag" class="task__tag">{{ task.tag }}</text>
						</view>
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
				<text class="empty__title">今天还没有安排</text>
				<text class="empty__desc">记下一件想做的小事，给今天一个起点。</text>
				<view class="empty__button" @click="addTask">
					<text class="empty__button-plus">＋</text>
					<text class="empty__button-text">添加今日待办</text>
				</view>
			</view>
		</view>

		<view class="section section--done">
			<view class="section__head section__head--done" @click="doneExpanded = !doneExpanded">
				<text class="section__caret">{{ doneExpanded ? '⌄' : '›' }}</text>
				<text class="section__title section__title--done">已完成</text>
				<text class="section__dot">·</text>
				<text class="section__count section__count--done">{{ doneCount }}</text>
			</view>

			<view v-if="doneExpanded && doneTasks.length" class="task-list">
				<view
					v-for="task in doneTasks"
					:key="task.id"
					class="task task--done"
					@click="toggleTask(task)"
				>
					<view class="task__check task__check--done">
						<text class="task__check-icon">✓</text>
					</view>

					<view class="task__main">
						<text class="task__title task__title--done">{{ task.title }}</text>
						<view class="task__meta">
							<text class="task__time">{{ task.time }}</text>
							<text class="task__note">已完成</text>
							<text v-if="task.tag" class="task__tag">{{ task.tag }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<custom-tabbar active="pages/index/index" />

		<login-sheet />
	</view>
</template>

<style scoped lang="scss">
.today {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 180rpx;
	box-sizing: border-box;
}

/* 标题 */
.today__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.today__title {
	font-size: 52rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.today__subtitle {
	margin-top: 16rpx;
	font-size: 26rpx;
	color: $y-text-sub;
}

/* 今日进度卡片 */
.progress {
	position: relative;
	margin-top: 32rpx;
	padding: 36rpx;
	border-radius: 32rpx;
	background-color: $y-green-soft;
	overflow: hidden;
}

.progress__deco {
	position: absolute;
	top: 24rpx;
	right: 24rpx;
	width: 150rpx;
	height: 140rpx;
}

.progress__head {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.progress__label {
	font-size: 26rpx;
	color: #5c6b52;
}

.progress__count {
	display: flex;
	align-items: baseline;
}

.progress__count-done {
	font-size: 48rpx;
	font-weight: 600;
	color: $y-text-main;
}

.progress__count-total {
	margin-left: 8rpx;
	font-size: 28rpx;
	color: #5c6b52;
}

.progress__track {
	margin-top: 28rpx;
	height: 8rpx;
	border-radius: 999rpx;
	background-color: $y-green-track;
}

.progress__fill {
	height: 100%;
	border-radius: 999rpx;
	background-color: $y-green-primary;
	transition: width 0.3s ease;
}

.progress__tip {
	display: block;
	margin-top: 26rpx;
	font-size: 24rpx;
	color: #6f8064;
}

/* 周日期条 */
.week {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 36rpx;
}

.week__item {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	width: 64rpx;
	padding: 10rpx 0;
	border-radius: 18rpx;
}

.week__label {
	font-size: 24rpx;
	color: $y-text-muted;
}

.week__date {
	margin-top: 8rpx;
	font-size: 34rpx;
	font-weight: 600;
	color: $y-text-main;
}

.week__item--active {
	background-color: $y-green-primary;

	.week__label {
		color: #cdd9c0;
	}

	.week__date {
		color: #ffffff;
	}
}

/* 分组 */
.section {
	margin-top: 56rpx;
}

.section--done {
	margin-top: 48rpx;
}

.section__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding-bottom: 24rpx;
	border-bottom: 2rpx solid $y-divider;
}

.section__head--done {
	justify-content: flex-start;
	padding-bottom: 0;
	border-bottom: none;
}

.section__title-wrap {
	display: flex;
	align-items: flex-start;
}

.section__title {
	font-size: 36rpx;
	font-weight: 600;
	color: $y-text-main;
}

.section__title--done {
	font-size: 28rpx;
	font-weight: 500;
	color: $y-text-sub;
}

.section__count {
	margin-left: 10rpx;
	font-size: 24rpx;
	font-weight: 600;
	color: $y-green-primary;
	line-height: 1;
}

.section__count--done {
	font-weight: 400;
	color: $y-text-muted;
}

.section__dot {
	margin: 0 8rpx;
	font-size: 28rpx;
	color: $y-text-muted;
}

.section__caret {
	margin-right: 12rpx;
	font-size: 28rpx;
	color: $y-text-muted;
}

.section__sort {
	display: flex;
	align-items: center;
}

.section__sort-icon {
	width: 28rpx;
	height: 28rpx;
	margin-right: 8rpx;
}

.section__sort-text {
	font-size: 24rpx;
	color: $y-text-sub;
}

/* 任务行 */
.task {
	display: flex;
	align-items: flex-start;
	padding: 32rpx 0;
	border-bottom: 2rpx solid $y-divider;
}

.task--done {
	border-bottom: none;
}

.task__check {
	flex-shrink: 0;
	width: 40rpx;
	height: 40rpx;
	margin-top: 4rpx;
	margin-right: 24rpx;
	box-sizing: border-box;
	border-radius: 50%;
	border: 3rpx solid #ccd1c3;
}

.task__check--done {
	display: flex;
	align-items: center;
	justify-content: center;
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

.task__meta {
	display: flex;
	align-items: center;
	margin-top: 14rpx;
}

.task__time,
.task__note {
	font-size: 24rpx;
	color: $y-text-muted;
}

.task__note {
	margin-left: 18rpx;
}

.task__tag {
	margin-left: 18rpx;
	padding: 4rpx 14rpx;
	border-radius: 8rpx;
	font-size: 22rpx;
	color: $y-green-tag-text;
	background-color: $y-green-tag-bg;
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
