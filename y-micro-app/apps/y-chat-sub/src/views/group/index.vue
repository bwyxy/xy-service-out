<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyGroups } from '@/api/group.ts'
import useChatStore from '@/store/chat.ts'
import useUserStore from '@/store/user.ts'

/** 群聊列表项 */
interface GroupItem {
  groupId: number
  name: string
  avatar?: string
  memberCount: number
  role: number
}

const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()

const groups = ref<GroupItem[]>([])
const keyword = ref('')

/** 按群名称过滤 */
const filteredGroups = computed(() => {
  const key = keyword.value.trim().toLowerCase()

  if (!key) {
    return groups.value
  }

  return groups.value.filter((item) => (item.name || '').toLowerCase().includes(key))
})

/** 获取我加入的群聊列表 */
const fetchGroups = async () => {
  try {
    const response = await getMyGroups()

    if (response.code === 200) {
      groups.value = response.data || []
    }
  } catch (error) {
    console.error('[GroupList] 查询群聊列表失败:', error)
  }
}

/** 群聊展示名称，无名称时兜底展示群 Id */
const getGroupName = (item: GroupItem) => item.name || `群聊${item.groupId}`

/** 打开群聊：确保会话存在后跳转到聊天页 */
const handleOpenGroup = (item: GroupItem) => {
  const channelId = `g_${item.groupId}`

  chatStore.addContact(channelId, getGroupName(item), item.avatar, 2)

  const contact = chatStore.contacts.find((c) => c.uid === channelId && c.channelType === 2)

  if (!contact) {
    return
  }

  userStore.currentChannel = { channelId, type: 2 }
  router.push('/chat')
  chatStore.selectContact(contact)
}

onMounted(() => {
  fetchGroups()
})
</script>

<template>
  <div class="group">
    <div class="group-list">
      <div class="group-title">
        <span>群聊列表</span>
      </div>

      <el-input v-model="keyword" size="small" placeholder="搜索群聊" clearable />

      <div
          v-for="item in filteredGroups"
          :key="item.groupId"
          class="group-item"
          @click="handleOpenGroup(item)"
      >
        <img v-if="item.avatar" class="group-head" :src="item.avatar" alt="avatar" />
        <img v-else class="group-head" src="@/assets/images/logo.jpg" alt="avatar" />
        <div class="group-item-info">
          <span class="group-item-name">{{ getGroupName(item) }}</span>
          <span class="group-item-count">{{ item.memberCount }} 人</span>
        </div>
      </div>

      <div v-if="!filteredGroups.length" class="group-empty">
        {{ keyword ? '未找到相关群聊' : '暂无群聊' }}
      </div>
    </div>

    <div class="group-main">
      <div class="group-main-tip">
        <span>选择左侧群聊即可进入聊天</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.group {
  width: 100%;
  height: 100%;
  display: flex;
}

.group-list {
  width: size(320);
  height: 100%;
  background-color: #fafafa;
  border-right: 1px solid rgba(0, 0, 0, .06);
  box-sizing: border-box;
  padding: size(24);
  display: flex;
  flex-direction: column;
  overflow-y: scroll;
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }

  .group-title {
    font-size: size(20);
    font-weight: 700;
    color: #1f1f1f;
    letter-spacing: 1px;
    margin-bottom: size(16);
  }

  .group-item {
    display: flex;
    align-items: center;
    padding: size(18) 0;
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    cursor: pointer;

    &:hover {
      background: rgba(0, 0, 0, .02);
    }

    &-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    &-name {
      font-size: size(20);
      font-weight: 500;
      color: #1f1f1f;
      letter-spacing: 1px;
      @include textEllipsis(1);
    }

    &-count {
      font-size: size(14);
      color: #888;
      margin-top: size(6);
    }
  }

  .group-empty {
    padding: size(24) 0;
    text-align: center;
    font-size: size(16);
    color: #999;
  }
}

.group-head {
  width: size(60);
  height: size(60);
  border-radius: size(8);
  margin-right: size(13);
  flex-shrink: 0;
}

.group-main {
  flex: 1;
  @include flexCenter;

  &-tip {
    font-size: size(18);
    color: #999;
    letter-spacing: 1px;
  }
}
</style>
