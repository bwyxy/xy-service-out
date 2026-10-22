<script setup>
import { computed } from 'vue'

/**
 * 自定义底部导航栏
 *
 * 原生 tabBar 无法承载中间的悬浮「+」按钮，因此由本组件绘制。
 * 使用方式：页面在 onShow 中调用 uni.hideTabBar() 隐藏原生 tabBar，并传入当前页路由 active。
 */
const props = defineProps({
	/** 当前选中项的路由路径，如 pages/index/index */
	active: { type: String, default: '' }
})

/** 导航项配置 */
const TABBARS = [
	{
		path: 'pages/index/index',
		text: '今日',
		icon: '/static/tabbar/home.png',
		activeIcon: '/static/tabbar/home_active.png'
	},
	{
		path: 'pages/list/index',
		text: '清单',
		icon: '/static/tabbar/list.png',
		activeIcon: '/static/tabbar/list_active.png'
	},
	{
		path: 'pages/tool/index',
		text: '工具',
		icon: '/static/tabbar/tool.png',
		activeIcon: '/static/tabbar/tool_active.png'
	},
	{
		path: 'pages/me/index',
		text: '我的',
		icon: '/static/tabbar/me.png',
		activeIcon: '/static/tabbar/me_active.png'
	}
]

/** 中间按钮左右各两个导航项 */
const leftTabs = computed(() => TABBARS.slice(0, 2))
const rightTabs = computed(() => TABBARS.slice(2))

/** 切换导航 */
function toPage(path) {
	if (path === props.active) {
		return
	}

	uni.switchTab({ url: '/' + path })
}

/** 新建待办 */
function handleCreate() {
	uni.navigateTo({ url: '/subpages/common/create/index' })
}
</script>

<template>
	<view class="tabbar">
		<view class="tabbar__content">
			<view
				v-for="item in leftTabs"
				:key="item.path"
				class="tabbar__item"
				:class="{ 'tabbar__item--active': active === item.path }"
				@click="toPage(item.path)"
			>
				<image
					class="tabbar__icon"
					:src="active === item.path ? item.activeIcon : item.icon"
					mode="aspectFit"
				/>
				<text class="tabbar__text">{{ item.text }}</text>
			</view>

			<view class="tabbar__fab" @click="handleCreate">
				<text class="tabbar__fab-icon">＋</text>
			</view>

			<view
				v-for="item in rightTabs"
				:key="item.path"
				class="tabbar__item"
				:class="{ 'tabbar__item--active': active === item.path }"
				@click="toPage(item.path)"
			>
				<image
					class="tabbar__icon"
					:src="active === item.path ? item.activeIcon : item.icon"
					mode="aspectFit"
				/>
				<text class="tabbar__text">{{ item.text }}</text>
			</view>
		</view>
	</view>
</template>

<style scoped lang="scss">
$green-primary: #4a6b3e;
$text-muted: #a9a9a0;

.tabbar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 100;
	background-color: rgba(247, 247, 242, 0.94);
	border-top: 2rpx solid #ecece5;
	padding-bottom: env(safe-area-inset-bottom);
}

.tabbar__content {
	display: flex;
	align-items: center;
	height: 110rpx;
}

.tabbar__item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
}

.tabbar__icon {
	width: 44rpx;
	height: 44rpx;
}

.tabbar__text {
	margin-top: 6rpx;
	font-size: 22rpx;
	color: $text-muted;
}

.tabbar__item--active .tabbar__text {
	color: $green-primary;
}

.tabbar__fab {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 100rpx;
	height: 100rpx;
	border-radius: 50%;
	background-color: $green-primary;
	box-shadow: 0 8rpx 20rpx rgba(74, 107, 62, 0.28);
	transform: translateY(-18rpx);
}

.tabbar__fab-icon {
	font-size: 48rpx;
	line-height: 1;
	color: #ffffff;
}
</style>
