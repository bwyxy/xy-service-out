<script setup>
import { computed, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'

import { completeTask, listTasks } from '@/api/task'
import { listTags } from '@/api/tag'
import { getQuota } from '@/api/notify'
import { isLoggedIn } from '@/utils/session'
import { reportAcceptedCount, requestRemindSubscribe } from '@/utils/subscribe'
import { appearance, themeClass } from '@/utils/appearance'
import { formatClock, pad2, toTimestamp } from '@/utils/date'

/** 周标签，索引 0 为周一 */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 月份短语，索引 0 为 1 月；标题下面那句轻描淡写，跟着月份换 */
const MONTH_PHRASES = [
	'冬日静好',
	'春信将至',
	'春意初生',
	'春和景明',
	'初夏将至',
	'夏木阴阴',
	'盛夏流金',
	'秋意渐起',
	'秋高气爽',
	'秋日晴好',
	'霜叶渐浓',
	'冬雪可期'
]

/** 标记图标 */
const FLAG_SRC = {
	important: '/static/task_important.png',
	ordinary: '/static/task_ordinary.png'
}

/** 后端 priority → 小旗子，与清单页保持一致 */
const FLAG_BY_PRIORITY = {
	1: 'ordinary',
	2: 'important'
}

/** 每天任务每页条数，上拉加载下一页 */
const PAGE_SIZE = 20

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 排序方向：true 为按时间正序 */
const sortAsc = ref(true)

/** 「已完成」分组是否展开 */
const doneExpanded = ref(true)

/** 当前选中的日期 key，格式 yyyy-MM-dd */
const selectedKey = ref('')

/** 当天任务（来自 POST /task/list，按页累积，含已完成） */
const tasks = ref([])

/** 已加载到的页码 */
const pageNum = ref(1)

/** 是否还有下一页 */
const hasMore = ref(false)

/** 加载中标记，挡住上拉时连续触发的重复请求 */
const loadingTasks = ref(false)

/** 今日任务总数，进度条分母（后端计数，前端列表不参与计算） */
const totalCount = ref(0)

/** 今日已完成数，进度条分子（后端计数，勾选成功后本地 ±1） */
const doneCount = ref(0)

/** 明细里按清单 Id 显示清单名（清单名只是行内装饰，失败不影响主流程） */
const tagNames = ref({})

/** 剩余提醒额度（一次性订阅：授权一次只能发一条） */
const quota = ref({ remain: 0, total: 0 })

/** 额度是否已从后端取回：没取回时不显示提示条，免得先闪一句「额度用完了」 */
const quotaLoaded = ref(false)

/** 正在提交完成状态的任务 Id，挡住连点 */
const togglingIds = ref([])

/** 清单名只拉一次 */
let tagsLoaded = false

/** 请求序号：切日期很快时，用来丢弃过期响应 */
let requestSeq = 0

/** 本周日期条数据：起始日是周一还是周日跟随「外观与显示」里的设置 */
const weekDays = computed(() => {
	const now = new Date()
	// 周一起始时 getDay() 的周日 0 要挪到最后，周日起始则直接用 getDay()
	const offset = appearance.weekStart ? (now.getDay() + 6) % 7 : now.getDay()
	const first = new Date(now.getFullYear(), now.getMonth(), now.getDate() - offset)
	const days = []

	for (let index = 0; index < 7; index += 1) {
		const date = new Date(first.getFullYear(), first.getMonth(), first.getDate() + index)
		// 补零成 yyyy-MM-dd，既给后端 LocalDate.parse 用，也保证和 selectedKey 能直接比较
		const key = `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`

		days.push({
			key,
			// 周标签恒按星期几取，起始日一变顺序自然跟着变
			week: WEEK_LABELS[(date.getDay() + 6) % 7],
			day: date.getDate(),
			active: key === selectedKey.value
		})
	}

	return days
})

/** 标题下方日期文案：月份短语跟着所选日期的月份走 */
const dateLabel = computed(() => {
	const [year, month, day] = selectedKey.value.split('-').map(Number)
	const weekIndex = (new Date(year, month - 1, day).getDay() + 6) % 7

	return `${month}月${day}日，星期${WEEK_LABELS[weekIndex]} · ${MONTH_PHRASES[month - 1]}`
})

/** 待完成列表 */
const pendingTasks = computed(() => {
	const list = tasks.value.filter((item) => !item.done)

	list.sort((a, b) => (sortAsc.value ? 1 : -1) * a.time.localeCompare(b.time))

	return list
})

/** 已完成列表 */
const doneTasks = computed(() => tasks.value.filter((item) => item.done))

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

/** 额度提示条文案：取回额度前不显示，免得先闪一句「额度用完了」 */
const quotaTip = computed(() => {
	if (!quotaLoaded.value) {
		return ''
	}

	return quota.value.remain > 0
		? `你还有 ${quota.value.remain} 条提醒额度`
		: '提醒额度用完了，点这里再授权'
})

/** 列表页脚文案：还有下一页时提示上拉 */
const footText = computed(() => {
	if (!hasMore.value) {
		return '没有更多了'
	}

	return loadingTasks.value ? '正在加载…' : '上拉加载更多'
})

/** 初始化选中今天 */
function initSelectedDate() {
	const now = new Date()
	selectedKey.value = `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}`
}

initSelectedDate()

/** 后端任务 → 当天任务项 */
function toTaskItem(item) {
	return {
		id: item.taskId,
		title: item.title,
		// 保留 HH:mm 字符串：下面的待完成分组还在用它做 localeCompare 排序
		time: formatClock(toTimestamp(item.dueDate)),
		note: item.description,
		tag: tagNames.value[item.tagId] || '',
		flag: FLAG_BY_PRIORITY[item.priority] || '',
		done: item.completed === 1
	}
}

/** 拉取清单名映射：只拉一次，清单名只是装饰，不为它触发登录 */
async function loadTagNames() {
	if (tagsLoaded || !isLoggedIn()) {
		return
	}

	tagsLoaded = true

	const res = await listTags()

	if (res.code !== 200 || !Array.isArray(res.data)) {
		return
	}

	const map = {}

	res.data.forEach((item) => {
		map[item.tagId] = item.tagName
	})

	tagNames.value = map
}

/** 清空列表并复位分页（未登录、或切日期时用） */
function resetDay() {
	requestSeq += 1
	tasks.value = []
	pageNum.value = 1
	hasMore.value = false
	loadingTasks.value = false
	totalCount.value = 0
	doneCount.value = 0
}

/**
 * 拉取选中日期的任务
 *
 * 未登录时清空并展示空态、不弹登录（首页是启动页，一进来就弹框太打扰）。
 * 任务与「今日进度」都取自后端：计数用 allCount/doneCount，本地只做勾选后的 ±1。
 *
 * @param {boolean} [reset] true-从第 1 页覆盖列表；false-追加下一页（上拉加载）
 */
async function loadDay(reset = true) {
	if (!isLoggedIn()) {
		resetDay()

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
		await loadTagNames()

		const res = await listTasks({
			status: 'all',
			date: selectedKey.value,
			pageNum: nextPage,
			pageSize: PAGE_SIZE
		})

		// 期间切了日期，这一页已是旧数据，直接丢弃
		if (seq !== requestSeq || res.code !== 200 || !res.data) {
			return
		}

		const page = res.data
		const list = Array.isArray(page.list) ? page.list.map(toTaskItem) : []

		tasks.value = reset ? list : tasks.value.concat(list)
		pageNum.value = Number(page.pageNum) || nextPage
		hasMore.value = Boolean(page.hasMore)
		totalCount.value = Number(page.allCount) || 0
		doneCount.value = Number(page.doneCount) || 0
	} catch (error) {
		console.warn('[today] 加载今日任务失败', error)
	} finally {
		if (seq === requestSeq) {
			loadingTasks.value = false
		}
	}
}

/** 拉取剩余提醒额度：未登录不请求，登录后回来会由 onShow 重新拉 */
async function loadQuota() {
	if (!isLoggedIn()) {
		quotaLoaded.value = false

		return
	}

	try {
		const res = await getQuota()

		if (res.code === 200 && res.data) {
			quota.value = res.data
			quotaLoaded.value = true
		}
	} catch (error) {
		console.warn('[today] 读取提醒额度失败', error)
	}
}

/**
 * 点击额度提示条：拉起订阅授权并上报，再用后端额度刷新提示条
 *
 * 微信要求订阅授权必须由用户点击触发，所以这一段只能挂在点击上，不能进页面自动弹。
 */
async function grantSubscribe() {
	const count = await requestRemindSubscribe()

	if (count > 0) {
		await reportAcceptedCount(count)
	}

	await loadQuota()
}

/** 切换选中日期：换一天就重新拉那天的任务 */
function selectDate(day) {
	selectedKey.value = day.key
	loadDay()
}

/** 切换排序方向 */
function toggleSort() {
	sortAsc.value = !sortAsc.value
}

/** 勾选 / 取消勾选任务：把目标状态提交给后端，成功后再落到本地 */
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
			task.done = target === 1
			doneCount.value += target === 1 ? 1 : -1

			// 已加载的待完成都被勾完了但后面还有数据，补拉一页，别停在一个假的空态上
			if (!pendingTasks.value.length && hasMore.value) {
				loadDay(false)
			}
		}
	} catch (error) {
		console.warn('[today] 标记任务失败', error)
	} finally {
		togglingIds.value = togglingIds.value.filter((id) => id !== task.id)
	}
}

/** 打开任务详情：点行进详情，点左侧圆圈才是勾选完成 */
function enterDetail(task) {
	uni.navigateTo({ url: `/subpages/common/detail/index?taskId=${task.id}` })
}

/** 添加今日待办 */
function addTask() {
	uni.navigateTo({ url: '/subpages/common/create/index' })
}

/** 上拉到底：还有下一页就追加加载（不够一屏时不会触发，先看首屏结果即可） */
onReachBottom(() => {
	if (hasMore.value) {
		loadDay(false)
	}
})

/** 隐藏原生 tabBar 改用自定义导航栏；回到本页时刷新（登录后 / 新建编辑任务后都能拿到最新数据） */
onShow(() => {
	uni.hideTabBar()
	loadDay()
	loadQuota()
})
</script>

<template>
	<view class="today" :class="themeClass">
		<view class="today__top">
			<view :style="{ height: statusBarHeight + 'px' }" />

			<brand-header />
		</view>

		<view class="today__hero">
			<text class="today__title">把今天，慢慢做好</text>
			<text class="today__subtitle">{{ dateLabel }}</text>
		</view>

		<view
			v-if="quotaTip"
			class="quota-tip"
			:class="{ 'quota-tip--empty': quota.remain <= 0 }"
			@click="grantSubscribe"
		>
			<y-icon name="bell" size="30rpx" />
			<text class="quota-tip__text">{{ quotaTip }}</text>
		</view>

		<view v-if="appearance.showProgress" class="progress">

			<view class="progress-lt">
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

			<image class="progress__deco" src="/static/task_progress.png" mode="aspectFit" />
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
					@click="enterDetail(task)"
				>
					<view class="task__check" @click.stop="toggleTask(task)" />

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

		<view v-if="appearance.showDone" class="section section--done">
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
					@click="enterDetail(task)"
				>
					<view class="task__check task__check--done" @click.stop="toggleTask(task)">
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

		<view v-if="tasks.length" class="more">
			<text class="more__text">{{ footText }}</text>
		</view>

		<custom-tabbar active="pages/index/index" />

		<login-sheet />
		<privacy-sheet />
	</view>
</template>

<style scoped lang="scss">
.today {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 180rpx;
	box-sizing: border-box;
}

/* 顶栏吸顶：状态栏留白与品牌顶栏一起钉在页面顶部，背景铺页面底色避免内容透出 */
.today__top {
	position: sticky;
	top: 0;
	z-index: 100;
	background-color: $y-page-bg;
}

/* 标题 */
.today__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.today__title {
	font-size: calc(52rpx * var(--y-font-scale, 1));
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.today__subtitle {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 提醒额度提示条：点击直接拉起订阅授权 */
.quota-tip {
	display: flex;
	align-items: center;
	margin-top: 28rpx;
	padding: 20rpx 28rpx;
	border-radius: 20rpx;
	background-color: $y-green-soft;
}

.quota-tip--empty {
	background-color: $y-warm-soft-bg;
}

.quota-tip__text {
	flex: 1;
	margin-left: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

.quota-tip--empty .quota-tip__text {
	color: $y-accent-warm;
}

/* 今日进度卡片 */
.progress {
	position: relative;
	margin-top: 32rpx;
	padding: 36rpx;
	border-radius: 32rpx;
	background-color: $y-green-soft;
	overflow: hidden;
	display: flex;
	&-lt {
		flex: 1;
		margin-right: 24rpx;
	}
}

.progress__deco {
	// position: absolute;
	// top: 24rpx;
	// right: 24rpx;
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
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

.progress__count {
	display: flex;
	align-items: baseline;
}

.progress__count-done {
	font-size: calc(48rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.progress__count-total {
	margin-left: 8rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-green-text;
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-green-text;
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.week__date {
	margin-top: 8rpx;
	font-size: calc(34rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.week__item--active {
	background-color: $y-green-primary;

	.week__label {
		color: $y-green-border;
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
	font-size: calc(36rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.section__title--done {
	font-size: calc(28rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-text-sub;
}

.section__count {
	margin-left: 10rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.section__caret {
	margin-right: 12rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	border: 3rpx solid $y-check-border;
}

.task__check--done {
	display: flex;
	align-items: center;
	justify-content: center;
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

.task__time,
.task__note {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.task__note {
	margin-left: 18rpx;
}

.task__tag {
	margin-left: 18rpx;
	padding: 4rpx 14rpx;
	border-radius: 8rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
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

/* 列表页脚：还有下一页时提示上拉 */
.more {
	display: flex;
	justify-content: center;
	padding: 36rpx 0 12rpx;
}

.more__text {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}
</style>
