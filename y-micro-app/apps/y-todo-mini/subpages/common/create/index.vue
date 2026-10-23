<script setup>
import { computed, getCurrentInstance, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

import { createTask, updateTask } from '@/api/task'
import { listTags } from '@/api/tag'
import { formatDateLabel, pad2 } from '@/utils/date'
import { DEFAULT_URGENCY_LEVEL, QUADRANTS, quadrantOf } from '@/utils/quadrant'
import { reportAcceptedCount, requestRemindSubscribe } from '@/utils/subscribe'

	/**
	 * 通用 · 新建 / 编辑任务
	 *
	 * 路由：subpages/common/create/index（分包 subpages/common，按需加载）
	 * 编辑态：任务管理页带 `mode=edit` 跳进来，并通过 eventChannel 把任务对象传过来，不额外开详情接口。
	 * 职责：填写任务名称、所属清单、日期时间、提醒、重复、优先级、四象限与备注后提交——新建走 POST /task/create，编辑走 PUT /task/{taskId}。
	 * 属性清单数据来自 GET /tag/list，且在选择时按需拉取，避免一进页面就触发登录；日期时间走原生 picker，提醒、重复与四象限走 actionSheet。
	 * 目录：状态（form、PRIORITIES、PRIORITY_VALUES、PRIORITY_BY_VALUE、REMIND_OPTIONS、REPEAT_OPTIONS、clientId、submitting、editingTaskId、isEdit、dateText、remindText、repeatText、urgencyText）→ 方法（genClientId、fillForm、pickCategory、pickRemind、pickRepeat、pickUrgency、onDateChange、onTimeChange、selectPriority、handleBack、submit）→ 样式（.create、.card、.priority、.submit）
	 */

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 页面实例：eventChannel 只能从实例上取，没有全局的 getOpenerEventChannel */
const { proxy } = getCurrentInstance()

/** 优先级选项 */
const PRIORITIES = [
	{ key: 'none', label: '不设置', icon: 'dash' },
	{ key: 'normal', label: '普通', icon: 'flag' },
	{ key: 'important', label: '重要', icon: 'flag' }
]

/** 优先级文案 → 后端 priority 值（0-无 1-普通 2-重要） */
const PRIORITY_VALUES = {
	none: 0,
	normal: 1,
	important: 2
}

/** 后端 priority 值 → 优先级文案，编辑态回填用 */
const PRIORITY_BY_VALUE = {
	0: 'none',
	1: 'normal',
	2: 'important'
}

/** 提醒选项：value 为提前分钟数，0 表示不提醒（与后端 remindBefore 对齐） */
const REMIND_OPTIONS = [
	{ label: '不提醒', value: 0 },
	{ label: '提前5分钟', value: 5 },
	{ label: '提前10分钟', value: 10 },
	{ label: '提前30分钟', value: 30 },
	{ label: '提前1小时', value: 60 },
	{ label: '提前1天', value: 1440 }
]

/** 重复选项：value 即后端 repeatRule，空串表示不重复 */
const REPEAT_OPTIONS = [
	{ label: '不重复', value: '' },
	{ label: '每天', value: 'DAILY' },
	{ label: '每周', value: 'WEEKLY' },
	{ label: '每月', value: 'MONTHLY' }
]

/** 表单：日期时间默认今天 10:00、提醒默认提前 10 分钟，与设计稿一致 */
const form = ref({
	title: '',
	tagId: null,
	tagName: '',
	priority: 'none',
	urgencyLevel: DEFAULT_URGENCY_LEVEL,
	remark: '',
	date: todayStr(),
	time: '10:00',
	remindBefore: 10,
	repeatRule: ''
})

/** 本次创建的幂等 Id：进页面生成一次，弱网重试沿用同一个值，后端不会建出重复任务 */
const clientId = ref(genClientId())

/** 提交中标记，避免连点建出多条 */
const submitting = ref(false)

/** 编辑态的任务 Id，null 表示新建 */
const editingTaskId = ref(null)

/** 是否为编辑态 */
const isEdit = computed(() => editingTaskId.value != null)

/** 日期文案：今天 / 明天 / 昨天 / 周X，M月D日 */
const dateText = computed(() => formatDateLabel(form.value.date))

/** 提醒文案 */
const remindText = computed(() => {
	const option = REMIND_OPTIONS.find((item) => item.value === form.value.remindBefore)

	return option ? option.label : '不提醒'
})

/** 重复文案 */
const repeatText = computed(() => {
	const option = REPEAT_OPTIONS.find((item) => item.value === form.value.repeatRule)

	return option ? option.label : '不重复'
})

/** 四象限文案（紧急程度） */
const urgencyText = computed(() => quadrantOf(form.value.urgencyLevel).name)

/** 今天，yyyy-MM-dd */
function todayStr() {
	const now = new Date()

	return `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}`
}

/** 选择所属清单（清单列表此时才拉取） */
async function pickCategory() {
	// 未登录时 request 会先拉起登录半弹框，登录成功后自动重放本次请求
	const res = await listTags()

	if (res.code !== 200 || !Array.isArray(res.data) || !res.data.length) {
		return
	}

	const tags = res.data

	uni.showActionSheet({
		itemList: tags.map((item) => item.tagName),
		success: ({ tapIndex }) => {
			const tag = tags[tapIndex]

			form.value.tagId = tag.tagId
			form.value.tagName = tag.tagName
		}
	})
}

/** 顶部更多 / 主题入口（暂未接入） */
function pickField(label) {
	uni.showToast({ title: `选择${label}`, icon: 'none' })
}

/** 选择提醒 */
function pickRemind() {
	uni.showActionSheet({
		itemList: REMIND_OPTIONS.map((item) => item.label),
		success: ({ tapIndex }) => {
			form.value.remindBefore = REMIND_OPTIONS[tapIndex].value
		}
	})
}

/** 选择重复 */
function pickRepeat() {
	uni.showActionSheet({
		itemList: REPEAT_OPTIONS.map((item) => item.label),
		success: ({ tapIndex }) => {
			form.value.repeatRule = REPEAT_OPTIONS[tapIndex].value
		}
	})
}

/** 选择四象限（紧急程度，四象限页按它分格） */
function pickUrgency() {
	uni.showActionSheet({
		itemList: QUADRANTS.map((item) => `${item.name}（${item.tip}）`),
		success: ({ tapIndex }) => {
			form.value.urgencyLevel = QUADRANTS[tapIndex].level
		}
	})
}

/** 选择日期 */
function onDateChange(event) {
	form.value.date = event.detail.value
}

/** 选择时间 */
function onTimeChange(event) {
	form.value.time = event.detail.value
}

/** 选择优先级 */
function selectPriority(key) {
	form.value.priority = key
}

/** 返回上一页 */
function handleBack() {
	if (getCurrentPages().length > 1) {
		uni.navigateBack()
	} else {
		uni.switchTab({ url: '/pages/index/index' })
	}
}

/** 生成客户端幂等 Id */
function genClientId() {
	return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

/**
 * 编辑态回填表单
 *
 * 任务对象由任务管理页通过 eventChannel 传入（列表接口返回的字段已够填满表单），因此不额外开详情接口。
 * dueDate 是后端原始的 `yyyy-MM-dd HH:mm:ss`，拆成日期与时间两段交给两个 picker。
 */
function fillForm(task) {
	if (!task || task.taskId == null) {
		return
	}

	editingTaskId.value = task.taskId

	const due = typeof task.dueDate === 'string' ? task.dueDate : ''

	form.value = {
		title: task.title || '',
		tagId: task.tagId == null ? null : task.tagId,
		tagName: task.tagName || '',
		priority: PRIORITY_BY_VALUE[task.priority] || 'none',
		urgencyLevel: task.urgencyLevel == null ? DEFAULT_URGENCY_LEVEL : task.urgencyLevel,
		remark: task.description || '',
		date: due ? due.slice(0, 10) : todayStr(),
		time: due ? due.slice(11, 16) : '10:00',
		remindBefore: task.remindBefore == null ? 10 : task.remindBefore,
		repeatRule: task.repeatRule || ''
	}
}

onLoad((query) => {
	if (!query || query.mode !== 'edit') {
		return
	}

	// eventChannel 挂在页面实例上（uni-app 没有全局的 getOpenerEventChannel）；
	// H5 直接开 URL 时没有上级页，拿不到就保持新建态
	const eventChannel = proxy.getOpenerEventChannel ? proxy.getOpenerEventChannel() : null

	if (!eventChannel) {
		return
	}

	eventChannel.on('task', fillForm)
})

/** 提交保存（新建走 POST /task/create，编辑走 PUT /task/{taskId}） */
async function submit() {
	const title = form.value.title.trim()

	if (!title) {
		uni.showToast({ title: '给这件事起个名字吧', icon: 'none' })

		return
	}

	if (submitting.value) {
		return
	}

	submitting.value = true

	try {
		// 订阅授权必须在点击事件的调用栈里发起（微信不允许异步之后再弹），所以放在所有 await 之前。
		// 设了提醒才需要授权：没设提醒的任务本来也不会下发消息，弹窗只会打扰用户
		const acceptedCount = form.value.remindBefore > 0 ? await requestRemindSubscribe() : 0

		const payload = {
			title,
			description: form.value.remark.trim() || null,
			tagId: form.value.tagId,
			priority: PRIORITY_VALUES[form.value.priority] || 0,
			urgencyLevel: form.value.urgencyLevel,
			// 后端要求 yyyy-MM-dd HH:mm:ss，秒固定补 0
			dueDate: `${form.value.date} ${form.value.time}:00`,
			repeatRule: form.value.repeatRule,
			remindBefore: form.value.remindBefore
		}
		// 未登录时 request 会先拉起登录半弹框，登录成功后自动重放本次请求
		const res = isEdit.value
			? await updateTask(editingTaskId.value, payload)
			: await createTask({ clientId: clientId.value, ...payload })

		// 失败提示已由 request 统一 toast，这里只走成功分支
		if (res.code !== 200) {
			return
		}

		// 授权成功才上报，失败不阻塞建任务：任务照建，只是这次不加额度
		if (acceptedCount > 0) {
			await reportAcceptedCount(acceptedCount)
		}

		uni.showToast({ title: isEdit.value ? '已保存' : '已创建任务', icon: 'success' })
		setTimeout(handleBack, 800)
	} catch (error) {
		// 网络异常提示同样由 request 统一处理
		console.warn('[create] 保存任务失败', error)
	} finally {
		submitting.value = false
	}
}
</script>

<template>
	<view class="create">
		<view :style="{ height: statusBarHeight + 'px' }" />

		<brand-header show-back @back="handleBack" @more="pickField('更多')" @theme="pickField('主题')" />

		<view class="create__hero">
			<text class="create__title">{{ isEdit ? '把这一件改顺手' : '留给下一步的自己' }}</text>
			<text class="create__subtitle">{{ isEdit ? '调整时间和清单，让它更贴合现在。' : '新建任务，把想做的事先记下来。' }}</text>
		</view>

		<view class="card card--field">
			<text class="field__label">任务名称</text>
			<input class="field__input" v-model="form.title" placeholder="给这件事起个名字" placeholder-style="color:#a9a9a0" />
		</view>

		<view class="card card--rows">
			<view class="row" @click="pickCategory">
				<y-icon name="folder" size="38rpx" />
				<text class="row__label">所属清单</text>
				<text class="row__value">{{ form.tagName || '未归类' }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row">
				<y-icon name="calendar" size="38rpx" />
				<text class="row__label">日期时间</text>
				<picker class="row__picker" mode="date" :value="form.date" @change="onDateChange">
					<text class="row__value">{{ dateText }}</text>
				</picker>
				<picker class="row__picker" mode="time" :value="form.time" @change="onTimeChange">
					<text class="row__value">{{ form.time }}</text>
				</picker>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row" @click="pickRemind">
				<y-icon name="bell" size="38rpx" />
				<text class="row__label">提醒</text>
				<text class="row__value">{{ remindText }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row" @click="pickRepeat">
				<y-icon name="repeat" size="38rpx" />
				<text class="row__label">重复</text>
				<text class="row__value">{{ repeatText }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row" @click="pickUrgency">
				<y-icon name="grid" size="38rpx" />
				<text class="row__label">四象限</text>
				<text class="row__value">{{ urgencyText }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>
		</view>

		<view class="section">
			<text class="section__title">优先级</text>

			<view class="priorities">
				<view
					v-for="item in PRIORITIES"
					:key="item.key"
					class="priority"
					:class="{ 'priority--active': form.priority === item.key }"
					@click="selectPriority(item.key)"
				>
					<y-icon
						:name="item.icon"
						size="30rpx"
						:color="form.priority === item.key ? '#B8652F' : '#8e8e85'"
					/>
					<text class="priority__label">{{ item.label }}</text>
					<y-icon v-if="form.priority === item.key" name="check" size="30rpx" color="#B8652F" />
				</view>
			</view>
		</view>

		<view class="card card--remark">
			<view class="remark__head">
				<text class="field__label">备注</text>
				<y-icon name="align" size="34rpx" color="#a9a9a0" />
			</view>
			<textarea
				class="remark__input"
				v-model="form.remark"
				placeholder="想补充点什么？"
				placeholder-style="color:#a9a9a0"
				:maxlength="200"
			/>
		</view>

		<view class="create__foot">
			<y-icon name="pencil" size="28rpx" color="#a9a9a0" />
			<text class="create__foot-text">不必一次做完，从这一件开始就好。</text>
		</view>

		<view class="submit">
			<view class="submit__button" @click="submit">
				<y-icon name="check" size="34rpx" color="#ffffff" />
				<text class="submit__text">{{ isEdit ? '保存修改' : '创建任务' }}</text>
			</view>
		</view>

		<login-sheet />
		<privacy-sheet />
	</view>
</template>

<style scoped lang="scss">
.create {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 240rpx;
	box-sizing: border-box;
}

/* 标题 */
.create__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.create__title {
	font-size: calc(52rpx * var(--y-font-scale, 1));
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.create__subtitle {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 卡片 */
.card {
	margin-top: 32rpx;
	padding: 12rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.card--field {
	padding-top: 28rpx;
	padding-bottom: 28rpx;
}

.field__label {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.field__input {
	margin-top: 16rpx;
	height: 56rpx;
	font-size: calc(32rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

/* 行 */
.card--rows {
	padding-top: 4rpx;
	padding-bottom: 4rpx;
}

.row {
	display: flex;
	align-items: center;
	padding: 30rpx 0;

	& + .row {
		border-top: 2rpx solid $y-divider;
	}
}

.row__label {
	flex: 1;
	margin-left: 24rpx;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__value {
	margin-right: 12rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.row__picker {
	display: flex;
	align-items: center;
}

/* 优先级 */
.section {
	margin-top: 44rpx;
}

.section__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.priorities {
	display: flex;
	margin-top: 24rpx;
}

.priority {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 82rpx;
	margin-right: 20rpx;
	border-radius: 999rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;

	&:last-child {
		margin-right: 0;
	}
}

.priority__label {
	margin: 0 8rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.priority--active {
	border-color: $y-warm-border;
	background-color: $y-warm-soft-bg;

	.priority__label {
		color: $y-accent-warm;
	}
}

/* 备注 */
.card--remark {
	padding-top: 28rpx;
	padding-bottom: 28rpx;
}

.remark__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.remark__input {
	width: 100%;
	height: 140rpx;
	margin-top: 16rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	line-height: 44rpx;
	color: $y-text-main;
}

/* 页脚 */
.create__foot {
	display: flex;
	align-items: center;
	justify-content: center;
	margin-top: 48rpx;
}

.create__foot-text {
	margin-left: 10rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

/* 底部主按钮 */
.submit {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 24rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
	background-color: $y-page-bg;
}

.submit__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	border-radius: 24rpx;
	background-color: $y-green-primary;
}

.submit__text {
	margin-left: 12rpx;
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: #ffffff;
}
</style>
