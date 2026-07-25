<script setup lang="ts">
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
</script>

<template>
  <div class="home-container">
    <el-card>
      <template #header>
        <span>欢迎回来</span>
      </template>
      <p>你好，{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}</p>
      <p>欢迎使用忆待办后台管理系统</p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.home-container {
  .el-card {
    max-width: 600px;
  }
}
</style>