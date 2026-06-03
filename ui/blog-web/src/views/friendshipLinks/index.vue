<script lang="ts" setup>
import { onMounted, onUnmounted, ref } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)

interface FriendLink {
  id: number
  name: string
  url: string
  avatar: string
  description: string
  tags: string[]
}

const friendLinks: FriendLink[] = [
  {
    id: 1,
    name: '阮一峰的网络日志',
    url: 'https://www.ruanyifeng.com',
    avatar: '📖',
    description: '科技爱好者周刊，记录分享前端技术与互联网见闻，内容深入浅出。',
    tags: ['前端', '科技', '周刊'],
  },
  {
    id: 2,
    name: '张鑫旭的博客',
    url: 'https://www.zhangxinxu.com',
    avatar: '🎨',
    description: 'CSS 领域深耕者，专注前端基础技术研究，著有大量高质量原创文章。',
    tags: ['CSS', '前端', '原创'],
  },
  {
    id: 3,
    name: '酷壳 CoolShell',
    url: 'https://coolshell.cn',
    avatar: '🛡️',
    description: '享受编程和技术带来的快乐，分享技术洞见与工程实践经验。',
    tags: ['编程', '架构', '思考'],
  },
  {
    id: 4,
    name: '美团技术团队',
    url: 'https://tech.meituan.com',
    avatar: '🍔',
    description: '美团技术团队官方博客，分享大规模分布式系统、算法与工程实践。',
    tags: ['后端', '分布式', '工程'],
  },
  {
    id: 5,
    name: '有赞技术团队',
    url: 'https://tech.youzan.com',
    avatar: '🛒',
    description: '有赞技术团队博客，涵盖微服务架构、中间件、大数据等技术领域。',
    tags: ['微服务', '架构', '中间件'],
  },
  {
    id: 6,
    name: 'A List Apart',
    url: 'https://alistapart.com',
    avatar: '📝',
    description: '探索网页设计、开发与内容策略的前沿思考，行业经典博客。',
    tags: ['设计', '前端', '内容'],
  },
  {
    id: 7,
    name: 'CSS-Tricks',
    url: 'https://css-tricks.com',
    avatar: '✨',
    description: 'CSS 技巧与前端开发教程的宝藏站点，从入门到精通。',
    tags: ['CSS', '教程', '前端'],
  },
  {
    id: 8,
    name: 'Smashing Magazine',
    url: 'https://www.smashingmagazine.com',
    avatar: '💥',
    description: '网页设计与开发领域的权威杂志，高质量教程与资源。',
    tags: ['设计', 'UX', '前端'],
  },
]

const avatarGradients = [
  ['#4dabf7', '#1c7ed6'],
  ['#da77f2', '#9c36b5'],
  ['#69db7c', '#2f9e44'],
  ['#ffa94d', '#e67700'],
  ['#f783ac', '#d6336c'],
  ['#63e6be', '#20c997'],
  ['#748ffc', '#4263eb'],
  ['#ff8787', '#e03131'],
]

const getAvatarGradient = (id: number) => {
  const [from, to] = avatarGradients[(id - 1) % avatarGradients.length]
  return `linear-gradient(135deg, ${from} 0%, ${to} 100%)`
}

const getGlowColor = (id: number) => {
  return avatarGradients[(id - 1) % avatarGradients.length][0]
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

  const stars: Array<{ x: number; y: number; r: number; twinkle: number; twinkleSpeed: number; color: string }> = []
  const colors = ['#4dabf7', '#da77f2', '#69db7c', '#ffa94d', '#f783ac', '#fff']

  for (let i = 0; i < 120; i++) {
    stars.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      r: Math.random() * 1.5 + 0.3,
      twinkle: Math.random() * Math.PI * 2,
      twinkleSpeed: Math.random() * 0.02 + 0.005,
      color: colors[Math.floor(Math.random() * colors.length)],
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    stars.forEach((s) => {
      s.twinkle += s.twinkleSpeed
      const alpha = 0.3 + Math.sin(s.twinkle) * 0.4

      ctx.beginPath()
      ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx.fillStyle = s.color
      ctx.globalAlpha = alpha
      ctx.fill()

      if (s.r > 1 && alpha > 0.55) {
        ctx.beginPath()
        ctx.arc(s.x, s.y, s.r * 2.5, 0, Math.PI * 2)
        ctx.fillStyle = s.color
        ctx.globalAlpha = alpha * 0.08
        ctx.fill()
      }
    })
    ctx.globalAlpha = 1
    animFrameId = requestAnimationFrame(animate)
  }
  animate()
}

const visitLink = (url: string) => {
  window.open(url, '_blank')
}

onMounted(() => {
  initCanvas()
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
})
</script>

<template>
  <div class="friend-page">
    <canvas ref="canvasRef" class="bg-canvas"></canvas>

    <div class="friend-hero">
      <div class="hero-constellation">
        <span class="constellation-star" v-for="i in 8" :key="i"
              :style="{
                left: (15 + (i - 1) * 10 + Math.sin(i * 1.2) * 4) + '%',
                top: (20 + Math.cos(i * 0.9) * 25) + '%',
                animationDelay: i * 0.15 + 's',
                width: (i % 3 === 0 ? 4 : 2) + 'px',
                height: (i % 3 === 0 ? 4 : 2) + 'px',
              }">
        </span>
      </div>
      <div class="hero-badge">
        <span class="badge-dot"></span>
        FRIENDSHIP
      </div>
      <h1 class="hero-title">
        <span class="title-char" v-for="(char, i) in '友情链接'.split('')" :key="i"
              :style="{ animationDelay: i * 0.1 + 's' }">{{ char }}</span>
      </h1>
      <p class="hero-subtitle">互联网因链接而互联，因分享而美好</p>
      <div class="hero-stats">
        <div class="stat-item">
          <span class="stat-num">{{ friendLinks.length }}</span>
          <span class="stat-label">位好友</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-num">∞</span>
          <span class="stat-label">无限可能</span>
        </div>
      </div>
      <div class="hero-scroll">
        <div class="scroll-orbit">
          <span class="orbit-dot"></span>
        </div>
        <span>向下探索星链</span>
      </div>
    </div>

    <div class="friend-body">
      <div class="links-grid">
        <div
          v-for="link in friendLinks"
          :key="link.id"
          class="link-card"
          :style="{ '--glow': getGlowColor(link.id) }"
          @click="visitLink(link.url)"
        >
          <div class="link-card-avatar" :style="{ background: getAvatarGradient(link.id) }">
            <span class="link-avatar-text">{{ link.avatar }}</span>
            <div class="link-card-orbital-ring"></div>
          </div>
          <div class="link-card-info">
            <h3 class="link-name">{{ link.name }}</h3>
            <p class="link-desc">{{ link.description }}</p>
            <div class="link-tags">
              <span v-for="tag in link.tags" :key="tag" class="link-tag">{{ tag }}</span>
            </div>
          </div>
          <div class="link-card-arrow">
            <span>→</span>
          </div>
        </div>
      </div>

      <div class="apply-section">
        <div class="apply-card">
          <div class="apply-icon">🤝</div>
          <h3 class="apply-title">申请友链</h3>
          <p class="apply-desc">
            如果您的博客内容优质、保持更新，欢迎与我交换友链。
            添加本站链接后，通过以下方式联系我即可。
          </p>
          <div class="apply-info-grid">
            <div class="apply-info-item">
              <span class="apply-label">站点名称</span>
              <span class="apply-value">Mr.XiaoYi's Blog</span>
            </div>
            <div class="apply-info-item">
              <span class="apply-label">站点地址</span>
              <span class="apply-value">https://xiaoyi.dev</span>
            </div>
            <div class="apply-info-item">
              <span class="apply-label">站点描述</span>
              <span class="apply-value">记录技术与生活的个人博客</span>
            </div>
            <div class="apply-info-item">
              <span class="apply-label">联系邮箱</span>
              <span class="apply-value">xiaoyi@example.com</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.friend-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #05060f 0%, #090d1a 30%, #05060f 100%);
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

.friend-hero {
  position: relative;
  z-index: 1;
  min-height: 65vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px 40px;
  box-sizing: border-box;
}

.hero-constellation {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.constellation-star {
  position: absolute;
  border-radius: 50%;
  background: #4dabf7;
  box-shadow: 0 0 6px #4dabf7, 0 0 12px rgba(77, 171, 247, 0.5);
  animation: starPulse 3s ease-in-out infinite;
}

@keyframes starPulse {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.6); }
}

.hero-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 5px;
  color: rgba(77, 171, 247, 0.65);
  padding: 7px 18px;
  border: 1px solid rgba(77, 171, 247, 0.18);
  border-radius: 20px;
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4dabf7;
  box-shadow: 0 0 8px #4dabf7;
  animation: badgeDotBlink 2s ease-in-out infinite;
}

@keyframes badgeDotBlink {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.hero-title {
  display: flex;
  gap: 6px;
  margin-bottom: 18px;
  position: relative;
  z-index: 1;
}

.title-char {
  font-size: 56px;
  font-weight: 900;
  letter-spacing: 6px;
  color: #fff;
  animation: charOrbit 4s ease-in-out infinite;
  text-shadow: 0 0 30px rgba(77, 171, 247, 0.2);
}

@keyframes charOrbit {
  0%, 100% { transform: translateY(0) rotateX(0); }
  25% { transform: translateY(-6px) rotateX(5deg); }
  75% { transform: translateY(4px) rotateX(-3deg); }
}

.hero-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 4px;
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 50px;
  position: relative;
  z-index: 1;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #4dabf7, #da77f2);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 2px;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.08);
}

.hero-scroll {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 1;

  span {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.18);
    letter-spacing: 3px;
  }
}

.scroll-orbit {
  width: 24px;
  height: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: orbitSpin 4s linear infinite;
}

.orbit-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(77, 171, 247, 0.6);
  position: absolute;
  top: -2px;
}

@keyframes orbitSpin {
  to { transform: rotate(360deg); }
}

.friend-body {
  position: relative;
  z-index: 1;
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 24px 80px;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(440px, 1fr));
  gap: 16px;
}

.link-card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 18px;
  padding: 20px 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;

  &:hover {
    border-color: color-mix(in srgb, var(--glow) 40%, transparent);
    background: rgba(255, 255, 255, 0.04);
    transform: translateY(-3px);
    box-shadow: 0 0 30px color-mix(in srgb, var(--glow) 12%, transparent),
                0 8px 30px rgba(0, 0, 0, 0.3);

    .link-card-orbital-ring {
      opacity: 1;
      transform: rotate(180deg);
    }

    .link-card-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }
}

.link-card-avatar {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.link-avatar-text {
  font-size: 24px;
  z-index: 1;
}

.link-card-orbital-ring {
  position: absolute;
  inset: -3px;
  border-radius: 19px;
  border: 1px solid var(--glow);
  opacity: 0;
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.link-card-info {
  flex: 1;
  min-width: 0;
}

.link-name {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.link-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  line-height: 1.6;
  letter-spacing: 0.3px;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.link-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.link-tag {
  font-size: 10px;
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 0.3px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.link-card-arrow {
  font-size: 18px;
  color: var(--glow);
  opacity: 0;
  transform: translateX(-8px);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  flex-shrink: 0;
}

.apply-section {
  margin-top: 60px;
}

.apply-card {
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  padding: 36px 32px;
  text-align: center;
  transition: border-color 0.3s ease;

  &:hover {
    border-color: rgba(255, 255, 255, 0.1);
  }
}

.apply-icon {
  font-size: 36px;
  margin-bottom: 16px;
}

.apply-title {
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 12px;
}

.apply-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
  line-height: 1.8;
  letter-spacing: 0.5px;
  max-width: 480px;
  margin: 0 auto 28px;
}

.apply-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 500px;
  margin: 0 auto;
  text-align: left;
}

.apply-info-item {
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 12px 16px;
}

.apply-label {
  display: block;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.25);
  letter-spacing: 1px;
  margin-bottom: 4px;
  text-transform: uppercase;
}

.apply-value {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 0.3px;
  word-break: break-all;
}

@media (max-width: 768px) {
  .hero-title {
    .title-char {
      font-size: 38px;
      letter-spacing: 3px;
    }
  }

  .hero-subtitle {
    font-size: 13px;
    letter-spacing: 2px;
  }

  .links-grid {
    grid-template-columns: 1fr;
  }

  .link-card {
    padding: 16px 18px;
    gap: 14px;
  }

  .link-card-avatar {
    width: 46px;
    height: 46px;
    border-radius: 14px;
  }

  .link-avatar-text {
    font-size: 20px;
  }

  .apply-info-grid {
    grid-template-columns: 1fr;
  }

  .apply-card {
    padding: 28px 20px;
  }
}
</style>