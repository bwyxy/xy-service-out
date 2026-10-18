<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import type {Message} from "@/store/chat.ts";
import { MessageType } from '@/services/im/constants'
import {toRefs} from "vue";
import {Warning} from "@element-plus/icons-vue";

const { userInfo } = storeToRefs(useUserStore())

const props = defineProps<{
  item: Message,
  avatar?: string
  channelId?: string
  members?: GroupMember[]
}>()
const { item, avatar, members } = toRefs(props)

const user = () :{name: string, avatar: string} => {
  if (!item.value || !members.value) {
    return {
      name: '',
      avatar: ''
    }
  }
  if (item.value.channelType === 1) {
    return {
      avatar: avatar.value || '',
      name: ''
    }
  }

  const info = members.value.find(member => String(member.uid) === item.value.fromUid)

  if (!info) {
    return {
      name: '',
      avatar: ''
    }
  }

  return {
    name: info.groupNickname || info.nickname,
    avatar: info.avatar
  }
}
</script>

<template>
  <div :class="['message-box', {'message-box_self': item.isSelf, 'message-box--tip': item.type === MessageType.RTC_RECORD}]">
    <!-- 通话记录：灰色居中提示（微信风格） -->
    <template v-if="item.type === MessageType.RTC_RECORD">
      <span class="rtc-record-tip">
        <el-icon class="rtc-record-icon"><PhoneFilled /></el-icon>
        {{ item.content }}
      </span>
    </template>
    <template v-else-if="channelId === item.fromUid && item.channelType === 2">
      <div class="channel-title">
        <span>{{ item.content }}</span>
      </div>
    </template>
    <template v-else>
      <!-- 自己 -->
      <div v-if="item.isSelf" class="me-user">
        <img v-if="userInfo.avatar" class="user-head" :src="userInfo.avatar" alt="avatar" />
        <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
      </div>
      <!-- 对方 -->
      <div v-if="!item.isSelf" class="me-user">
        <img v-if="user().avatar" class="user-head" :src="user().avatar" alt="avatar" />
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
        <div v-else>
          <div v-if="!item.isSelf" class="user-name">{{ user().name }}</div>
          <div class="message-bubble">
            <div class="message-content" v-html="item.content"></div>
          </div>
        </div>
      </div>
      <!-- 发送失败 -->
      <div v-if="item.isError" class="error-msg">
        <el-icon><Warning /></el-icon>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.message-box {
  width: 100%;
  display: flex;

  // 通话记录：灰色居中提示，不区分左右
  &--tip {
    justify-content: center;
  }

  .rtc-record-tip {
    display: inline-flex;
    align-items: center;
    gap: size(4);
    font-size: size(12);
    color: #999;
    background: #f2f2f2;
    border-radius: size(4);
    padding: size(3) size(10);

    .rtc-record-icon {
      font-size: size(13);
    }
  }

  .channel-title {
    width: 100%;
    border-radius: size(4);
    padding: size(3) size(10);
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    span {
      max-width: 60%;
      display: inline-flex;
      align-items: center;
      gap: size(4);
      font-size: size(12);
      color: #999;
      background: #f2f2f2;
    }
  }

  .user-name {
    font-size: size(10);
    letter-spacing: 1px;
    color: #8c8c8c;
    margin-bottom: size(4);
    margin-left: size(8);
  }

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