<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import { getQuota } from '@/api/notify'
import { getSetting, updateSetting } from '@/api/setting'

	/**
	 * 我的 · 提醒设置
	 *
	 * 路由：subpages/me/reminder/index（分包 subpages/me，按需加载）
	 * 职责：提醒设置页，该做事的时候轻轻叫你一声——任务/每日/番茄提醒开关、每日提醒时间、免打扰时段，以及剩余提醒额度。
	 * 数据：开关与时间读写 GET/PUT /user/setting，额度读 GET /notify/quota；
	 *      开关 key 与后端字段同名，保存时只提交被改动的那个字段，避免互相覆盖。
	 * 目录：状态（switches、dailyRemindTime、quietStart、quietEnd、quota）→ 方法（loadSetting、applySetting、save、toggle、onDailyTimeChange、onQuietStartChange、onQuietEndChange）→ 样式（.group、.row、.quota、.tip）
	 */

/** 开关项，key 与后端字段同名，改哪个提交哪个 */
const switches = ref([
	{ key: 'taskRemindOn', label: '任务到期提醒', desc: '到时间前提醒你该做的事', on: true },
	{ key: 'dailyRemindOn', label: '每日提醒', desc: '每天固定时间提醒看今天的安排', on: false },
	{ key: 'focusRemindOn', label: '番茄结束提醒', desc: '专注一轮结束时轻轻提醒', on: true }
])

/** 每日提醒时间 HH:mm */
const dailyRemindTime = ref('08:00')

/** 免打扰时段 HH:mm */
const quietStart = ref('22:00')

/** 免打扰时段 HH:mm */
const quietEnd = ref('07:00')

/** 剩余提醒额度（一次性订阅：授权一次只能发一条） */
const quota = ref({ remain: 0, total: 0 })

/** 每次进入页面都重新拉一次：额度会被后台下发消耗掉，缓存住就永远不刷新 */
onShow(loadSetting)

/** 拉取设置与额度 */
async function loadSetting() {
	try {
		const [settingRes, quotaRes] = await Promise.all([getSetting(), getQuota()])

		if (settingRes.code === 200 && settingRes.data) {
			applySetting(settingRes.data)
		}

		if (quotaRes.code === 200 && quotaRes.data) {
			quota.value = quotaRes.data
		}
	} catch (error) {
		// 失败提示由 request 统一 toast，这里保留默认值即可
		console.warn('[reminder] 读取提醒设置失败', error)
	}
}

/** 用后端数据回填页面 */
function applySetting(data) {
	switches.value.forEach((item) => {
		item.on = data[item.key] === 1
	})

	dailyRemindTime.value = data.dailyRemindTime || '08:00'
	quietStart.value = data.quietStart || '22:00'
	quietEnd.value = data.quietEnd || '07:00'
}

/**
 * 保存改动
 *
 * @returns {Promise<boolean>} 是否保存成功，失败时调用方要把本地状态回滚
 */
async function save(data) {
	try {
		const res = await updateSetting(data)

		return res.code === 200
	} catch (error) {
		console.warn('[reminder] 保存提醒设置失败', error)

		return false
	}
}

/** 切换开关（先改本地再提交，失败回滚，避免开关卡住不响应） */
async function toggle(item, event) {
	const previous = item.on
	item.on = event.detail.value

	if (!(await save({ [item.key]: item.on ? 1 : 0 }))) {
		item.on = previous
	}
}

/** 修改每日提醒时间 */
async function onDailyTimeChange(event) {
	const previous = dailyRemindTime.value
	dailyRemindTime.value = event.detail.value

	if (!(await save({ dailyRemindTime: dailyRemindTime.value }))) {
		dailyRemindTime.value = previous
	}
}

/** 修改免打扰开始时间 */
async function onQuietStartChange(event) {
	const previous = quietStart.value
	quietStart.value = event.detail.value

	if (!(await save({ quietStart: quietStart.value }))) {
		quietStart.value = previous
	}
}

/** 修改免打扰结束时间 */
async function onQuietEndChange(event) {
	const previous = quietEnd.value
	quietEnd.value = event.detail.value

	if (!(await save({ quietEnd: quietEnd.value }))) {
		quietEnd.value = previous
	}
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
			<view class="row">
				<text class="row__label">每日提醒时间</text>

				<picker class="row__picker" mode="time" :value="dailyRemindTime" @change="onDailyTimeChange">
					<text class="row__value row__value--pick">{{ dailyRemindTime }}</text>
				</picker>

				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row">
				<text class="row__label">免打扰时段</text>

				<view class="row__quiet">
					<picker mode="time" :value="quietStart" @change="onQuietStartChange">
						<text class="row__value row__value--pick">{{ quietStart }}</text>
					</picker>

					<text class="row__dash">-</text>

					<picker mode="time" :value="quietEnd" @change="onQuietEndChange">
						<text class="row__value row__value--pick">{{ quietEnd }}</text>
					</picker>
				</view>
			</view>
		</view>

		<view class="group">
			<view class="row">
				<text class="row__label">提醒额度</text>
				<text class="row__value">剩余 {{ quota.remain }} 条 / 共 {{ quota.total }} 条</text>
			</view>

			<view v-if="quota.remain <= 0" class="quota">
				<text class="quota__text">额度用完啦。新建带提醒的任务时点一下「允许」，就能继续收到提醒。</text>
			</view>
		</view>

		<view class="tip">
			<y-icon name="bell" size="30rpx" />
			<text class="tip__text">微信规定一次授权只能送达一条提醒，免打扰时段内的提醒会顺延到时段结束后。</text>
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
	flex: 1;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__desc {
	margin-top: 10rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.row--switch .row__label {
	flex: 1;
}

.row__value {
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
	text-align: right;
}

.row__value--pick {
	margin-right: 12rpx;
}

.row__quiet {
	display: flex;
	align-items: center;
}

.row__dash {
	margin: 0 12rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 额度提示 */
.quota {
	padding-bottom: 28rpx;
}

.quota__text {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 36rpx;
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
