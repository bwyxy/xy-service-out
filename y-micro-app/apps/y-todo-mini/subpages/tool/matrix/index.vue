<script setup>

	/**
	 * 工具 · 四象限
	 *
	 * 路由：subpages/tool/matrix/index（分包 subpages/tool，按需加载）
	 * 职责：四象限页，先做真正重要的事——按紧急与重要程度分四格展示任务。
	 * 目录：状态（QUADRANTS）→ 方法（openQuadrant）→ 样式（.matrix、.quadrant、.note）
	 */

/** 四象限（暂用本地数据，后续接入接口） */
const QUADRANTS = [
	{
		key: 'urgent-important',
		name: '重要且紧急',
		tip: '立刻去做',
		color: '#c2793c',
		bg: '#faf1e7',
		tasks: ['和设计团队对齐首页方案', '汇总本周用户反馈']
	},
	{
		key: 'important',
		name: '重要不紧急',
		tip: '安排时间',
		color: '#4a6b3e',
		bg: '#eaf0e0',
		tasks: ['整理产品改版思路', '学习 LiveKit 部署文档']
	},
	{
		key: 'urgent',
		name: '紧急不重要',
		tip: '尽快处理',
		color: '#a98a2f',
		bg: '#f6f2e2',
		tasks: ['回复合作方邮件']
	},
	{
		key: 'other',
		name: '不重要不紧急',
		tip: '有空再说',
		color: '#8e8e85',
		bg: '#f0f0ea',
		tasks: ['整理桌面收藏夹']
	}
]

/** 打开象限详情（待接入） */
function openQuadrant(name) {
	uni.showToast({ title: `${name}（待开发）`, icon: 'none' })
}
</script>

<template>
	<sub-page title="四象限" subtitle="先做真正重要的事。">
		<view class="matrix">
			<view
				v-for="item in QUADRANTS"
				:key="item.key"
				class="quadrant"
				:style="{ backgroundColor: item.bg }"
				@click="openQuadrant(item.name)"
			>
				<view class="quadrant__head">
					<view class="quadrant__bar" :style="{ backgroundColor: item.color }" />
					<text class="quadrant__name">{{ item.name }}</text>
				</view>

				<text class="quadrant__tip" :style="{ color: item.color }">{{ item.tip }}</text>

				<view class="quadrant__list">
					<text v-for="task in item.tasks" :key="task" class="quadrant__task">{{ task }}</text>
					<text v-if="!item.tasks.length" class="quadrant__empty">还没有任务</text>
				</view>

				<text class="quadrant__count">{{ item.tasks.length }} 件</text>
			</view>
		</view>

		<view class="note">
			<y-icon name="bulb" size="30rpx" />
			<text class="note__text">重要的先做，紧急的其次——顺序对了，一天就顺了。</text>
		</view>
	</sub-page>
</template>

<style scoped lang="scss">
.matrix {
	display: flex;
	flex-wrap: wrap;
	margin-top: 32rpx;
}

.quadrant {
	display: flex;
	flex-direction: column;
	width: calc(50% - 10rpx);
	min-height: 320rpx;
	margin-bottom: 20rpx;
	padding: 28rpx 24rpx;
	box-sizing: border-box;
	border-radius: 28rpx;

	&:nth-child(2n + 1) {
		margin-right: 20rpx;
	}
}

.quadrant__head {
	display: flex;
	align-items: center;
}

.quadrant__bar {
	width: 6rpx;
	height: 28rpx;
	margin-right: 12rpx;
	border-radius: 999rpx;
}

.quadrant__name {
	font-size: 28rpx;
	font-weight: 600;
	color: $y-text-main;
}

.quadrant__tip {
	margin-top: 12rpx;
	font-size: 22rpx;
}

.quadrant__list {
	flex: 1;
	margin-top: 24rpx;
}

.quadrant__task {
	display: block;
	margin-bottom: 14rpx;
	font-size: 24rpx;
	line-height: 34rpx;
	color: #5c5c55;
}

.quadrant__empty {
	font-size: 24rpx;
	color: $y-text-muted;
}

.quadrant__count {
	font-size: 22rpx;
	color: $y-text-muted;
}

/* 提示 */
.note {
	display: flex;
	align-items: center;
	margin-top: 24rpx;
	padding: 28rpx;
	border-radius: 24rpx;
	background-color: $y-green-soft;
}

.note__text {
	flex: 1;
	margin-left: 14rpx;
	font-size: 24rpx;
	line-height: 36rpx;
	color: #5c6b52;
}
</style>
