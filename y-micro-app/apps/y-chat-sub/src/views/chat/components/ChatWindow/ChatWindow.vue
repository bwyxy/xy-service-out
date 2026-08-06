<script setup lang="ts">
import {onMounted, ref, watch} from 'vue'
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import { getUserInfo } from '@/api/user.ts'

const { currentChannel } = storeToRefs(useUserStore())
const channelInfo = ref<any>({})

watch(() => currentChannel.value, () => {
  if (currentChannel.value !== null) {
    getChannelInfo()
  }
}, {
  deep: true
})

/** 获取频道信息 */
const getChannelInfo = async () => {
  const { channelId, type } = currentChannel.value
  if (type === 1) {
    const response = await getUserInfo(channelId)

    if (response.code === 200) {
      channelInfo.value = response.data
    }
  }
}

onMounted(() => {
  if (currentChannel.value !== null) {
    getChannelInfo()
  }
})
</script>

<template>
  <div class="chat-window">
    <div class="chat-window-header">
      <span class="channel-name">{{ channelInfo.nickname || '未知' }}</span>
    </div>
    <div class="chat-window-main"></div>
    <div class="chat-window-footer">
      <y-editor />
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-window {
  width: 100%;
  height: 100%;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  &-header {
    height: size(100);
    background: #fff;
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    box-sizing: border-box;
    padding: 0 size(32);
    display: flex;
    align-items: center;
    .channel-name {
      font-size: size(24);
      letter-spacing: 1px;
      color: #1f1f1f;
    }
  }
  &-main {
    flex: 1;
    overflow-y: auto;
    &::-webkit-scrollbar {
      appearance: none;
      width: 0;
    }
  }
  &-footer {
    box-sizing: border-box;
    padding: size(24);
  }
}
</style>