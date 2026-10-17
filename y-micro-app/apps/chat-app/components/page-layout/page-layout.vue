<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'

/**
 * 页面通用布局
 *
 * 提供「状态栏安全区 + 自定义导航栏 + 内容区」结构，供各页面复用。
 * - 所有页面在 pages.json 中均配置 navigationStyle: custom，导航栏由本组件绘制
 * - scrollable 为 true（默认）时，内容区自动包裹 scroll-view，页面内无需重复处理滚动
 */
const props = defineProps({
	/** 导航栏标题 */
	title: { type: String, default: '' },
	/** 是否显示返回按钮 */
	showBack: { type: Boolean, default: false },
	/** 内容区是否使用 scroll-view 包裹（聊天页需自行控制滚动时可关闭） */
	scrollable: { type: Boolean, default: true },
})

const emit = defineEmits(['back'])

/** 状态栏高度（刘海屏安全区） */
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

/** 导航栏（含状态栏）总高度 */
const navBarHeight = computed(() => statusBarHeight + 44)

function handleBack() {
	emit('back')

	if (getCurrentPages().length > 1) {
		uni.navigateBack()
	}
}

onShow(() => {
	uni.hideTabBar()
})
</script>

<template>
	<view class="page-layout">
		<slot></slot>

		<custom-tabbar />
	</view>
</template>

<style scoped lang="scss">

</style>
