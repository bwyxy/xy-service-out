<script setup lang="ts">
import { useRouter } from 'vue-router'
import Logo from './Logo.vue'
import UserInfo from './UserInfo.vue'
import useUserStore from '@/store/modules/user'
import {BellFilled, Setting, SwitchButton} from "@element-plus/icons-vue";

const router = useRouter()
const userStore = useUserStore()

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="header-bar">
    <logo />

    <user-info />

    <div class="header-bar-rt">

      <div class="option-btn">
        <el-icon><BellFilled /></el-icon>
        <span>消息通知</span>
      </div>

      <div class="setting-btn">
        <el-icon><Setting /></el-icon>
      </div>

      <div class="option-btn" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span>退出登录</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.header-bar {
  height: size(48);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 size(24);

  .header-left {
    font-size: 14px;
    color: #333;
  }

  .header-bar-rt {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
}

.setting-btn {
  @include boxBtn(13, 28, 4);
  margin: 0 size(24);
}

.option-btn {
  @include boxBtn(48, 28, 4);
}
</style>