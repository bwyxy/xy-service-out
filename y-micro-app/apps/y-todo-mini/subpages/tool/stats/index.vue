<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import { getTagDistribution, getWeekStats } from '@/api/stats'
import { appearance } from '@/utils/appearance'
import { dayDiff, parseDateTime, weekdayChar } from '@/utils/date'

	/**
	 * 工具 · 待办统计
	 *
	 * 路由：subpages/tool/stats/index（分包 subpages/tool，按需加载）
	 * 职责：待办统计页，看见每一步的积累——本周概览、本周完成柱状图与清单分布。
	 * 数据：概览与柱状图来自 GET /stats/week（周起始跟随外观设置里的周起始），
	 *       清单分布来自 GET /stats/distribution；两组都读后端算好的聚合值，前端不做统计。
	 * 目录：状态（week、distribution）→ 计算（bars、categories、categoryTotal）
	 *       → 方法（loadStats）→ 样式（.summary、.card、.chart、.dist）
	 */

/** 清单没配主题色时的兜底配色：绿系四档，浅色与深色模式下都有对比度 */
const DIST_COLORS = ['#4a6b3e', '#7f9a6b', '#a8bd93', '#cbd9ba']

/** 本周统计：先给 0，避免数字还没回来时页面空一片 */
const week = ref({ total: 0, rate: 0, streakDays: 0, days: [] })

/** 清单分布（后端已按任务数倒序） */
const distribution = ref([])

/** 柱状图：以本周最高的那天为满格，本周一件没完成时都不给高度 */
const bars = computed(() => {
	const days = week.value.days || []
	const max = days.reduce((top, day) => Math.max(top, Number(day.total) || 0), 0)

	return days.map((day) => {
		const value = Number(day.total) || 0

		return {
			key: day.date,
			label: weekdayChar(day.date),
			today: dayDiff(parseDateTime(day.date)) === 0,
			height: max ? Math.round((value / max) * 100) : 0
		}
	})
})

/** 清单分布总数 */
const categoryTotal = computed(() =>
	distribution.value.reduce((sum, item) => sum + (Number(item.total) || 0), 0))

/** 清单分布行：颜色与占比都在这算好，模板里不再做除法和兜底 */
const categories = computed(() => {
	const total = categoryTotal.value

	return distribution.value.map((item, index) => {
		const count = Number(item.total) || 0

		return {
			id: item.tagId == null ? 'untagged' : `tag-${item.tagId}`,
			name: item.tagName || '未归类',
			count,
			color: item.color || DIST_COLORS[index % DIST_COLORS.length],
			percent: total ? Math.round((count / total) * 100) : 0
		}
	})
})

/** 拉两块统计：并行发，等两边都回来再渲染，免得先闪一块 0 出来 */
async function loadStats() {
	try {
		const [weekRes, distRes] = await Promise.all([
			// 周起始跟随外观设置：true-周一开始 false-周日开始
			getWeekStats(appearance.weekStart ? 1 : 0),
			getTagDistribution()
		])

		if (weekRes.code === 200 && weekRes.data) {
			week.value = weekRes.data
		}

		if (distRes.code === 200 && Array.isArray(distRes.data)) {
			distribution.value = distRes.data
		}
	} catch (error) {
		// 网络异常已由 request 统一 toast，这里只保证不把异常抛到页面外
	}
}

/** 每次展示都重拉：在别处完成或删除任务后回到这页，数字与分布都是新的 */
onShow(loadStats)
</script>

<template>
	<sub-page title="待办统计" subtitle="看见每一步的积累。">
		<view class="summary">
			<view class="summary__item">
				<text class="summary__value">{{ week.total }}</text>
				<text class="summary__label">本周完成</text>
			</view>
			<view class="summary__item">
				<text class="summary__value">{{ week.rate }}%</text>
				<text class="summary__label">完成率</text>
			</view>
			<view class="summary__item">
				<text class="summary__value">{{ week.streakDays }}</text>
				<text class="summary__label">连续记录 / 天</text>
			</view>
		</view>

		<view class="card">
			<view class="card__head">
				<text class="card__title">本周完成</text>
				<text class="card__sub">共 {{ week.total }} 件</text>
			</view>

			<view class="chart">
				<view
					v-for="bar in bars"
					:key="bar.key"
					class="chart__col"
					:class="{ 'chart__col--today': bar.today }"
				>
					<view class="chart__track">
						<view class="chart__bar" :style="{ height: bar.height + '%' }" />
					</view>
					<text class="chart__label">{{ bar.label }}</text>
				</view>
			</view>
		</view>

		<view class="card">
			<view class="card__head">
				<text class="card__title">清单分布</text>
				<text class="card__sub">共 {{ categoryTotal }} 件</text>
			</view>

			<view v-for="item in categories" :key="item.id" class="dist">
				<view class="dist__head">
					<text class="dist__name">{{ item.name }}</text>
					<text class="dist__count">{{ item.count }} 件</text>
				</view>

				<view class="dist__track">
					<view
						class="dist__fill"
						:style="{ width: item.percent + '%', backgroundColor: item.color }"
					/>
				</view>
			</view>

			<text v-if="!categories.length" class="dist__empty">还没有任务，先建一条吧</text>
		</view>

		<text class="foot">数字不会催你，只是在陪你回头看。</text>
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

/* 卡片 */
.card {
	margin-top: 24rpx;
	padding: 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.card__head {
	display: flex;
	align-items: baseline;
	justify-content: space-between;
}

.card__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.card__sub {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 柱状图 */
.chart {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	height: 220rpx;
	margin-top: 32rpx;
}

.chart__col {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	height: 100%;
}

.chart__track {
	flex: 1;
	display: flex;
	align-items: flex-end;
	width: 36rpx;
}

.chart__bar {
	width: 100%;
	min-height: 6rpx;
	border-radius: 18rpx;
	background-color: $y-green-primary;
}

.chart__label {
	margin-top: 16rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 今天那一列标出来，方便和「今日」页对上 */
.chart__col--today .chart__label {
	font-weight: 600;
	color: $y-green-text;
}

/* 清单分布 */
.dist {
	margin-top: 32rpx;
}

.dist__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.dist__name {
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.dist__count {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.dist__track {
	height: 10rpx;
	margin-top: 16rpx;
	border-radius: 999rpx;
	background-color: $y-sunken-bg;
}

.dist__fill {
	height: 100%;
	border-radius: 999rpx;
}

.dist__empty {
	display: block;
	margin-top: 32rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
	text-align: center;
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
