<script setup>
import { computed } from 'vue'

	/**
	 * 工具 · 待办统计
	 *
	 * 路由：subpages/tool/stats/index（分包 subpages/tool，按需加载）
	 * 职责：待办统计页，看见每一步的积累——本周完成柱状图与清单分布。
	 * 目录：状态（WEEK_DATA、CATEGORIES、bars）→ 方法（openDetail）→ 样式（.summary、.card、.chart、.dist）
	 */

/** 周标签，索引 0 为周一 */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 本周每日完成数（暂用本地数据，后续接入接口） */
const WEEK_DATA = [3, 5, 2, 6, 4, 3, 0]

/** 清单分布（暂用本地数据） */
const CATEGORIES = [
	{ id: 'work', name: '工作', count: 12, color: '#4a6b3e' },
	{ id: 'life', name: '生活', count: 8, color: '#7f9a6b' },
	{ id: 'read', name: '阅读', count: 5, color: '#a8bd93' },
	{ id: 'growth', name: '个人成长', count: 3, color: '#cbd9ba' }
]

/** 本周完成总数 */
const weekTotal = computed(() => WEEK_DATA.reduce((sum, value) => sum + value, 0))

/** 完成率（暂用本地数据） */
const weekRate = 78

/** 连续记录天数（暂用本地数据） */
const streakDays = 12

/** 柱状图数据，含高度百分比 */
const bars = computed(() => {
	const max = Math.max(...WEEK_DATA)

	return WEEK_DATA.map((value, index) => ({
		key: WEEK_LABELS[index],
		label: WEEK_LABELS[index],
		value,
		height: max ? Math.round((value / max) * 100) : 0
	}))
})

/** 清单总任务数 */
const categoryTotal = computed(() => CATEGORIES.reduce((sum, item) => sum + item.count, 0))

/** 打开统计详情（待接入） */
function openDetail(label) {
	uni.showToast({ title: `${label}（待开发）`, icon: 'none' })
}
</script>

<template>
	<sub-page title="待办统计" subtitle="看见每一步的积累。">
		<view class="summary">
			<view class="summary__item">
				<text class="summary__value">{{ weekTotal }}</text>
				<text class="summary__label">本周完成</text>
			</view>
			<view class="summary__item">
				<text class="summary__value">{{ weekRate }}%</text>
				<text class="summary__label">完成率</text>
			</view>
			<view class="summary__item">
				<text class="summary__value">{{ streakDays }}</text>
				<text class="summary__label">连续记录 / 天</text>
			</view>
		</view>

		<view class="card">
			<view class="card__head">
				<text class="card__title">本周完成</text>
				<text class="card__sub">共 {{ weekTotal }} 件</text>
			</view>

			<view class="chart">
				<view v-for="bar in bars" :key="bar.key" class="chart__col">
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

			<view
				v-for="item in CATEGORIES"
				:key="item.id"
				class="dist"
				@click="openDetail(item.name)"
			>
				<view class="dist__head">
					<text class="dist__name">{{ item.name }}</text>
					<text class="dist__count">{{ item.count }} 件</text>
				</view>

				<view class="dist__track">
					<view
						class="dist__fill"
						:style="{
							width: Math.round((item.count / categoryTotal) * 100) + '%',
							backgroundColor: item.color
						}"
					/>
				</view>
			</view>
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
	font-size: 30rpx;
	font-weight: 600;
	color: $y-text-main;
}

.card__sub {
	font-size: 24rpx;
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
	font-size: 22rpx;
	color: $y-text-muted;
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
	font-size: 28rpx;
	color: $y-text-main;
}

.dist__count {
	font-size: 24rpx;
	color: $y-text-muted;
}

.dist__track {
	height: 10rpx;
	margin-top: 16rpx;
	border-radius: 999rpx;
	background-color: #f0f0ea;
}

.dist__fill {
	height: 100%;
	border-radius: 999rpx;
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
