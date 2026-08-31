<script setup lang="ts">
import {ref, computed} from "vue";
import { V3waterfall } from 'v3-waterfall'
import 'v3-waterfall/dist/style.css'
import PostItem from '@/components/PostItem/PostItem.vue'

defineProps<{
  list: ForumPost[]
}>()

const waterfallListRefs = ref<HTMLDivElement>()

/** 设置瀑布流item 宽度 */
const getColWidth = computed(() => {
  const colWidth = waterfallListRefs.value?.clientWidth || window.innerWidth

  const itemCount = colWidth / 180

  return colWidth / itemCount
})
</script>

<template>
  <div ref="waterfallListRefs" class="waterfall-list">
    <v3waterfall
      :list="list"
      :colWidth="getColWidth"
      :bottomGap="24"
      :virtual-time="400"
    >
      <template v-slot:default="{ item }">
        <post-item :item="item" />
      </template>
    </v3waterfall>
  </div>
</template>

<style scoped lang="scss">
.waterfall-list {
  width: 100%;
}
</style>