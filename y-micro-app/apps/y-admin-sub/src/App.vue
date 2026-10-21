<script setup lang="ts">
import { computed, onMounted } from 'vue'
import useThemeStore from '@/store/modules/theme'
import useLocaleStore from '@/store/modules/locale'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'

const themeStore = useThemeStore()
const localeStore = useLocaleStore()

// Element Plus 组件语言需跟随全局语言切换，否则分页、空数据等仍是中文
const elementLocale = computed(() => (localeStore.locale === 'en-US' ? en : zhCn))

onMounted(() => {
  themeStore.init()
})
</script>

<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<style scoped>

</style>
