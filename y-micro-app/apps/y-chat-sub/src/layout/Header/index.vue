<script setup lang="ts">
import WindowControls from '@/components/WindowControls/index.vue'
import { isMacOS, isTauri } from '@/utils/tauri.ts'

// macOS 使用系统原生红绿灯（叠加在窗口左上角），头部左侧需为其预留空间
const isMac = isTauri() && isMacOS()

const visible = isTauri() && !isMacOS()
</script>

<template>
  <div v-if="visible || isMac" class="chat-header" :class="{ 'chat-header--mac': isMac }" data-tauri-drag-region>

    <div class="chat-header-rt" data-tauri-drag-region>

      <window-controls />
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-header {
  height: size(32);
  background: var(--theme-color-light-9);
  box-sizing: border-box;
  padding: 0 size(12) 0 size(24);
  display: flex;
  align-items: center;

  // macOS：为左上角系统红绿灯预留空间
  &--mac {
    width: 100%;
    padding-left: size(78);
    height: size(16);
    background: none;
    cursor: pointer;
    z-index: 1;
    position: absolute;
    left: 0;
    top: 0;

  }

  &-rt {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
}

// 自定义窗口按钮与右侧功能区留出间距
.window-controls {
  margin-left: size(18);
}

.msg-btn {
  @include boxBtn(48, 32, 4);
  font-size: size(14);
  cursor: pointer;
}

.setting-btn {
  @include boxBtn(24, 32, 4);
  font-size: size(20);
  margin: 0 size(24);
}

.notice-badge {
  line-height: 1;
}
</style>
