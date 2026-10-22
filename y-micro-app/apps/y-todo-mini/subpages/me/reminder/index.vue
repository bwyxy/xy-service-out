<script setup>
import { ref } from 'vue'

	/**
	 * 我的 · 提醒设置
	 *
	 * 路由：subpages/me/reminder/index（分包 subpages/me，按需加载）
	 * 职责：提醒设置页，该做事的时候轻轻叫你一声——任务/每日/番茄提醒开关与时间类设置。
	 * 目录：状态（switches、times）→ 方法（toggle、pickTime）→ 样式（.group、.row、.tip）
	 */

/** 开关项（暂用本地数据，后续接入接口） */
const switches = ref([
	{ key: 'task', label: '任务到期提醒', desc: '到时间前提醒你该做的事', on: true },
	{ key: 'daily', label: '每日提醒', desc: '每天固定时间提醒看今天的安排', on: true },
	{ key: 'focus', label: '番茄结束提醒', desc: '专注一轮结束时轻轻提醒', on: false }
])

/** 时间类设置 */
const times = ref([
	{ key: 'dailyTime', label: '每日提醒时间', value: '08:00' },
	{ key: 'ahead', label: '默认提前提醒', value: '提前 10 分钟' },
	{ key: 'quiet', label: '免打扰时段', value: '22:00 - 08:00' }
])

/** 切换开关 */
function toggle(item, event) {
	item.on = event.detail.value
}

/** 修改时间（待接入时间选择器） */
function pickTime(label) {
	uni.showToast({ title: `设置${label}（待开发）`, icon: 'none' })
}
</script>

<template>
	<sub-page title="提醒设置" subtitle="该做事的时候，轻轻叫你一声。">
		<view class="group">
			<view v-for="item in switches" :key="item.key" class="row row--switch">
				<view class="row__text">
					<text class="row__label">{{ item.label }}</text>
					<text class="row__desc">{{ item.desc }}</text>
				</view>

				<switch :checked="item.on" color="#4a6b3e" @change="toggle(item, $event)" />
			</view>
		</view>

		<view class="group">
			<view v-for="item in times" :key="item.key" class="row" @click="pickTime(item.label)">
				<text class="row__label">{{ item.label }}</text>
				<text class="row__value">{{ item.value }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>
		</view>

		<view class="tip">
			<y-icon name="bell" size="30rpx" />
			<text class="tip__text">提醒只在你允许的时候送达，随时可以关掉。</text>
		</view>
	</sub-page>
</template>

<style scoped lang="scss">
.group {
	margin-top: 32rpx;
	padding: 4rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.row {
	display: flex;
	align-items: center;
	padding: 30rpx 0;

	& + .row {
		border-top: 2rpx solid $y-divider;
	}
}

.row__text {
	flex: 1;
	display: flex;
	flex-direction: column;
}

.row__label {
	font-size: 30rpx;
	color: $y-text-main;
}

.row__desc {
	margin-top: 10rpx;
	font-size: 22rpx;
	color: $y-text-sub;
}

.row--switch .row__label {
	flex: 1;
}

.row__value {
	flex: 1;
	margin-right: 12rpx;
	font-size: 28rpx;
	color: $y-text-sub;
	text-align: right;
}

/* 提示 */
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
</style>
