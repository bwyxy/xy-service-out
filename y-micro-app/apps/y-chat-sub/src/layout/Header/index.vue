<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { Bell, Setting, SwitchButton } from '@element-plus/icons-vue'
import useUserStore from '@/store/user.ts'
import useNotificationStore from '@/store/notification.ts'
import UserInfo from './UserInfo/UserInfo.vue'
import NotificationCenter from './NotificationCenter.vue'

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { unreadCount } = storeToRefs(notificationStore)

const notificationVisible = ref(false)

let timer: ReturnType<typeof setInterval> | null = null

const openNotification = () => {
  notificationVisible.value = true
}

const onNotificationRefresh = () => {
  notificationStore.fetchUnreadCount()
}

const handleLogout = () => {
  userStore.outLogin()
  notificationStore.reset()
}

onMounted(() => {
  notificationStore.fetchUnreadCount()
  // 每 60 秒同步一次后端未读数
  timer = setInterval(() => {
    notificationStore.fetchUnreadCount()
  }, 60000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <div class="chat-header">
    <user-info />

    <div class="chat-header-rt">
      <div class="msg-btn" @click="openNotification">
        <el-badge :value="unreadCount" :hidden="unreadCount <= 0" class="notice-badge">
          <el-icon><Bell /></el-icon>
        </el-badge>
        <span>消息通知</span>
      </div>

      <div class="setting-btn">
        <el-icon><Setting /></el-icon>
      </div>

      <div class="msg-btn" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span>退出登录</span>
      </div>
    </div>

    <notification-center v-model:visible="notificationVisible" @refresh="onNotificationRefresh" />
  </div>
</template>

<style scoped lang="scss">
.chat-header {
  height: size(52);
  box-sizing: border-box;
  padding: 0 size(24);
  display: flex;
  align-items: center;

  &-rt {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
}

.msg-btn {
  @include boxBtn(48, 32, 4);
  font-size: size(14);
  cursor: pointer;
}

.setting-btn {
  @include boxBtn(24, 32, 4);
  font-size: size(20);
  margin: 0 size(24);
}

.notice-badge {
  line-height: 1;
}
</style>
