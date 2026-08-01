<script setup lang="ts">
import UserInfo from './UserInfo/UserInfo.vue'
import {Bell, Setting, SwitchButton} from "@element-plus/icons-vue";
</script>

<template>
  <div class="chat-header">

    <user-info />

    <div class="chat-header-rt">
      <div class="msg-btn">
        <el-icon><Bell /></el-icon>
        <span>消息通知</span>
      </div>
      <div class="setting-btn">
        <el-icon><Setting /></el-icon>
      </div>
      <div class="msg-btn">
        <el-icon><SwitchButton /></el-icon>
        <span>退出登录</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-header {
  height: size(52);
  box-sizing: border-box;
  padding: 0 size(24);
  display: flex;
  align-items: center;
  &-rt {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
}

.msg-btn {
  @include boxBtn(48, 32, 4);
  font-size: size(14);
}

.setting-btn {
  @include boxBtn(24, 32, 4);
  font-size: size(20);
  margin: 0 size(24);
}
</style>