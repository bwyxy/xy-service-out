<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { Bell, Setting, SwitchButton } from '@element-plus/icons-vue'
import useUserStore from '@/store/user.ts'
import useNotificationStore from '@/store/notification.ts'
import UserInfo from './UserInfo/UserInfo.vue'
import NotificationCenter from './NotificationCenter.vue'
import WindowControls from '@/components/WindowControls/index.vue'
import { isMacOS, isTauri } from '@/utils/tauri.ts'

// macOS 使用系统原生红绿灯（叠加在窗口左上角），头部左侧需为其预留空间
const isMac = isTauri() && isMacOS()

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
  <div class="chat-header" :class="{ 'chat-header--mac': isMac }" data-tauri-drag-region>
    <user-info />

    <div class="chat-header-rt" data-tauri-drag-region>
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

      <window-controls />
    </div>

    <notification-center v-model:visible="notificationVisible" @refresh="onNotificationRefresh" />
  </div>
</template>

<style scoped lang="scss">
.chat-header {
  height: size(52);
  box-sizing: border-box;
  padding: 0 size(12) 0 size(24);
  display: flex;
  align-items: center;

  // macOS：为左上角系统红绿灯预留空间
  &--mac {
    padding-left: size(78);
  }

  &-rt {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
}

// 自定义窗口按钮与右侧功能区留出间距
.window-controls {
  margin-left: size(18);
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
