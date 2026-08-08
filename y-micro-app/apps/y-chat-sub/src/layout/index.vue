<script setup lang="ts">
import { onMounted } from 'vue'
import useUserStore from '@/store/user.ts'
import useChatStore from "@/store/chat.ts";
import { wukongIMService } from '@/services/im'
import LayoutHeader from './Header/index.vue'
import AppMain from './AppMain.vue'
import LayoutNav from './nav/index.vue'

const userStore = useUserStore()
const chatStore = useChatStore()

onMounted(() => {
  // 若 IM 尚未初始化，且已获取到用户信息，则触发连接
  if (!wukongIMService.isInitialized() && userStore.userInfo?.uid) {
    chatStore.initWKIM()
  }
})
</script>

<template>
  <div class="app-container">
    <layout-header />

    <div class="main-container">
      <layout-nav />
      <app-main />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.app-container {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.main-container {
  flex: 1;
  display: flex;
  background: rgba(255, 255, 255, .4);
  border-top: 1px solid rgba(0, 0, 0, .06);
}
</style>
