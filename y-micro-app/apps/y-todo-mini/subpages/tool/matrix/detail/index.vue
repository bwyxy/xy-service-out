<script setup>
import { computed, ref } from 'vue'
import { onLoad, onReachBottom, onShow } from '@dcloudio/uni-app'

import { completeTask, listTasks } from '@/api/task'
import { formatClock, toTimestamp } from '@/utils/date'
import { quadrantOf } from '@/utils/quadrant'

	/**
	 * 工具 · 四象限 · 象限任务
	 *
	 * 路由：subpages/tool/matrix/detail/index?level=4（分包 subpages/tool，按需加载）
	 * 职责：某一象限的待完成任务列表——分页加载 + 勾选完成，勾选交互与首页、日历页一致。
	 * 数据：复用 POST /task/list，传 urgencyLevel 按象限过滤 + status=pending（四象限只管还没做完的事）。
	 * 目录：状态（level、tasks、pageNum、hasMore、loading、togglingIds、requestSeq）
	 *       → 计算（quadrant、footText）→ 方法（toTaskItem、loadTasks、toggleTask）
	 *       → 样式（.task、.empty）
	 */

/** 每页条数，上拉加载下一页 */
const PAGE_SIZE = 20

/** 紧急程度：来自路由参数，取不到按最紧急的一档渲染标题与配色 */
const level = ref(4)

/** 待完成任务（按页累积） */
const tasks = ref([])

/** 已加载到的页码 */
const pageNum = ref(1)

/** 是否还有下一页 */
const hasMore = ref(false)

/** 加载中标记，挡住上拉时连续触发的重复请求 */
const loading = ref(false)

/** 正在提交完成状态的任务 Id，挡住连点 */
const togglingIds = ref([])

/** 请求序号：快速返回时丢弃过期响应 */
let requestSeq = 0

/** 当前象限的名称、提示与配色 */
const quadrant = computed(() => quadrantOf(level.value))

/** 页面副标题 */
const subtitle = computed(() => `${quadrant.value.tip} · 这一格还没做完的事`)

/** 列表页脚文案：还有下一页时提示上拉 */
const footText = computed(() => {
	if (!hasMore.value) {
		return '没有更多了'
	}

	return loading.value ? '正在加载…' : '上拉加载更多'
})

/** 后端任务 → 任务项 */
function toTaskItem(item) {
	return {
		id: item.taskId,
		title: item.title,
		time: formatClock(toTimestamp(item.dueDate)),
		done: item.completed === 1
	}
}

/**
 * 拉取该象限的待完成任务
 *
 * @param {boolean} [reset] true-从第 1 页覆盖列表；false-追加下一页（上拉加载）
 */
async function loadTasks(reset = true) {
	if (!reset && loading.value) {
		return
	}

	const nextPage = reset ? 1 : pageNum.value + 1
	const seq = (requestSeq += 1)

	loading.value = true

	try {
		const res = await listTasks({
			status: 'pending',
			urgencyLevel: level.value,
			pageNum: nextPage,
			pageSize: PAGE_SIZE
		})

		if (seq !== requestSeq || res.code !== 200 || !res.data) {
			return
		}

		const page = res.data
		const list = Array.isArray(page.list) ? page.list.map(toTaskItem) : []

		tasks.value = reset ? list : tasks.value.concat(list)
		pageNum.value = Number(page.pageNum) || nextPage
		hasMore.value = Boolean(page.hasMore)
	} catch (error) {
		console.warn('[matrix] 加载象限任务失败', error)
	} finally {
		if (seq === requestSeq) {
			loading.value = false
		}
	}
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
		console.warn('[matrix] 标记任务失败', error)
	} finally {
		togglingIds.value = togglingIds.value.filter((id) => id !== task.id)
	}
}

/** 打开任务详情：点行进详情，点左侧圆圈才是勾选完成 */
function enterDetail(task) {
	uni.navigateTo({ url: `/subpages/common/detail/index?taskId=${task.id}` })
}

/** 象限来自路由参数：点哪一格进哪一格 */
onLoad((query) => {
	const value = Number(query && query.level)

	if (value >= 1 && value <= 4) {
		level.value = value
	}
})

/** 上拉到底：还有下一页就追加加载 */
onReachBottom(() => {
	if (hasMore.value) {
		loadTasks(false)
	}
})

/** 每次展示都重拉：在别处改了任务状态后回到本页，列表是新的 */
onShow(() => {
	loadTasks()
})
</script>

<template>
	<sub-page :title="quadrant.name" :subtitle="subtitle">
		<view class="section">
			<view class="section__head">
				<view class="section__bar" :style="{ backgroundColor: quadrant.color }" />
				<text class="section__count">{{ tasks.length }} 件待完成</text>
			</view>

			<view v-if="tasks.length" class="tasks">
				<view v-for="task in tasks" :key="task.id" class="task" @click="enterDetail(task)">
					<view class="task__check" :class="{ 'task__check--done': task.done }" @click.stop="toggleTask(task)">
						<y-icon v-if="task.done" name="check" size="24rpx" color="#ffffff" />
					</view>

					<text class="task__title" :class="{ 'task__title--done': task.done }">{{ task.title }}</text>
					<text class="task__time">{{ task.time }}</text>
				</view>
			</view>

			<view v-else class="empty">
				<text class="empty__title">这一格还没有任务</text>
				<text class="empty__desc">把要事放进来，先从这里开始。</text>
			</view>
		</view>

		<view v-if="tasks.length" class="more">
			<text class="more__text">{{ footText }}</text>
		</view>

		<view class="note">
			<y-icon name="bulb" size="30rpx" />
			<text class="note__text">{{ quadrant.tip }}——勾掉一件，这一格就轻一点。</text>
		</view>
	</sub-page>
</template>

<style scoped lang="scss">
.section {
	margin-top: 32rpx;
}

.section__head {
	display: flex;
	align-items: center;
}

.section__bar {
	width: 6rpx;
	height: 28rpx;
	margin-right: 12rpx;
	border-radius: 999rpx;
}

.section__count {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
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

/* 列表页脚 */
.more {
	display: flex;
	justify-content: center;
	padding: 32rpx 0 8rpx;
}

.more__text {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 提示 */
.note {
	display: flex;
	align-items: center;
	margin-top: 32rpx;
	padding: 28rpx;
	border-radius: 24rpx;
	background-color: $y-green-soft;
}

.note__text {
	flex: 1;
	margin-left: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 36rpx;
	color: $y-green-text;
}
</style>
