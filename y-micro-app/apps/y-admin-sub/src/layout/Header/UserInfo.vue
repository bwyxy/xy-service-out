<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import useUserStore from "@/store/modules/user.ts";
import {ArrowDown, User} from "@element-plus/icons-vue";
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)

const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('退出成功')
  router.push('/login')
}
</script>

<template>
  <el-dropdown trigger="click">
    <div class="user-info">
      <div class="user-avatar">
        <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="avatar" />
        <el-icon v-else><User /></el-icon>
      </div>
      <span>{{ userInfo.nickname || '未登录' }}</span>
      <el-icon><ArrowDown /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item>个人资料</el-dropdown-item>
        <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
.user-info {
  display: flex;
  align-items: center;
  font-size: size(14);
  cursor: pointer;
  margin-left: size(24);
}

.user-avatar {
  width: size(24);
  height: size(24);
  background: #4096ff;
  border-radius: 50%;
  color: #fff;
  margin-right: size(8);
  @include flexCenter;
  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
  }
}
</style>