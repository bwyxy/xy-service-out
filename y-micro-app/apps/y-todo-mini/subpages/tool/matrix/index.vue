<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import { getQuadrantTasks } from '@/api/task'
import { QUADRANTS } from '@/utils/quadrant'

	/**
	 * 工具 · 四象限
	 *
	 * 路由：subpages/tool/matrix/index（分包 subpages/tool，按需加载）
	 * 职责：四象限页，先做真正重要的事——按紧急与重要程度分四格展示任务。
	 * 数据：POST /task/quadrant 一次返回四格的待完成件数与前几条预览（只算未完成、未归档的）；
	 *       点某一格进 subpages/tool/matrix/detail，那边复用 POST /task/list 的 urgencyLevel 过滤翻全部。
	 * 目录：状态（groups、requestSeq）→ 计算（quadrants）→ 方法（loadMatrix、openQuadrant）
	 *       → 样式（.matrix、.quadrant、.note）
	 */

/** 紧急程度 → { total, tasks }，后端没回来的那几格按空格渲染 */
const groups = ref({})

/** 请求序号：快速进出页面时丢弃过期响应 */
let requestSeq = 0

/** 四格数据：名称、配色、件数与预览都在这里拼好，模板里不再兜底 */
const quadrants = computed(() => QUADRANTS.map((item) => {
	const group = groups.value[item.level] || {}

	return {
		...item,
		total: Number(group.total) || 0,
		tasks: Array.isArray(group.tasks) ? group.tasks : []
	}
}))

/** 拉四象限概览；失败提示已由 request 统一 toast，这里只保证不把异常抛到页面外 */
async function loadMatrix() {
	const seq = (requestSeq += 1)

	try {
		const res = await getQuadrantTasks()

		if (seq !== requestSeq || res.code !== 200 || !res.data) {
			return
		}

		const map = {}

		;(res.data.groups || []).forEach((item) => {
			map[item.urgencyLevel] = item
		})

		groups.value = map
	} catch (error) {
		console.warn('[matrix] 加载四象限失败', error)
	}
}

/** 打开某个象限：进该象限的任务列表页看全部 */
function openQuadrant(level) {
	uni.navigateTo({ url: `/subpages/tool/matrix/detail/index?level=${level}` })
}

/** 每次展示都重拉：在别处完成或新建任务后回到本页，格子里的件数与预览都是新的 */
onShow(loadMatrix)
</script>

<template>
	<sub-page title="四象限" subtitle="先做真正重要的事。">
		<view class="matrix">
			<view
				v-for="item in quadrants"
				:key="item.key"
				class="quadrant"
				:style="{ backgroundColor: item.bg }"
				@click="openQuadrant(item.level)"
			>
				<view class="quadrant__head">
					<view class="quadrant__bar" :style="{ backgroundColor: item.color }" />
					<text class="quadrant__name">{{ item.name }}</text>
				</view>

				<text class="quadrant__tip" :style="{ color: item.color }">{{ item.tip }}</text>

				<view class="quadrant__list">
					<text v-for="task in item.tasks" :key="task.taskId" class="quadrant__task">{{ task.title }}</text>
					<text v-if="!item.tasks.length" class="quadrant__empty">还没有任务</text>
					<text v-if="item.total > item.tasks.length" class="quadrant__more">
						还有 {{ item.total - item.tasks.length }} 件
					</text>
				</view>

				<text class="quadrant__count">{{ item.total }} 件</text>
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
	width: calc(50% - 12rpx);
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.quadrant__tip {
	margin-top: 12rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
}

.quadrant__list {
	flex: 1;
	margin-top: 24rpx;
}

.quadrant__task {
	display: block;
	margin-bottom: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 34rpx;
	color: $y-text-body;
}

.quadrant__empty {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.quadrant__more {
	display: block;
	margin-top: 4rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.quadrant__count {
	font-size: calc(22rpx * var(--y-font-scale, 1));
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 36rpx;
	color: $y-green-text;
}
</style>
