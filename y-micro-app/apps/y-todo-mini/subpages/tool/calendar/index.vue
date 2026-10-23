<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import { completeTask, getMonthTasks, listTasks } from '@/api/task'
import { formatClock, pad2, toTimestamp, weekdayChar } from '@/utils/date'

	/**
	 * 工具 · 日历视图
	 *
	 * 路由：subpages/tool/calendar/index（分包 subpages/tool，按需加载）
	 * 职责：日历视图页，按日期安排每一件事——月历网格、月份切换与选中日任务。
	 * 数据：点阵与「本月有 N 天」来自 POST /task/month（只算未归档任务），
	 *       选中日的任务来自 POST /task/list 的 date 参数（同为未归档口径）。
	 * 目录：状态（viewYear、viewMonth、selectedDay、dayCountMap、dayTasks）
	 *       → 计算（monthLabel、todayDay、cells、selectedLabel、busyDays）
	 *       → 方法（loadMonth、loadDay、changeMonth、selectDay、toggleTask、addTask）
	 *       → 样式（.card、.month、.grid、.task）
	 */

/** 周标签，索引 0 为周一 */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 选中日的任务一次拉满：一天的任务量级很小，不做分页 */
const DAY_PAGE_SIZE = 100

const now = new Date()

/** 当前展示的年月 */
const viewYear = ref(now.getFullYear())
const viewMonth = ref(now.getMonth() + 1)

/** 当前选中的日 */
const selectedDay = ref(now.getDate())

/** 本月每天的到期任务数，键为 yyyy-MM-dd；没有任务的日期不在表里 */
const dayCountMap = ref({})

/** 本月有安排的天数（后端算好，前端不重复数一遍） */
const busyDays = ref(0)

/** 选中日的任务 */
const dayTasks = ref([])

/** 正在提交完成状态的任务 Id，挡住连点 */
const togglingIds = ref([])

/** 请求序号：切月份/切日期很快时，用来丢弃过期响应 */
let monthSeq = 0
let daySeq = 0

/** 年月标题 */
const monthLabel = computed(() => `${viewYear.value}年${viewMonth.value}月`)

/** 选中日期 key，格式 yyyy-MM-dd（后端 LocalDate 直接解析） */
const selectedKey = computed(() =>
	`${viewYear.value}-${pad2(viewMonth.value)}-${pad2(selectedDay.value)}`
)

/** 今天的日序（非本月时为 -1） */
const todayDay = computed(() => {
	if (viewYear.value !== now.getFullYear() || viewMonth.value !== now.getMonth() + 1) {
		return -1
	}

	return now.getDate()
})

/** 某天是否有安排 */
function taskCountOf(day) {
	return dayCountMap.value[`${viewYear.value}-${pad2(viewMonth.value)}-${pad2(day)}`] || 0
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

/** 选中日期文案：10月23日 · 周五 */
const selectedLabel = computed(() =>
	`${viewMonth.value}月${selectedDay.value}日 · 周${weekdayChar(selectedKey.value)}`
)

/** 后端任务 → 选中日任务项 */
function toTaskItem(item) {
	return {
		id: item.taskId,
		title: item.title,
		time: formatClock(toTimestamp(item.dueDate)),
		done: item.completed === 1
	}
}

/** 拉取本月点阵：月份切来切去时丢弃过期响应 */
async function loadMonth() {
	const seq = (monthSeq += 1)

	try {
		const res = await getMonthTasks({ month: `${viewYear.value}-${pad2(viewMonth.value)}` })

		if (seq !== monthSeq || res.code !== 200 || !res.data) {
			return
		}

		const map = {}

		;(res.data.days || []).forEach((item) => {
			map[item.date] = Number(item.total) || 0
		})

		dayCountMap.value = map
		busyDays.value = Number(res.data.busyDays) || 0
	} catch (error) {
		console.warn('[calendar] 加载月视图失败', error)
	}
}

/** 拉取选中日的任务；失败提示已由 request 统一 toast，这里不动本地状态 */
async function loadDay() {
	const seq = (daySeq += 1)

	try {
		const res = await listTasks({
			status: 'all',
			date: selectedKey.value,
			pageNum: 1,
			pageSize: DAY_PAGE_SIZE
		})

		if (seq !== daySeq || res.code !== 200 || !res.data) {
			return
		}

		dayTasks.value = Array.isArray(res.data.list) ? res.data.list.map(toTaskItem) : []
	} catch (error) {
		console.warn('[calendar] 加载当日任务失败', error)
	}
}

/** 切换月份：直接跳到 1 号，再把月视图与当日任务一起刷新 */
function changeMonth(step) {
	const date = new Date(viewYear.value, viewMonth.value - 1 + step, 1)

	viewYear.value = date.getFullYear()
	viewMonth.value = date.getMonth() + 1
	selectedDay.value = 1

	loadMonth()
	loadDay()
}

/** 选中日期 */
function selectDay(cell) {
	if (!cell.day || cell.day === selectedDay.value) {
		return
	}

	selectedDay.value = cell.day
	loadDay()
}

/** 勾选 / 取消勾选：把目标状态提交给后端，成功后再落到本地 */
async function toggleTask(task) {
	if (togglingIds.value.includes(task.id)) {
		return
	}

	const target = task.done ? 0 : 1

	togglingIds.value = togglingIds.value.concat(task.id)

	try {
		const res = await completeTask({ taskId: task.id, completed: target })

		if (res.code === 200) {
			task.done = target === 1
		}
	} catch (error) {
		console.warn('[calendar] 标记任务失败', error)
	} finally {
		togglingIds.value = togglingIds.value.filter((id) => id !== task.id)
	}
}

/** 打开任务详情：点行进详情，点左侧圆圈才是勾选完成 */
function enterDetail(task) {
	uni.navigateTo({ url: `/subpages/common/detail/index?taskId=${task.id}` })
}

/** 添加任务 */
function addTask() {
	uni.navigateTo({ url: '/subpages/common/create/index' })
}

/** 每次展示都重拉：在别处新建/完成/删除任务后回到本页，点阵与列表都是新的 */
onShow(() => {
	loadMonth()
	loadDay()
})
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
				<view v-for="task in dayTasks" :key="task.id" class="task" @click="enterDetail(task)">
					<view class="task__check" :class="{ 'task__check--done': task.done }" @click.stop="toggleTask(task)">
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
	background-color: $y-sunken-bg;
}

.month__label {
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.week {
	display: flex;
	margin-top: 28rpx;
}

.week__label {
	flex: 1;
	font-size: calc(22rpx * var(--y-font-scale, 1));
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	font-size: calc(32rpx * var(--y-font-scale, 1));
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	border: 3rpx solid $y-check-border;
}

.task__check--done {
	border-color: $y-green-done;
	background-color: $y-green-done;
}

.task__title {
	flex: 1;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.task__title--done {
	color: $y-text-muted;
}

.task__time {
	margin-left: 20rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}
</style>
