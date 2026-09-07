<script setup lang="ts">

import {ref} from "vue";
import { ArrowRight } from '@element-plus/icons-vue'
import AddGroup from "@/components/AddGroup/AddGroup.vue";

const chatDrawerRefs = ref<HTMLElement>()
const isShow = defineModel('modelValue', {
  type: Boolean
})
defineProps<{
  info: any
}>()
</script>

<template>
  <div
      ref="chatDrawerRefs"
      v-show="isShow"
      :class="['chat-drawer-box', { 'show-drawer': isShow }]"
  >
    <div class="info-top">
      <info-card
          :nickname="info.nickname"
          :head="info.avatar"
      />
      <!-- 添加群聊 -->
      <add-group />
    </div>

    <div class="view-chat-record">
      <span>查找聊天内容</span>
      <el-icon><ArrowRight /></el-icon>
    </div>

    <div class="remove-record-btn">
      <span>清空聊天记录</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-drawer-box {
  width: 0;
  height: 100%;
  background: #fff;
  border-left: 1px solid rgba(0, 0, 0, .06);
  transition: 1s;
  box-sizing: border-box;
  right: 0;
  overflow: hidden;
  overflow-y: scroll;
  .info-top {
    box-sizing: border-box;
    padding: size(14) 0;
    display: flex;
    flex-wrap: wrap;
  }
  .add-group {
    cursor: pointer;
    margin-left: size(14);
    &-icon {
      width: size(42);
      height: size(42);
      border-radius: size(6);
      box-sizing: border-box;
      border: 1px dashed #bfbfbf;
      color: #bfbfbf;
      background: #fafafa;
      font-size: size(14);
      @include flexCenter;
    }
    span {
      font-size: size(10);
      color: #bfbfbf;
      margin-top: size(4);
      @include textEllipsis(1);
    }
  }

  .view-chat-record {
    height: size(50);
    border-top: 1px solid #f0f0f0;
    border-bottom: 1px solid #f0f0f0;
    color: #1f1f1f;
    font-size: size(12);
    cursor: pointer;
    display: flex;
    align-items: center;
    span {
      flex: 1;
      letter-spacing: 1px;
    }
  }

  .remove-record-btn {
    width: 100%;
    text-align: center;
    margin-top: size(24);
    span {
      color: #f5222d;
      font-size: size(12);
      letter-spacing: 1px;
      cursor: pointer;
    }
  }
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}

.show-drawer {
  width: size(280);
  padding: 0 size(14);
  position: relative;
}
</style>