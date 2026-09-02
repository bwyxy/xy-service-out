<script setup lang="ts">
import {ref} from "vue";
import { V3waterfall } from 'v3-waterfall'
import 'v3-waterfall/dist/style.css'
import PostItem from '@/components/PostItem/PostItem.vue'
import PostDefault from "@/components/PostDefault/PostDefault.vue";

defineProps<{
  list: ForumPost[],
  isLoading?: boolean
}>()

const waterfallListRefs = ref<HTMLDivElement>()
const isShowDefault = ref<boolean>(false)
const currentPost = ref<ForumPost>({} as ForumPost)

/** 点击帖子 */
const handlePostItem = (row:ForumPost) => {
  isShowDefault.value = true
  currentPost.value = row
}

/** 设置瀑布流item 宽度 */
const getColWidth = () => {
  if (window.innerWidth <= 768) return 160;

  return 180
}
</script>

<template>
  <div ref="waterfallListRefs" class="waterfall-list">
    <v3waterfall
      :list="list"
      :colWidth="getColWidth"
      :bottomGap="24"
      :isMounted="true"
      scrollBodySelector="waterfall-list"
      :isLoading="Boolean(isLoading)"
      :virtual-time="400"
    >
      <template v-slot:default="{ item }">
        <post-item
            :item="item"
            @item-click="handlePostItem"
        />
      </template>
    </v3waterfall>
  </div>

  <post-default v-model="isShowDefault" :post="currentPost" />
</template>

<style scoped lang="scss">
.waterfall-list {
  width: 100%;
}

:deep(.el-dialog) {
  padding: 0;
}

:deep(.vue3-waterfall-wrapper) {
  box-sizing: border-box;
  padding-left: 16px;
  padding-right: 16px;
}

@media (min-width: 768px) {
  :deep(.vue3-waterfall-wrapper) {
    padding-left: 24px;
    padding-right: 24px;
  }
}
@media (min-width: 1024px) {
  :deep(.vue3-waterfall-wrapper) {
    padding-left: 32px;
    padding-right: 32px;
  }
}
</style>