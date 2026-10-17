<template>
	<scroll-view class="list-main" :scroll-y="true" @scrolltolower="$emit('scrolltolower')">
		<slot></slot>
	</scroll-view>
</template>

<script setup>
defineEmits(['scrolltolower'])
</script>

<style lang="scss">
	.list-main {
		flex: 1;
		overflow: hidden;
	}
</style>
