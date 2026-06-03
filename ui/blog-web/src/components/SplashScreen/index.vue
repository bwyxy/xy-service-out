<script lang="ts" setup>
import { onMounted, ref, onUnmounted } from 'vue'

const emit = defineEmits<{
  done: []
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const showContent = ref(true)
const contentFading = ref(false)
const componentVisible = ref(true)

const drawOverlay = (ctx: CanvasRenderingContext2D, w: number, h: number) => {
  ctx.globalCompositeOperation = 'source-over'
  ctx.fillStyle = '#05060f'
  ctx.fillRect(0, 0, w, h)
}

const eraseCircle = (ctx: CanvasRenderingContext2D, cx: number, cy: number, radius: number) => {
  ctx.globalCompositeOperation = 'destination-out'
  ctx.beginPath()
  ctx.arc(cx, cy, radius, 0, Math.PI * 2)
  ctx.fill()
}

const startReveal = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const w = canvas.width
  const h = canvas.height
  const cx = w / 2
  const cy = h / 2
  const maxRadius = Math.sqrt(cx * cx + cy * cy)
  const duration = 1000
  const startTime = performance.now()

  drawOverlay(ctx, w, h)

  const animate = (now: number) => {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)

    const eased = 1 - Math.pow(1 - progress, 3)

    const radius = eased * maxRadius

    eraseCircle(ctx, cx, cy, radius)

    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      componentVisible.value = false
      emit('done')
    }
  }

  requestAnimationFrame(animate)
}

let timer1 = 0
let timer2 = 0

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  drawOverlay(ctx, canvas.width, canvas.height)

  timer1 = window.setTimeout(() => {
    contentFading.value = true

    timer2 = window.setTimeout(() => {
      showContent.value = false
      startReveal()
    }, 400)
  }, 2000)
})

onUnmounted(() => {
  clearTimeout(timer1)
  clearTimeout(timer2)
})
</script>

<template>
  <div v-if="componentVisible" class="splash-screen">
    <canvas ref="canvasRef" class="splash-canvas"></canvas>

    <div class="splash-content" :class="{ fading: contentFading, hidden: !showContent }">
      <div class="splash-logo">
        <div class="logo-ring">
          <span class="logo-dot"></span>
        </div>
        <span class="logo-text">XY</span>
      </div>

      <h1 class="splash-title">
        <span
          v-for="(char, i) in 'XIAOYI'.split('')"
          :key="i"
          class="title-char"
          :style="{ animationDelay: (0.3 + i * 0.08) + 's' }"
        >{{ char }}</span>
      </h1>

      <div class="splash-line"></div>

      <p class="splash-subtitle">记录技术与生活的个人博客</p>

      <div class="splash-dots">
        <span v-for="i in 3" :key="i" class="dot" :style="{ animationDelay: (i * 0.2) + 's' }"></span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.splash-screen {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #05060f;
}

.splash-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
}

.splash-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: opacity 0.4s ease;

  &.fading {
    opacity: 0;
    transform: scale(0.96);
    transition: all 0.4s ease;
  }

  &.hidden {
    display: none;
  }
}

.splash-logo {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
}

.logo-ring {
  width: 64px;
  height: 64px;
  border: 2px solid rgba(77, 171, 247, 0.3);
  border-radius: 50%;
  position: absolute;
  animation: logoRingSpin 6s linear infinite;
}

.logo-dot {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #4dabf7;
  top: -3px;
  left: 50%;
  transform: translateX(-50%);
  box-shadow: 0 0 8px #4dabf7, 0 0 16px rgba(77, 171, 247, 0.6);
}

@keyframes logoRingSpin {
  to { transform: rotate(360deg); }
}

.logo-text {
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 4px;
  color: #fff;
  position: relative;
  z-index: 1;
}

.splash-title {
  display: flex;
  gap: 3px;
  margin-bottom: 20px;
}

.title-char {
  font-size: 42px;
  font-weight: 900;
  letter-spacing: 8px;
  color: #fff;
  opacity: 0;
  animation: charReveal 0.6s ease forwards;
}

@keyframes charReveal {
  from {
    opacity: 0;
    transform: translateY(20px);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

.splash-line {
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(77, 171, 247, 0.5), transparent);
  margin-bottom: 20px;
}

.splash-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.25);
  letter-spacing: 4px;
  margin-bottom: 36px;
}

.splash-dots {
  display: flex;
  gap: 8px;
}

.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(77, 171, 247, 0.4);
  animation: dotPulse 1.4s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}
</style>