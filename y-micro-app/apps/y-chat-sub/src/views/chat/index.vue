<script setup lang="ts">
import ContactList from './components/ContactList/index.vue'


</script>

<template>
  <div class="chat-container">
    <div class="chat-list">
      <!-- 消息列表 -->
      <contact-list />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-container {
  width: 100%;
  height: 100%;
  display: flex;
  .chat-list {
    width: size(240);
  }
}
</style>