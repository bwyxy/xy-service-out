<script setup lang="ts">
import { onMounted, onUnmounted, ref, reactive } from 'vue'
import { View, Hide } from '@element-plus/icons-vue'

const isLogin = ref(true)
const loading = ref(false)
const showPassword = ref(false)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
})

const particleColors = [
  'rgba(77, 171, 247, ALPHA)',
  'rgba(167, 139, 250, ALPHA)',
  'rgba(255, 107, 107, ALPHA)',
  'rgba(105, 219, 124, ALPHA)',
  'rgba(255, 169, 77, ALPHA)',
]

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  r: number
  color: string
  alpha: number
  pulse: number
}

let animFrameId = 0
let particles: Particle[] = []
let mouseX = -1000
let mouseY = -1000

const initCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const dpr = window.devicePixelRatio || 1
  const rect = canvas.getBoundingClientRect()
  canvas.width = rect.width * dpr
  canvas.height = rect.height * dpr
  ctx.scale(dpr, dpr)

  const w = rect.width
  const h = rect.height

  particles = []
  for (let i = 0; i < 80; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      r: Math.random() * 2 + 1,
      color: particleColors[i % particleColors.length],
      alpha: 0.4 + Math.random() * 0.4,
      pulse: Math.random() * Math.PI * 2,
    })
  }

  const draw = () => {
    ctx.clearRect(0, 0, w, h)

    particles.forEach((p) => {
      p.x += p.vx
      p.y += p.vy

      if (p.x < -20) p.x = w + 20
      if (p.x > w + 20) p.x = -20
      if (p.y < -20) p.y = h + 20
      if (p.y > h + 20) p.y = -20

      p.pulse += 0.015
      const dynamicAlpha = p.alpha * (0.6 + 0.4 * Math.sin(p.pulse))

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = p.color.replace('ALPHA', String(dynamicAlpha))
      ctx.fill()

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r * 3, 0, Math.PI * 2)
      ctx.fillStyle = p.color.replace('ALPHA', String(dynamicAlpha * 0.1))
      ctx.fill()
    })

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)

        if (dist < 120) {
          const alpha = (1 - dist / 120) * 0.12
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(167, 139, 250, ${alpha})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }

    if (mouseX > 0 && mouseY > 0) {
      particles.forEach((p) => {
        const dx = mouseX - p.x
        const dy = mouseY - p.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 180) {
          const alpha = (1 - dist / 180) * 0.2
          ctx.beginPath()
          ctx.moveTo(mouseX, mouseY)
          ctx.lineTo(p.x, p.y)
          ctx.strokeStyle = `rgba(255, 255, 255, ${alpha})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      })
    }

    animFrameId = requestAnimationFrame(draw)
  }

  draw()
}

const onMouseMove = (e: MouseEvent) => {
  mouseX = e.clientX
  mouseY = e.clientY
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  form.password = ''
  form.confirmPassword = ''
}

const handleSubmit = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 1500))
  loading.value = false
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  initCanvas()
  resizeObserver = new ResizeObserver(() => {
    if (canvasRef.value) initCanvas()
  })
  if (canvasRef.value) resizeObserver.observe(canvasRef.value)
  document.addEventListener('mousemove', onMouseMove)
})

onUnmounted(() => {
  if (animFrameId) cancelAnimationFrame(animFrameId)
  if (resizeObserver) resizeObserver.disconnect()
  document.removeEventListener('mousemove', onMouseMove)
})
</script>

<template>
  <div class="login-page">
    <!-- <canvas ref="canvasRef" class="particle-canvas"></canvas> -->

    <div class="orbital-ring ring-1"></div>
    <div class="orbital-ring ring-2"></div>
    <div class="orbital-ring ring-3"></div>

    <div class="nebula nebula-top-right"></div>
    <div class="nebula nebula-bottom-left"></div>

    <div class="form-container">
      <div class="form-glow-border"></div>

      <div class="form-header">
        <div class="logo-icon">
          <svg viewBox="0 0 40 40" fill="none" class="logo-svg">
            <circle cx="20" cy="20" r="18" stroke="url(#logo-stroke)" stroke-width="2" fill="none" />
            <circle cx="20" cy="14" r="4" fill="url(#logo-fill)" />
            <circle cx="12" cy="24" r="3" fill="url(#logo-fill2)" />
            <circle cx="28" cy="24" r="3" fill="url(#logo-fill2)" />
            <line x1="20" y1="14" x2="12" y2="24" stroke="url(#logo-stroke)" stroke-width="1.5" stroke-linecap="round" />
            <line x1="20" y1="14" x2="28" y2="24" stroke="url(#logo-stroke)" stroke-width="1.5" stroke-linecap="round" />
            <defs>
              <linearGradient id="logo-stroke" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#4dabf7" />
                <stop offset="100%" stop-color="#a78bfa" />
              </linearGradient>
              <linearGradient id="logo-fill" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#4dabf7" />
                <stop offset="100%" stop-color="#1c7ed6" />
              </linearGradient>
              <linearGradient id="logo-fill2" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#a78bfa" />
                <stop offset="100%" stop-color="#7950f2" />
              </linearGradient>
            </defs>
          </svg>
          <div class="logo-glow"></div>
        </div>

        <h2 class="form-title">{{ isLogin ? '欢迎回来' : '加入星域' }}</h2>
        <p class="form-subtitle">
          {{ isLogin ? '登录你的知识宇宙' : '探索无垠的知识星空' }}
        </p>
      </div>

      <div class="form-body">
        <template v-if="!isLogin">
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 20 20" width="18" height="18" fill="none">
                <path d="M3 5h14M3 5a2 2 0 012-2h10a2 2 0 012 2M3 5v10a2 2 0 002 2h10a2 2 0 002-2V5M8 9h4M3 5l7 4 7-4"
                  stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </div>
            <input
              v-model="form.email"
              type="email"
              class="input-field"
              placeholder="邮箱"
              autocomplete="email"
            />
            <div class="input-underline"></div>
          </div>
        </template>

        <div class="input-group">
          <div class="input-icon">
            <svg viewBox="0 0 20 20" width="18" height="18" fill="none">
              <circle cx="10" cy="7" r="4" stroke="currentColor" stroke-width="1.5" />
              <path d="M3 17c0-3.3 3.1-6 7-6s7 2.7 7 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
          </div>
          <input
            v-model="form.username"
            type="text"
            class="input-field"
            :placeholder="isLogin ? '用户名' : '用户名'"
            autocomplete="username"
          />
          <div class="input-underline"></div>
        </div>

        <div class="input-group">
          <div class="input-icon">
            <svg viewBox="0 0 20 20" width="18" height="18" fill="none">
              <rect x="4" y="8" width="12" height="9" rx="2" stroke="currentColor" stroke-width="1.5" />
              <circle cx="10" cy="12.5" r="1.5" fill="currentColor" />
              <path d="M10 14v1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              <path d="M6.5 8V5.5a3.5 3.5 0 017 0V8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
          </div>
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            class="input-field"
            placeholder="密码"
            autocomplete="current-password"
          />
          <button
            type="button"
            class="input-toggle"
            @click="showPassword = !showPassword"
          >
            <el-icon v-if="showPassword" :size="17"><View /></el-icon>
            <el-icon v-else :size="17"><Hide /></el-icon>
          </button>
          <div class="input-underline"></div>
        </div>

        <template v-if="!isLogin">
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 20 20" width="18" height="18" fill="none">
                <rect x="4" y="8" width="12" height="9" rx="2" stroke="currentColor" stroke-width="1.5" />
                <circle cx="10" cy="12.5" r="1.5" fill="currentColor" />
                <path d="M10 14v1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                <path d="M6.5 8V5.5a3.5 3.5 0 017 0V8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              </svg>
            </div>
            <input
              v-model="form.confirmPassword"
              type="password"
              class="input-field"
              placeholder="确认密码"
              autocomplete="new-password"
            />
            <div class="input-underline"></div>
          </div>
        </template>

        <button
          class="submit-btn"
          :class="{ 'submit-btn--loading': loading }"
          @click="handleSubmit"
        >
          <span class="submit-btn-content">
            <template v-if="loading">
              <span class="loading-dots">
                <span></span><span></span><span></span>
              </span>
            </template>
            <template v-else>
              {{ isLogin ? '启 程' : '加 入' }}
            </template>
          </span>
          <div class="submit-btn-glow"></div>
        </button>

        <div class="form-toggle">
          <span class="toggle-text">
            {{ isLogin ? '还没有账号？' : '已有账号？' }}
          </span>
          <button type="button" class="toggle-link" @click="toggleMode">
            {{ isLogin ? '立即注册' : '去登录' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse at 30% 20%, #141124 0%, #0c0a18 40%, #060411 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.particle-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.orbital-ring {
  position: fixed;
  border-radius: 50%;
  border: 1px solid rgba(167, 139, 250, 0.08);
  pointer-events: none;
  z-index: 0;
  animation: ringRotate 30s linear infinite;

  &::after {
    content: '';
    position: absolute;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #a78bfa;
    box-shadow: 0 0 12px #a78bfa, 0 0 24px #a78bfa66;
    top: 0;
    left: 50%;
    transform: translate(-50%, -50%);
  }
}

.ring-1 {
  width: 600px;
  height: 600px;
  animation-duration: 28s;
  animation-direction: reverse;
}

.ring-2 {
  width: 800px;
  height: 800px;
  animation-duration: 35s;
  border-color: rgba(77, 171, 247, 0.06);

  &::after {
    background: #4dabf7;
    box-shadow: 0 0 12px #4dabf7, 0 0 24px #4dabf766;
  }
}

.ring-3 {
  width: 1000px;
  height: 1000px;
  animation-duration: 42s;
  border-color: rgba(255, 107, 107, 0.05);
  transform: rotate(20deg);

  &::after {
    background: #ff6b6b;
    box-shadow: 0 0 12px #ff6b6b, 0 0 24px #ff6b6b66;
  }
}

@keyframes ringRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.nebula {
  position: fixed;
  border-radius: 50%;
  filter: blur(140px);
  opacity: 0.08;
  pointer-events: none;
  z-index: 0;
}

.nebula-top-right {
  top: -20%;
  right: -10%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, #4dabf7, transparent 70%);
}

.nebula-bottom-left {
  bottom: -20%;
  left: -10%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #a78bfa, transparent 70%);
}

.form-container {
  position: relative;
  z-index: 1;
  width: 400px;
  background: radial-gradient(
    ellipse at 50% 0%,
    rgba(255, 255, 255, 0.06) 0%,
    rgba(255, 255, 255, 0.03) 40%,
    transparent 100%
  );
  border-radius: 28px;
  padding: 48px 40px 40px;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow:
    0 4px 40px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  animation: formIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes formIn {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.form-glow-border {
  position: absolute;
  inset: -1px;
  border-radius: 28px;
  padding: 1px;
  background: linear-gradient(
    135deg,
    rgba(77, 171, 247, 0.3),
    rgba(167, 139, 250, 0.2),
    rgba(255, 107, 107, 0.2),
    rgba(77, 171, 247, 0.3)
  );
  background-size: 400% 400%;
  animation: borderGlow 4s ease infinite;
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask-composite: exclude;
  -webkit-mask-composite: xor;
  pointer-events: none;
  z-index: -1;
}

@keyframes borderGlow {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.form-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-icon {
  position: relative;
  display: inline-flex;
  margin-bottom: 20px;
}

.logo-svg {
  width: 48px;
  height: 48px;
  position: relative;
  z-index: 1;
  animation: logoFloat 3s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.logo-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.3), transparent 70%);
  animation: glowPulse 2s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.5; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.2); }
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0 0 8px;
  background: linear-gradient(135deg, #e8e0f0, #c4b5d8, #a78bfa);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: titleShine 3s ease-in-out infinite;
  background-size: 200% 200%;
}

@keyframes titleShine {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.form-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 1px;
  margin: 0;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  z-index: 1;
  color: rgba(255, 255, 255, 0.3);
  transition: color 0.3s ease;
  display: flex;
  align-items: center;
  pointer-events: none;
}

.input-toggle {
  position: absolute;
  right: 16px;
  z-index: 1;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.25);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color 0.3s ease;

  &:hover {
    color: rgba(255, 255, 255, 0.5);
  }
}

.input-field {
  width: 100%;
  padding: 14px 44px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  letter-spacing: 0.5px;
  outline: none;
  transition: all 0.3s ease;

  &::placeholder {
    color: rgba(255, 255, 255, 0.2);
  }

  &:focus {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(167, 139, 250, 0.4);
    box-shadow: 0 0 0 3px rgba(167, 139, 250, 0.06);

    & ~ .input-underline {
      transform: scaleX(1);
    }

    & ~ .input-icon {
      color: #a78bfa;
    }

    & ~ .input-icon,
    & ~ .input-icon + & {
      color: #a78bfa;
    }
  }

  &:focus ~ .input-icon,
  .input-group:focus-within & ~ .input-icon,
  .input-group:focus-within .input-icon {
    color: #a78bfa;
  }
}

.input-group:focus-within .input-icon {
  color: #a78bfa;
}

.input-underline {
  position: absolute;
  bottom: -1px;
  left: 12px;
  right: 12px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #a78bfa, transparent);
  transform: scaleX(0);
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: center;
}

.submit-btn {
  position: relative;
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #4dabf7, #a78bfa);
  border: none;
  border-radius: 14px;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;

  &:hover:not(&--loading) {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(167, 139, 250, 0.3);
  }

  &--loading {
    pointer-events: none;
    opacity: 0.8;
  }
}

.submit-btn-content {
  position: relative;
  z-index: 1;
}

.submit-btn-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    transparent 30%,
    rgba(255, 255, 255, 0.1) 50%,
    transparent 70%
  );
  background-size: 200% 200%;
  animation: btnShimmer 2s ease-in-out infinite;
}

@keyframes btnShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.loading-dots {
  display: inline-flex;
  gap: 6px;
  align-items: center;

  span {
    width: 6px;
    height: 6px;
    background: #fff;
    border-radius: 50%;
    animation: dotBounce 1.4s ease-in-out infinite;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes dotBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.form-toggle {
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.toggle-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 0.5px;
}

.toggle-link {
  background: none;
  border: none;
  font-size: 13px;
  font-weight: 600;
  color: #a78bfa;
  cursor: pointer;
  letter-spacing: 0.5px;
  padding: 0;
  transition: all 0.3s ease;

  &:hover {
    color: #c4b5d8;
    text-shadow: 0 0 12px rgba(167, 139, 250, 0.4);
  }
}

@media (max-width: 480px) {
  .form-container {
    width: calc(100% - 32px);
    padding: 36px 24px 32px;
    border-radius: 24px;
  }

  .form-glow-border {
    border-radius: 24px;
  }

  .orbital-ring {
    display: none;
  }

  .form-title {
    font-size: 20px;
  }
}
</style>