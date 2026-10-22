<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

	/**
	 * 通用 · 搜索
	 *
	 * 路由：subpages/common/search/index（分包 subpages/common，按需加载）
	 * 职责：搜索页，找找记下的那些事——按范围筛选并展示搜索结果、空态与联想词。
	 * 目录：状态（keyword、activeFilter、results）→ 方法（clearKeyword、applySuggestion、handleCancel）→ 样式（.search-page、.search、.segment、.empty、.tip）
	 */

/** 搜索范围筛选 */
const filters = [
	{ key: 'all', label: '全部' },
	{ key: 'pending', label: '待完成' },
	{ key: 'done', label: '已完成' }
]

/** 关键词联想词 */
const SUGGESTIONS = ['旅行', '计划']

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 搜索关键词 */
const keyword = ref('旅行计划')

/** 当前筛选 */
const activeFilter = ref('all')

/** 搜索结果（暂为空，后续接入接口） */
const results = ref([])

/** 结果条数文案 */
const resultText = computed(() => `共 ${results.value.length} 条`)

/** 清空关键词 */
function clearKeyword() {
	keyword.value = ''
}

/** 使用联想词重新搜索 */
function applySuggestion(word) {
	keyword.value = word
}

/** 取消并返回 */
function handleCancel() {
	if (getCurrentPages().length > 1) {
		uni.navigateBack()
		return
	}

	uni.switchTab({ url: '/pages/list/index' })
}

/** 隐藏原生 tabBar，改用自定义导航栏 */
onShow(() => {
	uni.hideTabBar()
})
</script>

<template>
	<view class="search-page">
		<view :style="{ height: statusBarHeight + 'px' }" />

		<brand-header />

		<view class="search-page__hero">
			<text class="search-page__title">找找，记下的那些事</text>
			<text class="search-page__subtitle">搜索所有清单里的任务与备注。</text>
		</view>

		<view class="search">
			<view class="search__field">
				<view class="search__icon">
					<y-icon name="search" size="30rpx" color="#8e8e85" />
				</view>
				<input
					v-model="keyword"
					class="search__input"
					type="text"
					confirm-type="search"
					placeholder="搜索任务、备注"
					placeholder-style="color:#a9a9a0;font-size:26rpx"
				/>
				<view v-if="keyword" class="search__clear" @click="clearKeyword">
					<y-icon name="close-circle" size="34rpx" color="#ccd1c4" />
				</view>
			</view>
			<text class="search__cancel" @click="handleCancel">取消</text>
		</view>

		<view class="segment">
			<view
				v-for="item in filters"
				:key="item.key"
				class="segment__item"
				:class="{ 'segment__item--active': activeFilter === item.key }"
				@click="activeFilter = item.key"
			>
				<text class="segment__label">{{ item.label }}</text>
			</view>
		</view>

		<view class="section">
			<view class="section__head">
				<text class="section__title">搜索结果</text>
				<text class="section__count">{{ resultText }}</text>
			</view>

			<view v-if="!results.length" class="empty">
				<view class="art">
					<view class="art__glow" />

					<view class="art__doc">
						<view class="art__line art__line--long" />
						<view class="art__line art__line--short" />
						<view class="art__line art__line--long" />
					</view>

					<view class="art__lens">
						<view class="art__lens-handle" />
					</view>
				</view>

				<text class="empty__title">没有找到「{{ keyword }}」</text>
				<text class="empty__desc">试试更短的关键词，</text>
				<text class="empty__desc">或检查一下有没有写错字。</text>

				<view class="empty__button" @click="clearKeyword">
					<text class="empty__button-icon">↺</text>
					<text class="empty__button-text">清空关键词</text>
				</view>
			</view>
		</view>

		<view class="tip">
			<view class="tip__head">
				<view class="tip__bulb">
					<y-icon name="bulb" size="30rpx" />
				</view>
				<text class="tip__title">换个方式找找</text>
			</view>

			<text class="tip__desc">可以搜索任务名称，也可以搜索备注里的内容。</text>

			<view class="tip__row">
				<text class="tip__hint">比如试试</text>
				<view
					v-for="word in SUGGESTIONS"
					:key="word"
					class="tip__chip"
					@click="applySuggestion(word)"
				>
					{{ word }}
				</view>
			</view>
		</view>

		<custom-tabbar active="pages/list/index" />
	</view>
</template>

<style scoped lang="scss">
.search-page {
	min-height: 100vh;
	background-color: $y-page-bg;
	padding: 0 40rpx 180rpx;
	box-sizing: border-box;
}

/* 标题 */
.search-page__hero {
	display: flex;
	flex-direction: column;
	margin-top: 24rpx;
}

.search-page__title {
	font-size: 52rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.search-page__subtitle {
	margin-top: 16rpx;
	font-size: 26rpx;
	color: $y-text-sub;
}

/* 搜索框 */
.search {
	display: flex;
	align-items: center;
	margin-top: 32rpx;
}

.search__field {
	flex: 1;
	display: flex;
	align-items: center;
	height: 84rpx;
	padding: 0 24rpx;
	box-sizing: border-box;
	border-radius: 20rpx;
	border: 2rpx solid #e6e6de;
	background-color: $y-card-bg;
}

.search__icon {
	display: flex;
	align-items: center;
	flex-shrink: 0;
	margin-right: 16rpx;
}

.search__input {
	flex: 1;
	height: 84rpx;
	font-size: 28rpx;
	color: $y-text-main;
}

.search__clear {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	margin-left: 12rpx;
}

.search__cancel {
	flex-shrink: 0;
	margin-left: 24rpx;
	font-size: 28rpx;
	color: $y-text-main;
}

/* 筛选分段 */
.segment {
	display: flex;
	margin-top: 28rpx;
	padding: 6rpx;
	border-radius: 18rpx;
	background-color: #edede6;
}

.segment__item {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 60rpx;
	border-radius: 14rpx;
}

.segment__item--active {
	background-color: $y-card-bg;
}

.segment__label {
	font-size: 24rpx;
	color: $y-text-sub;
}

.segment__item--active .segment__label {
	font-weight: 500;
	color: $y-text-main;
}

/* 搜索结果 */
.section {
	margin-top: 40rpx;
}

.section__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.section__title {
	font-size: 32rpx;
	font-weight: 600;
	color: $y-text-main;
}

.section__count {
	font-size: 24rpx;
	color: $y-text-muted;
}

/* 空状态 */
.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 44rpx 0 20rpx;
}

/* 放大镜插画 */
.art {
	position: relative;
	width: 240rpx;
	height: 240rpx;
}

.art__glow {
	position: absolute;
	left: 12rpx;
	top: 12rpx;
	width: 216rpx;
	height: 216rpx;
	border-radius: 50%;
	background-color: $y-green-soft;
}

.art__doc {
	position: absolute;
	left: 52rpx;
	top: 56rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	width: 108rpx;
	height: 132rpx;
	padding-top: 30rpx;
	box-sizing: border-box;
	border: 4rpx solid $y-green-primary;
	border-radius: 12rpx;
}

.art__line {
	height: 4rpx;
	border-radius: 2rpx;
	background-color: $y-green-primary;
}

.art__line--long {
	width: 56rpx;
}

.art__line--short {
	width: 34rpx;
	margin-top: 14rpx;
}

.art__line--long + .art__line--long {
	margin-top: 14rpx;
}

.art__lens {
	position: absolute;
	right: 30rpx;
	bottom: 46rpx;
	width: 88rpx;
	height: 88rpx;
	box-sizing: border-box;
	border: 5rpx solid $y-green-primary;
	border-radius: 50%;
	background-color: $y-green-soft;
}

.art__lens-handle {
	position: absolute;
	left: 58rpx;
	top: 62rpx;
	width: 44rpx;
	height: 8rpx;
	border-radius: 4rpx;
	background-color: $y-green-primary;
	transform: rotate(45deg);
	transform-origin: left center;
}

.empty__title {
	margin-top: 28rpx;
	font-size: 34rpx;
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 14rpx;
	font-size: 26rpx;
	color: $y-text-sub;
}

.empty__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	margin-top: 40rpx;
	padding: 0 44rpx;
	border-radius: 999rpx;
	background-color: $y-green-primary;
}

.empty__button-icon {
	margin-right: 10rpx;
	font-size: 32rpx;
	line-height: 1;
	color: #ffffff;
}

.empty__button-text {
	font-size: 30rpx;
	color: #ffffff;
}

/* 搜索建议 */
.tip {
	margin-top: 36rpx;
	padding: 28rpx;
	border-radius: 24rpx;
	background-color: $y-green-soft;
}

.tip__head {
	display: flex;
	align-items: center;
}

.tip__bulb {
	display: flex;
	align-items: center;
	flex-shrink: 0;
	margin-right: 14rpx;
}

.tip__title {
	font-size: 28rpx;
	font-weight: 600;
	color: $y-text-main;
}

.tip__desc {
	display: block;
	margin-top: 18rpx;
	font-size: 25rpx;
	color: #5c6b52;
}

.tip__row {
	display: flex;
	align-items: center;
	margin-top: 24rpx;
}

.tip__hint {
	font-size: 24rpx;
	color: #5c6b52;
}

.tip__chip {
	margin-left: 16rpx;
	padding: 8rpx 22rpx;
	border-radius: 12rpx;
	border: 2rpx solid #dbe4d0;
	background-color: #f2f6ec;
	font-size: 24rpx;
	color: $y-green-primary;
}
</style>
