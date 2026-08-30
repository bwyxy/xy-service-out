<script setup lang="ts">
import ForumHeader from './Header/header.vue'
import BoardTabs from './BoardTabs/BoardTabs.vue'
import ForumList from "./ForumList/ForumList.vue";
import {ref} from "vue";

const boardId = ref<number>(0)
</script>

<template>
  <div class="forum-box">
    <forum-header />

    <board-tabs v-model="boardId" />

    <div class="forum-box-content">
      <forum-list />
    </div>
  </div>
</template>

<style scoped lang="scss">
.forum-box {
  width: 100%;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: auto;
  overflow-y: scroll;
   &-content {
     flex: 1;
     box-sizing: border-box;
     overflow: hidden;
   }
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}
</style>