<script setup lang="ts">
import {onMounted, ref, toRefs} from 'vue'
import { getComments } from '@/api/comment.ts'

const props = defineProps<{
  commentType: string,
  articleId?: number
}>()
const { commentType, articleId } = toRefs(props)

const comments = ref([])

const getCommentList = async () => {
  const params = {
    commentType: commentType.value,
    articleId: 0
  }
  if (articleId.value) {
    params.articleId = articleId.value
  }
  const response :any = await getComments(params)

  if (response.code === 200) {
    comments.value = response.data
  }
}

onMounted(() => {
  getCommentList()
})
</script>

<template>

</template>

<style scoped lang="scss">

</style>