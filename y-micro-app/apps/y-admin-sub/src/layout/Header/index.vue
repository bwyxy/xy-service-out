<script setup lang="ts">
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="header-bar">
    <div class="header-left">
      <span class="breadcrumb">首页</span>
    </div>
    <div class="header-right">
      <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}</span>
      <el-button type="danger" text size="small" @click="handleLogout">退出</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.header-bar {
  height: 48px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

  .header-left {
    font-size: 14px;
    color: #333;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .username {
      font-size: 14px;
      color: #666;
    }
  }
}
</style>