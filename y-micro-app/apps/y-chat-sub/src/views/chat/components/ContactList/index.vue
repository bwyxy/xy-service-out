<script setup lang="ts">
import { getConversation } from '@/api/chat.ts'
import { onMounted, reactive, ref} from "vue";
import { ElMessage } from 'element-plus'
import { decodeBase64FromJava } from '@/utils/wk.ts'

const params = reactive({
  msgCount: 1
})
const conversations = ref<any[]>([])
const initConversation = async () => {
  try {
    const response = await getConversation(params)

    if (response.code === 200) {
      conversations.value = response.data
    }
  } catch {
    ElMessage.error('获取会话列表失败')
  }
}
/** 获取消息 */
const getMessage = (recents :any) => {
  if (!recents || recents.length === 0) {
    return ''
  }
  const payload = decodeBase64FromJava(recents[0].payload || '')
  const msg = JSON.parse(payload)


  return msg.content
}


onMounted(() => {
  initConversation()
})
</script>

<template>
  <div class="contact-list">
    <div
        v-for="(item, index) in conversations"
        :key="index"
        class="msg-item"
    >
      <img v-if="item.avatar" class="avatar-box" :src="item.avatar || ''" alt="avatar" />
      <img v-else class="avatar-box" src="@/assets/images/logo.jpg" alt="avatar" />
      <div class="msg-content">
        <div class="msg-title">
          <span>{{ item.name }}</span>
        </div>
        <span class="msg-text">{{ getMessage(item.recents) }}</span>
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
    .msg-content {
      flex: 1;
      height: 100%;
      display: flex;
      flex-direction: column;
      margin-left: size(12);
      .msg-title {
        flex: 1;
        span {
          font-size: size(20);
          font-weight: bold;
          letter-spacing: 1px;
          @include textEllipsis(1);
        }
      }
      .msg-text {
        font-size: size(16);
        color: #bfbfbf;
        letter-spacing: 1px;
        @include textEllipsis(1);
      }
    }

    &:hover {
      background: rgba(0, 0, 0, .01);
    }
  }
  .avatar-box {
    width: size(64);
    height: size(64);
  }
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}
</style>