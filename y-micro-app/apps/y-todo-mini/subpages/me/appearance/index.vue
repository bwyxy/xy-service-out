<script setup>
import { computed } from 'vue'
import { appearance, setFontSize, setPreference, setTheme } from '@/utils/appearance'

	/**
	 * 我的 · 外观与显示
	 *
	 * 路由：subpages/me/appearance/index（分包 subpages/me，按需加载）
	 * 职责：外观与显示设置页，看着顺眼才愿意每天打开——主题模式、字号档位与显示偏好。
	 * 数据：全部读写 utils/appearance 里的本地设置，改完立刻生效，不上传后端。
	 * 目录：状态（THEMES、FONT_SIZES、preferences）→ 方法（selectTheme、selectFontSize、toggle）→ 样式（.section、.themes、.size、.group）
	 */

/** 主题模式 */
const THEMES = [
	{ key: 'system', label: '跟随系统', icon: 'contrast' },
	{ key: 'light', label: '浅色', icon: 'sun' },
	{ key: 'dark', label: '深色', icon: 'moon' }
]

/** 字号档位 */
const FONT_SIZES = [
	{ key: 'small', label: '小' },
	{ key: 'normal', label: '标准' },
	{ key: 'large', label: '大' }
]

/** 显示偏好：开关值直接读设置项，改完即写回本地 */
const preferences = computed(() => [
	{ key: 'showDone', label: '显示已完成任务', desc: '关闭后「今日」只显示待完成', on: appearance.showDone },
	{ key: 'showProgress', label: '首页显示进度卡', desc: '在「今日」顶部展示完成进度', on: appearance.showProgress },
	{ key: 'weekStart', label: '周一开始一周', desc: '日历与周视图的起始日', on: appearance.weekStart }
])

/** 切换主题：本页根节点也挂着主题类，选完就能看到效果 */
function selectTheme(key) {
	setTheme(key)
}

/** 切换字号 */
function selectFontSize(key) {
	setFontSize(key)
}

/** 切换开关 */
function toggle(item, event) {
	setPreference(item.key, event.detail.value)
}
</script>

<template>
	<sub-page title="外观与显示" subtitle="看着顺眼，才愿意每天打开。">
		<view class="section">
			<text class="section__title">主题模式</text>

			<view class="themes">
				<view
					v-for="item in THEMES"
					:key="item.key"
					class="theme"
					:class="{ 'theme--active': appearance.theme === item.key }"
					@click="selectTheme(item.key)"
				>
					<view class="theme__icon" :class="'theme__icon--' + item.key">
						<y-icon :name="item.icon" size="40rpx" />
					</view>

					<text class="theme__label">{{ item.label }}</text>

					<y-icon v-if="appearance.theme === item.key" name="check" size="28rpx" />
				</view>
			</view>
		</view>

		<view class="section">
			<text class="section__title">字号</text>

			<view class="sizes">
				<view
					v-for="item in FONT_SIZES"
					:key="item.key"
					class="size"
					:class="{ 'size--active': appearance.fontSize === item.key }"
					@click="selectFontSize(item.key)"
				>
					<text class="size__label">{{ item.label }}</text>
				</view>
			</view>
		</view>

		<view class="section">
			<text class="section__title">显示偏好</text>

			<view class="group">
				<view
					v-for="item in preferences"
					:key="item.key"
					class="row row--switch"
				>
					<view class="row__text">
						<text class="row__label">{{ item.label }}</text>
						<text class="row__desc">{{ item.desc }}</text>
					</view>

					<switch :checked="item.on" color="#4a6b3e" @change="toggle(item, $event)" />
				</view>
			</view>
		</view>

		<text class="foot">再好看一点，日子也就顺一点。</text>
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

/* 主题 */
.themes {
	display: flex;
	margin-top: 24rpx;
}

.theme {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 28rpx 12rpx;
	margin-right: 20rpx;
	border-radius: 24rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;

	&:last-child {
		margin-right: 0;
	}
}

.theme--active {
	border-color: $y-green-border;
	background-color: $y-green-soft;
}

.theme__icon {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 88rpx;
	height: 88rpx;
	border-radius: 50%;
	background-color: $y-sunken-bg;
}

.theme__icon--dark {
	background-color: $y-contrast-chip;
}

.theme__label {
	margin: 16rpx 0 12rpx;
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

/* 字号 */
.sizes {
	display: flex;
	margin-top: 24rpx;
	padding: 6rpx;
	border-radius: 18rpx;
	background-color: $y-sunken-bg;
}

.size {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 64rpx;
	border-radius: 14rpx;
}

.size--active {
	background-color: $y-card-bg;
}

.size__label {
	font-size: calc(26rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.size--active .size__label {
	font-weight: 500;
	color: $y-text-main;
}

/* 显示偏好 */
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
