<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import type {Message, QuoteInfo} from "@/types/chat.ts";
import { MessageType } from '@/services/im/constants'
import { escapeHtml } from '@/utils/html.ts'
import { resolveMemberName } from '@/utils/chat.ts'
import {computed, toRefs} from "vue";
import { PhoneFilled, Warning } from "@element-plus/icons-vue";

const { userInfo } = storeToRefs(useUserStore())

const props = defineProps<{
  item: Message,
  avatar?: string
  channelId?: string
  members?: GroupMember[]
  /** 单聊会话联系人的昵称，用于引用块展示原消息发送者 */
  contactName?: string
}>()
const { item, avatar, members } = toRefs(props)

const emit = defineEmits<{
  /** 点击引用块，请求定位到原消息 */
  (e: 'quote-click', quote: QuoteInfo): void
}>()

/** 点击引用块 */
const onQuoteClick = () => {
  if (item.value?.quote) {
    emit('quote-click', item.value.quote)
  }
}

/**
 * 消息正文 HTML
 *
 * 普通消息直接渲染原内容（保持既有行为）；
 * 带 @提醒 的消息先转义，再把 @昵称 包一层高亮标签。
 */
const contentHtml = computed(() => {
  const msg = item.value
  if (!msg) return ''

  const mentions = msg.mentions
  if (!mentions || mentions.length === 0) {
    return msg.content
  }

  let html = escapeHtml(msg.content)
  mentions.forEach((mention) => {
    if (!mention?.name) return
    const label = `@${escapeHtml(mention.name)}`
    html = html.split(label).join(`<span class="mention-text">${label}</span>`)
  })
  return html
})

/** 是否为居中提示类消息（不区分左右、不展示头像昵称） */
const isTipMessage = computed(() =>
  item.value?.type === MessageType.RTC_RECORD
    || item.value?.type === MessageType.GROUP_MUTE
    || item.value?.type === MessageType.GROUP_MEMBER_CHANGE,
)

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
    name: resolveMemberName(info, ''),
    avatar: info.avatar
  }
}

/**
 * 引用块展示的发送者名称
 *
 * 引用快照里的 nickname 可能存的是发送者 uid（历史消息只带 uid），
 * 因此优先用群成员昵称（或单聊联系人昵称）覆盖，再回退到快照内容。
 */
const quoteSenderName = computed(() => {
  const quote = item.value?.quote
  if (!quote) {
    return ''
  }

  const member = members.value?.find((m) => String(m.uid) === String(quote.fromUid))
  if (member) {
    return resolveMemberName(member, quote.fromUid)
  }

  // 单聊中对方即引用消息的发送者；引用自己的消息时快照已写入本人昵称
  if (item.value?.channelType === 1 && String(quote.fromUid) !== String(userInfo.value?.uid)) {
    return props.contactName || quote.nickname || quote.fromUid
  }

  return quote.nickname || quote.fromUid
})

/** 撤回提示文案 */
const revokedTip = computed(() => {
  const msg = item.value
  if (!msg) {
    return '消息已撤回'
  }
  if (msg.isSelf) {
    return '你撤回了一条消息'
  }
  return `${user().name || '对方'}撤回了一条消息`
})
</script>

<template>
  <div :class="['message-box', {'message-box_self': item.isSelf, 'message-box--tip': isTipMessage || item.revoked}]">
    <!-- 已撤回：灰色居中提示（撤回/重新编辑见右键菜单） -->
    <template v-if="item.revoked">
      <span class="revoked-tip">{{ revokedTip }}</span>
    </template>
    <!-- 通话记录：灰色居中提示（微信风格） -->
    <template v-else-if="item.type === MessageType.RTC_RECORD">
      <span class="rtc-record-tip">
        <el-icon class="rtc-record-icon"><PhoneFilled /></el-icon>
        {{ item.content }}
      </span>
    </template>
    <!-- 群禁言通知：灰色居中提示 -->
    <template v-else-if="item.type === MessageType.GROUP_MUTE">
      <div class="channel-title">
        <span>{{ item.content }}</span>
      </div>
    </template>
    <!-- 群成员变更通知（入群 / 被移出）：灰色居中提示 -->
    <template v-else-if="item.type === MessageType.GROUP_MEMBER_CHANGE">
      <div class="channel-title">
        <span>{{ item.content }}</span>
      </div>
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
          <div v-if="!item.isSelf" class="user-name">
            <span>{{ user().name }}</span>
            <span v-if="item.mentionMe" class="mention-me-tag">有人@我</span>
          </div>
          <div class="message-bubble">
            <!-- 引用的消息（快照），点击定位原消息 -->
            <div
                v-if="item.quote"
                class="quote-block"
                @click.stop="onQuoteClick"
            >
              <div class="quote-block__name">{{ quoteSenderName }}</div>
              <div class="quote-block__text">{{ item.quote.content }}</div>
            </div>
            <div class="message-content" v-html="contentHtml"></div>
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

  // 已撤回：灰色居中提示
  .revoked-tip {
    display: inline-flex;
    align-items: center;
    gap: size(6);
    font-size: size(12);
    color: #999;
    background: #f2f2f2;
    border-radius: size(4);
    padding: size(3) size(10);
  }

  .user-name {
    font-size: size(10);
    letter-spacing: 1px;
    color: #8c8c8c;
    margin-bottom: size(4);
    margin-left: size(8);
    display: flex;
    align-items: center;
    gap: size(6);
  }

  // 有人@我 标记
  .mention-me-tag {
    font-size: size(10);
    color: #f56c6c;
    border: 1px solid #f56c6c;
    border-radius: size(2);
    padding: 0 size(3);
    line-height: 1.4;
    letter-spacing: 0;
  }

  &_self {
    flex-direction: row-reverse;
    .user-head {
      margin-left: size(13);
      margin-right: 0 !important;
    }
    .message-bubble {
      background: $theme-color;

      // 自己的引用块：半透明白底以适配主题色气泡
      .quote-block {
        background: rgba(255, 255, 255, .35);
        border-left-color: rgba(255, 255, 255, .7);

        &__name {
          color: rgba(255, 255, 255, .85);
        }

        &__text {
          color: rgba(255, 255, 255, .95);
        }
      }
    }
    .message-content {
      color: #fff !important;

      // 自己气泡为深色底，@昵称 改为浅色以免与背景冲突
      :deep(.mention-text) {
        color: rgba(255, 255, 255, .92);
        text-decoration: underline;
      }
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

    // 正文里的 @昵称 高亮（v-html 渲染，需 :deep）
    :deep(.mention-text) {
      color: #c8a165;
      cursor: pointer;
    }
  }

  // 引用块
  .quote-block {
    margin-bottom: size(6);
    padding: size(6) size(8);
    border-left: 2px solid #d9d9d9;
    border-radius: size(2);
    background: rgba(0, 0, 0, .04);
    cursor: pointer;
    user-select: none;

    &__name {
      font-size: size(12);
      color: #999;
      margin-bottom: size(2);
    }

    &__text {
      font-size: size(13);
      color: #666;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      line-clamp: 2;
      -webkit-box-orient: vertical;
      word-break: break-word;
    }

    &:hover {
      background: rgba(0, 0, 0, .07);
    }
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

.error-msg {
  font-size: size(24);
  color: #f5222d;
  margin: size(12) size(8) 0;
}
</style>