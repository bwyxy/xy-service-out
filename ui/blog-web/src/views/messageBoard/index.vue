<script setup lang="ts">
import {onMounted, ref} from 'vue'
import MessageSphere from "@/components/MessageSphere/MessageSphere.vue";
import WordPad from "@/components/WordPad/WordPad.vue";
import { commentList, sendComment } from '@/api/comment.ts'
import {ElMessage} from "element-plus";

const comments = ref([])
const emoji = ref('🐶')

const getCommentList = async () => {
  const response :any = await commentList({ commentType: 'message' })

  if (response.code === 200 ) {
    comments.value = response.data
  }
}

const submitComment = async (form :any) => {
  const params = {
    avatarEmoji: emoji.value,
    nickname: form.nickname,
    email: form.email,
    content: form.content,
    commentType: 'message',
    parentId: 0
  }
  const res :any = await sendComment(params)

  if (res.code === 200) {
    // @ts-ignore
    comments.value.push(res.data)
    ElMessage.success("感谢您的留言")
  }
}

onMounted(() => {
  getCommentList()
})
</script>

<template>
  <div class="message-board">
    <message-sphere :comments="comments" />

    <div class="message-body">
      <word-pad v-model="emoji" @submit="submitComment" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.message-board {
  width: 100%;
  min-height: 100vh;
}

.message-body {
  width: 1250px;
  margin: 0 auto;
}
</style>