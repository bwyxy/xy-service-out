<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { isMacOS, isTauri } from '@/utils/tauri.ts'

/**
 * 自定义窗口控制按钮（最小化 / 最大化-还原 / 关闭）。
 * 仅用于非 macOS 桌面端（这些平台窗口去掉了系统标题栏）；
 * macOS 保留系统原生红绿灯与圆角，故本组件不渲染。
 */
const props = withDefaults(
  defineProps<{
    /** 是否显示最大化/还原按钮（登录小窗口不可缩放，传 false） */
    maximizable?: boolean
    /** 是否显示最小化按钮 */
    minimizable?: boolean
  }>(),
  {
    maximizable: true,
    minimizable: true,
  }
)

// 仅非 macOS 桌面端渲染：浏览器/微前端环境与 macOS 均不显示
const visible = isTauri() && !isMacOS()
const isMaximized = ref(false)
let unlistenResized: (() => void) | null = null

const getWin = async () => {
  const { getCurrentWindow } = await import('@tauri-apps/api/window')
  return getCurrentWindow()
}

const handleMinimize = async () => {
  ;(await getWin()).minimize()
}

const handleToggleMaximize = async () => {
  ;(await getWin()).toggleMaximize()
}

const handleClose = async () => {
  ;(await getWin()).close()
}

onMounted(async () => {
  if (!visible || !props.maximizable) return
  const win = await getWin()
  isMaximized.value = await win.isMaximized()
  // 最大化状态可能被系统手势/双击标题栏改变，保持图标同步
  unlistenResized = await win.onResized(async () => {
    isMaximized.value = await win.isMaximized()
  })
})

onUnmounted(() => {
  unlistenResized?.()
})
</script>

<template>
  <div v-if="visible" class="window-controls">
    <div
        v-if="minimizable"
        class="wc-btn"
        title="最小化"
        @click="handleMinimize"
    >
      <svg width="12" height="12" viewBox="0 0 12 12">
        <rect x="1.5" y="5.5" width="9" height="1" rx="0.5" fill="currentColor" />
      </svg>
    </div>

    <div
        v-if="maximizable"
        class="wc-btn"
        :title="isMaximized ? '向下还原' : '最大化'"
        @click="handleToggleMaximize"
    >
      <svg v-if="!isMaximized" width="12" height="12" viewBox="0 0 12 12">
        <rect x="1.6" y="1.6" width="8.8" height="8.8" rx="1.6" fill="none" stroke="currentColor" stroke-width="1.1" />
      </svg>
      <svg v-else width="12" height="12" viewBox="0 0 12 12">
        <rect x="1.2" y="3.6" width="7.2" height="7.2" rx="1.4" fill="none" stroke="currentColor" stroke-width="1.1" />
        <path d="M3.6 3.6V2.8A1.6 1.6 0 0 1 5.2 1.2h4A1.6 1.6 0 0 1 10.8 2.8v4a1.6 1.6 0 0 1-1.6 1.6h-.8"
              fill="none" stroke="currentColor" stroke-width="1.1" />
      </svg>
    </div>

    <div class="wc-btn wc-btn--close" title="关闭" @click="handleClose">
      <svg width="12" height="12" viewBox="0 0 12 12">
        <path d="M2.2 2.2 9.8 9.8M9.8 2.2 2.2 9.8" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" />
      </svg>
    </div>
  </div>
</template>

<style scoped lang="scss">
.window-controls {
  display: flex;
  align-items: center;
  gap: size(2);
  flex-shrink: 0;
}

.wc-btn {
  width: size(30);
  height: size(26);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: size(6);
  color: #666;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s ease, color 0.15s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.07);
    color: #333;
  }

  &:active {
    background: rgba(0, 0, 0, 0.12);
  }

  &--close:hover {
    background: #e81123;
    color: #fff;
  }

  &--close:active {
    background: #c50f1f;
    color: #fff;
  }
}
</style>