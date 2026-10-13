<script setup lang="ts">
import { isTauriMainWindow } from '@/utils/tauri.ts'

// 桌面主窗口：整体淡入放大过渡，衔接登录窗口的缩放退出动画
const isDesktopMain = isTauriMainWindow()
</script>

<template>
  <div :class="['app-root', isDesktopMain ? 'app-root--desktop' : '']">
    <router-view />
  </div>
</template>

<style scoped lang="scss">
.app-root--desktop {
  height: 100vh;
  overflow: hidden;
  animation: desktop-app-fade-in 0.45s ease-out;
  transform-origin: center;
}

@keyframes desktop-app-fade-in {
  from {
    opacity: 0;
    transform: scale(0.985);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
