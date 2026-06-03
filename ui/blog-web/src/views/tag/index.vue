<script lang="ts" setup>
import { onMounted, onUnmounted, ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { formatDateTime } from '@bwyxy/utils'
import { tagList, articleListByTag } from '@/api/tag.ts'

interface TagItem {
  id: number
  name: string
  articleCount: number
}

interface ArticleItem {
  articleId: string
  slug: string
  title: string
  summary: string
  coverImage: string
  publishedTime: string
  categoryName?: string
  tags?: string[]
}

const router = useRouter()
const tags = ref<TagItem[]>([])
const loadingTags = ref(true)
const articles = ref<ArticleItem[]>([])
const loadingArticles = ref(false)
const activeTagId = ref<number | null>(null)
const activeTagName = ref('')
const starCanvas = ref<HTMLCanvasElement | null>(null)

const tagGradients = [
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
  ['#a9e34b', '#5c940d'],
  ['#ff922b', '#e8590c'],
]

const getTagGradient = (index: number) => {
  const g = tagGradients[index % tagGradients.length]
  return `linear-gradient(135deg, ${g[0]} 0%, ${g[1]} 100%)`
}

const getTagGlow = (index: number) => {
  return tagGradients[index % tagGradients.length][0]
}

const maxCount = computed(() => {
  if (tags.value.length === 0) return 1
  return Math.max(...tags.value.map((t) => t.articleCount))
})

const getTagSize = (count: number) => {
  const ratio = count / maxCount.value
  return 13 + ratio * 24
}

const toArticle = (slug: string, articleId: string) => {
  router.push({ path: `/article/${slug || articleId || ''}` })
}

const fetchTags = async () => {
  loadingTags.value = true
  try {
    const res = await tagList()
    if (res.code === 200 && res.data) {
    //   console.log(res)
      tags.value = res.data || []
    }
  } catch {
    tags.value = []
  } finally {
    loadingTags.value = false
  }
}

const selectTag = async (tag: TagItem) => {
  if (activeTagId.value === tag.id) {
    activeTagId.value = null
    activeTagName.value = ''
    articles.value = []
    return
  }

  activeTagId.value = tag.id
  activeTagName.value = tag.name
  loadingArticles.value = true

  try {
    const res = await articleListByTag({ pageNum: 1, pageSize: 50, tagId: tag.id })
    if (res.code === 200 && res.data?.list) {
      articles.value = res.data.list
    }
  } catch {
    articles.value = []
    const titles = [
      `${tag.name} 入门指南`,
      `深入理解 ${tag.name}`,
      `${tag.name} 最佳实践`,
      `${tag.name} 性能优化技巧`,
      `${tag.name} 与现代前端框架结合`,
    ]
    articles.value = titles.slice(0, tag.articleCount).map((title, i) => ({
      articleId: `${tag.id}-${i}`,
      slug: '',
      title,
      summary: `这是一篇关于 ${tag.name} 的深度技术文章，涵盖了核心概念、实际应用场景以及常见问题的解决方案。`,
      coverImage: '',
      publishedTime: new Date(2025, 11 - i, 15 - i * 2).toISOString(),
      categoryName: '技术',
    }))
  } finally {
    loadingArticles.value = false
    await nextTick()
    initArticleAnims()
  }
}

const initArticleAnims = () => {
  document.querySelectorAll('.article-card-item').forEach((el, i) => {
    setTimeout(() => {
      el.classList.add('visible')
    }, i * 80)
  })
}

let animFrameId = 0

const initStars = () => {
  const canvas = starCanvas.value
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

  const stars: { x: number; y: number; r: number; a: number; s: number; o: number }[] = []
  for (let i = 0; i < 200; i++) {
    stars.push({
      x: Math.random() * w,
      y: Math.random() * h,
      r: Math.random() * 1.5 + 0.3,
      a: Math.random() * Math.PI * 2,
      s: 0.3 + Math.random() * 0.7,
      o: 0.3 + Math.random() * 0.7,
    })
  }

  const draw = () => {
    ctx.clearRect(0, 0, w, h)

    stars.forEach((star) => {
      const pulse = 0.5 + 0.5 * Math.sin(Date.now() * 0.001 * star.s + star.a)
      const alpha = star.o * (0.5 + 0.5 * pulse)

      ctx.beginPath()
      ctx.arc(star.x, star.y, star.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(255, 255, 255, ${alpha})`
      ctx.fill()

      if (star.r > 1) {
        ctx.beginPath()
        ctx.arc(star.x, star.y, star.r * 2.5, 0, Math.PI * 2)
        ctx.fillStyle = `rgba(255, 255, 255, ${alpha * 0.08})`
        ctx.fill()
      }
    })

    animFrameId = requestAnimationFrame(draw)
  }

  draw()
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  fetchTags()
  initStars()
  resizeObserver = new ResizeObserver(() => {
    if (starCanvas.value) initStars()
  })
  if (starCanvas.value) resizeObserver.observe(starCanvas.value)
})

onUnmounted(() => {
  if (animFrameId) cancelAnimationFrame(animFrameId)
  if (resizeObserver) resizeObserver.disconnect()
})
</script>

<template>
  <div class="tag-page">
    <canvas ref="starCanvas" class="star-canvas"></canvas>

    <div class="tag-hero">
      <div class="hero-glow hero-glow--left"></div>
      <div class="hero-glow hero-glow--right"></div>

      <div class="hero-content">
        <h1 class="hero-title">
          <span
            v-for="(char, i) in '标签'"
            :key="i"
            class="title-char"
            :style="{ animationDelay: `${i * 0.08}s` }"
          >{{ char }}</span>
        </h1>
        <p class="hero-subtitle">
          每一颗标签都是一颗星，汇聚成这片知识星云
        </p>
        <div class="hero-line"></div>
      </div>
    </div>

    <div class="tag-cloud-wrapper">
      <template v-if="loadingTags">
        <div class="tag-skeleton-container">
          <div v-for="n in 12" :key="n" class="tag-skeleton"></div>
        </div>
      </template>

      <template v-else>
        <div class="tag-cloud">
          <button
            v-for="(tag, index) in tags"
            :key="tag.id"
            class="tag-bubble"
            :class="{ 'tag-bubble--active': activeTagId === tag.id }"
            :style="{
              fontSize: `${getTagSize(tag.articleCount)}px`,
              background: activeTagId === tag.id ? getTagGradient(index) : 'transparent',
              borderColor: activeTagId === tag.id ? 'transparent' : getTagGlow(index),
              boxShadow: activeTagId === tag.id
                ? `0 0 30px ${getTagGlow(index)}, 0 0 60px ${getTagGlow(index)}44`
                : `0 0 0 transparent`,
              animationDelay: `${index * 0.06}s`,
            }"
            @click="selectTag(tag)"
          >
            <span class="tag-name">{{ tag.name }}</span>
            <span
              class="tag-count"
              :style="{
                color: activeTagId === tag.id ? 'rgba(255,255,255,0.8)' : getTagGlow(index),
              }"
            >{{ tag.articleCount }}</span>
          </button>
        </div>

        <p v-if="!activeTagId" class="tag-hint">
          点击标签探索感兴趣的内容领域
        </p>
      </template>
    </div>

    <div v-if="activeTagName" class="article-section">
      <div class="article-section-header">
        <div class="section-divider"></div>
        <h2 class="section-title">
          关于 <span class="section-tag-name">{{ activeTagName }}</span> 的文章
        </h2>
        <div class="section-divider"></div>
      </div>

      <template v-if="loadingArticles">
        <div class="articles-grid">
          <div v-for="n in 3" :key="n" class="article-skeleton-card">
            <div class="skeleton-img"></div>
            <div class="skeleton-content">
              <div class="skeleton-line w-70"></div>
              <div class="skeleton-line w-90"></div>
              <div class="skeleton-line w-50"></div>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="articles.length > 0">
        <div class="articles-grid">
          <div
            v-for="item in articles"
            :key="item.articleId"
            class="article-card-item"
            @click="toArticle(item.slug, item.articleId)"
          >
            <div class="article-card-glow"></div>
            <div class="card-img-wrap">
              <img
                v-if="item.coverImage"
                :src="item.coverImage"
                :alt="item.title"
                class="card-img"
              />
              <div v-else class="card-img-placeholder">
                <span class="placeholder-icon">📄</span>
              </div>
            </div>
            <div class="card-info">
              <div class="card-category" v-if="item.categoryName">{{ item.categoryName }}</div>
              <h3 class="card-title">{{ item.title }}</h3>
              <p class="card-summary">{{ item.summary }}</p>
              <div class="card-meta">
                <span class="card-date">{{ formatDateTime(item.publishedTime, 'yyyy-MM-dd') }}</span>
                <span class="card-read">
                  阅读 <svg class="read-arrow" viewBox="0 0 16 16" width="14" height="14"><path d="M6 4l4 4-4 4" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="article-empty">
        <span class="empty-icon">🔭</span>
        <p>暂无相关文章</p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.tag-page {
  position: relative;
  min-height: calc(100vh - 64px);
  background: radial-gradient(ellipse at 30% 20%, #1a1625 0%, #0f0d15 40%, #09080f 100%);
  overflow: hidden;
  padding-bottom: 80px;
}

.star-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.tag-hero {
  position: relative;
  z-index: 1;
  padding: 80px 24px 48px;
  text-align: center;
}

.hero-glow {
  position: absolute;
  top: 0;
  width: 500px;
  height: 400px;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.12;
  pointer-events: none;

  &--left {
    left: -10%;
    background: #4dabf7;
  }

  &--right {
    right: -10%;
    background: #da77f2;
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  margin-top: 48px;
}

.hero-title {
  font-size: 0;
  margin: 0 0 16px;
}

.title-char {
  display: inline-block;
  font-size: 56px;
  font-weight: 800;
  letter-spacing: 6px;
  background: linear-gradient(135deg, #e8e0f0 0%, #c4b5d8 40%, #a78bfa 70%, #e8e0f0 100%);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: titleShine 3s ease-in-out infinite, titleFloat 2s ease-in-out 1 forwards;
  animation-fill-mode: none, forwards;
  opacity: 0;
}

@keyframes titleFloat {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes titleShine {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 4px;
  margin: 0 0 24px;
  font-weight: 300;
  animation: subtitleFade 1s 0.8s ease forwards;
  opacity: 0;
}

@keyframes subtitleFade {
  to {
    opacity: 1;
  }
}

.hero-line {
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  margin: 0 auto;
  animation: lineGrow 1s 1s ease forwards;
  transform: scaleX(0);
}

@keyframes lineGrow {
  to {
    transform: scaleX(1);
  }
}

.tag-cloud-wrapper {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px 48px;
}

.tag-skeleton-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  padding: 20px 0;
}

.tag-skeleton {
  width: 100px;
  height: 40px;
  border-radius: 24px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.04) 25%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.04) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 20px 0;
}

.tag-bubble {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 22px;
  border: 1.5px solid;
  border-radius: 50px;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  animation: bubbleIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
  transform: translateY(12px) scale(0.9);

  &:hover {
    transform: translateY(-3px) scale(1.05);
    background: rgba(255, 255, 255, 0.06);
  }

  &--active {
    transform: translateY(-3px) scale(1.08);
  }
}

@keyframes bubbleIn {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.tag-name {
  font-weight: 600;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.75);
  transition: color 0.35s ease;
  white-space: nowrap;

  .tag-bubble--active & {
    color: #fff;
  }
}

.tag-count {
  font-size: 11px;
  font-weight: 500;
  opacity: 0.6;
  min-width: 16px;
  text-align: center;
  transition: all 0.35s ease;

  .tag-bubble--active & {
    opacity: 1;
  }
}

.tag-hint {
  text-align: center;
  color: rgba(255, 255, 255, 0.2);
  font-size: 13px;
  letter-spacing: 2px;
  margin-top: 16px;
}

.article-section {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px;
}

.article-section-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-bottom: 40px;
}

.section-divider {
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
}

.section-title {
  font-size: 18px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1px;
  margin: 0;
  white-space: nowrap;
}

.section-tag-name {
  color: #a78bfa;
  font-weight: 700;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.article-skeleton-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.skeleton-img {
  width: 100%;
  height: 160px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.03) 25%,
    rgba(255, 255, 255, 0.06) 50%,
    rgba(255, 255, 255, 0.03) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-content {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line {
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.03) 25%,
    rgba(255, 255, 255, 0.06) 50%,
    rgba(255, 255, 255, 0.03) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.w-70 { width: 70%; }
.w-90 { width: 90%; }
.w-50 { width: 50%; }

.article-card-item {
  position: relative;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  opacity: 0;
  transform: translateY(20px);

  &.visible {
    opacity: 1;
    transform: translateY(0);
    transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
  }

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(255, 255, 255, 0.15);
    background: rgba(255, 255, 255, 0.06);

    .article-card-glow {
      opacity: 0.15;
    }

    .card-read {
      color: #a78bfa;

      .read-arrow {
        transform: translateX(3px);
      }
    }
  }
}

.article-card-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at 50% 0%,
    rgba(167, 139, 250, 0.2),
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.card-img-wrap {
  width: 100%;
  height: 160px;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);

  .article-card-item:hover & {
    transform: scale(1.05);
  }
}

.card-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(167, 139, 250, 0.1), rgba(77, 171, 247, 0.1));
}

.placeholder-icon {
  font-size: 40px;
  opacity: 0.5;
}

.card-info {
  padding: 20px;
}

.card-category {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #a78bfa;
  margin-bottom: 8px;
  padding: 3px 10px;
  background: rgba(167, 139, 250, 0.1);
  border-radius: 6px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
  line-height: 1.6;
  margin: 0 0 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.25);
}

.card-read {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.3);
  transition: color 0.3s ease;
}

.read-arrow {
  transition: transform 0.3s ease;
}

.article-empty {
  text-align: center;
  padding: 60px 20px;

  .empty-icon {
    font-size: 48px;
    display: block;
    margin-bottom: 12px;
    opacity: 0.5;
  }

  p {
    color: rgba(255, 255, 255, 0.25);
    font-size: 14px;
    letter-spacing: 1px;
  }
}

@media (max-width: 768px) {
  .hero-title .title-char {
    font-size: 36px;
    letter-spacing: 4px;
  }

  .hero-subtitle {
    font-size: 13px;
    letter-spacing: 2px;
  }

  .tag-cloud {
    gap: 12px;
  }

  .tag-bubble {
    padding: 6px 16px;
  }

  .articles-grid {
    grid-template-columns: 1fr;
  }

  .article-section-header {
    gap: 12px;
  }

  .section-divider {
    width: 20px;
  }

  .section-title {
    font-size: 15px;
  }
}
</style>