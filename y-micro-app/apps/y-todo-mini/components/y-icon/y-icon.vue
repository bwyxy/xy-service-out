<script setup>
import { computed } from 'vue'
import { buildIcon } from './icons'

/**
 * 线稿图标
 *
 * 以 URL 编码的 SVG data uri 作为背景图渲染，无需字体文件与静态资源，
 * 颜色随 color 动态生成，便于整体替换为设计稿导出的图标。
 */
const props = defineProps({
	/** 图标名，见 icons.js 的 ICON_PATHS */
	name: { type: String, required: true },
	/** 图标尺寸 */
	size: { type: String, default: '44rpx' },
	/** 描边颜色 */
	color: { type: String, default: '#4a6b3e' }
})

const style = computed(() => ({
	width: props.size,
	height: props.size,
	backgroundImage: `url(${buildIcon(props.name, props.color)})`
}))
</script>

<template>
	<view class="y-icon" :style="style" />
</template>

<style scoped lang="scss">
.y-icon {
	flex-shrink: 0;
	background-repeat: no-repeat;
	background-position: center;
	background-size: 100% 100%;
}
</style>
