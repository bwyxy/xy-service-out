<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import { checkHabit, listHabits } from '@/api/habit'

	/**
	 * 工具 · 习惯打卡
	 *
	 * 路由：subpages/tool/habit/index（分包 subpages/tool，按需加载）
	 * 职责：习惯打卡页，让小坚持慢慢生长——本周概览、习惯列表与逐日打卡格子。
	 * 数据：习惯来自 GET /habit/list，格子点击走 POST /habit/{habitId}/check（同一天再点一次即取消）。
	 * 目录：状态（habits、togglingIds、loading、requestSeq）→ 计算（todayDone、weekRate、bestStreak）
	 *       → 方法（toHabitItem、freqText、weekDateStr、loadHabits、toggle、openEditor）
	 *       → 样式（.summary、.habit、.days、.foot）
	 */

/** 周标签，索引 0 为周一 */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 今天在本周中的索引 */
const todayIndex = (new Date().getDay() + 6) % 7

/** 习惯列表 */
const habits = ref([])

/** 正在提交打卡的习惯 Id，挡住连点 */
const togglingIds = ref([])

/** 加载中标记，避免重复请求 */
const loading = ref(false)

/** 请求序号：快速返回时丢弃过期响应 */
let requestSeq = 0

/** 今日已完成习惯数 */
const todayDone = computed(() => habits.value.filter((item) => item.days[todayIndex]).length)

/** 本周总打卡格子数 */
const weekTotal = computed(() => habits.value.length * 7)

/** 本周已打卡格子数 */
const weekDone = computed(() =>
	habits.value.reduce((sum, item) => sum + item.days.filter(Boolean).length, 0)
)

/** 本周打卡率 */
const weekRate = computed(() => (weekTotal.value ? Math.round((weekDone.value / weekTotal.value) * 100) : 0))

/** 最长连续天数 */
const bestStreak = computed(() =>
	habits.value.reduce((best, item) => Math.max(best, item.streak || 0), 0)
)

/** 后端习惯 → 页面习惯项（weekDays 用 days 承接，索引 0 为周一） */
function toHabitItem(item) {
	return {
		id: item.habitId,
		name: item.name || '',
		icon: item.icon || 'sprout',
		color: item.color || '',
		freqType: item.freqType || 'daily',
		freqDays: Array.isArray(item.freqDays) ? item.freqDays : [],
		targetDays: item.targetDays || 0,
		streak: item.streak || 0,
		days: Array.isArray(item.weekDays) ? item.weekDays.slice(0, 7) : new Array(7).fill(false)
	}
}

/** 频率文案：每天 / 每周一、三、五（设了目标天数再补一句） */
function freqText(habit) {
	const base =
		habit.freqType === 'weekly' && habit.freqDays.length
			? `每周${habit.freqDays.map((day) => WEEK_LABELS[day - 1]).join('、')}`
			: '每天'

	return habit.targetDays > 0 ? `${base} · 目标 ${habit.targetDays} 天` : base
}

/** 本周第 index 天的日期（yyyy-MM-dd，索引 0 为周一），打卡时作为 logDate 传给后端 */
function weekDateStr(index) {
	const date = new Date()

	date.setHours(0, 0, 0, 0)
	date.setDate(date.getDate() - todayIndex + index)

	const month = String(date.getMonth() + 1).padStart(2, '0')
	const day = String(date.getDate()).padStart(2, '0')

	return `${date.getFullYear()}-${month}-${day}`
}

/** 拉取习惯列表（未登录时请求层会先拉起登录半弹框） */
async function loadHabits() {
	const seq = (requestSeq += 1)

	loading.value = true

	try {
		const res = await listHabits()

		if (seq !== requestSeq || res.code !== 200 || !Array.isArray(res.data)) {
			return
		}

		habits.value = res.data.map(toHabitItem)
	} catch (error) {
		console.warn('[habit] 加载习惯失败', error)
	} finally {
		if (seq === requestSeq) {
			loading.value = false
		}
	}
}

/** 打卡 / 取消打卡：提交成功后再用后端返回的最新状态刷新这一行 */
async function toggle(habit, index) {
	if (index > todayIndex) {
		uni.showToast({ title: '还没到那一天', icon: 'none' })

		return
	}

	if (togglingIds.value.includes(habit.id)) {
		return
	}

	togglingIds.value = togglingIds.value.concat(habit.id)

	try {
		const res = await checkHabit(habit.id, weekDateStr(index))

		if (res.code === 200 && res.data) {
			const latest = toHabitItem(res.data)

			habit.days = latest.days
			habit.streak = latest.streak
		}
	} catch (error) {
		console.warn('[habit] 打卡失败', error)
	} finally {
		togglingIds.value = togglingIds.value.filter((id) => id !== habit.id)
	}
}

/** 打开新增 / 编辑页：编辑态把习惯对象带过去回显，不用再查一次接口 */
function openEditor(habit) {
	uni.navigateTo({
		url: `/subpages/tool/habit/edit/index?mode=${habit ? 'edit' : 'create'}`,
		success: (res) => {
			if (habit && res.eventChannel) {
				res.eventChannel.emit('habit', habit)
			}
		}
	})
}

onShow(() => {
	loadHabits()
})
</script>

<template>
	<sub-page title="习惯打卡" subtitle="让小坚持慢慢生长。">
		<view class="summary">
			<view class="summary__item">
				<text class="summary__value">{{ bestStreak }}</text>
				<text class="summary__label">最长连续 / 天</text>
			</view>
			<view class="summary__item">
				<text class="summary__value">{{ weekRate }}%</text>
				<text class="summary__label">本周打卡率</text>
			</view>
			<view class="summary__item">
				<text class="summary__value">{{ todayDone }}/{{ habits.length }}</text>
				<text class="summary__label">今日已完成</text>
			</view>
		</view>

		<view class="section">
			<view class="section__head">
				<text class="section__title">我的习惯</text>

				<view class="section__add" @click="openEditor(null)">
					<y-icon name="plus" size="28rpx" />
					<text class="section__add-text">添加</text>
				</view>
			</view>

			<view v-for="habit in habits" :key="habit.id" class="habit">
				<view class="habit__head" @click="openEditor(habit)">
					<view class="habit__icon">
						<y-icon :name="habit.icon" size="34rpx" :color="habit.color || undefined" />
					</view>

					<view class="habit__meta">
						<text class="habit__name">{{ habit.name }}</text>
						<text class="habit__freq">{{ freqText(habit) }}</text>
					</view>

					<view class="habit__streak">
						<y-icon name="flame" size="28rpx" color="#c2793c" />
						<text class="habit__streak-text">{{ habit.streak }} 天</text>
					</view>

					<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
				</view>

				<view class="days">
					<view
						v-for="(done, index) in habit.days"
						:key="index"
						class="days__cell"
						:class="{
							'days__cell--done': done,
							'days__cell--today': index === todayIndex
						}"
						@click="toggle(habit, index)"
					>
						<y-icon v-if="done" name="check" size="24rpx" color="#ffffff" />
						<text v-else class="days__label">{{ WEEK_LABELS[index] }}</text>
					</view>
				</view>
			</view>

			<view v-if="!habits.length && !loading" class="empty">
				<text class="empty__title">还没有在坚持的习惯</text>
				<text class="empty__desc">挑一件小事，从今天开始记第一笔。</text>
			</view>
		</view>

		<text class="foot">不必每天都做到，做到就值得记一笔。</text>
	</sub-page>
</template>

<style scoped lang="scss">
/* 概览 */
.summary {
	display: flex;
	align-items: center;
	margin-top: 32rpx;
	padding: 36rpx 0;
	border-radius: 32rpx;
	background-color: $y-green-soft;
}

.summary__item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;

	& + & {
		border-left: 2rpx solid $y-green-border;
	}
}

.summary__value {
	font-size: calc(44rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.summary__label {
	margin-top: 10rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

/* 习惯列表 */
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

.habit {
	margin-top: 24rpx;
	padding: 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.habit__head {
	display: flex;
	align-items: center;
}

.habit__icon {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 64rpx;
	height: 64rpx;
	border-radius: 20rpx;
	background-color: $y-green-soft;
}

.habit__meta {
	flex: 1;
	display: flex;
	flex-direction: column;
	margin-left: 20rpx;
	min-width: 0;
}

.habit__name {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.habit__freq {
	margin-top: 8rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.habit__streak {
	display: flex;
	align-items: center;
	margin-right: 12rpx;
}

.habit__streak-text {
	margin-left: 8rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-flame;
}

.days {
	display: flex;
	justify-content: space-between;
	margin-top: 24rpx;
}

.days__cell {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 64rpx;
	height: 64rpx;
	border-radius: 18rpx;
	background-color: $y-sunken-bg;
}

.days__cell--done {
	background-color: $y-green-primary;
}

.days__cell--today {
	border: 2rpx solid $y-green-border;
}

.days__label {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 空状态 */
.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 24rpx;
	padding: 60rpx 0;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.empty__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 12rpx;
	font-size: calc(25rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 页脚 */
.foot {
	display: block;
	margin-top: 56rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
	text-align: center;
}
</style>
