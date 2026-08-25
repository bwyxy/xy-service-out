<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import type {Message} from "@/store/chat.ts";
import { MessageType } from '@/services/im/constants'
import {toRefs, watch} from "vue";

const { userInfo } = storeToRefs(useUserStore())

const props = defineProps<{
  item: Message,
  avatar?: string
}>()
const { item } = toRefs(props)

watch(() => item.value, () => {
  console.log(item.value)
}, {immediate: true})
</script>

<template>
  <div :class="['message-box', {'message-box_self': item.isSelf}]">
    <!-- 自己 -->
    <div v-if="item.isSelf" class="me-user">
      <img v-if="userInfo.avatar" class="user-head" :src="userInfo.avatar" alt="avatar" />
      <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
    </div>
    <!-- 对方 -->
    <div v-if="!item.isSelf" class="me-user">
      <img v-if="avatar" class="user-head" :src="avatar" alt="avatar" />
      <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
    </div>
    <!--  消息  -->
    <div>
      <div v-if="item.type === MessageType.IMAGE" class="msg-image-box">
        <el-image
            class="message-image"
            :src="item.content"
            :show-progress="true"
            :preview-src-list="[item.content]"
        />
      </div>
      <div v-else class="message-bubble">
        <div class="message-content" v-html="item.content"></div>
      </div>
    </div>
    <!-- 发送失败 -->
    <div v-if="item.isError" class="error-msg">
      <el-icon><Warning /></el-icon>
    </div>
  </div>
</template>

<style scoped lang="scss">
.message-box {
  width: 100%;
  display: flex;
  &_self {
    flex-direction: row-reverse;
    .user-head {
      margin-left: size(13);
      margin-right: 0 !important;
    }
    .message-bubble {
      background: $theme-color;
    }
    .message-content {
      color: #fff !important;
    }

    .message-meta {
      justify-content: flex-end;

      .sender-name {
        color: #67c23a;
      }
    }
  }
  .me-user {
    .user-head {
      width: size(40);
      height: size(40);
      border-radius: 50px;
      margin-right: size(13);
      margin-top: size(4);
    }
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

.msg-image-box {
  .message-image {
    max-width: size(240);
    max-height: size(240);
    border-radius: size(8);
    cursor: pointer;
    object-fit: cover;
  }
}
</style>