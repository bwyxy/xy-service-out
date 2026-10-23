<script setup>
import { ref } from 'vue'

	/**
	 * 我的 · 帮助与反馈
	 *
	 * 路由：subpages/me/help/index（分包 subpages/me，按需加载）
	 * 职责：帮助与反馈页，有问题我们一起看看——常见问题折叠与意见反馈入口。
	 * 目录：状态（faqs）→ 方法（toggleFaq、openFeedback）→ 样式（.section、.faqs、.faq、.group）
	 */

/** 常见问题（暂用本地数据） */
const faqs = ref([
	{
		id: 1,
		question: '任务没有按时提醒我，怎么办？',
		answer: '先检查「我的 → 提醒设置」里的任务到期提醒是否开启，再确认手机的「通知」权限是否允许忆发送提醒。',
		open: false
	},
	{
		id: 2,
		question: '换手机后数据会丢吗？',
		answer: '开启「数据同步与备份」后，任务与清单会保存到云端，换设备登录同一账号即可恢复。',
		open: false
	},
	{
		id: 3,
		question: '怎么把已完成的任务藏起来？',
		answer: '在「外观与显示」里关闭「显示已完成任务」，或在「已完成与归档」里把记录归档。',
		open: false
	},
	{
		id: 4,
		question: '清空已完成的任务会影响到别的设备吗？',
		answer: '清空只作用在当前账号的这份记录上，同步后其他设备也会保持一致。',
		open: false
	}
])

/** 展开 / 收起问题 */
function toggleFaq(item) {
	item.open = !item.open
}

/** 进入意见反馈 */
function openFeedback() {
	uni.navigateTo({ url: '/subpages/me/feedback/index' })
}
</script>

<template>
	<sub-page title="帮助与反馈" subtitle="有问题，我们一起看看。">
		<view class="section">
			<text class="section__title">常见问题</text>

			<view class="faqs">
				<view v-for="item in faqs" :key="item.id" class="faq">
					<view class="faq__head" @click="toggleFaq(item)">
						<text class="faq__question">{{ item.question }}</text>
						<y-icon
							:name="item.open ? 'chevron-left' : 'chevron-right'"
							size="30rpx"
							color="#c2c2b9"
							:style="{ transform: `rotate(${item.open ? '90deg' : '0deg'})` }"
						/>
					</view>

					<text v-if="item.open" class="faq__answer">{{ item.answer }}</text>
				</view>
			</view>
		</view>

		<view class="section">
			<text class="section__title">还想要更多帮助</text>

			<view class="group">
				<view class="row" @click="openFeedback">
					<y-icon name="message" size="38rpx" />

					<view class="row__text">
						<text class="row__label">意见反馈</text>
						<text class="row__desc">告诉我们哪里可以更好</text>
					</view>

					<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
				</view>
			</view>
		</view>

		<text class="foot">你的每一条反馈，都会被读到。</text>
	</sub-page>
</template>

<style scoped lang="scss">
.section {
	margin-top: 44rpx;
}

.section__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

/* 常见问题 */
.faqs {
	margin-top: 24rpx;
	padding: 4rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.faq {
	padding: 30rpx 0;

	& + .faq {
		border-top: 2rpx solid $y-divider;
	}
}

.faq__head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.faq__question {
	flex: 1;
	margin-right: 16rpx;
	font-size: calc(28rpx * var(--y-font-scale, 1));
	line-height: 40rpx;
	color: $y-text-main;
}

.faq__answer {
	display: block;
	margin-top: 16rpx;
	padding: 20rpx 24rpx;
	border-radius: 18rpx;
	background-color: $y-sunken-bg;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 38rpx;
	color: $y-text-sub;
}

/* 联系方式 */
.group {
	margin-top: 24rpx;
	padding: 4rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.row {
	display: flex;
	align-items: center;
	padding: 30rpx 0;

	& + .row {
		border-top: 2rpx solid $y-divider;
	}
}

.row__text {
	flex: 1;
	display: flex;
	flex-direction: column;
	margin-left: 24rpx;
}

.row__label {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__desc {
	margin-top: 10rpx;
	font-size: calc(22rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
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
