<script setup lang="ts">
import { ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { searchUsers, sendFriendRequest } from '@/api/friend.ts'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const keyword = ref('')
const loading = ref(false)
const users = ref<any[]>([])
const sendingMap = ref<Record<string, boolean>>({})
const applyMsg = ref('')

let timer: any = null

watch(keyword, (val) => {
  if (timer) clearTimeout(timer)
  if (!val || !val.trim()) {
    users.value = []
    return
  }
  timer = setTimeout(() => {
    doSearch(val.trim())
  }, 300)
})

const doSearch = async (kw: string) => {
  loading.value = true
  try {
    const res = await searchUsers(kw)
    if (res.code === 200) {
      users.value = res.data || []
    }
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

const handleSendRequest = async (user: any) => {
  sendingMap.value[user.uId] = true
  try {
    const res = await sendFriendRequest({
      toUid: user.uId,
      applyMsg: applyMsg.value
    })
    if (res.code === 200) {
      ElMessage.success('好友请求已发送')
      emit('success')
    }
  } finally {
    sendingMap.value[user.uId] = false
  }
}

const handleClose = () => {
  keyword.value = ''
  users.value = []
  applyMsg.value = ''
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    v-model="props.visible"
    title="添加朋友"
    width="520px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-input
      v-model="keyword"
      placeholder="搜索昵称、手机号或邮箱"
      clearable
      :prefix-icon="Search"
    />
    <div v-loading="loading" class="user-list">
      <div v-if="users.length === 0 && keyword.trim()" class="empty-text">未找到相关用户</div>
      <div v-for="user in users" :key="user.uId" class="user-item">
        <img v-if="user.avatar" class="avatar" :src="user.avatar" alt="avatar" />
        <img v-else class="avatar" src="@/assets/images/logo.jpg" alt="avatar" />
        <div class="user-info">
          <div class="nickname">{{ user.nickname || '用户' + user.uId }}</div>
          <div class="sub">{{ user.phone || user.email || '' }}</div>
        </div>
        <el-button
          type="primary"
          size="small"
          :loading="sendingMap[user.uId]"
          @click="handleSendRequest(user)"
        >
          添加
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.apply-msg {
  margin-top: size(12);
}
.user-list {
  margin-top: size(16);
  max-height: size(400);
  overflow-y: auto;
}
.empty-text {
  text-align: center;
  color: #999;
  padding: size(24) 0;
}
.user-item {
  display: flex;
  align-items: center;
  padding: size(12);
  border-bottom: 1px solid rgba(0, 0, 0, .06);
  .avatar {
    width: size(48);
    height: size(48);
    border-radius: 50%;
  }
  .user-info {
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
}
</style>
