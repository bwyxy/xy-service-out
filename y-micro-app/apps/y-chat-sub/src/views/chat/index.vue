<script setup lang="ts">
import ListTop from "@/views/chat/components/ListTop/ListTop.vue";

</script>

<template>
  <div class="chat">
    <div class="chat-list">
      <!-- 列表头部 -->
      <list-top />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-list {
  width: 240px;
  height: 100%;
  box-sizing: border-box;
  background: $theme-color-1;
  border-right: 1px solid $border-color-1;
  padding: $padding;
}
</style>
