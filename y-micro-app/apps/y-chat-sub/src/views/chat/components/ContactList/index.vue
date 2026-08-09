<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import useUserStore from '@/store/user.ts'
import useChatStore from '@/store/chat.ts'
import { getConversation } from '@/api/chat.ts'
import { decodeBase64FromJava, extractMessageContent } from '@/utils/wk.ts'
import type { Contact } from '@/store/chat.ts'

const userStore = useUserStore()
const chatStore = useChatStore()
const { contacts } = storeToRefs(chatStore)

const loading = ref(false)

interface ApiConversationItem {
  avatar?: string
  name: string
  recents?: { payload?: string }[]
  channel_id: string
  channel_type: number
  unread_count?: number
}

/**
 * 从后端获取会话列表并同步到 chat store
 */
const fetchConversations = async () => {
  loading.value = true
  try {
    const response = await getConversation({ msgCount: 1 })
    if (response.code === 200) {
      syncToChatStore(response.data || [])
    }
  } catch {
    ElMessage.error('获取会话列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 将后端会话列表数据合并到 chat store
 */
const syncToChatStore = (list: ApiConversationItem[]) => {
  for (const item of list) {
    const exists = contacts.value.find(
      (c) => c.uid === item.channel_id && c.channelType === item.channel_type,
    )

    const lastMessage = resolveLatestMessageContent(item.recents)

    if (exists) {
      exists.name = item.name || exists.name
      exists.avatar = item.avatar || exists.avatar
      exists.lastMessage = lastMessage || exists.lastMessage
    } else {
      chatStore.contacts.push({
        uid: item.channel_id,
        name: item.name,
        type: item.channel_type === 2 ? 'group' : 'person',
        channelType: item.channel_type as 1 | 2,
        unreadCount: item.unread_count || 0,
        lastMessage,
        lastMessageTime: Date.now(),
        avatar: item.avatar,
      })
    }
  }

  chatStore.contacts.sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))
}

/**
 * 解析最新消息内容
 */
const resolveLatestMessageContent = (recents?: { payload?: string }[]): string => {
  if (!recents || recents.length === 0) {
    return ''
  }

  try {
    const decoded = decodeBase64FromJava(recents[0].payload || '')
    const payload = JSON.parse(decoded)
    return extractMessageContent(payload)
  } catch {
    return ''
  }
}

/**
 * 选择频道
 */
const handleSelectChannel = (row: Contact) => {
  console.log('selected channel', row)
  userStore.currentChannel = {
    channelId: row.uid,
    type: row.channelType,
  }
  chatStore.selectContact(row)
}

onMounted(() => {
  fetchConversations()
})
</script>

<template>
  <div v-loading="loading" class="contact-list">
    <div
      v-for="(item, index) in contacts"
      :key="index"
      class="msg-item"
      :class="{ active: userStore.currentChannel?.channelId === item.uid }"
      @click="handleSelectChannel(item)"
    >
      <div class="avatar-wrap">
        <img
          v-if="item.avatar"
          class="avatar-box"
          :src="item.avatar"
          alt="avatar"
        />
        <img
          v-else
          class="avatar-box"
          src="@/assets/images/logo.jpg"
          alt="avatar"
        />
        <span v-if="item.unreadCount > 0" class="unread-badge">{{ item.unreadCount }}</span>
      </div>
      <div class="msg-content">
        <div class="msg-title">
          <span>{{ item.name }}</span>
          <span v-if="item.lastMessageTime" class="msg-time">{{
            new Date(item.lastMessageTime).toLocaleTimeString()
          }}</span>
        </div>
        <span class="msg-text">{{ item.lastMessage }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.contact-list {
  width: 100%;
  flex: 1;
  box-sizing: border-box;
  padding: size(24);
  overflow: hidden;
  overflow-y: scroll;

  .msg-item {
    height: size(100);
    background: #fff;
    display: flex;
    align-items: center;
    margin-bottom: size(13);
    border-radius: size(13);
    box-shadow: rgba(50, 50, 93, 0.15) 0 2px 5px -1px,
                rgba(0, 0, 0, 0.2) 0 1px 3px -1px;
    box-sizing: border-box;
    cursor: pointer;
    padding: size(18);

    &:hover,
    &.active {
      background: rgba(0, 0, 0, .04);
    }
  }

  .avatar-wrap {
    position: relative;
  }

  .avatar-box {
    width: size(64);
    height: size(64);
  }

  .unread-badge {
    position: absolute;
    top: size(-6);
    right: size(-6);
    min-width: size(24);
    height: size(24);
    line-height: size(24);
    padding: 0 size(6);
    background: #f56c6c;
    color: #fff;
    border-radius: size(12);
    font-size: size(12);
    text-align: center;
  }

  .msg-content {
    flex: 1;
    height: 100%;
    display: flex;
    flex-direction: column;
    margin-left: size(12);

    .msg-title {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;

      span {
        font-size: size(20);
        font-weight: bold;
        letter-spacing: 1px;
        @include textEllipsis(1);
      }

      .msg-time {
        font-size: size(12);
        color: #999;
        font-weight: normal;
      }
    }

    .msg-text {
      font-size: size(16);
      color: #bfbfbf;
      letter-spacing: 1px;
      @include textEllipsis(1);
    }
  }

  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}
</style>
