<script setup lang="ts">
import { ref, watch } from 'vue'
import { getReceivedRequests, handleFriendRequest } from '@/api/friend.ts'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'refresh'): void
}>()

const requests = ref<any[]>([])
const loading = ref(false)
const processingMap = ref<Record<string, boolean>>({})

watch(() => props.visible, (val) => {
  if (val) {
    loadRequests()
  }
})

const loadRequests = async () => {
  loading.value = true
  try {
    const res = await getReceivedRequests(0)
    if (res.code === 200) {
      requests.value = res.data || []
    }
  } catch {
    requests.value = []
  } finally {
    loading.value = false
  }
}

const handleAction = async (request: any, action: number) => {
  processingMap.value[request.id] = true
  try {
    const res = await handleFriendRequest({
      requestId: request.id,
      action,
      replyMsg: ''
    })
    if (res.code === 200) {
      ElMessage.success(action === 1 ? '已同意' : '已拒绝')
      loadRequests()
      emit('refresh')
    }
  } finally {
    processingMap.value[request.id] = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    v-model="props.visible"
    title="新的朋友"
    width="520px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading" class="request-list">
      <div v-if="requests.length === 0" class="empty-text">暂无待处理的好友请求</div>
      <div v-for="request in requests" :key="request.id" class="request-item">
        <img v-if="request.fromAvatar" class="avatar" :src="request.fromAvatar" alt="avatar" />
        <img v-else class="avatar" src="@/assets/images/logo.jpg" alt="avatar" />
        <div class="request-info">
          <div class="nickname">{{ request.fromNickname || '用户' + request.fromUid }}</div>
          <div class="sub">{{ request.applyMsg || '请求添加您为好友' }}</div>
        </div>
        <div class="actions">
          <el-button
            type="primary"
            size="small"
            :loading="processingMap[request.id]"
            @click="handleAction(request, 1)"
          >
            同意
          </el-button>
          <el-button
            size="small"
            :loading="processingMap[request.id]"
            @click="handleAction(request, 2)"
          >
            拒绝
          </el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.request-list {
  max-height: size(450);
  overflow-y: auto;
}
.empty-text {
  text-align: center;
  color: #999;
  padding: size(24) 0;
}
.request-item {
  display: flex;
  align-items: center;
  padding: size(12);
  border-bottom: 1px solid rgba(0, 0, 0, .06);
  .avatar {
    width: size(48);
    height: size(48);
    border-radius: 50%;
  }
  .request-info {
    flex: 1;
    margin-left: size(12);
    .nickname {
      font-size: size(16);
      font-weight: bold;
    }
    .sub {
      font-size: size(12);
      color: #999;
      margin-top: size(4);
    }
  }
  .actions {
    display: flex;
    gap: size(8);
  }
}
</style>
