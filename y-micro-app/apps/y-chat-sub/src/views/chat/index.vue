<script setup lang="ts">
import ContactList from './components/ContactList/index.vue'
import ChatWindow from './components/ChatWindow/index.vue'
</script>

<template>
  <div class="chat">
    <ContactList />
    <ChatWindow />
  </div>
</template>

<style lang="scss" scoped>
.chat {
  flex: 1;
  display: flex;
  height: 100%;
  overflow: hidden;
}
</style>