<script lang="ts" setup>
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { formatDateTime } from '@bwyxy/utils'
import { articleList } from '@/api/blog.ts'

interface ArticleItem {
  articleId: string
  slug: string
  title: string
  summary: string
  coverImage: string
  publishedTime: string
  categoryName?: string
}

interface YearGroup {
  year: string
  articles: ArticleItem[]
}

const router = useRouter()
const yearGroups = ref<YearGroup[]>([])
const loading = ref(true)
const timelineRef = ref<HTMLElement | null>(null)
const activeYear = ref('')
const particleCanvas = ref<HTMLCanvasElement | null>(null)

const colors = [
  '#ff6b6b', '#ffa94d', '#ffd43b', '#69db7c',
  '#4dabf7', '#da77f2', '#f783ac', '#63e6be',
  '#748ffc', '#ff8787'
]

const toArticle = (slug: string, articleId: string) => {
  router.push({ path: `/article/${slug || articleId || ''}` })
}

const getArticleList = async () => {
  loading.value = true
  try {
    const res = await articleList({ pageNum: 1, pageSize: 999 })
    if (res.code === 200 && res.data?.list) {
      groupByYear(res.data.list)
    }
  } finally {
    loading.value = false
    await nextTick()
    initObservers()
  }
}

const groupByYear = (articles: ArticleItem[]) => {
  const map = new Map<string, ArticleItem[]>()
  articles.forEach((article) => {
    const year = formatDateTime(article.publishedTime, 'yyyy')
    if (!map.has(year)) map.set(year, [])
    map.get(year)!.push(article)
  })
  yearGroups.value = Array.from(map.entries())
    .sort((a, b) => parseInt(b[0]) - parseInt(a[0]))
    .map(([year, articles]) => ({ year, articles }))
}

const initObservers = () => {
  if (!timelineRef.value) return

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible')
          const yearEl = (entry.target as HTMLElement).closest('.year-section')
          if (yearEl) {
            activeYear.value = yearEl.getAttribute('data-year') || ''
          }
        }
      })
    },
    { threshold: 0.15, rootMargin: '-50px 0px -50px 0px' }
  )

  timelineRef.value.querySelectorAll('.timeline-item').forEach((el) => {
    observer.observe(el)
  })

  const yearObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting && entry.intersectionRatio > 0.3) {
          activeYear.value = (entry.target as HTMLElement).getAttribute('data-year') || ''
        }
      })
    },
    { threshold: [0, 0.3, 0.6] }
  )

  timelineRef.value.querySelectorAll('.year-section').forEach((el) => {
    yearObserver.observe(el)
  })
}

const getCardColor = (index: number) => {
  return colors[index % colors.length]
}

let animFrameId = 0

const initParticles = () => {
  const canvas = particleCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const particles: Array<{
    x: number; y: number; r: number; dx: number; dy: number; alpha: number; color: string
  }> = []

  for (let i = 0; i < 40; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      r: Math.random() * 2 + 1,
      dx: (Math.random() - 0.5) * 0.5,
      dy: (Math.random() - 0.5) * 0.5,
      alpha: Math.random() * 0.5 + 0.1,
      color: colors[Math.floor(Math.random() * colors.length)]
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    particles.forEach((p) => {
      p.x += p.dx
      p.y += p.dy
      if (p.x < 0 || p.x > canvas.width) p.dx *= -1
      if (p.y < 0 || p.y > canvas.height) p.dy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = p.color
      ctx.globalAlpha = p.alpha
      ctx.fill()
    })

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 150) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = particles[i].color
          ctx.globalAlpha = 0.05 * (1 - dist / 150)
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    ctx.globalAlpha = 1
    animFrameId = requestAnimationFrame(animate)
  }
  animate()
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  getArticleList()
  initParticles()
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
})
</script>

<template>
  <div class="archive-page">
    <canvas ref="particleCanvas" class="particle-bg"></canvas>

    <div class="archive-hero">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="title-char" v-for="(char, i) in '时光隧道'" :key="i"
                :style="{ animationDelay: `${i * 0.12}s`, color: colors[i % colors.length] }">
            {{ char }}
          </span>
        </h1>
        <p class="hero-subtitle">沿着时间的轨迹，回顾每一篇文章</p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">{{ yearGroups.reduce((sum, g) => sum + g.articles.length, 0) }}</span>
            <span class="stat-label">篇文章</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">{{ yearGroups.length }}</span>
            <span class="stat-label">个年份</span>
          </div>
        </div>
      </div>
      <div class="scroll-hint" @click="scrollToTop">
        <span>向下探索</span>
        <div class="scroll-arrow">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <div class="timeline-container" ref="timelineRef">
      <div class="timeline-line"></div>

      <div v-if="loading" class="loading-state">
        <div class="loading-sphere">
          <div class="sphere-ring" v-for="i in 3" :key="i"></div>
        </div>
        <p>正在加载时空数据...</p>
      </div>

      <template v-else>
        <div
          v-for="group in yearGroups"
          :key="group.year"
          class="year-section"
          :data-year="group.year"
        >
          <div class="year-marker">
            <div class="year-dot" :style="{ background: getCardColor(yearGroups.indexOf(group)) }"></div>
            <h2 class="year-label" :style="{ color: getCardColor(yearGroups.indexOf(group)) }">
              {{ group.year }}
            </h2>
          </div>

          <div
            v-for="(article, idx) in group.articles"
            :key="article.articleId"
            class="timeline-item"
            :class="idx % 2 === 0 ? 'left' : 'right'"
            :style="{ '--accent-color': getCardColor(yearGroups.indexOf(group) + idx) }"
          >
            <div class="timeline-dot" :style="{ background: getCardColor(yearGroups.indexOf(group) + idx) }">
              <div class="dot-pulse"></div>
            </div>

            <div class="timeline-date">
              <span class="date-month">{{ formatDateTime(article.publishedTime, 'MM') }}</span>
              <span class="date-divider">/</span>
              <span class="date-day">{{ formatDateTime(article.publishedTime, 'dd') }}</span>
            </div>

            <div class="timeline-card" @click="toArticle(article.slug, article.articleId)">
              <div class="card-glow"></div>
              <div class="card-image" v-if="article.coverImage">
                <img :src="article.coverImage" :alt="article.title" />
                <div class="card-image-overlay"></div>
              </div>
              <div class="card-body">
                <div class="card-category" v-if="article.categoryName">
                  {{ article.categoryName }}
                </div>
                <h3 class="card-title">{{ article.title }}</h3>
                <p class="card-summary">{{ article.summary }}</p>
                <div class="card-footer">
                  <div class="card-avatar">
                    <img src="@/assets/images/head.jpg" alt="author" />
                    <span>Mr.XiaoYi</span>
                  </div>
                  <div class="card-action">
                    <span>阅读全文</span>
                    <svg class="arrow-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M5 12h14M12 5l7 7-7 7"/>
                    </svg>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="timeline-end">
          <div class="end-dot">
            <div class="end-dot-inner"></div>
          </div>
          <p class="end-text">— 一切从这里开始 —</p>
          <div class="end-stars">
            <span v-for="i in 5" :key="i" class="star" :style="{ animationDelay: `${i * 0.3}s` }">✦</span>
          </div>
        </div>
      </template>
    </div>

    <div class="time-navigator" :class="{ visible: activeYear }">
      <div class="nav-current" v-if="activeYear">
        <span class="nav-year">{{ activeYear }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.archive-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #0a0a1a 0%, #0d1117 30%, #0a0a1a 100%);
  overflow-x: hidden;
}

.particle-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.archive-hero {
  position: relative;
  z-index: 1;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-bottom: 80px;
  box-sizing: border-box;
}

.hero-content {
  text-align: center;
}

.hero-title {
  font-size: 72px;
  font-weight: 900;
  letter-spacing: 12px;
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
  gap: 4px;
}

.title-char {
  display: inline-block;
  animation: titleFloat 3s ease-in-out infinite;
  text-shadow: 0 0 30px currentColor, 0 0 60px currentColor;
}

@keyframes titleFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 4px;
  margin-bottom: 48px;
}

.hero-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
}

.stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 42px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff6b6b, #ffd43b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 2px;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.15);
}

.scroll-hint {
  position: absolute;
  bottom: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  span {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.4);
    letter-spacing: 3px;
    margin-bottom: 12px;
  }
}

.scroll-arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  span {
    display: block;
    width: 8px;
    height: 8px;
    border-right: 1px solid rgba(255, 255, 255, 0.5);
    border-bottom: 1px solid rgba(255, 255, 255, 0.5);
    transform: rotate(45deg);
    animation: scrollBounce 1.5s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.15s; }
    &:nth-child(3) { animation-delay: 0.3s; }
  }
}

@keyframes scrollBounce {
  0%, 100% { opacity: 0.3; transform: rotate(45deg) translate(0, 0); }
  50% { opacity: 1; transform: rotate(45deg) translate(4px, 4px); }
}

.timeline-container {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 40px 120px;
}

.timeline-line {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(255, 255, 255, 0.1) 5%,
    rgba(255, 255, 255, 0.2) 50%,
    rgba(255, 255, 255, 0.1) 95%,
    transparent 100%
  );
  transform: translateX(-50%);
}

.loading-state {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  p {
    color: rgba(255, 255, 255, 0.4);
    letter-spacing: 2px;
    font-size: 14px;
    margin-top: 24px;
  }
}

.loading-sphere {
  position: relative;
  width: 60px;
  height: 60px;
}

.sphere-ring {
  position: absolute;
  inset: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  animation: ringSpin 2s linear infinite;
  &:nth-child(1) {
    border-top-color: #ff6b6b;
    animation-duration: 2s;
  }
  &:nth-child(2) {
    inset: 8px;
    border-right-color: #4dabf7;
    animation-duration: 1.5s;
    animation-direction: reverse;
  }
  &:nth-child(3) {
    inset: 16px;
    border-bottom-color: #ffd43b;
    animation-duration: 1s;
  }
}

@keyframes ringSpin {
  to { transform: rotate(360deg); }
}

.year-section {
  position: relative;
  z-index: 2;
}

.year-marker {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px 0 40px;
  position: relative;
  z-index: 3;
}

.year-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  box-shadow: 0 0 20px currentColor, 0 0 40px currentColor;
  flex-shrink: 0;
}

.year-label {
  font-size: 48px;
  font-weight: 900;
  letter-spacing: 6px;
  margin: 0;
  text-shadow: 0 0 40px currentColor;
}

.timeline-item {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-start;
  margin-bottom: 48px;
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);

  &.visible {
    opacity: 1;
    transform: translateY(0);
  }

  &.left {
    flex-direction: row;
    padding-right: calc(50% + 40px);

    .timeline-dot { right: -9px; }
    .timeline-date { right: -80px; text-align: right; }
  }

  &.right {
    flex-direction: row-reverse;
    padding-left: calc(50% + 40px);

    .timeline-dot { left: -9px; }
    .timeline-date { left: -80px; text-align: left; }
  }
}

.timeline-dot {
  position: absolute;
  top: 24px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  z-index: 3;
  box-shadow: 0 0 15px currentColor, 0 0 30px currentColor;
  transition: transform 0.3s ease;

  .timeline-item:hover & {
    transform: scale(1.5);
  }
}

.dot-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: inherit;
  opacity: 0.4;
  animation: dotPulse 2s ease-out infinite;
}

@keyframes dotPulse {
  0% { transform: scale(1); opacity: 0.4; }
  100% { transform: scale(2.5); opacity: 0; }
}

.timeline-date {
  position: absolute;
  top: 18px;
  display: flex;
  align-items: baseline;
  gap: 1px;

  .date-month {
    font-size: 20px;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.7);
    letter-spacing: 2px;
  }

  .date-divider {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.3);
    margin: 0 2px;
  }

  .date-day {
    font-size: 14px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.45);
  }
}

.timeline-card {
  position: relative;
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: translateY(-4px);
    border-color: var(--accent-color);
    box-shadow:
      0 8px 32px rgba(0, 0, 0, 0.3),
      0 0 60px color-mix(in srgb, var(--accent-color) 20%, transparent);

    .card-glow {
      opacity: 1;
    }

    .card-image img {
      transform: scale(1.08);
    }

    .card-action {
      color: var(--accent-color);
      .arrow-icon {
        transform: translateX(4px);
      }
    }
  }
}

.card-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at center,
    color-mix(in srgb, var(--accent-color) 15%, transparent) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
  z-index: 0;
}

.card-image {
  width: 100%;
  height: 180px;
  overflow: hidden;
  position: relative;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  }
}

.card-image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(transparent, rgba(13, 17, 23, 0.8));
}

.card-body {
  position: relative;
  z-index: 1;
  padding: 20px 24px 24px;
}

.card-category {
  display: inline-block;
  font-size: 11px;
  color: var(--accent-color);
  border: 1px solid color-mix(in srgb, var(--accent-color) 30%, transparent);
  border-radius: 20px;
  padding: 2px 12px;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 1px;
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.5px;
  line-height: 1.8;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 16px;
}

.card-avatar {
  display: flex;
  align-items: center;
  flex: 1;

  img {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    border: 2px solid rgba(255, 255, 255, 0.15);
  }

  span {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.35);
    margin-left: 8px;
    letter-spacing: 0.5px;
  }
}

.card-action {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1px;
  transition: color 0.3s ease;

  .arrow-icon {
    transition: transform 0.3s ease;
  }
}

.timeline-end {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 80px 0 40px;
}

.end-dot {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  animation: endGlow 3s ease-in-out infinite;
}

.end-dot-inner {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
}

@keyframes endGlow {
  0%, 100% {
    box-shadow: 0 0 20px rgba(255, 255, 255, 0.05);
  }
  50% {
    box-shadow: 0 0 40px rgba(255, 255, 255, 0.15), 0 0 80px rgba(255, 255, 255, 0.05);
  }
}

.end-text {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 4px;
  margin-bottom: 16px;
}

.end-stars {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.star {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.25);
  animation: starTwinkle 2s ease-in-out infinite;
}

@keyframes starTwinkle {
  0%, 100% { opacity: 0.2; transform: scale(0.8); }
  50% { opacity: 0.6; transform: scale(1.2); }
}

.time-navigator {
  position: fixed;
  right: 32px;
  top: 50%;
  transform: translateY(-50%) translateX(20px);
  z-index: 10;
  opacity: 0;
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
  pointer-events: none;

  &.visible {
    opacity: 1;
    transform: translateY(-50%) translateX(0);
  }
}

.nav-current {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 12px 20px;
}

.nav-year {
  font-size: 28px;
  font-weight: 900;
  letter-spacing: 3px;
  background: linear-gradient(135deg, #ff6b6b, #ffd43b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 36px;
    letter-spacing: 4px;
  }

  .hero-subtitle {
    font-size: 14px;
    letter-spacing: 2px;
  }

  .hero-stats {
    gap: 20px;
  }

  .stat-num {
    font-size: 28px;
  }

  .timeline-container {
    padding: 0 20px 80px;
  }

  .timeline-line {
    left: 24px;
  }

  .year-marker {
    justify-content: flex-start;
    padding-left: 24px;
    gap: 12px;
  }

  .year-label {
    font-size: 32px;
    letter-spacing: 3px;
  }

  .timeline-item {
    &.left,
    &.right {
      flex-direction: row;
      padding-left: 60px;
      padding-right: 0;

      .timeline-dot { left: 16px; right: auto; }
      .timeline-date {
        left: auto;
        right: 0;
        top: -28px;
        text-align: right;
      }
    }
  }

  .timeline-card {
    border-radius: 12px;
  }

  .card-image {
    height: 140px;
  }

  .time-navigator {
    display: none;
  }
}
</style>