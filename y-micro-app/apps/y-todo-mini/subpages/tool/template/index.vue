<script setup>

	/**
	 * 工具 · 清单模板
	 *
	 * 路由：subpages/tool/template/index（分包 subpages/tool，按需加载）
	 * 职责：清单模板页，晨间计划、旅行准备，从容开始——列出模板并可一键套用。
	 * 目录：状态（TEMPLATES）→ 方法（useTemplate）→ 样式（.tip、.list、.template、.foot）
	 */

/** 清单模板（暂用本地数据，后续接入接口） */
const TEMPLATES = [
	{
		id: 'morning',
		icon: 'sun',
		name: '晨间计划',
		desc: '喝水、拉伸、写下今天最重要的一件事',
		count: 5,
		category: '生活'
	},
	{
		id: 'travel',
		icon: 'briefcase',
		name: '旅行准备',
		desc: '证件、衣物、充电设备，出发前逐项确认',
		count: 8,
		category: '生活'
	},
	{
		id: 'review',
		icon: 'chart',
		name: '周复盘',
		desc: '回看本周完成的事，列出下周三个重点',
		count: 4,
		category: '工作'
	},
	{
		id: 'reading',
		icon: 'book',
		name: '读书笔记',
		desc: '摘抄、感受、可以马上用起来的一句话',
		count: 3,
		category: '阅读'
	}
]

/** 使用模板（待接入清单创建） */
function useTemplate(name) {
	uni.showToast({ title: `已套用「${name}」`, icon: 'none' })
}
</script>

<template>
	<sub-page title="清单模板" subtitle="晨间计划、旅行准备，从容开始。">
		<view class="tip">
			<y-icon name="bulb" size="30rpx" />
			<text class="tip__text">套用模板会新建一个清单，里面的任务可以随时增删。</text>
		</view>

		<view class="list">
			<view v-for="item in TEMPLATES" :key="item.id" class="template">
				<view class="template__icon">
					<y-icon :name="item.icon" size="44rpx" />
				</view>

				<view class="template__main">
					<view class="template__head">
						<text class="template__name">{{ item.name }}</text>
						<text class="template__count">{{ item.count }} 个任务</text>
					</view>

					<text class="template__desc">{{ item.desc }}</text>

					<view class="template__foot">
						<text class="template__tag">{{ item.category }}</text>

						<view class="template__use" @click="useTemplate(item.name)">
							<text class="template__use-text">使用</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<text class="foot">从一个现成的开始，比空着容易。</text>
	</sub-page>
</template>

<style scoped lang="scss">
/* 说明 */
.tip {
	display: flex;
	align-items: center;
	margin-top: 32rpx;
	padding: 28rpx;
	border-radius: 24rpx;
	background-color: $y-green-soft;
}

.tip__text {
	flex: 1;
	margin-left: 14rpx;
	font-size: 24rpx;
	line-height: 36rpx;
	color: #5c6b52;
}

/* 模板列表 */
.list {
	margin-top: 12rpx;
}

.template {
	display: flex;
	margin-top: 24rpx;
	padding: 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.template__icon {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 88rpx;
	height: 88rpx;
	margin-right: 24rpx;
	border-radius: 24rpx;
	background-color: $y-green-tag-bg;
}

.template__main {
	flex: 1;
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.template__head {
	display: flex;
	align-items: baseline;
	justify-content: space-between;
}

.template__name {
	font-size: 30rpx;
	font-weight: 500;
	color: $y-text-main;
}

.template__count {
	font-size: 22rpx;
	color: $y-text-muted;
}

.template__desc {
	margin-top: 12rpx;
	font-size: 24rpx;
	line-height: 34rpx;
	color: $y-text-sub;
}

.template__foot {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 24rpx;
}

.template__tag {
	padding: 6rpx 18rpx;
	border-radius: 999rpx;
	background-color: #f2f2ec;
	font-size: 22rpx;
	color: $y-text-sub;
}

.template__use {
	padding: 10rpx 30rpx;
	border-radius: 999rpx;
	background-color: $y-green-primary;
}

.template__use-text {
	font-size: 24rpx;
	color: #ffffff;
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
