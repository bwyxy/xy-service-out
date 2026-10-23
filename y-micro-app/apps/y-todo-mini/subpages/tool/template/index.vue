<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import { applyTemplate, listTemplates } from '@/api/template'

/**
 * 工具 · 清单模板
 *
 * 路由：subpages/tool/template/index（分包 subpages/tool，按需加载）
 * 职责：清单模板页，晨间计划、旅行准备，从容开始——列出模板并可一键套用。
 * 数据：模板列表来自 GET /template/list，套用走 POST /template/{code}/apply（后端建清单 + 预置任务）。
 * 目录：状态（templates、loaded、applyingCode）→ 方法（genBatchId、loadTemplates、useTemplate）
 *       → 样式（.tip、.list、.template、.empty、.foot）
 */

/** 模板列表（code/name/description/category/icon/itemCount） */
const templates = ref([])

/** 列表是否已拉过：没拉完不显示空态，避免先闪一句「暂无模板」 */
const loaded = ref(false)

/** 正在套用的模板 code，用来挡连点 */
const applyingCode = ref('')

/** 生成幂等批次号：同一次套用前后端共用一个，弱网重试不会建出第二个清单 */
function genBatchId() {
	return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

/** 拉模板列表 */
async function loadTemplates() {
	try {
		const res = await listTemplates()

		if (res.code === 200 && Array.isArray(res.data)) {
			templates.value = res.data
		}
	} catch (error) {
		// 网络异常已由 request 统一 toast，这里只保证不把异常抛到页面外
	} finally {
		loaded.value = true
	}
}

/** 套用模板：先确认要建什么，再等后端把清单和任务一起建好 */
async function useTemplate(item) {
	if (applyingCode.value) {
		return
	}

	const confirmed = await new Promise((resolve) => {
		uni.showModal({
			title: '套用模板',
			content: `会新建清单「${item.name}」并加入 ${item.itemCount} 个任务，之后可以随时增删。`,
			confirmText: '套用',
			success: (res) => resolve(!!res.confirm),
			fail: () => resolve(false)
		})
	})

	if (!confirmed) {
		return
	}

	applyingCode.value = item.code

	try {
		const res = await applyTemplate(item.code, { batchId: genBatchId() })

		if (res.code !== 200 || !res.data) {
			return
		}

		// 清单名以后端返回的为准：模板名被占用时后端会补序号
		uni.showToast({
			title: `已新建「${res.data.tagName}」等 ${res.data.taskCount} 个任务`,
			icon: 'none'
		})
	} catch (error) {
		// 未登录会由 request 拉起登录弹框并重放，失败提示也已统一 toast
	} finally {
		applyingCode.value = ''
	}
}

/** 每次展示都重拉：模板内容可能已经在后台调整过 */
onShow(loadTemplates)
</script>

<template>
	<sub-page title="清单模板" subtitle="晨间计划、旅行准备，从容开始。">
		<view class="tip">
			<y-icon name="bulb" size="30rpx" />
			<text class="tip__text">套用模板会新建一个清单，里面的任务可以随时增删。</text>
		</view>

		<view class="list">
			<view v-for="item in templates" :key="item.code" class="template">
				<view class="template__icon">
					<y-icon :name="item.icon" size="44rpx" />
				</view>

				<view class="template__main">
					<view class="template__head">
						<text class="template__name">{{ item.name }}</text>
						<text class="template__count">{{ item.itemCount }} 个任务</text>
					</view>

					<text class="template__desc">{{ item.description }}</text>

					<view class="template__foot">
						<text class="template__tag">{{ item.category }}</text>

						<view
							class="template__use"
							:class="{ 'template__use--busy': applyingCode === item.code }"
							@click="useTemplate(item)"
						>
							<text class="template__use-text">
								{{ applyingCode === item.code ? '套用中' : '使用' }}
							</text>
						</view>
					</view>
				</view>
			</view>

			<text v-if="loaded && !templates.length" class="empty">暂时没有可用的模板</text>
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 36rpx;
	color: $y-green-text;
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
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: $y-text-main;
}

.template__count {
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.template__desc {
	margin-top: 12rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	background-color: $y-sunken-bg;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.template__use {
	padding: 10rpx 30rpx;
	border-radius: 999rpx;
	background-color: $y-green-primary;
}

/* 套用中置灰，配合 applyingCode 挡连点 */
.template__use--busy {
	opacity: 0.6;
}

.template__use-text {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: #ffffff;
}

/* 空态 */
.empty {
	display: block;
	margin-top: 48rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
	text-align: center;
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
