<script setup>
import { computed, ref } from 'vue'

	/**
	 * 通用 · 新建任务
	 *
	 * 路由：subpages/common/create/index（分包 subpages/common，按需加载）
	 * 职责：新建任务页，留给下一步的自己——填写任务名称、所属清单/日期/提醒/重复、优先级与备注，底部固定创建按钮。
	 * 目录：状态（form、PRIORITIES、scheduleText）→ 方法（pickField、selectPriority、handleBack、submit）→ 样式（.create、.card、.priority、.submit）
	 */

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 优先级选项 */
const PRIORITIES = [
	{ key: 'none', label: '不设置', icon: 'dash' },
	{ key: 'normal', label: '普通', icon: 'flag' },
	{ key: 'important', label: '重要', icon: 'flag' }
]

/** 表单（暂用本地数据，后续接入接口） */
const form = ref({
	title: '整理产品改版思路',
	category: '工作',
	priority: 'important',
	remark: '梳理首页信息层级，准备两版方向，下午和团队一起讨论。'
})

/** 日期时间文案，默认今天 10:00 */
const scheduleText = computed(() => {
	const now = new Date()

	return `今天，${now.getMonth() + 1}月${now.getDate()}日 10:00`
})

/** 选择所属清单、日期、提醒、重复（待接入选择器） */
function pickField(label) {
	uni.showToast({ title: `选择${label}`, icon: 'none' })
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

/** 创建任务（待接入接口） */
function submit() {
	if (!form.value.title.trim()) {
		uni.showToast({ title: '给这件事起个名字吧', icon: 'none' })

		return
	}

	uni.showToast({ title: '已创建任务', icon: 'none' })
}
</script>

<template>
	<view class="create">
		<view :style="{ height: statusBarHeight + 'px' }" />

		<brand-header show-back @back="handleBack" @more="pickField('更多')" @theme="pickField('主题')" />

		<view class="create__hero">
			<text class="create__title">留给下一步的自己</text>
			<text class="create__subtitle">新建任务，把想做的事先记下来。</text>
		</view>

		<view class="card card--field">
			<text class="field__label">任务名称</text>
			<input class="field__input" v-model="form.title" placeholder="给这件事起个名字" placeholder-style="color:#a9a9a0" />
		</view>

		<view class="card card--rows">
			<view class="row" @click="pickField('所属清单')">
				<y-icon name="folder" size="38rpx" />
				<text class="row__label">所属清单</text>
				<text class="row__value">{{ form.category }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row" @click="pickField('日期时间')">
				<y-icon name="calendar" size="38rpx" />
				<text class="row__label">日期时间</text>
				<text class="row__value">{{ scheduleText }}</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row" @click="pickField('提醒')">
				<y-icon name="bell" size="38rpx" />
				<text class="row__label">提醒</text>
				<text class="row__value">提前10分钟</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view class="row" @click="pickField('重复')">
				<y-icon name="repeat" size="38rpx" />
				<text class="row__label">重复</text>
				<text class="row__value">不重复</text>
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
				<text class="submit__text">创建任务</text>
			</view>
		</view>
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
	font-size: 52rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.create__subtitle {
	margin-top: 16rpx;
	font-size: 26rpx;
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
	font-size: 24rpx;
	color: $y-text-sub;
}

.field__input {
	margin-top: 16rpx;
	height: 56rpx;
	font-size: 32rpx;
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
	font-size: 30rpx;
	color: $y-text-main;
}

.row__value {
	margin-right: 12rpx;
	font-size: 26rpx;
	color: $y-text-sub;
}

/* 优先级 */
.section {
	margin-top: 44rpx;
}

.section__title {
	font-size: 30rpx;
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
	font-size: 28rpx;
	color: $y-text-sub;
}

.priority--active {
	border-color: #e8c7a8;
	background-color: #f8ebdf;

	.priority__label {
		color: #b8652f;
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
	font-size: 28rpx;
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
	font-size: 24rpx;
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
	font-size: 32rpx;
	font-weight: 500;
	color: #ffffff;
}
</style>
