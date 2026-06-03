<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useAppStore from "@/store/modules/app.ts";

defineProps<{
  theme: boolean
}>()

const { blogInfo } = storeToRefs(useAppStore())

</script>

<template>
  <div :class="`layout-footer${theme ? '_dark' : ''}`">
    <span>©2024-{{ new Date().getFullYear() }} by Mr.XiaoYi's leisure space</span>
    <span>{{ blogInfo.recordNumber || '' }}</span>
  </div>
</template>

<style scoped lang="scss">
.layout-footer, .layout-footer_dark {
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 24px;
  margin-top: 48px;
  span {
    font-size: 14px;
    font-weight: 400;
    letter-spacing: 1px;
    line-height: 32px;
  }
}

.layout-footer_dark {
  span {
    color: rgba(255, 255, 255, 0.8);
  }
}
</style>