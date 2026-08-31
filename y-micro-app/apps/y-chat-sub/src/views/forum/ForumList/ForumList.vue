<script setup lang="ts">
import { reactive, ref, watch, toRefs} from "vue";
import { getPosts } from '@/api/forum.ts'
import WaterfallList from "@/components/WaterfallList/WaterfallList.vue";

const props = defineProps<{
  boardId?: number
}>()
const params = reactive({
  pageNum: 1,
  pageSize: 30,
  boardId: 0
})
const forums = ref<ForumPost[]>([])
const { boardId }  = toRefs(props)

const getPostList = async () => {
  const response = await getPosts(params)

  if (response.code !== 200) return;

  if (response.data.pageNum !== 1) {
    forums.value.push(...response.data.list)
  } else {
    forums.value = response.data.list
  }
}

watch(() => boardId.value, () => {
  params.boardId = boardId.value || 0
  params.pageNum = 1
  getPostList()
}, {immediate: true})
</script>

<template>
  <div class="forum-list">

    <waterfall-list :list="forums" />
  </div>
</template>

<style scoped lang="scss">
.forum-list {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  overflow: hidden;
  overflow-y: scroll;
  &::-webkit-scrollbar {
    appearance: none;
  }
}
</style>
