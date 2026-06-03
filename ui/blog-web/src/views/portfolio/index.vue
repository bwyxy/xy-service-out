<script lang="ts" setup>
import { onMounted, onUnmounted, ref, computed } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)
const activeCategory = ref('all')
const selectedWork = ref<Work | null>(null)
const observerRefs = ref<Map<number, HTMLElement>>(new Map())

interface Work {
  id: number
  title: string
  category: string
  description: string
  cover: string
  tags: string[]
  link?: string
  detail: string
}

const categories = [
  { key: 'all', label: '全部作品', icon: '🌟' },
  { key: 'web', label: 'Web 应用', icon: '🌐' },
  { key: 'mobile', label: '移动端', icon: '📱' },
  { key: 'design', label: 'UI 设计', icon: '🎨' },
  { key: 'opensource', label: '开源项目', icon: '💎' },
]

const works: Work[] = [
  {
    id: 1,
    title: 'XY-Blog 博客系统',
    category: 'web',
    description: '基于 Vue3 + TypeScript 构建的现代化个人博客，支持文章管理、评论、归档、分类等功能。',
    cover: '',
    tags: ['Vue3', 'TypeScript', 'SCSS', 'Pinia'],
    link: 'https://github.com/xiaoyi/xy-blog',
    detail: '一套完整的个人博客解决方案，包含文章发布系统、评论系统、分类归档、个人简历等模块。采用 Pinia 进行状态管理，Element Plus 作为 UI 组件库，支持暗色主题切换。',
  },
  {
    id: 2,
    title: 'E-Commerce 电商平台',
    category: 'web',
    description: '全栈电商平台，包含商品展示、购物车、订单管理、支付集成等完整交易链路。',
    cover: '',
    tags: ['React', 'Next.js', 'Node.js', 'MongoDB'],
    link: '',
    detail: '大型全栈电商项目，支持商品检索与筛选、购物车实时同步、订单状态追踪、支付宝/微信支付集成。采用 Next.js SSR 优化首屏性能，Redis 缓存热点数据。',
  },
  {
    id: 3,
    title: '低代码可视化搭建平台',
    category: 'web',
    description: '拖拽式页面搭建引擎，支持组件自由布局、实时预览与一键发布。',
    cover: '',
    tags: ['React', 'TypeScript', 'Canvas', 'WebSocket'],
    link: '',
    detail: '面向运营人员的低代码平台，通过拖拽组件快速搭建活动页面。自研画布渲染引擎，支持组件嵌套、样式编辑、数据绑定。WebSocket 实现多人协同编辑。',
  },
  {
    id: 4,
    title: 'Fitness Tracker 运动助手',
    category: 'mobile',
    description: '跨平台运动记录 App，支持 GPS 轨迹追踪、训练计划制定与数据分析。',
    cover: '',
    tags: ['Flutter', 'Dart', 'Firebase', 'Google Maps'],
    link: '',
    detail: '基于 Flutter 开发的跨平台健身应用，支持 GPS 跑步轨迹记录、心率监测、训练计划定制、运动数据可视化分析。使用 Firebase 实现实时数据同步。',
  },
  {
    id: 5,
    title: 'Component Library',
    category: 'opensource',
    description: '企业级 Vue3 组件库，30+ 高质量组件，支持主题定制与按需加载。',
    cover: '',
    tags: ['Vue3', 'TypeScript', 'Rollup', 'Storybook'],
    link: 'https://github.com/xiaoyi/component-lib',
    detail: '从零构建的企业级 UI 组件库，涵盖表单、数据展示、导航、反馈等类别。支持 Tree Shaking 按需加载，完善的 TypeScript 类型定义，Storybook 交互文档。',
  },
  {
    id: 6,
    title: 'CLI 脚手架工具',
    category: 'opensource',
    description: '一键生成前端项目的命令行工具，内置多种项目模板与最佳实践配置。',
    cover: '',
    tags: ['Node.js', 'Commander', 'Inquirer', 'EJS'],
    link: 'https://github.com/xiaoyi/cli-tool',
    detail: '前端项目脚手架工具，提供 Vue/React 多套模板选择。集成 ESLint、Prettier、Husky 等工程化配置，支持自定义模板扩展。NPM 周下载量 2000+。',
  },
  {
    id: 7,
    title: 'Dashboard 数据分析面板',
    category: 'design',
    description: '企业级数据可视化仪表盘，多维度数据图表展示与实时监控。',
    cover: '',
    tags: ['Figma', 'ECharts', 'Vue3', 'WebSocket'],
    link: '',
    detail: '为企业客户定制的数据监控仪表盘，包含折线图、柱状图、热力图等 20+ 种图表类型。支持数据大屏投放、实时告警推送、报表导出等功能。',
  },
  {
    id: 8,
    title: 'AI Chat Assistant',
    category: 'web',
    description: '基于大语言模型的智能对话助手，支持多轮对话、上下文记忆与流式输出。',
    cover: '',
    tags: ['Vue3', 'TypeScript', 'SSE', 'Markdown'],
    link: '',
    detail: '集成 OpenAI API 的智能对话应用，支持 Markdown 渲染、代码高亮、会话管理。采用 SSE 实现流式响应，打字机效果展示 AI 回复，支持多模型切换。',
  },
  {
    id: 9,
    title: 'Social Media App',
    category: 'mobile',
    description: '社交动态分享应用，支持图文发布、点赞评论与实时消息推送。',
    cover: '',
    tags: ['React Native', 'TypeScript', 'GraphQL', 'AWS'],
    link: '',
    detail: '跨平台社交应用，支持图文/视频动态发布、好友关注、私信聊天。使用 GraphQL 优化数据请求，AWS S3 存储媒体资源，FCM 实现实时推送通知。',
  },
]

const coverGradients = [
  ['#4dabf7', '#1c7ed6'],
  ['#da77f2', '#9c36b5'],
  ['#69db7c', '#2f9e44'],
  ['#ffa94d', '#e67700'],
  ['#f783ac', '#d6336c'],
  ['#63e6be', '#20c997'],
  ['#748ffc', '#4263eb'],
  ['#ff8787', '#e03131'],
  ['#fab005', '#f08c00'],
]

const categoryIcons: Record<string, string> = {
  web: '🌐',
  mobile: '📱',
  design: '🎨',
  opensource: '💎',
}

const filteredWorks = computed(() => {
  if (activeCategory.value === 'all') return works
  return works.filter((w) => w.category === activeCategory.value)
})

const changeCategory = (key: string) => {
  activeCategory.value = key
}

const openDetail = (work: Work) => {
  selectedWork.value = work
  document.body.style.overflow = 'hidden'
}

const closeDetail = () => {
  selectedWork.value = null
  document.body.style.overflow = ''
}

const getCoverGradient = (id: number) => {
  const [from, to] = coverGradients[(id - 1) % coverGradients.length]
  return `linear-gradient(135deg, ${from} 0%, ${to} 100%)`
}

const getCoverEmoji = (index: number) => {
  return ['🖥️', '🛒', '🧩', '🏃', '📦', '⚡', '📊', '🤖', '💬'][index % 9]
}

let animFrameId = 0

const initCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const shapes: Array<{
    x: number; y: number; size: number; speed: number; opacity: number
    color: string; rotation: number; rotationSpeed: number; type: number
  }> = []

  const colors = ['rgba(77,171,247,0.12)', 'rgba(218,119,242,0.12)', 'rgba(105,219,124,0.12)', 'rgba(255,169,77,0.12)']
  for (let i = 0; i < 15; i++) {
    shapes.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      size: Math.random() * 60 + 30,
      speed: Math.random() * 0.3 + 0.1,
      opacity: Math.random() * 0.6 + 0.2,
      color: colors[i % colors.length],
      rotation: Math.random() * 360,
      rotationSpeed: (Math.random() - 0.5) * 0.3,
      type: Math.floor(Math.random() * 3),
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    shapes.forEach((s) => {
      s.y -= s.speed
      s.rotation += s.rotationSpeed
      if (s.y + s.size < 0) {
        s.y = canvas.height + s.size
        s.x = Math.random() * canvas.width
      }

      ctx.save()
      ctx.globalAlpha = s.opacity
      ctx.translate(s.x, s.y)
      ctx.rotate((s.rotation * Math.PI) / 180)
      ctx.fillStyle = s.color

      if (s.type === 0) {
        ctx.fillRect(-s.size / 2, -s.size / 2, s.size, s.size)
      } else if (s.type === 1) {
        ctx.beginPath()
        ctx.arc(0, 0, s.size / 2, 0, Math.PI * 2)
        ctx.fill()
      } else {
        ctx.beginPath()
        for (let j = 0; j < 3; j++) {
          const angle = (j * 2 * Math.PI) / 3 - Math.PI / 2
          const px = Math.cos(angle) * s.size / 2
          const py = Math.sin(angle) * s.size / 2
          if (j === 0) ctx.moveTo(px, py)
          else ctx.lineTo(px, py)
        }
        ctx.closePath()
        ctx.fill()
      }

      ctx.restore()
    })
    animFrameId = requestAnimationFrame(animate)
  }
  animate()
}

onMounted(() => {
  initCanvas()
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
  document.body.style.overflow = ''
})
</script>

<template>
  <div class="portfolio-page">
    <canvas ref="canvasRef" class="bg-canvas"></canvas>

    <div class="portfolio-hero">
      <div class="hero-badge">PORTFOLIO</div>
      <h1 class="hero-title">
        <span class="title-word" v-for="(char, i) in '作品集'.split('')" :key="i"
              :style="{ animationDelay: i * 0.08 + 's' }">{{ char }}</span>
      </h1>
      <p class="hero-subtitle">每一个项目，都是一次探索与创造的旅程</p>
      <div class="hero-scroll">
        <div class="scroll-line"></div>
        <span>向下滚动探索</span>
      </div>
    </div>

    <div class="portfolio-body">
      <div class="filter-bar">
        <button
          v-for="cat in categories"
          :key="cat.key"
          :class="['filter-btn', { active: activeCategory === cat.key }]"
          @click="changeCategory(cat.key)"
        >
          <span class="filter-icon">{{ cat.icon }}</span>
          <span class="filter-label">{{ cat.label }}</span>
        </button>
      </div>

      <TransitionGroup name="works-grid" tag="div" class="works-grid">
        <div
          v-for="work in filteredWorks"
          :key="work.id"
          class="work-card"
          :ref="(el) => { if (el) observerRefs.set(work.id, el as HTMLElement) }"
          @click="openDetail(work)"
        >
          <div class="work-cover" :style="{ background: getCoverGradient(work.id) }">
            <span class="work-cover-emoji">{{ getCoverEmoji(work.id - 1) }}</span>
            <div class="work-cover-overlay">
              <span class="overlay-text">查看详情</span>
            </div>
            <span class="work-category-badge">{{ categoryIcons[work.category] }} {{ categories.find(c => c.key === work.category)?.label }}</span>
          </div>
          <div class="work-info">
            <h3 class="work-title">{{ work.title }}</h3>
            <p class="work-desc">{{ work.description }}</p>
            <div class="work-tags">
              <span v-for="tag in work.tags.slice(0, 3)" :key="tag" class="work-tag">{{ tag }}</span>
              <span v-if="work.tags.length > 3" class="work-tag more-tag">+{{ work.tags.length - 3 }}</span>
            </div>
          </div>
        </div>
      </TransitionGroup>

      <Transition name="modal">
        <div v-if="selectedWork" class="detail-overlay" @click.self="closeDetail">
          <div class="detail-modal">
            <button class="detail-close" @click="closeDetail">
              <span></span><span></span>
            </button>
            <div class="detail-cover" :style="{ background: getCoverGradient(selectedWork.id) }">
              <span class="detail-cover-emoji">{{ getCoverEmoji(selectedWork.id - 1) }}</span>
            </div>
            <div class="detail-body">
              <div class="detail-header">
                <h2 class="detail-title">{{ selectedWork.title }}</h2>
                <span class="detail-category">
                  {{ categoryIcons[selectedWork!.category] }}
                  {{ categories.find(c => c.key === selectedWork!.category)?.label }}
                </span>
              </div>
              <p class="detail-desc">{{ selectedWork.detail }}</p>
              <div class="detail-tags">
                <span v-for="tag in selectedWork.tags" :key="tag" class="detail-tag">{{ tag }}</span>
              </div>
              <a
                v-if="selectedWork.link"
                :href="selectedWork.link"
                target="_blank"
                class="detail-link"
              >
                访问项目 →
              </a>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.portfolio-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #06060e 0%, #0a1020 30%, #06060e 100%);
  color: #e0e0e0;
  overflow-x: hidden;
}

.bg-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.portfolio-hero {
  position: relative;
  z-index: 1;
  min-height: 70vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px 60px;
  box-sizing: border-box;
}

.hero-badge {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 6px;
  color: rgba(77, 171, 247, 0.7);
  padding: 8px 20px;
  border: 1px solid rgba(77, 171, 247, 0.2);
  border-radius: 24px;
  margin-bottom: 28px;
}

.hero-title {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.title-word {
  display: inline-block;
  font-size: 64px;
  font-weight: 900;
  letter-spacing: 8px;
  color: #fff;
  animation: titleFloat 3s ease-in-out infinite;
  text-shadow: 0 0 40px rgba(77, 171, 247, 0.3);
}

@keyframes titleFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 4px;
  margin-bottom: 60px;
}

.hero-scroll {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;

  span {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.2);
    letter-spacing: 3px;
  }
}

.scroll-line {
  width: 1px;
  height: 40px;
  background: linear-gradient(180deg, rgba(77, 171, 247, 0.6), transparent);
  animation: scrollLine 2s ease-in-out infinite;
}

@keyframes scrollLine {
  0%, 100% { height: 30px; opacity: 0.5; }
  50% { height: 50px; opacity: 1; }
}

.portfolio-body {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px 80px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-bottom: 48px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 22px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.45);
  font-size: 14px;
  letter-spacing: 1px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;

  &:hover {
    color: rgba(255, 255, 255, 0.7);
    background: rgba(255, 255, 255, 0.04);
  }

  &.active {
    color: #fff;
    background: rgba(77, 171, 247, 0.15);
    box-shadow: 0 0 20px rgba(77, 171, 247, 0.1);

    .filter-icon {
      transform: scale(1.2);
    }
  }
}

.filter-icon {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.works-grid-enter-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.works-grid-leave-active {
  transition: all 0.3s ease;
  position: absolute;
}

.works-grid-enter-from {
  opacity: 0;
  transform: translateY(30px) scale(0.95);
}

.works-grid-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.works-grid-move {
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.work-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;

  &:hover {
    transform: translateY(-6px);
    border-color: rgba(255, 255, 255, 0.15);
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.4);

    .work-cover-emoji {
      transform: scale(1.15);
    }

    .work-cover-overlay {
      opacity: 1;
    }
  }
}

.work-cover {
  position: relative;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.work-cover-emoji {
  font-size: 56px;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.3));
}

.work-cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.4s ease;
  backdrop-filter: blur(4px);
}

.overlay-text {
  font-size: 14px;
  color: #fff;
  letter-spacing: 2px;
  padding: 8px 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
}

.work-category-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  font-size: 11px;
  padding: 5px 12px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.35);
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.5px;
  backdrop-filter: blur(8px);
}

.work-info {
  padding: 20px 24px 24px;
}

.work-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.work-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  line-height: 1.7;
  letter-spacing: 0.3px;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.work-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.work-tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.3px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.more-tag {
  color: rgba(77, 171, 247, 0.6);
  border-color: rgba(77, 171, 247, 0.15);
}

.detail-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  box-sizing: border-box;
}

.modal-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);

  .detail-modal {
    transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  }
}

.modal-leave-active {
  transition: all 0.25s ease;

  .detail-modal {
    transition: all 0.25s ease;
  }
}

.modal-enter-from {
  opacity: 0;

  .detail-modal {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
}

.modal-leave-to {
  opacity: 0;

  .detail-modal {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
  }
}

.detail-modal {
  background: #111827;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  max-width: 640px;
  width: 100%;
  max-height: 85vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.6);

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
  }
}

.detail-close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(255, 255, 255, 0.3);
    background: rgba(255, 255, 255, 0.1);
  }

  span {
    position: absolute;
    width: 16px;
    height: 1px;
    background: rgba(255, 255, 255, 0.7);

    &:first-child { transform: rotate(45deg); }
    &:last-child { transform: rotate(-45deg); }
  }
}

.detail-cover {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px 24px 0 0;
}

.detail-cover-emoji {
  font-size: 64px;
  filter: drop-shadow(0 4px 16px rgba(0, 0, 0, 0.4));
}

.detail-body {
  padding: 28px 32px 32px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.detail-title {
  font-size: 22px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 1px;
  margin: 0;
}

.detail-category {
  font-size: 12px;
  padding: 5px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.detail-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.9;
  letter-spacing: 0.3px;
  margin-bottom: 24px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.detail-tag {
  font-size: 12px;
  padding: 5px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.3px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #4dabf7;
  letter-spacing: 1px;
  text-decoration: none;
  padding: 10px 24px;
  border: 1px solid rgba(77, 171, 247, 0.3);
  border-radius: 12px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(77, 171, 247, 0.1);
    border-color: rgba(77, 171, 247, 0.5);
  }
}

@media (max-width: 768px) {
  .hero-title {
    .title-word {
      font-size: 42px;
      letter-spacing: 4px;
    }
  }

  .hero-subtitle {
    font-size: 13px;
    letter-spacing: 2px;
  }

  .filter-bar {
    gap: 4px;
  }

  .filter-btn {
    padding: 10px 14px;
    font-size: 12px;
    gap: 4px;
  }

  .filter-label {
    display: none;
  }

  .works-grid {
    grid-template-columns: 1fr;
  }

  .detail-overlay {
    padding: 16px;
    align-items: flex-end;
  }

  .detail-modal {
    border-radius: 24px 24px 0 0;
    max-height: 80vh;
  }

  .detail-body {
    padding: 20px 24px 24px;
  }

  .detail-header {
    flex-direction: column;
  }
}
</style>