<template>
	<view class="user-info" @click="toUserPage">
		<wd-img width="64rpx" height="64rpx" round :src="userInfo.avatar"></wd-img>
		<view class="user-info-content">
			<text>{{ userInfo.nickname }}</text>
			<text>在线</text>
		</view>
	</view>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import useUserStore from '@/store/user';

const { userInfo } = storeToRefs(useUserStore())

const toUserPage = () => {
	uni.navigateTo({
		url: '/pages/user/index'
	})
}
</script>

<style lang="scss" scoped>
.user-info {
	height: 100%;
	display: flex;
	align-items: center;
	&-content {
		height: 64rpx;
		display: flex;
		flex-direction: column;
		margin-left: 16rpx;
		overflow: hidden;
		text:nth-child(1) {
			flex: 1;
			font-size: 24rpx;
			line-height: 36rpx;
			font-weight: 600;
			letter-spacing: 1rpx;
			color: #1f1f1f;
		}
		text:nth-child(2) {
			font-size: 20rpx;
			color: #52c41a;
			line-height: 22rpx;
			position: relative;
			display: flex;
			align-items: center;
			padding-left: 16rpx;
			&:before {
				content: '';
				width: 10rpx;
				height: 10rpx;
				border-radius: 50px;
				background: #52c41a;
				position: absolute;
				left: 0;
			}
		}
	}
}
</style>
