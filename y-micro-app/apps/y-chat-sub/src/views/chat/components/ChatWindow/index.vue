<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import useChatStore from '@/store/chat'
import { Promotion } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const inputText = ref('')
const msgContainer = ref<HTMLElement>()

const handleSend = () => {
  if (!inputText.value.trim()) return
  chatStore.sendMessage(inputText.value.trim())
  inputText.value = ''
}

// 自动滚动到底部
watch(
  () => chatStore.messages.length,
  () => {
    nextTick(() => {
      if (msgContainer.value) {
        msgContainer.value.scrollTop = msgContainer.value.scrollHeight
      }
    })
  }
)
</script>

<template>
  <div class="chat-window">
    <!-- 无选中联系人 -->
    <div v-if="!chatStore.activeContact" class="no-contact">
      <el-icon :size="48" color="#ccc"><Promotion /></el-icon>
      <p>选择联系人开始聊天</p>
    </div>

    <!-- 聊天区域 -->
    <template v-else>
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="avatar">{{ chatStore.activeContact.name.charAt(0).toUpperCase() }}</div>
        <span class="name">{{ chatStore.activeContact.name }}</span>
      </div>

      <!-- 消息列表 -->
      <div ref="msgContainer" class="msg-container">
        <div
          v-for="msg in chatStore.messages.filter(
            m => m.channelId === chatStore.activeContact?.uid
          )"
          :key="msg.id"
          class="msg-item"
          :class="msg.isSelf ? 'self' : 'other'"
        >
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>

        <div
          v-if="chatStore.messages.filter(m => m.channelId === chatStore.activeContact?.uid).length === 0"
          class="empty-msg"
        >
          暂无消息，发送一条消息开始聊天吧
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入消息..."
          @keyup.enter.exact="handleSend"
          resize="none"
        />
        <el-button type="primary" size="small" @click="handleSend" style="margin-top: 8px">
          发送
        </el-button>
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;
}

.no-contact {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  gap: 12px;
  font-size: 14px;
}

.chat-header {
  height: 52px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid $border-color-1;

  .avatar {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: #faad14;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: bold;
  }

  .name {
    font-size: 15px;
    font-weight: 500;
    color: #333;
  }
}

.msg-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-item {
  display: flex;
  max-width: 70%;

  &.self {
    align-self: flex-end;

    .msg-bubble {
      background: #95ec69;
      border-radius: 8px 2px 8px 8px;
    }
  }

  &.other {
    align-self: flex-start;

    .msg-bubble {
      background: #fff;
      border-radius: 2px 8px 8px 8px;
    }
  }

  .msg-bubble {
    padding: 8px 12px;
    font-size: 14px;
    line-height: 1.5;
    word-break: break-word;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }
}

.empty-msg {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 40px 0;
}

.input-area {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid $border-color-1;
}
</style>