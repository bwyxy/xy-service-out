<template>
	<!-- 微信小程序弹出层 -->
	<view v-if="isShowPopup" class="model-popup" :style="{zIndex: zIndex}">
		<page-container
			:show="modelValue"
			:z-index="zIndex"
			:overlay="overlay"
			:position="position"
			:duration="duration"
			custom-style="background: none;"
			@beforeleave="handleClose"
		>
			<slot></slot>
		</page-container>
	</view>
</template>

<script setup>
	import { ref, toRefs, watch } from 'vue'

	/** 弹框是否显示 */
	const isShowPopup = ref(false)
	const props = defineProps({
		modelValue: {
			type: Boolean,
			default: false
		},
		zIndex: {
			type: Number,
			default: 9999
		},
		overlay: {
			type: Boolean,
			default: true
		},
		position: {
			type: String,
			default: 'bottom'
		},
		duration: {
			type: Number,
			default: 300
		}
	})
	const emit = defineEmits(['update:modelValue', 'beforeClose'])
	const { modelValue, duration } = toRefs(props)

	watch(() => modelValue.value, () => {
		console.log(modelValue.value)
		if (modelValue.value) {
			isShowPopup.value = true
		} else {
			const timer = setTimeout(() => {
				isShowPopup.value = false
				clearTimeout(timer)
			}, duration.value)
		}
	}, { immediate: true })

	const handleClose = () => {
		emit('update:modelValue', false)
		emit('beforeClose')
	}
</script>

<style lang="scss" scoped>
	.model-popup {
		width: 100vw;
		height: 100vh;
		position: fixed;
		left: 0;
		top: 0;
	}
</style>
