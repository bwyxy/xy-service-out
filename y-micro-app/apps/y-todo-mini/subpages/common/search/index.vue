<script setup>
import { computed, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'

import { listTasks } from '@/api/task'
import { listTags } from '@/api/tag'
import { isLoggedIn } from '@/utils/session'
import { formatDueLabel, toTimestamp } from '@/utils/date'

	/**
	 * 通用 · 搜索
	 *
	 * 路由：subpages/common/search/index（分包 subpages/common，按需加载）
	 * 职责：搜索所有清单里的任务与备注——输入关键词回车搜索，按状态筛选，结果按页累积。
	 * 数据：复用 POST /task/list（不传 tagId 即跨清单，keyword 由后端按标题与备注模糊匹配），
	 *       所以这里不额外开搜索接口，排序与分页行为也和列表页完全一致。
	 * 目录：状态（keyword、activeFilter、results、total、pageNum、hasMore、loading、searched、tagNames）
	 *       → 方法（onSearch、resetResults、loadTags、toResultItem、loadResults、selectFilter、clearKeyword、applySuggestion、handleCancel）
	 *       → 样式（.search-page、.search、.segment、.result、.empty、.tip）
	 */

/** 任务列表每页条数 */
const PAGE_SIZE = 20

/** 搜索范围筛选 */
const filters = [
	{ key: 'all', label: '全部' },
	{ key: 'pending', label: '待完成' },
	{ key: 'done', label: '已完成' }
]

/** 关键词联想词 */
const SUGGESTIONS = ['旅行', '计划']

/** 标记图标 */
const FLAG_SRC = {
	important: '/static/task_important.png',
	ordinary: '/static/task_ordinary.png'
}

/** 后端 priority → 小旗子 */
const FLAG_BY_PRIORITY = {
	1: 'ordinary',
	2: 'important'
}

/** 状态栏高度，用于自定义导航栏留白 */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 搜索关键词 */
const keyword = ref('')

/** 当前筛选 */
const activeFilter = ref('all')

/** 搜索结果（按页累积） */
const results = ref([])

/** 命中总数（后端返回，分页后本地数不出来） */
const total = ref(0)

/** 已加载到的页码 */
const pageNum = ref(1)

/** 是否还有下一页 */
const hasMore = ref(false)

/** 加载中标记，挡住上拉时连续触发的重复请求 */
const loading = ref(false)

/** 是否已经搜过一次：没搜过就整块不显示，免得出现「没有找到「」」 */
const searched = ref(false)

/** 清单 Id → 清单名，结果行上标出所属清单用 */
const tagNames = ref({})

/** 清单只拉一次；它只是装饰，不为它去弹登录 */
let tagsLoaded = false

/** 请求序号：连续改关键词时用来丢弃先发后到的过期响应 */
let requestSeq = 0

/** 结果条数文案 */
const resultText = computed(() => `共 ${total.value} 条`)

/** 列表页脚文案：还有下一页时提示上拉 */
const footText = computed(() => {
	if (!hasMore.value) {
		return '没有更多了'
	}

	return loading.value ? '正在加载…' : '上拉加载更多'
})

/** 关键词为空就回到「还没搜」的状态，不拿空词去扫全表 */
function onSearch() {
	if (!keyword.value.trim()) {
		resetResults()

		return
	}

	searched.value = true
	loadResults()
}

/** 清空结果并复位分页 */
function resetResults() {
	requestSeq += 1
	searched.value = false
	results.value = []
	total.value = 0
	pageNum.value = 1
	hasMore.value = false
	loading.value = false
}

/** 拉取清单名（未登录时列表接口本来会拉起登录，这里不额外触发） */
async function loadTags() {
	if (tagsLoaded || !isLoggedIn()) {
		return
	}

	tagsLoaded = true

	const res = await listTags()

	if (res.code !== 200 || !Array.isArray(res.data)) {
		return
	}

	const map = {}

	res.data.forEach((item) => {
		map[item.tagId] = item.tagName
	})

	tagNames.value = map
}

/** 后端任务 → 结果行 */
function toResultItem(item) {
	return {
		id: item.taskId,
		title: item.title,
		description: item.description,
		tagId: item.tagId,
		priority: item.priority,
		dueTime: toTimestamp(item.dueDate),
		done: item.completed === 1
	}
}

/**
 * 拉取搜索结果
 *
 * @param {boolean} [reset] true-从第 1 页覆盖结果；false-追加下一页（上拉加载）
 */
async function loadResults(reset = true) {
	const kw = keyword.value.trim()

	if (!kw || (!reset && loading.value)) {
		return
	}

	const nextPage = reset ? 1 : pageNum.value + 1
	const seq = (requestSeq += 1)

	loading.value = true

	try {
		// 装饰性的清单名，晚一点到无所谓，不 await
		loadTags()

		// 失败提示已由 request 统一 toast；未登录时它也会先拉起登录再重放这次请求
		const res = await listTasks({
			status: activeFilter.value,
			keyword: kw,
			pageNum: nextPage,
			pageSize: PAGE_SIZE
		})

		// 期间又改了关键词或筛选，这一页已是旧数据，直接丢弃
		if (seq !== requestSeq || res.code !== 200 || !res.data) {
			return
		}

		const page = res.data
		const list = Array.isArray(page.list) ? page.list.map(toResultItem) : []

		results.value = reset ? list : results.value.concat(list)
		total.value = Number(page.total) || 0
		pageNum.value = Number(page.pageNum) || nextPage
		hasMore.value = Boolean(page.hasMore)
	} finally {
		if (seq === requestSeq) {
			loading.value = false
		}
	}
}

/** 切换「全部/待完成/已完成」：搜过就重搜，没搜过只记住选择 */
function selectFilter(key) {
	if (key === activeFilter.value) {
		return
	}

	activeFilter.value = key

	if (searched.value) {
		loadResults()
	}
}

/** 打开任务详情：搜索结果点行进详情 */
function enterDetail(item) {
	uni.navigateTo({ url: `/subpages/common/detail/index?taskId=${item.id}` })
}

/** 清空关键词（同时收起结果） */
function clearKeyword() {
	keyword.value = ''
	resetResults()
}

/** 点联想词：填进输入框并直接搜 */
function applySuggestion(word) {
	keyword.value = word
	onSearch()
}

/** 取消并返回 */
function handleCancel() {
	if (getCurrentPages().length > 1) {
		uni.navigateBack()
		return
	}

	uni.switchTab({ url: '/pages/list/index' })
}

/** 上拉到底：还有下一页就追加加载 */
onReachBottom(() => {
	if (hasMore.value) {
		loadResults(false)
	}
})
</script>

<template>
	<sub-page title="搜索" subtitle="搜索所有清单里的任务与备注。">
		<!-- <view :style="{ height: statusBarHeight + 'px' }" /> -->

		<!-- <brand-header /> -->

		<!-- <view class="search-page__hero">
			<text class="search-page__title">找找，记下的那些事</text>
			<text class="search-page__subtitle">搜索所有清单里的任务与备注。</text>
		</view> -->

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
					@confirm="onSearch"
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
				@click="selectFilter(item.key)"
			>
				<text class="segment__label">{{ item.label }}</text>
			</view>
		</view>

		<view v-if="searched" class="section">
			<view class="section__head">
				<text class="section__title">搜索结果</text>
				<text class="section__count">{{ resultText }}</text>
			</view>

			<view v-if="results.length" class="result-list">
				<view v-for="item in results" :key="item.id" class="result" @click="enterDetail(item)">
					<view class="result__main">
						<text class="result__title" :class="{ 'result__title--done': item.done }">{{ item.title }}</text>

						<view class="result__meta">
							<text v-if="tagNames[item.tagId]" class="result__tag">{{ tagNames[item.tagId] }}</text>
							<text class="result__due">{{ formatDueLabel(item.dueTime, item.done) }}</text>
						</view>

						<text v-if="item.description" class="result__desc">{{ item.description }}</text>
					</view>

					<image
						v-if="FLAG_BY_PRIORITY[item.priority]"
						class="result__flag"
						:src="FLAG_SRC[FLAG_BY_PRIORITY[item.priority]]"
						mode="aspectFit"
					/>
				</view>

				<view class="result-foot">
					<text class="result-foot__text">{{ footText }}</text>
				</view>
			</view>

			<view v-else class="empty">
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
	</sub-page>
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
	font-size: calc(52rpx * var(--y-font-scale, 1));
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $y-text-main;
}

.search-page__subtitle {
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
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
	border: 2rpx solid $y-divider;
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

/* 筛选分段 */
.segment {
	display: flex;
	margin-top: 28rpx;
	padding: 6rpx;
	border-radius: 18rpx;
	background-color: $y-sunken-bg;
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
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.section__count {
	font-size: calc(24rpx * var(--y-font-scale, 1));
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
	font-size: calc(34rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 14rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
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
	font-size: calc(32rpx * var(--y-font-scale, 1));
	line-height: 1;
	color: #ffffff;
}

.empty__button-text {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: #ffffff;
}

/* 搜索结果列表 */
.result-list {
	margin-top: 12rpx;
}

.result {
	display: flex;
	align-items: flex-start;
	padding: 32rpx 0;
	border-bottom: 2rpx solid $y-divider;
}

.result__main {
	flex: 1;
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.result__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.result__title--done {
	color: $y-text-muted;
}

.result__meta {
	display: flex;
	align-items: center;
	margin-top: 14rpx;
}

.result__tag {
	margin-right: 16rpx;
	padding: 4rpx 16rpx;
	border-radius: 999rpx;
	background-color: $y-sunken-bg;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.result__due {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.result__desc {
	margin-top: 12rpx;
	font-size: calc(25rpx * var(--y-font-scale, 1));
	line-height: 1.5;
	color: $y-text-sub;
}

.result__flag {
	flex-shrink: 0;
	width: 36rpx;
	height: 36rpx;
	margin-top: 4rpx;
	margin-left: 20rpx;
}

/* 分页页脚 */
.result-foot {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 32rpx 0 8rpx;
}

.result-foot__text {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
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
	font-size: calc(28rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.tip__desc {
	display: block;
	margin-top: 18rpx;
	font-size: calc(25rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

.tip__row {
	display: flex;
	align-items: center;
	margin-top: 24rpx;
}

.tip__hint {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-green-text;
}

.tip__chip {
	margin-left: 16rpx;
	padding: 8rpx 22rpx;
	border-radius: 12rpx;
	border: 2rpx solid $y-green-border;
	background-color: #f2f6ec;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-green-primary;
}
</style>
