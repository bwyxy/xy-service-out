<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { getCurrentWindow } from '@tauri-apps/api/window'
import PetStage from './pet/PetStage.vue'

// 纯浏览器（`pnpm dev`）里没有 Tauri IPC，调用窗口 API 会直接抛错把整个组件树打挂，
// 这里降级为 null，让动画预览仍能在浏览器里跑，只是窗口能力失效
const appWindow = '__TAURI_INTERNALS__' in window ? getCurrentWindow() : null

const petStage = ref<InstanceType<typeof PetStage> | null>(null)
const alwaysOnTop = ref(true)
const dragging = ref(false)
const bubble = ref('')

/** 统一清理定时器，避免窗口隐藏/组件卸载后仍持有回调 */
const timers = new Set<number>()
function later(fn: () => void, delay: number) {
  const id = window.setTimeout(() => {
    timers.delete(id)
    fn()
  }, delay)
  timers.add(id)
}

function say(text: string, duration = 2000) {
  bubble.value = text
  later(() => (bubble.value = ''), duration)
}

function reportError(label: string, error: unknown) {
  console.error(label, error)
  say(`${label}失败：${error instanceof Error ? error.message : String(error)}`, 4000)
}

// 拖拽与点击共用 pointerdown：位移超过阈值才判定为拖拽。
// startDragging 会把鼠标交给系统、之后收不到 pointerup，所以状态在下一次 pointerdown 时重置
let startX = 0
let startY = 0
let pending = false

function onPointerDown(event: PointerEvent) {
  if (event.button !== 0) return
  pending = true
  dragging.value = false
  startX = event.clientX
  startY = event.clientY
}

function onPointerMove(event: PointerEvent) {
  if (!pending) return
  if (Math.hypot(event.clientX - startX, event.clientY - startY) < 5) return
  pending = false
  dragging.value = true
  // 浏览器预览没有原生窗口可拖，直接复位，免得光标一直停在抓取态
  if (!appWindow) {
    dragging.value = false
    return
  }
  appWindow.startDragging().catch((error: unknown) => {
    dragging.value = false
    reportError('拖动窗口', error)
  })
}

function onPointerUp() {
  if (!pending) {
    dragging.value = false
    return
  }
  pending = false
  // 未发生位移即视为点击：交给骨骼动画自己做反馈
  petStage.value?.play('tap', false)
}

function onPetReady() {
  say('Spine 待机动画已启动', 2400)
}

function onPetError(message: string) {
  say(`形象加载失败：${message}`, 5000)
}

async function toggleAlwaysOnTop() {
  const next = !alwaysOnTop.value
  if (!appWindow) {
    say('浏览器预览环境不支持置顶')
    return
  }
  try {
    await appWindow.setAlwaysOnTop(next)
    alwaysOnTop.value = next
    say(next ? '已恢复置顶' : '已取消置顶')
  } catch (error) {
    reportError('切换置顶', error)
  }
}

/**
 * 穿透只能在「从外部恢复」时才安全：一旦开启，窗口自身就收不到点击，
 * 按钮点不动自己。所以这里做限时穿透，到期自动恢复，避免把自己锁死。
 */
async function temporaryClickThrough() {
  if (!appWindow) {
    say('浏览器预览环境不支持穿透')
    return
  }
  try {
    await appWindow.setIgnoreCursorEvents(true)
    say('3 秒内鼠标可穿透本窗口，点击下层应用试试', 3000)
    later(() => {
      appWindow.setIgnoreCursorEvents(false).catch(() => {})
    }, 3000)
  } catch (error) {
    reportError('开启穿透', error)
  }
}

function quit() {
  if (!appWindow) {
    say('浏览器预览环境无法退出窗口')
    return
  }
  appWindow.close().catch((error: unknown) => reportError('退出', error))
}

onMounted(() => {
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  timers.forEach((id) => window.clearTimeout(id))
  timers.clear()
})
</script>

<template>
  <div class="stage">
    <!-- 窗口无边框且不占任务栏，必须留一个可操作的出口，否则只能杀进程 -->
    <div class="panel">
      <div class="panel__row">
        <span class="tag">透明</span>
        <span class="tag">无边框</span>
        <span class="tag" :class="{ 'tag--off': !alwaysOnTop }">
          {{ alwaysOnTop ? '置顶中' : '未置顶' }}
        </span>
      </div>
      <div class="panel__row">
        <button type="button" @click="toggleAlwaysOnTop">
          {{ alwaysOnTop ? '取消置顶' : '恢复置顶' }}
        </button>
        <button type="button" @click="temporaryClickThrough">穿透 3s</button>
        <button type="button" class="panel__quit" @click="quit">退出</button>
      </div>
    </div>

    <!-- 整窗透明区域都是点击目标，无边框窗口需要一个明确的拖拽/点击热区 -->
    <div class="pet" :class="{ 'pet--dragging': dragging }" @pointerdown="onPointerDown">
      <PetStage ref="petStage" @ready="onPetReady" @error="onPetError" />
    </div>

    <Transition name="bubble">
      <div v-if="bubble" class="bubble">{{ bubble }}</div>
    </Transition>
  </div>
</template>

<style scoped>
.stage {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.pet {
  position: absolute;
  inset: 0;
  cursor: grab;
}

.pet--dragging {
  cursor: grabbing;
}

.panel {
  position: absolute;
  top: 6px;
  left: 6px;
  /* .pet 铺满整窗且排在后面，不抬高层级会让面板被 canvas 覆盖，按钮点不动 */
  z-index: 1;
  padding: 6px 8px;
  border-radius: 10px;
  background: rgba(24, 28, 33, 0.72);
  color: #e8f5ee;
  font-size: 11px;
  line-height: 1.6;
  backdrop-filter: blur(6px);
}

.panel__row {
  display: flex;
  gap: 6px;
  align-items: center;
}

.panel__row + .panel__row {
  margin-top: 5px;
}

.panel button {
  padding: 2px 7px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 6px;
  background: transparent;
  color: inherit;
  font-size: 11px;
  cursor: pointer;
}

.panel button:hover {
  background: rgba(255, 255, 255, 0.14);
}

.panel__quit:hover {
  border-color: #ff7a7a;
  color: #ff9a9a;
}

.tag {
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(7, 193, 96, 0.28);
  color: #7ff0b0;
}

.tag--off {
  background: rgba(255, 255, 255, 0.14);
  color: #c9d1cd;
}

.bubble {
  position: absolute;
  top: 54px;
  left: 50%;
  /* 同理，气泡要浮在形象之上，不能被 canvas 盖住 */
  z-index: 2;
  max-width: 260px;
  padding: 7px 11px;
  border-radius: 12px;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.95);
  color: #1c2a24;
  font-size: 12px;
  line-height: 1.5;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
}

.bubble-enter-active,
.bubble-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.bubble-enter-from,
.bubble-leave-to {
  opacity: 0;
  transform: translate(-50%, 6px);
}
</style>
