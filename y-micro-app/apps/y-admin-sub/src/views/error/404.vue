<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()
const goHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <p class="error-desc">{{ t('error.pageNotFound') }}</p>
      <el-button type="primary" @click="goHome">{{ t('error.goHome') }}</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
}

.error-content {
  text-align: center;
}

.error-code {
  font-size: 80px;
  font-weight: bold;
  color: var(--el-text-color-placeholder);
  margin: 0 0 16px;
}

.error-desc {
  font-size: 16px;
  color: var(--app-text-secondary);
  margin-bottom: 24px;
}
</style>