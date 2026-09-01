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

/** 设置瀑布流item 宽度 */
const getColWidth = () => {
  if (window.innerWidth <= 768) return 160;
  if (window.innerWidth <= 1024) return 220;

  const colWidth =  waterfallListRefs.value?.clientWidth || 0

  return colWidth / 9
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
        <post-item :item="item" />
      </template>
    </v3waterfall>
  </div>

  <post-default v-model="isShowDefault" />
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