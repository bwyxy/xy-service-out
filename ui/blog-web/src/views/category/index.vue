<script lang="ts" setup>
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { formatDateTime } from '@bwyxy/utils'
import { categoryList } from '@/api/category.ts'
import { articleList } from '@/api/blog.ts'

interface CategoryItem {
  categoryId: number
  name: string
}

interface ArticleItem {
  articleId: string
  slug: string
  title: string
  summary: string
  coverImage: string
  publishedTime: string
  categoryName?: string
}

const router = useRouter()
const categories = ref<(CategoryItem & { articleCount: number; loading: boolean })[]>([])
const expandedId = ref<number | null>(null)
const articles = ref<ArticleItem[]>([])
const totalArticles = ref(0)
const starCanvas = ref<HTMLCanvasElement | null>(null)
const cardRefs = ref<Map<number, HTMLElement>>(new Map())

const gradients = [
  ['#ff6b6b', '#ee5a24'],
  ['#ffa94d', '#ff6b35'],
  ['#ffd43b', '#fab005'],
  ['#69db7c', '#2f9e44'],
  ['#4dabf7', '#1c7ed6'],
  ['#da77f2', '#9c36b5'],
  ['#f783ac', '#d6336c'],
  ['#63e6be', '#20c997'],
  ['#748ffc', '#4263eb'],
  ['#ff8787', '#e03131'],
]

const getGradient = (index: number) => {
  const g = gradients[index % gradients.length]
  return `linear-gradient(135deg, ${g[0]} 0%, ${g[1]} 100%)`
}

const getGlowColor = (index: number) => {
  return gradients[index % gradients.length][0]
}

const toArticle = (slug: string, articleId: string) => {
  router.push({ path: `/article/${slug || articleId || ''}` })
}

const fetchCategories = async () => {
  const res = await categoryList({})
  if (res.code === 200) {
    categories.value = res.data.map((cat: CategoryItem) => ({
      ...cat,
      articleCount: 0,
      loading: false
    }))
    // await fetchAllArticleCounts()
  }
}

const fetchAllArticleCounts = async () => {
  await Promise.all(
    categories.value.map(async (cat) => {
      const res = await articleList({ pageNum: 1, pageSize: 1, categoryId: cat.categoryId })
      if (res.code === 200) {
        cat.articleCount = res.data.total || 0
        totalArticles.value += cat.articleCount
      }
    })
  )
}

const toggleCategory = async (cat: (typeof categories.value)[0]) => {
  if (expandedId.value === cat.categoryId) {
    expandedId.value = null
    articles.value = []
    return
  }

  expandedId.value = cat.categoryId
  cat.loading = true

  try {
    const res = await articleList({ pageNum: 1, pageSize: 50, categoryId: cat.categoryId })
    if (res.code === 200) {
      articles.value = res.data.list || []
      await nextTick()
      initArticleAnimations()
    }
  } finally {
    cat.loading = false
  }
}

const initArticleAnimations = () => {
  document.querySelectorAll('.article-star').forEach((el, i) => {
    setTimeout(() => {
      el.classList.add('visible')
    }, i * 80)
  })
}

const setCardRef = (el: any, id: number) => {
  if (el) cardRefs.value.set(id, el)
}

let animFrameId = 0

const initStars = () => {
  const canvas = starCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const stars: Array<{
    x: number; y: number; r: number; alpha: number; twinkle: number; twinkleSpeed: number; color: string
  }> = []

  const glowColors = gradients.map(g => g[0])

  for (let i = 0; i < 100; i++) {
    stars.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      r: Math.random() * 2 + 0.5,
      alpha: Math.random() * 0.8 + 0.2,
      twinkle: Math.random() * Math.PI * 2,
      twinkleSpeed: Math.random() * 0.02 + 0.005,
      color: glowColors[Math.floor(Math.random() * glowColors.length)]
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    stars.forEach((s) => {
      s.twinkle += s.twinkleSpeed
      const currentAlpha = s.alpha * (0.5 + 0.5 * Math.sin(s.twinkle))
      ctx.beginPath()
      ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx.fillStyle = s.color
      ctx.globalAlpha = currentAlpha
      ctx.fill()

      if (s.r > 1.2) {
        ctx.beginPath()
        ctx.arc(s.x, s.y, s.r * 3, 0, Math.PI * 2)
        ctx.fillStyle = s.color
        ctx.globalAlpha = currentAlpha * 0.1
        ctx.fill()
      }
    })
    ctx.globalAlpha = 1
    animFrameId = requestAnimationFrame(animate)
  }
  animate()
}

const scrollToCards = () => {
  const section = document.querySelector('.category-section')
  if (section) {
    section.scrollIntoView({ behavior: 'smooth' })
  }
}

onMounted(() => {
  fetchCategories()
  initStars()
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
})
</script>

<template>
  <div class="category-page">
    <canvas ref="starCanvas" class="star-bg"></canvas>

    <div class="category-hero">
      <div class="hero-constellation">
        <svg class="constellation-svg" viewBox="0 0 400 300">
          <g v-for="(cat, i) in categories.slice(0, 8)" :key="cat.categoryId">
            <circle
              :cx="50 + (i % 4) * 100 + Math.sin(i * 1.2) * 20"
              :cy="50 + Math.floor(i / 4) * 120 + Math.cos(i * 1.5) * 20"
              :r="6"
              :fill="getGlowColor(i)"
              :opacity="0.8"
            >
              <animate attributeName="r" :values="'6;10;6'" dur="3s" :begin="`${i * 0.3}s`" repeatCount="indefinite" />
              <animate attributeName="opacity" :values="'0.8;0.3;0.8'" dur="3s" :begin="`${i * 0.3}s`" repeatCount="indefinite" />
            </circle>
          </g>
          <line
            v-for="(cat, i) in categories.slice(0, 7)" :key="'line-' + cat.categoryId"
            :x1="50 + (i % 4) * 100 + Math.sin(i * 1.2) * 20"
            :y1="50 + Math.floor(i / 4) * 120 + Math.cos(i * 1.5) * 20"
            :x2="50 + ((i + 1) % 4) * 100 + Math.sin((i + 1) * 1.2) * 20"
            :y2="50 + Math.floor((i + 1) / 4) * 120 + Math.cos((i + 1) * 1.5) * 20"
            stroke="rgba(255,255,255,0.15)"
            stroke-width="0.5"
            stroke-dasharray="3 3"
          >
            <animate attributeName="stroke-dashoffset" from="6" to="0" dur="2s" repeatCount="indefinite" />
          </line>
        </svg>
      </div>

      <div class="hero-content">
        <h1 class="hero-title">
          <span
            v-for="(char, i) in '分类'"
            :key="i"
            class="title-char"
            :style="{
              animationDelay: `${i * 0.1}s`,
              color: gradients[i % gradients.length][0]
            }"
          >
            {{ char }}
          </span>
        </h1>
        <p class="hero-subtitle">探索不同领域的知识星球</p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">{{ categories.length }}</span>
            <span class="stat-label">个分类</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">{{ totalArticles }}</span>
            <span class="stat-label">篇文章</span>
          </div>
        </div>
      </div>

      <div class="scroll-hint" @click="scrollToCards">
        <span>开始探索</span>
        <div class="scroll-arrow">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <div class="category-section">
      <div class="category-grid">
        <div
          v-for="(cat, index) in categories"
          :key="cat.categoryId"
          :ref="(el) => setCardRef(el, cat.categoryId)"
          class="category-card"
          :class="{ expanded: expandedId === cat.categoryId }"
          :style="{
            '--card-bg': getGradient(index),
            '--card-glow': getGlowColor(index),
            animationDelay: `${index * 0.08}s`
          }"
          @click="toggleCategory(cat)"
        >
          <div class="card-orbit">
            <div class="orbit-ring"></div>
            <div class="orbit-ring orbit-ring-2"></div>
          </div>

          <div class="card-core">
            <span class="core-emoji">{{ ['🚀','🌟','💡','🔥','🎨','🔬','📚','🎵','🎮','💎'][index % 10] }}</span>
          </div>

          <div class="card-info">
            <h3 class="card-name">{{ cat.name }}</h3>
            <span class="card-count">{{ cat.articleCount }} 篇文章</span>
          </div>

          <div class="card-expand-icon" :class="{ rotating: cat.loading }">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
              <path v-if="expandedId !== cat.categoryId" d="M12 5v14M5 12h14" />
              <path v-else d="M5 12h14" />
            </svg>
          </div>

          <div class="card-loader" v-if="cat.loading">
            <span class="loader-dot" v-for="d in 3" :key="d" :style="{ animationDelay: `${d * 0.15}s` }"></span>
          </div>
        </div>
      </div>

      <transition name="articles-expand">
        <div v-if="expandedId !== null && articles.length > 0" class="articles-panel">
          <div class="articles-header">
            <h2 class="articles-title">
              共 <span class="count-highlight">{{ articles.length }}</span> 篇文章
            </h2>
            <button class="close-btn" @click.stop="expandedId = null; articles = []">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
              收起
            </button>
          </div>

          <div class="articles-grid">
            <div
              v-for="(article, idx) in articles"
              :key="article.articleId"
              class="article-star"
              :style="{ '--article-glow': getGlowColor(idx), animationDelay: `${idx * 0.06}s` }"
              @click="toArticle(article.slug, article.articleId)"
            >
              <div class="article-glow-bg"></div>
              <div class="article-image" v-if="article.coverImage">
                <img :src="article.coverImage" :alt="article.title" />
              </div>
              <div class="article-body">
                <span class="article-date">{{ formatDateTime(article.publishedTime, 'yyyy-MM-dd') }}</span>
                <h3 class="article-title">{{ article.title }}</h3>
                <p class="article-summary">{{ article.summary }}</p>
                <div class="article-footer">
                  <span class="article-read">阅读全文</span>
                  <svg class="article-arrow" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M5 12h14M12 5l7 7-7 7"/>
                  </svg>
                </div>
              </div>
              <div class="article-trail">
                <span class="trail-dot" v-for="t in 3" :key="t" :style="{ animationDelay: `${t * 0.2}s` }"></span>
              </div>
            </div>
          </div>
        </div>
      </transition>

      <div v-if="expandedId !== null && articles.length === 0 && !categories.find(c => c.categoryId === expandedId)?.loading"
           class="articles-panel empty-panel">
        <div class="empty-state">
          <span class="empty-emoji">🪐</span>
          <p>这个星球上还没有文章</p>
          <span class="empty-hint">敬请期待...</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.category-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #07071a 0%, #0a0f1e 30%, #07071a 100%);
  overflow-x: hidden;
}

.star-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.category-hero {
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

.hero-constellation {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500px;
  height: 350px;
  opacity: 0.4;
  pointer-events: none;
}

.constellation-svg {
  width: 100%;
  height: 100%;
}

.hero-content {
  position: relative;
  z-index: 2;
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
  background: linear-gradient(135deg, #4dabf7, #da77f2);
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
  z-index: 2;
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

.category-section {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 40px 120px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.category-card {
  position: relative;
  background: var(--card-bg);
  border-radius: 20px;
  padding: 32px 24px 28px;
  cursor: pointer;
  overflow: hidden;
  animation: cardAppear 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: translateY(-6px);
    box-shadow:
      0 12px 40px rgba(0, 0, 0, 0.4),
      0 0 80px color-mix(in srgb, var(--card-glow) 25%, transparent);

    .card-orbit .orbit-ring {
      border-color: rgba(255, 255, 255, 0.4);
    }

    .card-core {
      transform: scale(1.1);
    }
  }

  &.expanded {
    grid-column: 1 / -1;
    transform: scale(1);
    box-shadow:
      0 16px 48px rgba(0, 0, 0, 0.5),
      0 0 100px color-mix(in srgb, var(--card-glow) 35%, transparent);
  }
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.card-orbit {
  position: absolute;
  inset: 16px;
  pointer-events: none;
}

.orbit-ring {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  animation: orbitSpin 8s linear infinite;
  transition: border-color 0.4s ease;
}

.orbit-ring-2 {
  inset: 16px;
  border-color: rgba(255, 255, 255, 0.08);
  animation-duration: 5s;
  animation-direction: reverse;
}

@keyframes orbitSpin {
  to { transform: rotate(360deg); }
}

.card-core {
  position: relative;
  z-index: 2;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.core-emoji {
  font-size: 24px;
  line-height: 1;
}

.card-info {
  position: relative;
  z-index: 2;
  text-align: center;
}

.card-name {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 6px;
}

.card-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
}

.card-expand-icon {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;

  &.rotating {
    animation: iconRotate 1s linear infinite;
  }
}

@keyframes iconRotate {
  to { transform: rotate(360deg); }
}

.card-loader {
  position: absolute;
  bottom: 18px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
}

.loader-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  animation: loaderBounce 0.8s ease-in-out infinite;

  &:nth-child(2) { animation-delay: 0.15s; }
  &:nth-child(3) { animation-delay: 0.3s; }
}

@keyframes loaderBounce {
  0%, 100% { transform: translateY(0); opacity: 0.4; }
  50% { transform: translateY(-8px); opacity: 1; }
}

.articles-expand-enter-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.articles-expand-leave-active {
  transition: all 0.3s ease-in;
}

.articles-expand-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.articles-expand-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.articles-panel {
  margin-top: 32px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  padding: 32px;
  backdrop-filter: blur(12px);
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.empty-state {
  text-align: center;

  .empty-emoji {
    font-size: 48px;
    display: block;
    margin-bottom: 16px;
    animation: emptyFloat 3s ease-in-out infinite;
  }

  p {
    color: rgba(255, 255, 255, 0.4);
    font-size: 15px;
    letter-spacing: 1px;
    margin-bottom: 8px;
  }

  .empty-hint {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.25);
    letter-spacing: 2px;
  }
}

@keyframes emptyFloat {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-10px) rotate(5deg); }
}

.articles-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.articles-title {
  font-size: 18px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 1px;
  margin: 0;
}

.count-highlight {
  font-size: 24px;
  font-weight: 900;
  background: linear-gradient(135deg, #ffd43b, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.close-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  cursor: pointer;
  letter-spacing: 1px;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(255, 255, 255, 0.25);
    color: rgba(255, 255, 255, 0.8);
    background: rgba(255, 255, 255, 0.08);
  }
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.article-star {
  position: relative;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  opacity: 0;
  transform: translateY(20px);
  animation: starAppear 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);

  &.visible {
    opacity: 1;
    transform: translateY(0);
  }

  &:hover {
    transform: translateY(-4px);
    border-color: var(--article-glow);
    box-shadow:
      0 8px 32px rgba(0, 0, 0, 0.3),
      0 0 50px color-mix(in srgb, var(--article-glow) 15%, transparent);

    .article-glow-bg {
      opacity: 1;
    }

    .article-image img {
      transform: scale(1.06);
    }

    .article-arrow {
      transform: translateX(4px);
    }

    .article-trail .trail-dot {
      opacity: 1;
    }
  }
}

@keyframes starAppear {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.article-glow-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at center,
    color-mix(in srgb, var(--article-glow) 12%, transparent) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
  z-index: 0;
}

.article-image {
  width: 100%;
  height: 150px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
  }
}

.article-body {
  position: relative;
  z-index: 1;
  padding: 16px 20px 20px;
}

.article-date {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 1px;
}

.article-title {
  font-size: 15px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 0.5px;
  line-height: 1.5;
  margin: 6px 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-summary {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 0.5px;
  line-height: 1.7;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-footer {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 1px;
}

.article-arrow {
  margin-left: 4px;
  transition: transform 0.3s ease;
}

.article-trail {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 4px;
  z-index: 2;
}

.trail-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  opacity: 0;
  transition: opacity 0.3s ease;
  animation: trailPulse 1.5s ease-in-out infinite;
}

@keyframes trailPulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.8); opacity: 0.8; }
}

@media (max-width: 1024px) {
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .articles-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .hero-title {
    font-size: 36px;
    letter-spacing: 4px;
  }

  .hero-subtitle {
    font-size: 14px;
    letter-spacing: 2px;
  }

  .hero-constellation {
    width: 300px;
    height: 220px;
  }

  .category-grid {
    grid-template-columns: 1fr;
  }

  .category-section {
    padding: 0 16px 80px;
  }

  .articles-grid {
    grid-template-columns: 1fr;
  }

  .articles-panel {
    padding: 20px;
  }
}
</style>