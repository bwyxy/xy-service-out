<script setup lang="ts">
import {ref, watch} from 'vue'
import { storeToRefs } from 'pinia'
import useForumStore from "@/store/forum.ts";
import { getBoards } from '@/api/forum.ts'

const { currentBoardId } = storeToRefs(useForumStore())
const boards = ref<ForumBoard[]>([])

const boardId = defineModel('modelValue', {
  type: Number
})

const handleBoardId = (id: number) => {
  boardId.value = id
}
const getBoardList = async (boardId: number) => {
  const response = await getBoards({
    parentId: boardId
  })

  if (response.code === 200) {
    response.data.unshift({
      boardId: 0,
      name: '全部'
    })
    boards.value = response.data
  }
}

watch(() => currentBoardId.value, () => {
  if (currentBoardId.value) {
    getBoardList(currentBoardId.value)
  }
}, {immediate: true})
</script>

<template>
  <div class="board-tabs">
    <div
        v-for="(item, index) in boards"
        :key="index"
        :class="['board-tabs-item', {'is-active': boardId === item.boardId}]"
        @click="handleBoardId(item.boardId)"
    >
      <span>{{ item.name }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.board-tabs {
  width: 100%;
  display: flex;
  box-sizing: border-box;
  padding: 0 size(28);
  &-item {
    font-size: size(14);
    color: #8c8c8c;
    cursor: pointer;
    letter-spacing: 1px;
    padding: size(12) size(16);
    margin-bottom: size(14);
  }

  .is-active {
    color: #595959;
    font-weight: 700;
    background: rgba(0, 0, 0, .04);
    border-radius: 50px;
  }
}
</style>