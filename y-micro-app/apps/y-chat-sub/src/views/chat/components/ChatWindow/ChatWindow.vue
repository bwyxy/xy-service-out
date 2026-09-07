<script setup lang="ts">
import { onMounted, ref, watch, nextTick, computed } from 'vue'
import { storeToRefs } from 'pinia'
import useChatStore from '@/store/chat.ts'
import { MessageType } from '@/services/im/constants'
import { getUserInfo, getSystemUserInfo } from '@/api/user.ts'
import RichInput from './RichInput.vue'
import ChatInfo from "@/views/chat/components/ChatWindow/ChatInfo.vue";
import MessageBox from "@/components/MessageBox/MessageBox.vue";

const chatStore = useChatStore()
const { activeContact, messages } = storeToRefs(chatStore)

const sending = ref(false)
const isShowChatInfo = ref<boolean>(false)
const messageListRef = ref<HTMLDivElement | null>(null)
const channelInfo = ref<{ nickname?: string; avatar?: string }>({})
const richInputRef = ref<InstanceType<typeof RichInput> | null>(null)

/** 当前会话的消息列表 */
const currentMessages = computed(() => {
  if (!activeContact.value) return []

  return messages.value.filter(
    (m) =>
      m.channelId === activeContact.value!.uid &&
      m.channelType === activeContact.value!.channelType,
  )
})
/** 是否系统用户 */
const isSystemUser = computed(() => {
  if (!activeContact.value || activeContact.value.channelType !== 1) return false

  return activeContact.value.channelType === 1 && isNaN(Number(activeContact.value.uid));

})

/** 获取频道信息 */
const loadChannelInfo = async () => {
  if (!activeContact.value) {
    channelInfo.value = {}
    return
  }

  if (activeContact.value.channelType === 1) {
    let response;
    if (!isNaN(Number(activeContact.value.uid))) {
      response = await getUserInfo(Number(activeContact.value.uid))
    } else {
      response = await getSystemUserInfo(activeContact.value.uid)
    }
    if (response && response.code === 200) {
      channelInfo.value = response.data || {}
      // 同步昵称/头像到联系人
      activeContact.value.name = response.data?.nickname || activeContact.value.name
      activeContact.value.avatar = response.data?.avatar || activeContact.value.avatar
    } else {
      channelInfo.value = { nickname: activeContact.value.name }
    }
  } else {
    channelInfo.value = { nickname: activeContact.value.name }
  }
}

/** 发送文本消息 */
const handleSendText = async (text: string) => {
  if (sending.value || !activeContact.value) return

  sending.value = true
  try {
    await chatStore.sendMessage({ type: MessageType.TEXT, content: text })
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

/** 发送图片消息 */
const handleSendImage = async (url: string) => {
  if (sending.value || !activeContact.value) return

  sending.value = true
  try {
    await chatStore.sendMessage({ type: MessageType.IMAGE, content: url })
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

/** 滚动到底部 */
const scrollToBottom = () => {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

watch(
  () => activeContact.value?.uid,
  () => {
    loadChannelInfo()
    scrollToBottom()
  },
  { immediate: true },
)

watch(
  () => currentMessages.value.length,
  () => {
    scrollToBottom()
  },
)

onMounted(() => {
  loadChannelInfo()
  scrollToBottom()
})
</script>

<template>
  <div class="chat-window">
    <div class="chat-window-header">
      <img
          v-if="activeContact?.avatar"
          class="header-avatar"
          :src="activeContact.avatar"
          alt="avatar"
      />
      <span class="channel-name">{{ channelInfo.nickname || activeContact?.name || '未知' }}</span>
      <div class="chat-window-header-rt">
        <div v-if="!isSystemUser" class="chat-rt-icon">
          <img src="@/assets/images/video_audio_icon.png" alt="icon" />
        </div>
        <div class="chat-rt-icon" @click="isShowChatInfo = !isShowChatInfo">
          <img src="@/assets/images/more_icon.png" alt="icon" />
        </div>
      </div>
    </div>

    <div class="chat-window-content">
      <div class="chat-window-lt">
        <div ref="messageListRef" class="chat-window-main">
          <div
              v-for="msg in currentMessages"
              :key="msg.id"
              class="message-item"
              :class="{ self: msg.isSelf }"
          >
            <message-box
                :item="msg"
                :avatar="activeContact ? activeContact.avatar : ''"
            />
          </div>

          <div v-if="currentMessages.length === 0" class="empty-hint">
            暂无消息，发送一条消息开始聊天吧
          </div>
        </div>

        <div class="chat-window-footer">
          <RichInput
              ref="richInputRef"
              :isSystemUser="isSystemUser"
              :disabled="!activeContact"
              @send-text="handleSendText"
              @send-image="handleSendImage"
          />
        </div>
      </div>
      <!-- 聊天信息 -->
      <chat-info v-model="isShowChatInfo" :info="channelInfo" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-window {
  width: 100%;
  height: 100%;
  background: #fafafa;
  transition: 1s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  .chat-window-content {
    flex: 1;
    transition: 1s;
    display: flex;
    overflow: hidden;
  }
  &-lt {
    flex: 1;
    height: 100%;
    transition: 1s;
    display: flex;
    flex-direction: column;
    position: relative;
  }

  &-header {
    height: size(70);
    background: #fff;
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    box-sizing: border-box;
    padding: 0 size(32);
    display: flex;
    align-items: center;

    .header-avatar {
      width: size(36);
      height: size(36);
      border-radius: 50%;
      margin-right: size(8);
    }

    .channel-name {
      font-size: size(18);
      color: #1f1f1f;
    }

    &-rt {
      flex: 1;
      display: flex;
      justify-content: flex-end;
      .chat-rt-icon {
        width: size(42);
        height: size(42);
        border-radius: 4px;
        cursor: pointer;
        @include flexCenter;
        img {
          width: size(24);
          height: size(24);
        }
        &:hover {
          background: rgba(0, 0, 0, .06);
        }
      }
    }
  }

  &-main {
    flex: 1;
    overflow-y: auto;
    padding: size(24);
    box-sizing: border-box;

    &::-webkit-scrollbar {
      appearance: none;
      width: 0;
    }

    .message-item {
      display: flex;
      margin-bottom: size(16);

      .msg-image-box {
        .message-image {
          max-width: size(240);
          max-height: size(240);
          border-radius: size(8);
          cursor: pointer;
          object-fit: cover;
        }
      }

      .message-bubble {
        max-width: size(480);
        padding: size(12) size(16);
        background: #fff;
        border-radius: size(8);
        box-shadow: 0 1px 2px rgba(0, 0, 0, .06);

        .message-meta {
          display: flex;
          align-items: center;
          gap: size(12);
          margin-bottom: size(6);

          .sender-name {
            font-size: size(14);
            color: #409eff;
            font-weight: bold;
          }

          .message-time {
            font-size: size(12);
            color: #999;
          }
        }

        .message-content {
          font-size: size(15);
          color: #333;
          line-height: 1.5;
          white-space: pre-wrap;
          word-break: break-word;
        }
      }

      &.self {
        flex-direction: row-reverse;
        //justify-content: flex-;

        .user-head {
          margin-left: size(13);
          margin-right: 0 !important;
        }

        .error-msg {
          margin-left: 0 !important;
          margin-right: size(12);
        }

        .message-bubble {
          background: $theme-color;

          .message-content {
            color: #fff;
          }

          .message-meta {
            justify-content: flex-end;

            .sender-name {
              color: #67c23a;
            }
          }
        }
      }
    }

    .empty-hint {
      text-align: center;
      color: #999;
      padding-top: size(120);
    }
  }

  &-footer {
    box-sizing: border-box;
    padding: size(8);
    //background: #fff;
    //border-top: 1px solid rgba(0, 0, 0, .06);
  }
}

.user-head {
  width: size(40);
  height: size(40);
  border-radius: 50px;
  margin-right: size(13);
  margin-top: size(4);
}

.error-msg {
  font-size: size(24);
  color: #f5222d;
  margin-top: size(12);
  margin-left: size(8);
}
</style>
