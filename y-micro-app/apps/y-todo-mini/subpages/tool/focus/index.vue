<script setup>
import { computed, ref } from 'vue'
import { onShow, onUnload } from '@dcloudio/uni-app'

import { finishFocusSession, getFocusToday, startFocusSession } from '@/api/focus'
import { isLoggedIn } from '@/utils/session'

	/**
	 * 工具 · 番茄专注
	 *
	 * 路由：subpages/tool/focus/index（分包 subpages/tool，按需加载）
	 * 职责：番茄专注计时页，支持 25/5/15 三种模式，可开始/暂停/重置；今日番茄与累计分钟取自 GET /focus/today。
	 * 上报：点开始时 POST /focus/session/start 写一条进行中记录，跑满一轮报 finished，
	 *       重置 / 切换模式 / 离开页面时这一轮还没结算就报 aborted；进程被杀来不及上报的由后端超时回收兜住。
	 * 目录：状态（modeKey、remain、running、pending、todayCount、todayMinutes、yesterdayCount、statsLoaded）
	 *       → 计算（mode、total、remainText、percent、statusText、todayDesc）
	 *       → 方法（genClientId、loadToday、stopTimer、tick、settle、begin、toggle、reset、selectMode）
	 *       → 样式（.modes、.timer、.actions、.today）
	 */

/** 专注模式 */
const MODES = [
	{ key: 'focus', label: '专注', minutes: 25 },
	{ key: 'short', label: '短休息', minutes: 5 },
	{ key: 'long', label: '长休息', minutes: 15 }
]

/** 今日统计里点阵的格数，超出后全部点亮 */
const DOT_TOTAL = 4

/** 当前模式 */
const modeKey = ref('focus')

/** 剩余秒数 */
const remain = ref(MODES[0].minutes * 60)

/** 是否计时中 */
const running = ref(false)

/** 提交中标记，挡住连点（点开始要先上报成功才走表） */
const pending = ref(false)

/** 今日已完成番茄数与累计分钟（来自后端） */
const todayCount = ref(0)
const todayMinutes = ref(0)

/** 昨日番茄数，用于「比昨天多几个」 */
const yesterdayCount = ref(0)

/** 统计是否已取回：没取回时不显示对比文案，免得先闪一句「比昨天多」 */
const statsLoaded = ref(false)

/** 当前这一轮的 clientId，空串表示没有待结算的会话 */
let sessionClientId = ''

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

/** 今日统计下方那句文案：未登录、加载中、以及与昨天的对比 */
const todayDesc = computed(() => {
	if (!isLoggedIn()) {
		return '登录后就能记下每一次专注。'
	}

	if (!statsLoaded.value) {
		return '正在读取今天的记录…'
	}

	if (!todayCount.value) {
		return '今天还没有开始的番茄，慢慢来。'
	}

	if (!yesterdayCount.value) {
		return `累计 ${todayMinutes.value} 分钟，昨天还没开始。`
	}

	const diff = todayCount.value - yesterdayCount.value

	if (diff > 0) {
		return `累计 ${todayMinutes.value} 分钟，比昨天多 ${diff} 个。`
	}

	if (diff < 0) {
		return `累计 ${todayMinutes.value} 分钟，比昨天少 ${-diff} 个。`
	}

	return `累计 ${todayMinutes.value} 分钟，和昨天一样。`
})

/** 生成客户端幂等 Id，与新建任务页保持同一套写法 */
function genClientId() {
	return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

/** 拉取今日统计：未登录不请求（进页面就弹登录太打扰），展示占位文案 */
async function loadToday() {
	if (!isLoggedIn()) {
		statsLoaded.value = false
		todayCount.value = 0
		todayMinutes.value = 0
		yesterdayCount.value = 0

		return
	}

	try {
		const res = await getFocusToday()

		if (res.code === 200 && res.data) {
			todayCount.value = Number(res.data.todayCount) || 0
			todayMinutes.value = Number(res.data.todayMinutes) || 0
			yesterdayCount.value = Number(res.data.yesterdayCount) || 0
			statsLoaded.value = true
		}
	} catch (error) {
		console.warn('[focus] 读取今日专注失败', error)
	}
}

/** 停止计时 */
function stopTimer() {
	if (timer) {
		clearInterval(timer)
		timer = null
	}
}

/** 每秒走一格，归零即完整跑完一轮 */
function tick() {
	if (remain.value > 0) {
		remain.value -= 1

		return
	}

	stopTimer()
	running.value = false

	// 必须在重置剩余秒数之前结算：实际专注秒数是「计划秒数 - 剩余秒数」算出来的
	settle('finished')
	remain.value = total.value

	uni.showToast({ title: '这一轮完成了', icon: 'none' })

	// 休息轮次不计入统计，不用白跑一次请求
	if (modeKey.value === 'focus') {
		loadToday()
	}
}

/**
 * 结算当前这一轮并上报
 *
 * @param {string} status finished-完整跑完一轮 aborted-中途放弃
 *
 * 实际专注秒数用「计划秒数 - 剩余秒数」：暂停时剩余秒数不动，所以这个差值天然不含暂停时间。
 * 上报前先把 clientId 清掉，保证同一轮只结算一次。
 */
async function settle(status) {
	if (!sessionClientId) {
		return
	}

	const data = {
		clientId: sessionClientId,
		status,
		actualSeconds: Math.max(0, total.value - remain.value),
		mode: modeKey.value,
		plannedSeconds: total.value
	}

	sessionClientId = ''

	try {
		await finishFocusSession(data)
	} catch (error) {
		console.warn('[focus] 上报专注结果失败', error)
	}
}

/** 开始计时：这一轮第一次开始时先上报，写入成功才走表 */
async function begin() {
	if (pending.value) {
		return
	}

	pending.value = true

	try {
		if (!sessionClientId) {
			sessionClientId = genClientId()

			const res = await startFocusSession({
				clientId: sessionClientId,
				mode: modeKey.value,
				plannedSeconds: total.value
			})

			// 失败提示已由 request 统一 toast；没写进库就不走表，免得只记在本地
			if (res.code !== 200) {
				sessionClientId = ''

				return
			}
		}

		running.value = true
		timer = setInterval(tick, 1000)
	} catch (error) {
		// 清掉 clientId，下次点开始会重新上报一条
		sessionClientId = ''
		console.warn('[focus] 开始专注失败', error)
	} finally {
		pending.value = false
	}
}

/** 开始 / 暂停 */
function toggle() {
	if (running.value) {
		// 暂停不结算：继续时沿用同一个 clientId，暂停的时间也不计入实际专注秒数
		running.value = false
		stopTimer()

		return
	}

	begin()
}

/** 重置：还没结算的这一轮记为中途放弃，再回到本轮起点 */
function reset() {
	settle('aborted')
	stopTimer()
	running.value = false
	remain.value = total.value
}

/** 切换模式：先把上一轮记为放弃（settle 读的还是旧的模式与时长），再换模式回到起点 */
function selectMode(key) {
	settle('aborted')
	stopTimer()
	running.value = false
	modeKey.value = key
	remain.value = total.value
}

/** 每次展示都刷新今日统计 */
onShow(() => {
	loadToday()
})

/** 离开页面：计时器清掉，还没结算的这一轮记为中途放弃（进程被杀的情况由后端超时回收兜底） */
onUnload(() => {
	stopTimer()
	settle('aborted')
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
				<text class="today__title">今天已专注 {{ todayCount }} 个番茄</text>
			</view>

			<text class="today__desc">{{ todayDesc }}</text>

			<view class="today__dots">
				<view
					v-for="index in DOT_TOTAL"
					:key="index"
					class="today__dot"
					:class="{ 'today__dot--done': index <= todayCount }"
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
	background-color: $y-sunken-bg;
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
	font-size: calc(26rpx * var(--y-font-scale, 1));
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
	border: 2rpx solid $y-green-border;
	background-color: $y-green-soft;
}

.timer__time {
	font-size: calc(96rpx * var(--y-font-scale, 1));
	font-weight: 600;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.timer__status {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-green-text;
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
	font-size: calc(32rpx * var(--y-font-scale, 1));
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
	border: 2rpx solid $y-green-border;
	background-color: $y-card-bg;
}

.actions__reset-text {
	margin-left: 10rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
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
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-text-main;
}

.today__desc {
	display: block;
	margin-top: 16rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	background-color: $y-divider;
}

.today__dot--done {
	background-color: $y-green-primary;
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
