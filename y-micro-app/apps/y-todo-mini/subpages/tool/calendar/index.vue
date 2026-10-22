<script setup>
import { computed, ref } from 'vue'

	/**
	 * 工具 · 日历视图
	 *
	 * 路由：subpages/tool/calendar/index（分包 subpages/tool，按需加载）
	 * 职责：日历视图页，按日期安排每一件事——月历网格、月份切换与选中日任务。
	 * 目录：状态（viewYear、viewMonth、selectedDay、cells）→ 方法（changeMonth、selectDay、addTask）→ 样式（.card、.month、.grid、.task）
	 */

/** 周标签，索引 0 为周一 */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 各日期的任务（暂用本地数据，后续接入接口） */
const MOCK_TASKS = {
	'10-3': [{ id: 1, title: '整理季度总结提纲', time: '09:30' }],
	'10-8': [
		{ id: 2, title: '和设计团队对齐首页方案', time: '14:00' },
		{ id: 3, title: '去超市买晚餐食材', time: '18:30' }
	],
	'10-12': [{ id: 4, title: '预约周末体检', time: '10:00' }],
	'10-18': [{ id: 5, title: '读《也许你该找个人聊聊》', time: '21:00' }],
	'10-22': [
		{ id: 6, title: '整理产品改版思路', time: '10:00' },
		{ id: 7, title: '汇总本周用户反馈', time: '15:30' },
		{ id: 8, title: '晨间拉伸 15 分钟', time: '08:15', done: true }
	]
}

const now = new Date()

/** 当前展示的年月 */
const viewYear = ref(now.getFullYear())
const viewMonth = ref(now.getMonth() + 1)

/** 当前选中的日 */
const selectedDay = ref(now.getDate())

/** 年月标题 */
const monthLabel = computed(() => `${viewYear.value}年${viewMonth.value}月`)

/** 今天的日序（非本月时为 -1） */
const todayDay = computed(() => {
	if (viewYear.value !== now.getFullYear() || viewMonth.value !== now.getMonth() + 1) {
		return -1
	}

	return now.getDate()
})

/** 某天是否有安排 */
function taskCountOf(day) {
	return (MOCK_TASKS[`${viewMonth.value}-${day}`] || []).length
}

/** 日期网格，固定 6 行 42 格 */
const cells = computed(() => {
	const first = new Date(viewYear.value, viewMonth.value - 1, 1)
	const offset = (first.getDay() + 6) % 7
	const days = new Date(viewYear.value, viewMonth.value, 0).getDate()
	const list = []

	for (let index = 0; index < offset; index += 1) {
		list.push({ key: `prev-${index}`, day: '' })
	}

	for (let day = 1; day <= days; day += 1) {
		list.push({
			key: `day-${day}`,
			day,
			today: day === todayDay.value,
			hasTask: taskCountOf(day) > 0,
			selected: day === selectedDay.value
		})
	}

	while (list.length < 42) {
		list.push({ key: `next-${list.length}`, day: '' })
	}

	return list
})

/** 选中日期的任务 */
const dayTasks = computed(() => MOCK_TASKS[`${viewMonth.value}-${selectedDay.value}`] || [])

/** 选中日期文案 */
const selectedLabel = computed(() => {
	const date = new Date(viewYear.value, viewMonth.value - 1, selectedDay.value)
	const week = WEEK_LABELS[(date.getDay() + 6) % 7]

	return `${viewMonth.value}月${selectedDay.value}日 · 周${week}`
})

/** 本月有安排的天数 */
const busyDays = computed(() => {
	let count = 0

	for (let day = 1; day <= new Date(viewYear.value, viewMonth.value, 0).getDate(); day += 1) {
		if (taskCountOf(day)) {
			count += 1
		}
	}

	return count
})

/** 切换月份 */
function changeMonth(step) {
	const date = new Date(viewYear.value, viewMonth.value - 1 + step, 1)

	viewYear.value = date.getFullYear()
	viewMonth.value = date.getMonth() + 1
	selectedDay.value = 1
}

/** 选中日期 */
function selectDay(cell) {
	if (!cell.day) {
		return
	}

	selectedDay.value = cell.day
}

/** 添加任务 */
function addTask() {
	uni.navigateTo({ url: '/subpages/common/create/index' })
}
</script>

<template>
	<sub-page title="日历视图" subtitle="按日期安排每一件事。">
		<view class="card">
			<view class="month">
				<view class="month__nav" @click="changeMonth(-1)">
					<y-icon name="chevron-left" size="30rpx" color="#8e8e85" />
				</view>

				<text class="month__label">{{ monthLabel }}</text>

				<view class="month__nav" @click="changeMonth(1)">
					<y-icon name="chevron-right" size="30rpx" color="#8e8e85" />
				</view>
			</view>

			<view class="week">
				<text v-for="label in WEEK_LABELS" :key="label" class="week__label">{{ label }}</text>
			</view>

			<view class="grid">
				<view
					v-for="cell in cells"
					:key="cell.key"
					class="grid__cell"
					:class="{ 'grid__cell--selected': cell.selected, 'grid__cell--today': cell.today }"
					@click="selectDay(cell)"
				>
					<text v-if="cell.day" class="grid__day">{{ cell.day }}</text>
					<view v-if="cell.hasTask" class="grid__dot" />
				</view>
			</view>

			<text class="card__foot">本月有 {{ busyDays }} 天安排了事情</text>
		</view>

		<view class="section">
			<view class="section__head">
				<text class="section__title">{{ selectedLabel }}</text>

				<view class="section__add" @click="addTask">
					<y-icon name="plus" size="28rpx" />
					<text class="section__add-text">添加</text>
				</view>
			</view>

			<view v-if="dayTasks.length" class="tasks">
				<view v-for="task in dayTasks" :key="task.id" class="task">
					<view class="task__check" :class="{ 'task__check--done': task.done }">
						<y-icon v-if="task.done" name="check" size="24rpx" color="#ffffff" />
					</view>

					<text class="task__title" :class="{ 'task__title--done': task.done }">{{ task.title }}</text>
					<text class="task__time">{{ task.time }}</text>
				</view>
			</view>

			<view v-else class="empty">
				<text class="empty__title">这一天还没有安排</text>
				<text class="empty__desc">留白也是很好的一天。</text>
			</view>
		</view>
	</sub-page>
</template>

<style scoped lang="scss">
/* 日历卡片 */
.card {
	margin-top: 32rpx;
	padding: 32rpx 24rpx 28rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.month {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.month__nav {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 56rpx;
	height: 56rpx;
	border-radius: 50%;
	background-color: #f2f2ec;
}

.month__label {
	font-size: 32rpx;
	font-weight: 600;
	color: $y-text-main;
}

.week {
	display: flex;
	margin-top: 28rpx;
}

.week__label {
	flex: 1;
	font-size: 22rpx;
	color: $y-text-muted;
	text-align: center;
}

.grid {
	display: flex;
	flex-wrap: wrap;
	margin-top: 12rpx;
}

.grid__cell {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	width: 14.285%;
	height: 88rpx;
}

.grid__day {
	font-size: 28rpx;
	color: $y-text-main;
}

.grid__dot {
	width: 8rpx;
	height: 8rpx;
	margin-top: 6rpx;
	border-radius: 50%;
	background-color: $y-green-primary;
}

.grid__cell--today .grid__day {
	font-weight: 600;
	color: $y-green-primary;
}

.grid__cell--selected {
	border-radius: 50%;
	background-color: $y-green-soft;
}

.grid__cell--selected .grid__dot {
	background-color: $y-green-primary;
}

.card__foot {
	display: block;
	margin-top: 24rpx;
	padding-top: 24rpx;
	border-top: 2rpx solid $y-divider;
	font-size: 24rpx;
	color: $y-text-sub;
	text-align: center;
}

/* 当日任务 */
.section {
	margin-top: 44rpx;
}

.section__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.section__title {
	font-size: 32rpx;
	font-weight: 600;
	color: $y-text-main;
}

.section__add {
	display: flex;
	align-items: center;
	padding: 8rpx 20rpx;
	border-radius: 999rpx;
	background-color: $y-green-tag-bg;
}

.section__add-text {
	margin-left: 6rpx;
	font-size: 24rpx;
	color: $y-green-tag-text;
}

.tasks {
	margin-top: 8rpx;
}

.task {
	display: flex;
	align-items: center;
	padding: 30rpx 0;
	border-bottom: 2rpx solid $y-divider;
}

.task__check {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 40rpx;
	height: 40rpx;
	margin-right: 24rpx;
	box-sizing: border-box;
	border-radius: 50%;
	border: 3rpx solid #ccd1c3;
}

.task__check--done {
	border-color: $y-green-done;
	background-color: $y-green-done;
}

.task__title {
	flex: 1;
	font-size: 30rpx;
	color: $y-text-main;
}

.task__title--done {
	color: $y-text-muted;
}

.task__time {
	margin-left: 20rpx;
	font-size: 24rpx;
	color: $y-text-muted;
}

/* 空状态 */
.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 60rpx 0;
}

.empty__title {
	font-size: 30rpx;
	font-weight: 500;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 14rpx;
	font-size: 24rpx;
	color: $y-text-sub;
}
</style>
