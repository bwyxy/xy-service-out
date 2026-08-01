<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from '@/store/user.ts'

const { userInfo } = storeToRefs(useUserStore())
</script>

<template>
  <div class="user-info">
    <div class="user-info-avatar">
      <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="avatar" />
      <img v-else src="@/assets/images/logo.jpg" alt="avatar" />
    </div>
    <span>{{ userInfo.nickname || '未登录' }}</span>
  </div>
</template>

<style scoped lang="scss">
.user-info {
  display: flex;
  align-items: center;
  span {
    font-size: size(16);
    font-weight: 700;
    letter-spacing: 1px;
    margin-left: size(6);
  }
}

.user-info-avatar {
  width: size(28);
  height: size(28);
  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
  }
}
</style>