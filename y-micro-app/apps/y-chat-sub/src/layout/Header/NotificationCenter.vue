<script setup lang="ts">
import { watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import useNotificationStore from '@/store/notification.ts'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'refresh'): void
}>()

const notificationStore = useNotificationStore()
const { notifications, loading } = storeToRefs(notificationStore)

watch(
  () => props.visible,
  (val) => {
    if (val) {
      notificationStore.fetchNotifications()
    }
  },
)

const handleMarkRead = async (item: any) => {
  if (item.isRead === 1) return

  const success = await notificationStore.markRead(item.id)
  if (success) {
    emit('refresh')
  }
}

const handleMarkAllRead = async () => {
  const success = await notificationStore.markAllRead()
  if (success) {
    emit('refresh')
    ElMessage.success('已全部标记为已读')
  }
}

const handleClose = () => {
  emit('update:visible', false)
}

const formatTime = (time: string) => {
  if (!time) return ''
  return time.replace('T', ' ')
}
</script>

<template>
  <el-dialog
    v-model="props.visible"
    title="通知中心"
    width="560px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="notification-header">
      <span>共 {{ notifications.length }} 条通知</span>
      <el-button link type="primary" @click="handleMarkAllRead">全部已读</el-button>
    </div>
    <div v-loading="loading" class="notification-list">
      <div v-if="notifications.length === 0" class="empty-text">暂无通知</div>
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: item.isRead === 0 }"
        @click="handleMarkRead(item)"
      >
        <div class="dot" />
        <div class="notification-info">
          <div class="title">
            <span>{{ item.title }}</span>
            <el-tag v-if="item.isRead === 0" type="danger" size="small">未读</el-tag>
            <el-tag v-else type="info" size="small">已读</el-tag>
          </div>
          <div class="content">{{ item.content }}</div>
          <div class="time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: size(12);
  font-size: size(14);
  color: #666;
}

.notification-list {
  max-height: size(500);
  overflow-y: auto;
}

.empty-text {
  text-align: center;
  color: #999;
  padding: size(24) 0;
}

.notification-item {
  display: flex;
  padding: size(12);
  border-bottom: 1px solid rgba(0, 0, 0, .06);
  cursor: pointer;

  .dot {
    width: size(8);
    height: size(8);
    border-radius: 50%;
    background: #ccc;
    margin-right: size(10);
    margin-top: size(6);
  }

  .notification-info {
    flex: 1;

    .title {
      display: flex;
      align-items: center;
      gap: size(8);
      font-size: size(15);
      font-weight: bold;
    }

    .content {
      font-size: size(13);
      color: #666;
      margin-top: size(6);
    }

    .time {
      font-size: size(12);
      color: #999;
      margin-top: size(6);
    }
  }

  &.unread .dot {
    background: #f56c6c;
  }

  &:hover {
    background: rgba(0, 0, 0, .02);
  }
}
</style>
