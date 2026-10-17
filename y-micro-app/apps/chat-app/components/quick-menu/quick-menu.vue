<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { onShow } from '@dcloudio/uni-app'
import useNotificationStore from '@/store/notification'
import useUserStore from '@/store/user'

/**
 * 首页导航栏右侧快捷入口
 *
 * 通知铃铛（带未读角标）+ 用户头像入口，替代网页端 Header 中的对应按钮。
 */
const { unreadCount } = storeToRefs(useNotificationStore())
const { userInfo } = storeToRefs(useUserStore())

const hasUnread = computed(() => unreadCount.value > 0)
const badgeText = computed(() => (unreadCount.value > 99 ? '99+' : String(unreadCount.value)))

function goNotice() {
	uni.navigateTo({ url: '/pages/notice/index' })
}

function goUser() {
	uni.navigateTo({ url: '/pages/user/index' })
}

onShow(() => {
	uni.hideTabBar()
})
</script>

<template>
	<view class="quick-menu">
		<image src="@/static/plus.png" mode="scaleToFill"></image>
	</view>
</template>

<style scoped lang="scss">
.quick-menu {
	display: flex;
	align-items: center;

	image {
		width: 34rpx;
		height: 34rpx;
	}
}
</style>
