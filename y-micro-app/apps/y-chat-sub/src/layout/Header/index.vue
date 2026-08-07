<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import UserInfo from './UserInfo/UserInfo.vue'
import NotificationCenter from './NotificationCenter.vue'
import { Bell, Setting, SwitchButton } from "@element-plus/icons-vue";
import { getUnreadCount } from '@/api/notification.ts'

const notificationVisible = ref(false)
const unreadCount = ref(0)
let timer: any = null

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    if (res.code === 200) {
      unreadCount.value = Number(res.data || 0)
    }
  } catch {
    // ignore
  }
}

const openNotification = () => {
  notificationVisible.value = true
}

const onNotificationRefresh = () => {
  fetchUnreadCount()
}

onMounted(() => {
  fetchUnreadCount()
  timer = setInterval(fetchUnreadCount, 30000)
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
      <div class="msg-btn">
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