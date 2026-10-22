<script setup>
import { computed, ref } from 'vue'
import { onUnload } from '@dcloudio/uni-app'

	/**
	 * 工具 · 番茄专注
	 *
	 * 路由：subpages/tool/focus/index（分包 subpages/tool，按需加载）
	 * 职责：番茄专注计时页，支持 25/5/15 三种模式，可开始/暂停/重置并统计今日番茄。
	 * 目录：状态（modeKey、remain、running、doneCount）→ 方法（toggle、reset、selectMode）→ 样式（.modes、.timer、.actions、.today）
	 */

/** 专注模式 */
const MODES = [
	{ key: 'focus', label: '专注', minutes: 25 },
	{ key: 'short', label: '短休息', minutes: 5 },
	{ key: 'long', label: '长休息', minutes: 15 }
]

/** 当前模式 */
const modeKey = ref('focus')

/** 剩余秒数 */
const remain = ref(MODES[0].minutes * 60)

/** 是否计时中 */
const running = ref(false)

/** 今日已完成番茄数与累计分钟（暂用本地数据） */
const doneCount = ref(3)
const doneMinutes = ref(75)

/** 计时器句柄 */
let timer = null

/** 当前模式配置 */
const mode = computed(() => MODES.find((item) => item.key === modeKey.value) || MODES[0])

/** 当前模式总秒数 */
const total = computed(() => mode.value.minutes * 60)

/** 剩余时间文案 */
const remainText = computed(() => {
	const minute = String(Math.floor(remain.value / 60)).padStart(2, '0')
	const second = String(remain.value % 60).padStart(2, '0')

	return `${minute}:${second}`
})

/** 计时进度百分比 */
const percent = computed(() => Math.round(((total.value - remain.value) / total.value) * 100))

/** 状态文案 */
const statusText = computed(() => {
	if (running.value) {
		return '专注进行中'
	}

	return remain.value === total.value ? '准备开始' : '已暂停'
})

/** 停止计时 */
function stopTimer() {
	if (timer) {
		clearInterval(timer)
		timer = null
	}
}

/** 开始 / 暂停 */
function toggle() {
	if (running.value) {
		running.value = false
		stopTimer()

		return
	}

	running.value = true
	timer = setInterval(() => {
		if (remain.value > 0) {
			remain.value -= 1

			return
		}

		stopTimer()
		running.value = false

		if (modeKey.value === 'focus') {
			doneCount.value += 1
			doneMinutes.value += mode.value.minutes
		}

		uni.showToast({ title: '这一轮完成了', icon: 'none' })
	}, 1000)
}

/** 重置计时 */
function reset() {
	stopTimer()
	running.value = false
	remain.value = total.value
}

/** 切换模式 */
function selectMode(key) {
	modeKey.value = key
	reset()
}

/** 离开页面时清理计时器 */
onUnload(() => {
	stopTimer()
})
</script>

<template>
	<sub-page title="番茄专注" subtitle="留出 25 分钟，只做一件事。">
		<view class="modes">
			<view
				v-for="item in MODES"
				:key="item.key"
				class="modes__item"
				:class="{ 'modes__item--active': modeKey === item.key }"
				@click="selectMode(item.key)"
			>
				<text class="modes__label">{{ item.label }} {{ item.minutes }}</text>
			</view>
		</view>

		<view class="timer">
			<view class="timer__circle">
				<text class="timer__time">{{ remainText }}</text>
				<text class="timer__status">{{ statusText }}</text>
			</view>

			<view class="timer__track">
				<view class="timer__fill" :style="{ width: percent + '%' }" />
			</view>
		</view>

		<view class="actions">
			<view class="actions__primary" @click="toggle">
				<y-icon :name="running ? 'pause' : 'play'" size="34rpx" color="#ffffff" />
				<text class="actions__primary-text">{{ running ? '暂停' : '开始专注' }}</text>
			</view>

			<view class="actions__reset" @click="reset">
				<y-icon name="rotate" size="32rpx" />
				<text class="actions__reset-text">重置</text>
			</view>
		</view>

		<view class="today">
			<view class="today__head">
				<y-icon name="flame" size="36rpx" color="#c2793c" />
				<text class="today__title">今天已专注 {{ doneCount }} 个番茄</text>
			</view>

			<text class="today__desc">累计 {{ doneMinutes }} 分钟，比昨天多 1 个。</text>

			<view class="today__dots">
				<view
					v-for="index in 4"
					:key="index"
					class="today__dot"
					:class="{ 'today__dot--done': index <= doneCount }"
				/>
			</view>
		</view>

		<text class="foot">专注的时候，世界会安静一点。</text>
	</sub-page>
</template>

<style scoped lang="scss">
/* 模式切换 */
.modes {
	display: flex;
	margin-top: 32rpx;
	padding: 6rpx;
	border-radius: 18rpx;
	background-color: #edede6;
}

.modes__item {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 64rpx;
	border-radius: 14rpx;
}

.modes__item--active {
	background-color: $y-card-bg;
}

.modes__label {
	font-size: 26rpx;
	color: $y-text-sub;
}

.modes__item--active .modes__label {
	font-weight: 500;
	color: $y-text-main;
}

/* 计时器 */
.timer {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 56rpx;
}

.timer__circle {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	width: 400rpx;
	height: 400rpx;
	border-radius: 50%;
	border: 2rpx solid #c4d2b4;
	background-color: $y-green-soft;
}

.timer__time {
	font-size: 96rpx;
	font-weight: 600;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.timer__status {
	margin-top: 16rpx;
	font-size: 26rpx;
	color: #6f8064;
}

.timer__track {
	width: 100%;
	height: 8rpx;
	margin-top: 56rpx;
	border-radius: 999rpx;
	background-color: $y-green-track;
}

.timer__fill {
	height: 100%;
	border-radius: 999rpx;
	background-color: $y-green-primary;
	transition: width 0.3s ease;
}

/* 操作 */
.actions {
	display: flex;
	align-items: center;
	margin-top: 48rpx;
}

.actions__primary {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	border-radius: 24rpx;
	background-color: $y-green-primary;
}

.actions__primary-text {
	margin-left: 12rpx;
	font-size: 32rpx;
	font-weight: 500;
	color: #ffffff;
}

.actions__reset {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	margin-left: 20rpx;
	padding: 0 36rpx;
	border-radius: 24rpx;
	border: 2rpx solid #d8e2cb;
	background-color: $y-card-bg;
}

.actions__reset-text {
	margin-left: 10rpx;
	font-size: 30rpx;
	color: $y-green-primary;
}

/* 今日统计 */
.today {
	margin-top: 48rpx;
	padding: 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.today__head {
	display: flex;
	align-items: center;
}

.today__title {
	margin-left: 12rpx;
	font-size: 30rpx;
	font-weight: 500;
	color: $y-text-main;
}

.today__desc {
	display: block;
	margin-top: 16rpx;
	font-size: 24rpx;
	color: $y-text-sub;
}

.today__dots {
	display: flex;
	margin-top: 24rpx;
}

.today__dot {
	width: 32rpx;
	height: 32rpx;
	margin-right: 16rpx;
	border-radius: 50%;
	background-color: #ecece5;
}

.today__dot--done {
	background-color: $y-green-primary;
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
