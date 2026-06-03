<script setup lang="ts">
import { ChatDotSquare, Star} from "@element-plus/icons-vue";
import WordPad from "@/components/WordPad/WordPad.vue";
import {ref, toRefs} from "vue";
import { sendComment } from '@/api/comment.ts'

const props = defineProps<{
  item: any,
  replyName?: string
}>()
const { item } = toRefs(props)


const isReply = ref<boolean>(false)

const emoji = ref<string>('🐥')

const handleReply = async (form :any) => {
  const params = {
    avatarEmoji: emoji.value,
    nickname: form.nickname,
    email: form.email,
    content: form.content,
    commentType: 'article',
    articleId: item.value.articleId,
    parentId: item.value.id
  }
  const res :any = await sendComment(params)

  if (res.code === 200) {
    item.value.children.unshift(res.data)
  }
}
</script>

<template>
  <div class="comment-item">
    <div class="comment-top">
      <div class="avatar">
        <span>{{ item.avatarEmoji }}</span>
      </div>
      <div class="user-info">
        <span>{{ item.nickname }}<span v-if="replyName">回复{{ replyName }}</span></span>
        <span>{{ item.ipAddress }}</span>
      </div>
      <div class="comment-time">{{ item.createTime }}</div>
    </div>
    <div class="comment-content">
      <p>{{ item.content }}</p>
    </div>

    <div class="comment-operation">
      <div class="comment-operation-item">
        <el-icon><Star /></el-icon>
        <span>点赞</span>
      </div>
      <div class="comment-operation-item">
        <el-icon><ChatDotSquare /></el-icon>
        <span
            :style="{ color: isReply ? '#6b5a45' : '#888585' }"
            @click="isReply = !isReply"
        >
          回复
        </span>
      </div>
    </div>

    <word-pad
        v-if="isReply"
        v-model="emoji"
        theme="light"
        :placeholder="`回复${item.nickname}`"
        @submit="handleReply"
    />

    <div style="display: flex;justify-content: flex-end">
      <div v-if="item.children.length > 0" :style="{ width: item.level === 0 ? '96%' : '100%' }">
        <div v-for="(row, index) in item.children" :key="index">
          <comment-item :item="row" :replyName="item.nickname" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.comment-top {
  display: flex;
  align-items: center;
  .avatar {
    width: 50px;
    height: 50px;
    box-shadow: rgba(0, 0, 0, 0.3) 0 1px 2px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    span {
      font-size: 24px;
    }
  }
  .user-info {
    display: flex;
    flex-direction: column;
    margin-left: 14px;
    span:nth-child(1) {
      font-size: 20px;
      font-weight: 700;
      color: #1f1f1f;
      letter-spacing: 1px;
    }
    span:nth-child(2) {
      font-size: 14px;
      color: #888;
      letter-spacing: 1px;
    }
  }
  .comment-time {
    flex: 1;
    display: flex;
    justify-content: flex-end;
    font-size: 14px;
    color: #888;
    letter-spacing: 1px;
  }
}

.comment-content {
  margin: 24px 0;
  p {
    color: #1f1f1f;
    font-size: 18px;
    letter-spacing: 1px;
    text-align: justify;
    text-indent: 2em;
  }
}

.comment-operation {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  &-item {
    font-size: 18px;
    display: flex;
    align-items: center;
    margin-right: 18px;
    span {
      color: #888585;
      font-size: 17px;
      letter-spacing: 1px;
      margin-left: 6px;
      cursor: pointer;
    }
    &:last-child {
      margin-right: 0;
    }
  }
}
</style>