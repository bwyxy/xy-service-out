<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'

import { archiveTask, completeTask, getTaskDetail, removeTask } from '@/api/task'
import { formatDueLabel, formatHistoryLabel, toTimestamp } from '@/utils/date'
import { quadrantOf } from '@/utils/quadrant'

	/**
	 * 通用 · 任务详情
	 *
	 * 路由：subpages/common/detail/index?taskId=123（分包 subpages/common，按需加载）
	 * 职责：把一件事的来龙去脉放齐——状态、清单、时间、提醒、重复、四象限与备注，
	 *       并在这一页完成/取消完成、编辑、归档/取消归档与删除。
	 * 数据：详情来自 GET /task/{taskId}；完成后回到列表页，列表页 onShow 会自行刷新。
	 * 入口：各列表页点任务行进这里（点左侧圆圈仍是勾选完成）。
	 * 目录：状态（taskId、task、loading、notFound、submitting、requestSeq）
	 *       → 计算（statusText、statusKey、subtitle、remindText、rows、canArchive、archiveText）
	 *       → 方法（loadDetail、toggleComplete、toggleArchive、editTask、removeCurrent）
	 *       → 样式（.badges、.card、.row、.footer）
	 */

/** 提醒提前分钟数 → 文案，与新建页的 REMIND_OPTIONS 保持一致 */
const REMIND_LABELS = {
	0: '不提醒',
	5: '提前 5 分钟',
	10: '提前 10 分钟',
	30: '提前 30 分钟',
	60: '提前 1 小时',
	1440: '提前 1 天'
}

/** 重复规则 → 文案，空串表示不重复 */
const REPEAT_LABELS = {
	DAILY: '每天',
	WEEKLY: '每周',
	MONTHLY: '每月'
}

/** 优先级 → 文案 */
const PRIORITY_LABELS = {
	0: '不设置',
	1: '普通',
	2: '重要'
}

/** 任务 Id，来自路由参数 */
const taskId = ref(null)

/** 任务详情 */
const task = ref(null)

/** 加载中标记 */
const loading = ref(false)

/** 任务已不存在（被删掉或凭 URL 直接打开），展示空态而不是一直转圈 */
const notFound = ref(false)

/** 提交中标记，挡住连点 */
const submitting = ref(false)

/** 请求序号：快速返回时丢弃过期响应 */
let requestSeq = 0

/** 页面大标题 */
const pageTitle = computed(() => (task.value ? task.value.title : '任务详情'))

/** 状态文案 */
const statusText = computed(() => {
	if (!task.value) {
		return '未完成'
	}

	if (task.value.archived === 1) {
		return '已归档'
	}

	return task.value.completed === 1 ? '已完成' : '未完成'
})

/** 状态对应的徽标配色 key */
const statusKey = computed(() => (task.value && task.value.archived === 1 ? 'archived' : task.value && task.value.completed === 1 ? 'done' : 'pending'))

/** 页面副标题 */
const subtitle = computed(() => (task.value ? `${statusText.value} · ${task.value.tagName || '未归类'}` : '看清这件事的全貌。'))

/** 提醒文案：设了提醒才带上具体的下次提醒时间 */
const remindText = computed(() => {
	if (!task.value) {
		return '不提醒'
	}

	const label = REMIND_LABELS[task.value.remindBefore] || '不提醒'

	if (task.value.remindBefore > 0 && task.value.remindAt) {
		return `${label} · ${formatHistoryLabel(task.value.remindAt)}`
	}

	return label
})

/** 信息行：有值才拼进来，避免详情页出现一排「无」 */
const rows = computed(() => {
	const item = task.value

	if (!item) {
		return []
	}

	const list = [
		{ label: '所属清单', value: item.tagName || '未归类' },
		{ label: '日期时间', value: formatDueLabel(toTimestamp(item.dueDate), item.completed === 1) },
		{ label: '提醒', value: remindText.value },
		{ label: '重复', value: REPEAT_LABELS[item.repeatRule] || '不重复' },
		{ label: '四象限', value: quadrantOf(item.urgencyLevel).name },
		{ label: '优先级', value: PRIORITY_LABELS[item.priority] || '不设置' },
		{ label: '创建于', value: formatHistoryLabel(item.createTime) }
	]

	if (item.completed === 1 && item.completionTime) {
		list.push({ label: '完成于', value: formatHistoryLabel(item.completionTime) })
	}

	if (item.archived === 1 && item.archiveTime) {
		list.push({ label: '归档于', value: formatHistoryLabel(item.archiveTime) })
	}

	return list
})

/** 是否可归档 / 取消归档：只有已完成的任务能归档 */
const canArchive = computed(() => !!task.value && task.value.completed === 1)

/** 归档按钮文案：已归档的点一下就回到已完成分组 */
const archiveText = computed(() => (task.value && task.value.archived === 1 ? '取消归档' : '归档'))

/** 拉取任务详情 */
async function loadDetail() {
	if (taskId.value == null) {
		notFound.value = true

		return
	}

	const seq = (requestSeq += 1)

	loading.value = true

	try {
		const res = await getTaskDetail(taskId.value)

		if (seq !== requestSeq) {
			return
		}

		if (res.code === 200 && res.data) {
			task.value = res.data
			notFound.value = false
		} else {
			task.value = null
			notFound.value = true
		}
	} catch (error) {
		console.warn('[detail] 加载任务详情失败', error)
	} finally {
		if (seq === requestSeq) {
			loading.value = false
		}
	}
}

/** 完成 / 取消完成：传目标状态，提交成功后重拉一次拿最新的完成时间 */
async function toggleComplete() {
	if (!task.value || submitting.value) {
		return
	}

	submitting.value = true

	try {
		const target = task.value.completed === 1 ? 0 : 1
		const res = await completeTask({ taskId: task.value.taskId, completed: target })

		if (res.code === 200) {
			uni.showToast({ title: target === 1 ? '已完成' : '已取消完成', icon: 'none' })
			await loadDetail()
		}
	} catch (error) {
		console.warn('[detail] 标记任务失败', error)
	} finally {
		submitting.value = false
	}
}

/** 归档 / 取消归档 */
async function toggleArchive() {
	if (!task.value || submitting.value) {
		return
	}

	submitting.value = true

	try {
		const target = task.value.archived === 1 ? 0 : 1
		const res = await archiveTask({ taskId: task.value.taskId, archived: target })

		if (res.code === 200) {
			uni.showToast({ title: target === 1 ? '已归档' : '已取消归档', icon: 'none' })
			await loadDetail()
		}
	} catch (error) {
		console.warn('[detail] 归档任务失败', error)
	} finally {
		submitting.value = false
	}
}

/** 编辑：跳创建页的编辑态，任务对象用 eventChannel 带过去（详情字段比列表更全） */
function editTask() {
	if (!task.value) {
		return
	}

	const item = task.value

	uni.navigateTo({
		url: '/subpages/common/create/index?mode=edit',
		success: (res) => {
			if (!res.eventChannel) {
				return
			}

			res.eventChannel.emit('task', {
				taskId: item.taskId,
				title: item.title,
				description: item.description,
				tagId: item.tagId,
				tagName: item.tagName,
				priority: item.priority,
				urgencyLevel: item.urgencyLevel,
				remindBefore: item.remindBefore,
				repeatRule: item.repeatRule,
				dueDate: item.dueDate
			})
		}
	})
}

/** 删除（逻辑删除，二次确认后返回列表） */
async function removeCurrent() {
	if (!task.value || submitting.value) {
		return
	}

	const confirmed = await new Promise((resolve) => {
		uni.showModal({
			title: '删除任务',
			content: `「${task.value.title}」会从所有列表里移除。`,
			confirmText: '删除',
			confirmColor: '#b5543f',
			success: (res) => resolve(!!res.confirm),
			fail: () => resolve(false)
		})
	})

	if (!confirmed) {
		return
	}

	submitting.value = true

	try {
		const res = await removeTask(task.value.taskId)

		if (res.code !== 200) {
			return
		}

		uni.showToast({ title: '已删除', icon: 'none' })
		setTimeout(() => uni.navigateBack(), 600)
	} catch (error) {
		console.warn('[detail] 删除任务失败', error)
	} finally {
		submitting.value = false
	}
}

onLoad((query) => {
	const value = Number(query && query.taskId)

	if (Number.isFinite(value) && value > 0) {
		taskId.value = value
	}
})

/** 每次展示都重拉：从编辑页返回后这一页也是新的 */
onShow(() => {
	loadDetail()
})
</script>

<template>
	<sub-page :title="pageTitle" :subtitle="subtitle" :fixed-footer="!!task">
		<view v-if="task" class="detail">
			<view class="badges">
				<view class="badge" :class="`badge--${statusKey}`">
					<text class="badge__text">{{ statusText }}</text>
				</view>

				<view v-if="task.priority > 0" class="badge badge--flag">
					<y-icon name="flag" size="26rpx" color="#a05a3f" />
					<text class="badge__text">{{ PRIORITY_LABELS[task.priority] }}</text>
				</view>
			</view>

			<view class="card">
				<view v-for="row in rows" :key="row.label" class="row">
					<text class="row__label">{{ row.label }}</text>
					<text class="row__value">{{ row.value }}</text>
				</view>
			</view>

			<view v-if="task.description" class="card card--note">
				<view class="note__head">
					<text class="card__title">备注</text>
					<y-icon name="align" size="32rpx" color="#a9a9a0" />
				</view>
				<text class="note__text">{{ task.description }}</text>
			</view>

			<view class="tip">
				<y-icon name="bulb" size="30rpx" />
				<text class="tip__text">重复任务在完成时会自动生成下一条，取消完成会把它收回去。</text>
			</view>
		</view>

		<view v-else-if="notFound && !loading" class="empty">
			<text class="empty__title">这件事找不到了</text>
			<text class="empty__desc">它可能已经被删除，回列表看看吧。</text>
		</view>

		<template #footer>
			<view v-if="task" class="footer">
				<view class="footer__main" @click="toggleComplete">
					<y-icon name="check" size="32rpx" color="#ffffff" />
					<text class="footer__main-text">{{ task.completed === 1 ? '标记为未完成' : '标记为已完成' }}</text>
				</view>

				<view class="footer__row">
					<view class="footer__ghost" @click="editTask">
						<text class="footer__ghost-text">编辑</text>
					</view>

					<view v-if="canArchive" class="footer__ghost" @click="toggleArchive">
						<text class="footer__ghost-text">{{ archiveText }}</text>
					</view>

					<view class="footer__ghost footer__ghost--danger" @click="removeCurrent">
						<text class="footer__ghost-text footer__ghost-text--danger">删除</text>
					</view>
				</view>
			</view>
		</template>
	</sub-page>
</template>

<style scoped lang="scss">
/* 状态徽标 */
.badges {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	margin-top: 32rpx;
}

.badge {
	display: flex;
	align-items: center;
	margin: 0 16rpx 12rpx 0;
	padding: 8rpx 22rpx;
	border-radius: 999rpx;
	background-color: $y-green-tag-bg;
}

.badge__text {
	margin-left: 6rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-green-tag-text;
}

.badge--done {
	background-color: $y-green-soft;

	.badge__text {
		color: $y-green-text;
	}
}

.badge--archived {
	background-color: $y-sunken-bg;

	.badge__text {
		color: $y-text-muted;
	}
}

.badge--flag {
	background-color: $y-warm-soft-bg;

	.badge__text {
		color: $y-accent-warm;
	}
}

/* 信息卡 */
.card {
	margin-top: 28rpx;
	padding: 8rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.row {
	display: flex;
	align-items: flex-start;
	padding: 26rpx 0;

	& + .row {
		border-top: 2rpx solid $y-divider;
	}
}

.row__label {
	flex-shrink: 0;
	width: 140rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.row__value {
	flex: 1;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	line-height: 40rpx;
	color: $y-text-main;
	word-break: break-all;
}

/* 备注 */
.card--note {
	padding: 28rpx 32rpx;
}

.note__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.card__title {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.note__text {
	display: block;
	margin-top: 16rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	line-height: 44rpx;
	color: $y-text-main;
}

/* 提示 */
.tip {
	display: flex;
	align-items: flex-start;
	margin-top: 32rpx;
	padding: 24rpx 28rpx;
	border-radius: 20rpx;
	background-color: $y-green-soft;
}

.tip__text {
	flex: 1;
	margin-left: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 1.6;
	color: $y-green-text;
}

/* 空状态 */
.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 160rpx;
}

.empty__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 14rpx;
	font-size: calc(25rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 底部操作 */
.footer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 24rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
	background-color: $y-page-bg;
}

.footer__main {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	border-radius: 24rpx;
	background-color: $y-green-primary;
}

.footer__main-text {
	margin-left: 10rpx;
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: #ffffff;
}

.footer__row {
	display: flex;
	align-items: center;
	margin-top: 16rpx;
}

.footer__ghost {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 76rpx;
	margin-right: 20rpx;
	border-radius: 20rpx;
	background-color: $y-card-bg;

	&:last-child {
		margin-right: 0;
	}
}

.footer__ghost-text {
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.footer__ghost-text--danger {
	color: $y-danger;
}
</style>
