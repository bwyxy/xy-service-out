<script setup>
import { ref } from 'vue'

	/**
	 * 我的 · 数据同步与备份
	 *
	 * 路由：subpages/me/sync/index（分包 subpages/me，按需加载）
	 * 职责：数据同步与备份页，换台设备日子还是接着过——同步状态、立即同步/导出与自动备份开关。
	 * 目录：状态（syncing、lastSync、settings）→ 方法（sync、exportData、toggle）→ 样式（.status、.actions、.group、.tip）
	 */

/** 同步状态（暂用本地数据，后续接入接口） */
const syncing = ref(false)

/** 最近同步时间文案 */
const lastSync = ref('今天 09:24')

/** 同步项数量 */
const recordCount = ref('共 128 条任务、4 个清单')

/** 设置项 */
const settings = ref([
	{ key: 'wifi', label: '仅在 Wi-Fi 下同步', desc: '省下一点流量，也省下一点电量', on: true },
	{ key: 'auto', label: '自动备份', desc: '每天自动保存一份数据快照', on: true }
])

/** 立即同步 */
function sync() {
	if (syncing.value) {
		return
	}

	syncing.value = true

	setTimeout(() => {
		syncing.value = false
		lastSync.value = '刚刚'
		uni.showToast({ title: '同步完成', icon: 'none' })
	}, 800)
}

/** 导出数据（待接入导出） */
function exportData() {
	uni.showToast({ title: '导出数据（待开发）', icon: 'none' })
}

/** 切换开关 */
function toggle(item, event) {
	item.on = event.detail.value
}
</script>

<template>
	<sub-page title="数据同步与备份" subtitle="换台设备，日子还是接着过。">
		<view class="status">
			<view class="status__icon">
				<y-icon name="cloud" size="52rpx" />
			</view>

			<view class="status__text">
				<text class="status__title">{{ syncing ? '正在同步…' : '已同步' }}</text>
				<text class="status__desc">最近同步：{{ lastSync }}</text>
				<text class="status__desc">{{ recordCount }}</text>
			</view>
		</view>

		<view class="actions">
			<view class="actions__primary" @click="sync">
				<y-icon name="refresh" size="32rpx" color="#ffffff" />
				<text class="actions__primary-text">立即同步</text>
			</view>

			<view class="actions__second" @click="exportData">
				<y-icon name="file-text" size="32rpx" />
				<text class="actions__second-text">导出数据</text>
			</view>
		</view>

		<view class="group">
			<view v-for="item in settings" :key="item.key" class="row row--switch">
				<view class="row__text">
					<text class="row__label">{{ item.label }}</text>
					<text class="row__desc">{{ item.desc }}</text>
				</view>

				<switch :checked="item.on" color="#4a6b3e" @change="toggle(item, $event)" />
			</view>
		</view>

		<view class="tip">
			<y-icon name="bulb" size="30rpx" />
			<text class="tip__text">数据默认只保存在本机，开启同步后才会备份到云端。</text>
		</view>
	</sub-page>
</template>

<style scoped lang="scss">
/* 状态卡 */
.status {
	display: flex;
	align-items: center;
	margin-top: 32rpx;
	padding: 36rpx;
	border-radius: 32rpx;
	background-color: $y-green-soft;
}

.status__icon {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 112rpx;
	height: 112rpx;
	margin-right: 28rpx;
	border-radius: 50%;
	background-color: $y-glass-bg;
}

.status__text {
	flex: 1;
	display: flex;
	flex-direction: column;
}

.status__title {
	font-size: calc(34rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.status__desc {
	margin-top: 8rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

/* 操作 */
.actions {
	display: flex;
	margin-top: 24rpx;
}

.actions__primary {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 92rpx;
	border-radius: 24rpx;
	background-color: $y-green-primary;
}

.actions__primary-text {
	margin-left: 12rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: #ffffff;
}

.actions__second {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 92rpx;
	margin-left: 20rpx;
	border-radius: 24rpx;
	border: 2rpx solid $y-green-border;
	background-color: $y-card-bg;
}

.actions__second-text {
	margin-left: 12rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-green-primary;
}

/* 设置 */
.group {
	margin-top: 24rpx;
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
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__desc {
	margin-top: 10rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 36rpx;
	color: $y-green-text;
}
</style>
