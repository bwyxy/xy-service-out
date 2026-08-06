<script setup lang="ts">
import {computed} from "vue";
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import ContactList from './components/ContactList/index.vue'
import SearchFun from './components/SearchFun/SearchFun.vue'
import ChannelNull from "@/components/ChannelNull/ChannelNull.vue";
import ChatWindow from "./components/ChatWindow/ChatWindow.vue"

const { currentChannel } = storeToRefs(useUserStore())

const mainContainer = computed(() => {
  if (currentChannel.value !== null) {
    return ChannelNull
  } else {
    return ChatWindow
  }
})
</script>

<template>
  <div class="chat-container">
    <div class="chat-list">
      <!-- 搜索功能区 -->
      <search-fun />
      <!-- 消息列表 -->
      <contact-list />
    </div>
    <div class="chat-main">
      <component :is="mainContainer" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-container {
  width: 100%;
  height: 100%;
  display: flex;
  .chat-list {
    width: size(320);
    height: 100%;
    background-color: #fafafa;
    border-right: 1px solid rgba(0, 0, 0, .06);
    display: flex;
    flex-direction: column;
  }
  .chat-main {
    flex: 1;
  }
}
</style>