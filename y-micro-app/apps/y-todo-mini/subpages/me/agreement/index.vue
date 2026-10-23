<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

	/**
	 * 我的 · 隐私政策与用户协议
	 *
	 * 路由：subpages/me/agreement/index?tab=privacy（默认隐私政策）或 ?tab=terms（用户协议）
	 * 职责：一页装下两篇协议，顶部可切换；入口在「关于忆」页与首次启动的隐私同意弹框。
	 * 内容：静态文案，按这个小程序实际收集与使用的信息来写；功能有大改动时同步更新日期。
	 * 目录：状态（tab）→ 计算（current）→ 方法（selectTab）→ 样式（.tabs、.doc、.section、.foot）
	 */

/** 协议更新日期 */
const UPDATED_AT = '2026-10-23'

/** 切换项 */
const TABS = [
	{ key: 'privacy', label: '隐私政策' },
	{ key: 'terms', label: '用户协议' }
]

/** 两篇协议 */
const AGREEMENTS = {
	privacy: {
		title: '隐私政策',
		subtitle: `本指引是忆待办小程序开发者 （以下简称“开发者”）为处理你的个人信息而制定。`,
		sections: [
			{
				heading: '一、开发者处理的信息',
				paragraphs: [
					'根据法律规定，开发者仅处理实现小程序功能所必要的信息。',
					'1. 开发者将在获取你的明示同意后，收集你的微信昵称、头像，用途是【用于完善个人信息，便于视觉展示】',
					'2. 你主动填写的内容：待办、清单、习惯与专注记录，用于为你保存和展示。',
					'3. 设备与运行信息：机型、系统版本与错误日志，只用于排查故障与改进体验。'
				]
			},
			{
				heading: '二、我们如何使用这些信息',
				paragraphs: [
					'1. 收集你的微信昵称、头像, 用于完善个人信息，便于视觉展示',
					'2. 使用你的相册（仅写入）权限, 用于完善您的个人头像',
					'3. 提供待办记录、清单分类、习惯打卡、番茄专注与多端同步等基础功能。',
					'4. 在你设置了提醒之后，通过微信订阅消息把对应提醒送达给你。',
					'5. 汇总你的累计完成数与连续记录天数，展示在「我的」页。',
					'除上述用途外，我们不会把这些信息用于其他目的。'
				]
			},
			{
				heading: '三、信息的存储与保护',
				paragraphs: [
					'仅在为你提供服务所必需的期间内保留数据；你删除的内容不再展示，并在备份周期结束后清除。',
					'开发者承诺，除法律法规另有规定外，开发者对你的信息的保存期限应当为实现处理目的所必要的最短时间。'
				]
			},
			{
				heading: '四、我们如何共享信息',
				paragraphs: [
					'我们不会向任何第三方出售或提供你的个人信息。',
					'只有在你授权订阅消息后，微信才会按我们的请求下发提醒内容。',
					'法律法规另有规定，或主管部门依法要求时，我们可能依法提供必要信息。',
					'如开发者使用你的信息超出本指引目的或合理范围，开发者必须在变更使用目的或范围前，再次以用户授权的方式告知并征得你的明示同意。'
				]
			},
			{
				heading: '五、你你的权益',
				paragraphs: [
					'查看与修改：在「我的」页和各列表页随时查看、编辑你的内容。',
					'删除：可以删除单条任务，也可以用「清空已完成」批量清理。',
					'撤回授权：在微信「设置 - 隐私 - 授权管理」中关闭对忆待办的授权。',
					'注销账号：通过「我的 - 帮助与反馈」联系我们，我们会删除你的账号与全部数据。',
					'关于你的个人信息，你可以通过以下方式与开发者联系，行使查阅、复制、更正、删除等法定权利。',
					'若你在小程序中注册了账号，你可以通过以下方式与开发者联系，申请注销你在小程序中使用的账号。在受理你的申请后，开发者承诺在十五个工作日内完成核查和处理，并按照法律法规要求处理你的相关信息。',
					'邮箱: bwyxy@qq.com'
				]
			},
			{
				heading: '六、未成年人',
				paragraphs: [
					'根据相关法律法规的规定，若你是14周岁以下的未成年人，你需要和你的监护人共同仔细阅读本指引，并在征得监护人明示同意后继续使用小程序服务。开发者将根据相关法律法规的规定及本指引内容，处理经监护人同意而收集的未成年人用户信息，并通过【本指引你的权益部分】披露的内容保障未成年人在个人信息处理活动中的各项权利。'
				]
			},
			{
				heading: '七、政策的更新与联系',
				paragraphs: [
					'本政策可能随功能调整而更新，更新后会在本页展示新的日期。',
					'如对本政策有疑问，请通过「我的 - 帮助与反馈」联系我们。'
				]
			}
		]
	},
	terms: {
		title: '用户协议',
		subtitle: `更新日期 ${UPDATED_AT}`,
		sections: [
			{
				heading: '一、服务说明',
				paragraphs: [
					'忆待办为你提供待办记录、清单整理、提醒、番茄专注、习惯打卡与数据同步等功能。'
				]
			},
			{
				heading: '二、账号与登录',
				paragraphs: [
					'你通过微信授权登录使用本小程序，一个微信身份对应一个账号。',
					'账号仅限你本人使用，请勿转让他人，因转让造成的损失由你自行承担。'
				]
			},
			{
				heading: '三、你填写的内容',
				paragraphs: [
					'你对自己填写的内容负责，请勿发布违法违规或侵犯他人权益的信息。',
					'内容归你所有；为了展示与同步，你授权我们存储与处理这些内容。'
				]
			},
			{
				heading: '四、关于提醒服务',
				paragraphs: [
					'提醒依赖微信订阅消息下发，授权额度与送达情况由微信平台决定，可能出现延迟或未送达。',
					'重要事项请另行留意，不要仅依赖本小程序发出的提醒。'
				]
			},
			{
				heading: '五、知识产权',
				paragraphs: [
					'小程序的界面、图标与代码归我们所有，未经许可请勿复制或用于商业用途。'
				]
			},
			{
				heading: '六、服务的变更与终止',
				paragraphs: [
					'我们可能调整或停止部分功能，涉及你权益的重大调整会提前告知。',
					'你可以随时停止使用；如需删除数据，请通过「帮助与反馈」联系我们。'
				]
			},
			{
				heading: '七、免责与争议解决',
				paragraphs: [
					'因不可抗力、网络故障等导致服务中断的，我们不承担责任，但会尽力恢复。',
					'与我们发生争议时，双方应先友好协商解决。'
				]
			},
			{
				heading: '八、协议的更新与联系',
				paragraphs: [
					'本协议可能更新，更新后会在本页展示；继续使用即视为接受更新后的内容。',
					'如有疑问，请通过「我的 - 帮助与反馈」联系我们。'
				]
			}
		]
	}
}

/** 当前查看的协议 */
const tab = ref('privacy')

/** 当前协议内容 */
const current = computed(() => AGREEMENTS[tab.value] || AGREEMENTS.privacy)

/** 切换协议：切完回到顶部，别让新的一篇停在上一篇的位置 */
function selectTab(key) {
	if (key === tab.value) {
		return
	}

	tab.value = key
	uni.pageScrollTo({ scrollTop: 0, duration: 0 })
}

/** 入口带参数：privacy 或 terms，其余值一律回落隐私政策 */
onLoad((query) => {
	const value = query && query.tab

	if (value && AGREEMENTS[value]) {
		tab.value = value
	}
})
</script>

<template>
	<sub-page :title="current.title" :subtitle="current.subtitle">
		<view class="tabs">
			<view
				v-for="item in TABS"
				:key="item.key"
				class="tabs__item"
				:class="{ 'tabs__item--active': tab === item.key }"
				@click="selectTab(item.key)"
			>
				<text class="tabs__label">{{ item.label }}</text>
			</view>
		</view>

		<view class="doc">
			<view v-for="section in current.sections" :key="section.heading" class="section">
				<text class="section__heading">{{ section.heading }}</text>

				<text
					v-for="(paragraph, index) in section.paragraphs"
					:key="index"
					class="section__paragraph"
				>
					{{ paragraph }}
				</text>
			</view>
		</view>

		<text class="foot">如对以上内容有疑问，可通过「我的 - 帮助与反馈」联系我们。</text>
	</sub-page>
</template>

<style scoped lang="scss">
/* 切换 */
.tabs {
	display: flex;
	margin-top: 32rpx;
	padding: 6rpx;
	border-radius: 18rpx;
	background-color: $y-sunken-bg;
}

.tabs__item {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 64rpx;
	border-radius: 14rpx;
}

.tabs__item--active {
	background-color: $y-card-bg;
}

.tabs__label {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.tabs__item--active .tabs__label {
	font-weight: 500;
	color: $y-text-main;
}

/* 正文 */
.doc {
	margin-top: 28rpx;
	padding: 40rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.section + .section {
	margin-top: 40rpx;
}

.section__heading {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.section__paragraph {
	display: block;
	margin-top: 16rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	line-height: 46rpx;
	color: $y-text-body;
}

/* 页脚 */
.foot {
	display: block;
	margin-top: 40rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 1.7;
	color: $y-text-muted;
	text-align: center;
}
</style>
