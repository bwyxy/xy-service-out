<script setup lang="ts">

import {ArrowRightBold} from "@element-plus/icons-vue";

defineProps<{
  title?: string,
  topPath?: () => void
}>()
</script>

<template>
  <div class="statistics-box">
    <div v-if="title" class="statistics-box-title">
      <span>{{ title }}</span>
      <el-icon v-if="topPath"><ArrowRightBold /></el-icon>
    </div>
    <div class="statistics-box-content">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped lang="scss">
.statistics-box {
  width: 100%;
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  border-radius: 20px;
  box-sizing: border-box;
  padding: 24px;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow:
      0 2px 4px rgba(0, 0, 0, 0.06),
      0 8px 24px rgba(0, 0, 0, 0.08);
  }
}

.statistics-box-title {
  border-bottom: 1px solid #f0f2f5;
  display: flex;
  align-items: center;
  font-size: 15px;
  padding-bottom: 18px;

  span {
    flex: 1;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: #1a1a1a;
  }
}

.statistics-box-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>