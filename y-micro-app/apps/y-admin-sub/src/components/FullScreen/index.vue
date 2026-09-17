<script setup lang="ts">
import { ref } from 'vue'
import { FullScreen, Crop } from '@element-plus/icons-vue'

const isFullscreen = ref(false)

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().then(() => {
      isFullscreen.value = true
    }).catch(() => {
      // 浏览器不支持或用户拒绝
    })
  } else {
    document.exitFullscreen().then(() => {
      isFullscreen.value = false
    })
  }
}

document.addEventListener('fullscreenchange', () => {
  isFullscreen.value = !!document.fullscreenElement
})
</script>

<template>
  <div class="fullscreen-btn" @click="toggleFullscreen">
    <el-icon :size="18">
      <FullScreen v-if="!isFullscreen" />
      <Crop v-else />
    </el-icon>
  </div>
</template>

<style scoped lang="scss">
.fullscreen-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--app-text-color);
  transition: background-color 0.2s;

  &:hover {
    background-color: var(--app-hover-bg);
  }
}
</style>
