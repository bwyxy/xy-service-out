<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const goHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <p class="error-desc">抱歉，你访问的页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
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
  color: #dcdfe6;
  margin: 0 0 16px;
}

.error-desc {
  font-size: 16px;
  color: #909399;
  margin-bottom: 24px;
}
</style>