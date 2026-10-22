<script setup>
import { computed, ref } from 'vue'

	/**
	 * 工具 · 习惯打卡
	 *
	 * 路由：subpages/tool/habit/index（分包 subpages/tool，按需加载）
	 * 职责：习惯打卡页，让小坚持慢慢生长——本周概览、习惯列表与逐日打卡格子。
	 * 目录：状态（habits、todayIndex、weekRate）→ 方法（toggle、addHabit）→ 样式（.summary、.habit、.days、.foot）
	 */

/** 周标签，索引 0 为周一 */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 今天在本周中的索引 */
const todayIndex = (new Date().getDay() + 6) % 7

/** 习惯列表（暂用本地数据，后续接入接口） */
const habits = ref([
	{ id: 1, name: '晨间拉伸 15 分钟', streak: 12, days: [true, true, true, true, true, false, false] },
	{ id: 2, name: '读 20 页书', streak: 5, days: [true, false, true, true, false, false, false] },
	{ id: 3, name: '十一点前睡觉', streak: 3, days: [false, true, true, false, false, false, false] },
	{ id: 4, name: '写三行日记', streak: 7, days: [true, true, true, true, false, false, false] }
])

/** 今日已完成习惯数 */
const todayDone = computed(() => habits.value.filter((item) => item.days[todayIndex]).length)

/** 本周总打卡格子数 */
const weekTotal = computed(() => habits.value.length * 7)

/** 本周已打卡格子数 */
const weekDone = computed(() =>
	habits.value.reduce((sum, item) => sum + item.days.filter(Boolean).length, 0)
)

/** 本周打卡率 */
const weekRate = computed(() => Math.round((weekDone.value / weekTotal.value) * 100))

/** 最长连续天数 */
const bestStreak = computed(() => Math.max(...habits.value.map((item) => item.streak)))

/** 打卡 / 取消打卡 */
function toggle(habit, index) {
	if (index > todayIndex) {
		uni.showToast({ title: '还没到那一天', icon: 'none' })

		return
	}

	habit.days[index] = !habit.days[index]
}

/** 新增习惯（待接入表单） */
function addHabit() {
	uni.showToast({ title: '添加习惯（待开发）', icon: 'none' })
}
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

				<view class="section__add" @click="addHabit">
					<y-icon name="plus" size="28rpx" />
					<text class="section__add-text">添加</text>
				</view>
			</view>

			<view v-for="habit in habits" :key="habit.id" class="habit">
				<view class="habit__head">
					<text class="habit__name">{{ habit.name }}</text>

					<view class="habit__streak">
						<y-icon name="flame" size="28rpx" color="#c2793c" />
						<text class="habit__streak-text">{{ habit.streak }} 天</text>
					</view>
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
		border-left: 2rpx solid #d3ddc6;
	}
}

.summary__value {
	font-size: 44rpx;
	font-weight: 600;
	color: $y-text-main;
}

.summary__label {
	margin-top: 10rpx;
	font-size: 22rpx;
	color: #6f8064;
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

.habit {
	margin-top: 24rpx;
	padding: 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.habit__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.habit__name {
	font-size: 30rpx;
	color: $y-text-main;
}

.habit__streak {
	display: flex;
	align-items: center;
}

.habit__streak-text {
	margin-left: 8rpx;
	font-size: 24rpx;
	color: #c2793c;
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
	background-color: #f2f2ec;
}

.days__cell--done {
	background-color: $y-green-primary;
}

.days__cell--today {
	border: 2rpx solid #c4d2b4;
}

.days__label {
	font-size: 24rpx;
	color: $y-text-muted;
}

/* 页脚 */
.foot {
	display: block;
	margin-top: 56rpx;
	font-size: 24rpx;
	color: $y-text-muted;
	text-align: center;
}
</style>
