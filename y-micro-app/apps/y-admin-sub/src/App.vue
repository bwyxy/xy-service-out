<script setup lang="ts">
import { onMounted } from 'vue'
import useThemeStore from '@/store/modules/theme'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const themeStore = useThemeStore()

onMounted(() => {
  themeStore.init()
})
</script>

<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<style scoped>

</style>
