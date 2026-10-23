<script setup>
import { computed, getCurrentInstance, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

import { createHabit, removeHabit, updateHabit } from '@/api/habit'

	/**
	 * 工具 · 习惯打卡 · 新建 / 编辑
	 *
	 * 路由：subpages/tool/habit/edit/index（分包 subpages/tool，按需加载）
	 * 编辑态：习惯列表页带 `mode=edit` 跳进来，并通过 eventChannel 把习惯对象传过来，不额外开详情接口。
	 * 职责：填写习惯名称、图标、颜色、频率（每天 / 按周选星期）与目标天数后提交——新建走 POST /habit/create，编辑走 PUT /habit/{habitId}，删除走 DELETE /habit/{habitId}。
	 * 目录：状态（form、editingHabitId、saving、deleting）→ 计算（isEdit、editorTitle）
	 *       → 方法（toggleDay、selectFreq、fillForm、save、removeEditing）→ 样式（.card、.chips、.footer）
	 */

/** 习惯名称长度上限，与后端 ContentCheckUtils 的校验保持一致 */
const HABIT_NAME_MAX = 64

/** 可选图标（取自 y-icon 图标表） */
const ICON_OPTIONS = ['sprout', 'book', 'moon', 'sun', 'coffee', 'star', 'bell', 'flame', 'clock', 'cloud', 'list', 'pencil']

/** 可选颜色 */
const COLOR_OPTIONS = ['#4a6b3e', '#c2793c', '#b5543f', '#3f7d8c', '#6b5b95', '#8e8e85']

/** 周标签，索引 0 为周一，与后端 freqDays 的 1 ~ 7 对应 */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 频率选项 */
const FREQ_OPTIONS = [
	{ value: 'daily', label: '每天' },
	{ value: 'weekly', label: '按周' }
]

/** 页面实例：eventChannel 只能从实例上取，没有全局的 getOpenerEventChannel */
const { proxy } = getCurrentInstance()

/** 表单 */
const form = ref({
	name: '',
	icon: ICON_OPTIONS[0],
	color: COLOR_OPTIONS[0],
	freqType: 'daily',
	freqDays: [],
	targetDays: ''
})

/** 编辑态的习惯 Id，null 表示新建 */
const editingHabitId = ref(null)

/** 保存中标记，避免连点建出多条 */
const saving = ref(false)

/** 删除中标记，避免连点 */
const deleting = ref(false)

/** 是否为编辑态 */
const isEdit = computed(() => editingHabitId.value != null)

/** 页面标题 */
const editorTitle = computed(() => (isEdit.value ? '编辑习惯' : '新建习惯'))

/** 页面副标题 */
const subtitle = computed(() =>
	isEdit.value ? '调整频率和目标，让它更贴合现在。' : '挑一件想坚持的小事，从今天开始记。'
)

/** 勾选 / 取消某个星期（仅按周频率用） */
function toggleDay(day) {
	const days = form.value.freqDays

	form.value.freqDays = days.includes(day) ? days.filter((item) => item !== day) : days.concat(day).sort()
}

/** 切换频率；切回每天时清掉星期选择，避免留下上一次的脏数据 */
function selectFreq(value) {
	form.value.freqType = value

	if (value !== 'weekly') {
		form.value.freqDays = []
	}
}

/**
 * 编辑态回填表单
 *
 * 习惯对象由列表页通过 eventChannel 传入（列表接口返回的字段已够填满表单），因此不额外开详情接口。
 */
function fillForm(habit) {
	if (!habit || habit.id == null) {
		return
	}

	editingHabitId.value = habit.id

	form.value = {
		name: habit.name || '',
		icon: habit.icon || ICON_OPTIONS[0],
		color: habit.color || COLOR_OPTIONS[0],
		freqType: habit.freqType || 'daily',
		freqDays: Array.isArray(habit.freqDays) ? habit.freqDays.slice().sort() : [],
		targetDays: habit.targetDays > 0 ? String(habit.targetDays) : ''
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

	eventChannel.on('habit', fillForm)
})

/** 保存（新建或修改） */
async function save() {
	const name = form.value.name.trim()

	if (!name) {
		uni.showToast({ title: '给习惯起个名字吧', icon: 'none' })

		return
	}

	if (form.value.freqType === 'weekly' && !form.value.freqDays.length) {
		uni.showToast({ title: '按周打卡请至少选一天', icon: 'none' })

		return
	}

	if (saving.value) {
		return
	}

	saving.value = true

	try {
		const payload = {
			name,
			icon: form.value.icon,
			color: form.value.color,
			freqType: form.value.freqType,
			freqDays: form.value.freqType === 'weekly' ? form.value.freqDays : [],
			// 空串或非法值都按「不限」交给后端处理
			targetDays: Number(form.value.targetDays) || 0
		}
		// 失败提示已由 request 统一 toast，这里只走成功分支
		const res = isEdit.value ? await updateHabit(editingHabitId.value, payload) : await createHabit(payload)

		if (res.code !== 200) {
			return
		}

		uni.showToast({ title: isEdit.value ? '已保存' : '已新建习惯', icon: 'none' })
		setTimeout(() => uni.navigateBack(), 600)
	} catch (error) {
		console.warn('[habit] 保存习惯失败', error)
	} finally {
		saving.value = false
	}
}

/** 删除当前习惯（历史打卡记录保留） */
async function removeEditing() {
	if (!isEdit.value || deleting.value) {
		return
	}

	const confirmed = await new Promise((resolve) => {
		uni.showModal({
			title: '删除习惯',
			content: `「${form.value.name}」会从列表移除，已经记下的打卡不会丢。`,
			confirmText: '删除',
			confirmColor: '#b5543f',
			success: (res) => resolve(!!res.confirm),
			fail: () => resolve(false)
		})
	})

	if (!confirmed) {
		return
	}

	deleting.value = true

	try {
		const res = await removeHabit(editingHabitId.value)

		if (res.code !== 200) {
			return
		}

		uni.showToast({ title: '已删除', icon: 'none' })
		setTimeout(() => uni.navigateBack(), 600)
	} catch (error) {
		console.warn('[habit] 删除习惯失败', error)
	} finally {
		deleting.value = false
	}
}
</script>

<template>
	<sub-page :title="editorTitle" :subtitle="subtitle" :fixed-footer="true">
		<view class="card">
			<text class="field__label">习惯名称</text>
			<input
				v-model="form.name"
				class="field__input"
				type="text"
				:maxlength="HABIT_NAME_MAX"
				placeholder="比如：晨间拉伸 15 分钟"
				placeholder-style="color:#a9a9a0"
			/>
		</view>

		<view class="section">
			<text class="section__title">频率</text>

			<view class="chips">
				<view
					v-for="item in FREQ_OPTIONS"
					:key="item.value"
					class="chip"
					:class="{ 'chip--active': form.freqType === item.value }"
					@click="selectFreq(item.value)"
				>
					<text class="chip__text">{{ item.label }}</text>
				</view>
			</view>

			<view v-if="form.freqType === 'weekly'" class="week">
				<view
					v-for="(label, index) in WEEK_LABELS"
					:key="label"
					class="week__cell"
					:class="{ 'week__cell--active': form.freqDays.includes(index + 1) }"
					@click="toggleDay(index + 1)"
				>
					<text class="week__label">{{ label }}</text>
				</view>
			</view>
		</view>

		<view class="card card--row">
			<text class="field__label">目标天数</text>
			<input
				v-model="form.targetDays"
				class="field__input field__input--right"
				type="number"
				placeholder="不限"
				placeholder-style="color:#a9a9a0"
			/>
			<text class="field__unit">天</text>
		</view>

		<view class="section">
			<text class="section__title">图标</text>

			<view class="icons">
				<view
					v-for="icon in ICON_OPTIONS"
					:key="icon"
					class="icons__item"
					:class="{ 'icons__item--active': form.icon === icon }"
					@click="form.icon = icon"
				>
					<y-icon :name="icon" size="40rpx" :color="form.icon === icon ? form.color : '#8e8e85'" />
				</view>
			</view>
		</view>

		<view class="section">
			<text class="section__title">颜色</text>

			<view class="colors">
				<view
					v-for="color in COLOR_OPTIONS"
					:key="color"
					class="colors__item"
					:class="{ 'colors__item--active': form.color === color }"
					:style="{ backgroundColor: color }"
					@click="form.color = color"
				>
					<y-icon v-if="form.color === color" name="check" size="28rpx" color="#ffffff" />
				</view>
			</view>
		</view>

		<view class="tip">
			<y-icon name="bulb" size="30rpx" />
			<text class="tip__text">今天之前的日子也能补记，但还没到的那一天得等到当天再打。</text>
		</view>

		<template #footer>
			<view class="footer">
				<view class="footer__button" @click="save">
					<y-icon name="check" size="32rpx" color="#ffffff" />
					<text class="footer__text">{{ saving ? '保存中…' : isEdit ? '保存修改' : '创建习惯' }}</text>
				</view>

				<view v-if="isEdit" class="footer__danger" @click="removeEditing">
					<text class="footer__danger-text">删除这个习惯</text>
				</view>
			</view>
		</template>
	</sub-page>
</template>

<style scoped lang="scss">
/* 卡片与字段 */
.card {
	margin-top: 32rpx;
	padding: 28rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.card--row {
	display: flex;
	align-items: center;
}

.field__label {
	flex-shrink: 0;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.field__input {
	width: 100%;
	margin-top: 16rpx;
	height: 56rpx;
	font-size: calc(32rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.field__input--right {
	flex: 1;
	margin-top: 0;
	margin-left: 24rpx;
	text-align: right;
}

.field__unit {
	margin-left: 8rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 频率 */
.section {
	margin-top: 44rpx;
}

.section__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.chips {
	display: flex;
	margin-top: 24rpx;
}

.chip {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 76rpx;
	margin-right: 20rpx;
	padding: 0 48rpx;
	border-radius: 999rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;
}

.chip__text {
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.chip--active {
	border-color: $y-green-border;
	background-color: $y-green-soft;

	.chip__text {
		color: $y-green-text;
	}
}

.week {
	display: flex;
	justify-content: space-between;
	margin-top: 24rpx;
}

.week__cell {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 76rpx;
	height: 76rpx;
	border-radius: 20rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;
}

.week__label {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.week__cell--active {
	border-color: $y-green-primary;
	background-color: $y-green-primary;

	.week__label {
		color: #ffffff;
	}
}

/* 图标 */
.icons {
	display: flex;
	flex-wrap: wrap;
	margin-top: 20rpx;
}

.icons__item {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 96rpx;
	height: 96rpx;
	margin: 0 20rpx 20rpx 0;
	border-radius: 24rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;
}

.icons__item--active {
	border-color: $y-green-primary;
	background-color: $y-green-soft;
}

/* 颜色 */
.colors {
	display: flex;
	flex-wrap: wrap;
	margin-top: 20rpx;
}

.colors__item {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 72rpx;
	height: 72rpx;
	margin: 0 24rpx 20rpx 0;
	border-radius: 50%;
}

.colors__item--active {
	border: 4rpx solid $y-card-bg;
	box-shadow: 0 0 0 3rpx $y-green-primary;
}

/* 说明 */
.tip {
	display: flex;
	align-items: flex-start;
	margin-top: 32rpx;
	padding: 24rpx 28rpx;
	border-radius: 20rpx;
	background-color: $y-green-soft;
}

.tip__text {
	flex: 1;
	margin-left: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 1.6;
	color: $y-green-text;
}

/* 底部主按钮 */
.footer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 24rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
	background-color: $y-page-bg;
}

.footer__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	border-radius: 24rpx;
	background-color: $y-green-primary;
}

.footer__text {
	margin-left: 10rpx;
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: #ffffff;
}

.footer__danger {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 80rpx;
	margin-top: 8rpx;
}

.footer__danger-text {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-danger;
}
</style>
