<script setup lang="ts">
import { onMounted } from 'vue'
import useThemeStore from '@/store/modules/theme'

const themeStore = useThemeStore()

onMounted(() => {
  themeStore.init()
})
</script>

<template>
  <router-view />
</template>

<style scoped>

</style>
