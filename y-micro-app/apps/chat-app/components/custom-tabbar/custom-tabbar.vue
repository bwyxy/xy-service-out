<template>
	<view class="custom-tabbar">
		<view class="custom-tabbar-content">
			<view
				v-for="(item, index) in tabbars"
				:key="index"
				:class="['tabbar-item', {'tabbar-item_active': currentPath === item.path}]"
				@click="toPage(item.path)"
			>
				<image
					:src="currentPath === item.path ? item.activeIcon : item.icon"
					mode="scaleToFill"
				></image>
				<text>{{ item.text }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue'
import { onShow } from '@dcloudio/uni-app'
import homeIcon from '@/static/tabbar/home.png'
import homeActiveIcon from '@/static/tabbar/home_active.png'
import communityIcon from '@/static/tabbar/community.png'
import communityActiveIcon from '@/static/tabbar/community_active.png'
import contactIcon from '@/static/tabbar/contact.png'
import contactActiveIcon from '@/static/tabbar/contact_active.png'
import dynamicIcon from '@/static/tabbar/dynamic.png'
import dynamicActiveIcon from '@/static/tabbar/dynamic_active.png'

const currentPath = ref('')
const tabbars = reactive([
	{
		icon: homeActiveIcon,
		activeIcon: homeIcon,
		path: 'pages/index/index',
		text: '首页'
	},
	{
		icon: communityIcon,
		activeIcon: communityActiveIcon,
		path: 'pages/community/index',
		text: '社区'
	},
	{
		icon: contactIcon,
		activeIcon: contactActiveIcon,
		path: 'pages/contact/index',
		text: '联系人'
	},
	{
		icon: dynamicIcon,
		activeIcon: dynamicActiveIcon,
		path: 'pages/dynamic/index',
		text: '动态'
	}
])


const toPage = (url) => {
	uni.switchTab({
		url: '/' + url
	})
}

onShow(() => {
	const pages = getCurrentPages()

	currentPath.value = pages[0].route
})
</script>

<style lang="scss" scoped>

.custom-tabbar {
	width: 100%;
	box-sizing: border-box;
	padding: 0 24rpx 24rpx;
	position: fixed;
	left: 0;
	bottom: 0;
}

.custom-tabbar-content {
	width: 100%;
	height: 100rpx;
	background: rgba(255, 255, 255, .6);
	border: 1rpx solid rgba(255, 255, 255, .8);
	box-sizing: border-box;
	border-radius: 45rpx;
	backdrop-filter: blur(10px);
	display: flex;
}

.tabbar-item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	image {
		width: 42rpx;
		height: 42rpx;
	}
	text {
		font-size: 20rpx;
		color: #1f1f1f;
		letter-spacing: 1px;
		margin-top: 4rpx;
	}
}

.tabbar-item_active {
	text {
		color: #1890ff;
	}
}
</style>
