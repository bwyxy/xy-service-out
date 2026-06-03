<script lang="ts" setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import useAppStore from '@/store/modules/app.ts'
import { tagLevelList } from '@/api/tag.ts'

const { blogInfo } = storeToRefs(useAppStore())
const canvasRef = ref<HTMLCanvasElement | null>(null)
const activeSection = ref('about')

interface Skill {
  name: string
  level: number
  color: string
}

interface Experience {
  company: string
  role: string
  period: string
  description: string[]
  color: string
}

interface Project {
  name: string
  description: string
  tags: string[]
  color: string
}

interface Education {
  school: string
  degree: string
  period: string
  description: string
}

const skills = ref<Skill[]>([])

const experiences: Experience[] = [
  {
    company: '某某科技有限公司',
    role: '高级前端工程师',
    period: '2023.03 - 至今',
    description: [
      '负责公司核心产品前端架构设计与开发',
      '带领 5 人前端团队完成多个重点项目',
      '推动组件库建设，提升团队开发效率 40%',
    ],
    color: '#4dabf7',
  },
  {
    company: '某互联网公司',
    role: '前端开发工程师',
    period: '2021.07 - 2023.02',
    description: [
      '参与电商平台前端开发，负责商品、订单等核心模块',
      '优化首屏加载性能，LCP 从 3.2s 降至 1.1s',
      '使用 Vue3 + TypeScript 重构旧项目',
    ],
    color: '#69db7c',
  },
  {
    company: '某初创企业',
    role: '全栈开发实习生',
    period: '2020.03 - 2021.06',
    description: [
      '独立完成公司官网及后台管理系统的前后端开发',
      '使用 Vue + Express + MongoDB 技术栈',
      '获得年度优秀实习生称号',
    ],
    color: '#ffa94d',
  },
]

const projects: Project[] = [
  {
    name: 'XY-Blog 个人博客',
    description: '基于 Vue3 + TypeScript + Pinia 构建的个人博客系统，支持文章发布、评论、归档、分类等功能',
    tags: ['Vue3', 'TypeScript', 'SCSS', 'Element Plus'],
    color: '#4dabf7',
  },
  {
    name: '组件库建设',
    description: '从零搭建公司内部 UI 组件库，包含 30+ 组件，支持按需加载和主题定制',
    tags: ['Vue3', 'Storybook', 'Rollup', 'Jest'],
    color: '#da77f2',
  },
  {
    name: '低代码平台',
    description: '参与低代码平台前端核心引擎开发，实现拖拽式页面搭建与实时预览',
    tags: ['React', 'TypeScript', 'MobX', 'Canvas'],
    color: '#69db7c',
  },
]

const educationList: Education[] = [
  {
    school: '某知名大学',
    degree: '计算机科学与技术 · 本科',
    period: '2016.09 - 2020.06',
    description: 'GPA 3.8/4.0，多次获得校级奖学金，参加 ACM 竞赛获省级银奖',
  },
]

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

  const particles: Array<{
    x: number; y: number; vx: number; vy: number; r: number; alpha: number; color: string
  }> = []

  const colors = ['#4dabf7', '#da77f2', '#69db7c', '#ffa94d', '#f783ac', '#63e6be']
  for (let i = 0; i < 50; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.3,
      vy: (Math.random() - 0.5) * 0.3,
      r: Math.random() * 1.5 + 0.5,
      alpha: Math.random() * 0.5 + 0.2,
      color: colors[Math.floor(Math.random() * colors.length)],
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    particles.forEach((p, i) => {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = p.color
      ctx.globalAlpha = p.alpha
      ctx.fill()

      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[j].x - p.x
        const dy = particles[j].y - p.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = p.color
          ctx.globalAlpha = 0.04 * (1 - dist / 120)
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    })
    ctx.globalAlpha = 1
    animFrameId = requestAnimationFrame(animate)
  }
  animate()
}

const scrollToSection = (id: string) => {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

/** 获取技能树 */
const getTagLevel = async () :Promise<any> => {
  try {
    const res = await tagLevelList()

    if (res.code === 200) {
      skills.value = res.data
    }
  } catch (err) {
    console.log(err)
  }
}

const getSkillGradient = (skill: Skill) => {
  return `linear-gradient(90deg, ${skill.color} 0%, ${skill.color}88 ${skill.level}%, transparent ${skill.level}%)`
}

onMounted(() => {
  initCanvas()

  getTagLevel()
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
})
</script>

<template>
  <div class="resume-page">
    <canvas ref="canvasRef" class="bg-canvas"></canvas>

    <div class="resume-hero">
      <div class="hero-avatar-wrapper">
        <div class="avatar-ring"></div>
        <img class="hero-avatar" src="@/assets/images/head.jpg" alt="avatar" />
      </div>
      <h1 class="hero-name">
        <span class="name-glitch" data-text="Mr.XiaoYi">Mr.XiaoYi</span>
      </h1>
      <p class="hero-title">{{ blogInfo.motto || '全栈开发者 / 开源爱好者 / 终身学习者' }}</p>
      <div class="hero-contact">
        <span class="contact-chip">
          <!-- <span class="chip-icon">{{ item.icon }}</span> -->
          {{ blogInfo.gitee || '' }}
        </span>
        <span class="contact-chip">
          <!-- <span class="chip-icon">{{ item.icon }}</span> -->
          {{ blogInfo.github || '' }}
        </span>
         <span class="contact-chip">
          <!-- <span class="chip-icon">{{ item.icon }}</span> -->
          {{ blogInfo.email || '' }}
        </span>
         <span class="contact-chip">
          <!-- <span class="chip-icon">{{ item.icon }}</span> -->
          {{ blogInfo.address || '' }}
        </span>
      </div>
      <div class="hero-nav">
        <button
          v-for="nav in [
            { id: 'about', label: '关于' },
            { id: 'skills', label: '技能' },
            { id: 'experience', label: '经历' },
            { id: 'projects', label: '项目' },
            { id: 'education', label: '教育' },
          ]"
          :key="nav.id"
          :class="['nav-btn', { active: activeSection === nav.id }]"
          @click="scrollToSection(nav.id)"
        >
          {{ nav.label }}
          <span class="nav-underline"></span>
        </button>
      </div>
      <div class="scroll-hint">
        <span></span><span></span><span></span>
      </div>
    </div>

    <div class="resume-body">
      <section id="about" class="resume-section about-section">
        <div class="section-header">
          <span class="section-number">01</span>
          <h2 class="section-title">关于我</h2>
          <div class="section-line"></div>
        </div>
        <div class="about-content">
          <p class="about-text">
            {{ blogInfo.about || '' }}
          </p>
          <!-- <p class="about-text">
            你好！我是一名热爱技术的前端开发者，拥有 <span class="highlight">{{ new Date().getFullYear() - 2020 }}</span> Web 开发经验。
            热衷于探索前沿技术，追求极致的用户体验和代码质量。
          </p>
          <p class="about-text">
            擅长 <span class="highlight">Vue 生态</span>和 <span class="highlight">TypeScript</span>，
            同时在 React、Node.js 等领域也有丰富的实战经验。
            相信技术的力量可以让世界变得更美好。
          </p> -->
          <div class="about-stats">
            <div class="stat-card">
              <span class="stat-num" style="color: #4dabf7">{{ new Date().getFullYear() - 2020 }}+</span>
              <span class="stat-label">开发经验</span>
            </div>
            <div class="stat-card">
              <span class="stat-num" style="color: #69db7c">30+</span>
              <span class="stat-label">完成项目</span>
            </div>
            <div class="stat-card">
              <span class="stat-num" style="color: #ffa94d">3+</span>
              <span class="stat-label">个人项目</span>
            </div>
            <div class="stat-card">
              <span class="stat-num" style="color: #da77f2">100+</span>
              <span class="stat-label">技术文章</span>
            </div>
            <!-- <div class="stat-card" v-for="(stat, i) in [
              { num: '5+', label: '年开发经验' },
              { num: '30+', label: '完成项目' },
              { num: '100+', label: '技术文章' },
              { num: '50K+', label: '开源 Stars' },
            ]" :key="i">
              <span class="stat-num" :style="{ color: ['#4dabf7', '#69db7c', '#da77f2', '#ffa94d'][i] }">
                {{ stat.num }}
              </span>
              <span class="stat-label">{{ stat.label }}</span>
            </div> -->
          </div>
        </div>
      </section>

      <section id="skills" class="resume-section skills-section">
        <div class="section-header">
          <span class="section-number">02</span>
          <h2 class="section-title">技能栈</h2>
          <div class="section-line"></div>
        </div>
        <div class="skills-grid">
          <div
            v-for="skill in skills"
            :key="skill.name"
            class="skill-item"
            :style="{ '--skill-color': skill.color }"
          >
            <div class="skill-header">
              <span class="skill-name">{{ skill.name }}</span>
              <span class="skill-level">{{ skill.level }}%</span>
            </div>
            <div class="skill-bar">
              <div
                class="skill-fill"
                :style="{ width: skill.level + '%', background: getSkillGradient(skill) }"
              ></div>
              <div class="skill-glow" :style="{ left: skill.level + '%', background: skill.color }"></div>
            </div>
          </div>
        </div>
      </section>

      <section id="experience" class="resume-section experience-section">
        <div class="section-header">
          <span class="section-number">03</span>
          <h2 class="section-title">工作经历</h2>
          <div class="section-line"></div>
        </div>
        <div class="experience-timeline">
          <div class="timeline-line"></div>
          <div
            v-for="(exp, i) in experiences"
            :key="i"
            class="exp-item"
            :style="{ '--exp-color': exp.color }"
          >
            <div class="exp-dot" :style="{ background: exp.color }">
              <div class="exp-dot-pulse"></div>
            </div>
            <div class="exp-card">
              <div class="exp-card-header">
                <div>
                  <h3 class="exp-role">{{ exp.role }}</h3>
                  <p class="exp-company">{{ exp.company }}</p>
                </div>
                <span class="exp-period">{{ exp.period }}</span>
              </div>
              <ul class="exp-desc">
                <li v-for="(desc, j) in exp.description" :key="j">{{ desc }}</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      <section id="projects" class="resume-section projects-section">
        <div class="section-header">
          <span class="section-number">04</span>
          <h2 class="section-title">项目经验</h2>
          <div class="section-line"></div>
        </div>
        <div class="projects-grid">
          <div
            v-for="project in projects"
            :key="project.name"
            class="project-card"
            :style="{ '--proj-color': project.color }"
          >
            <div class="project-card-header">
              <div class="project-icon" :style="{ background: project.color }">
                <span>{{ ['🖥️', '🧩', '⚡'][projects.indexOf(project)] }}</span>
              </div>
              <h3 class="project-name">{{ project.name }}</h3>
            </div>
            <p class="project-desc">{{ project.description }}</p>
            <div class="project-tags">
              <span v-for="tag in project.tags" :key="tag" class="project-tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </section>

      <section id="education" class="resume-section education-section">
        <div class="section-header">
          <span class="section-number">05</span>
          <h2 class="section-title">教育背景</h2>
          <div class="section-line"></div>
        </div>
        <div v-for="edu in educationList" :key="edu.school" class="edu-card">
          <div class="edu-icon">🎓</div>
          <div class="edu-info">
            <h3 class="edu-school">{{ edu.school }}</h3>
            <p class="edu-degree">{{ edu.degree }}</p>
            <p class="edu-desc">{{ edu.description }}</p>
          </div>
          <span class="edu-period">{{ edu.period }}</span>
        </div>
      </section>

      <div class="resume-footer">
        <p>— 感谢阅读，期待与你相遇 —</p>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.resume-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #06060e 0%, #0a0f1a 30%, #06060e 100%);
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

.resume-hero {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  box-sizing: border-box;
}

.hero-avatar-wrapper {
  position: relative;
  margin-bottom: 32px;
}

.avatar-ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 2px solid transparent;
  border-top-color: #4dabf7;
  border-right-color: #da77f2;
  animation: avatarSpin 3s linear infinite;
}

@keyframes avatarSpin {
  to { transform: rotate(360deg); }
}

.hero-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.1);
}

.hero-name {
  font-size: 48px;
  font-weight: 900;
  letter-spacing: 4px;
  margin-bottom: 12px;
}

.name-glitch {
  position: relative;
  color: #fff;

  &::before,
  &::after {
    content: attr(data-text);
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }

  &::before {
    color: #4dabf7;
    animation: glitchTop 2s infinite linear alternate-reverse;
    clip-path: polygon(0 0, 100% 0, 100% 35%, 0 35%);
  }

  &::after {
    color: #da77f2;
    animation: glitchBottom 3s infinite linear alternate-reverse;
    clip-path: polygon(0 65%, 100% 65%, 100% 100%, 0 100%);
  }
}

@keyframes glitchTop {
  0%, 100% { transform: translate(0); }
  20% { transform: translate(-2px, 1px); }
  40% { transform: translate(2px, -1px); }
  60% { transform: translate(-1px, -1px); }
  80% { transform: translate(1px, 1px); }
}

@keyframes glitchBottom {
  0%, 100% { transform: translate(0); }
  25% { transform: translate(1px, -1px); }
  50% { transform: translate(-1px, 1px); }
  75% { transform: translate(2px); }
}

.hero-title {
  font-size: 17px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 3px;
  margin-bottom: 32px;
  position: relative;

  &::after {
    content: '|';
    animation: cursorBlink 1s step-end infinite;
    color: #4dabf7;
    margin-left: 2px;
  }
}

@keyframes cursorBlink {
  50% { opacity: 0; }
}

.hero-contact {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-bottom: 48px;
}

.contact-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.5px;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(255, 255, 255, 0.2);
    background: rgba(255, 255, 255, 0.07);
    color: rgba(255, 255, 255, 0.9);
    transform: translateY(-2px);
  }
}

.chip-icon {
  font-size: 14px;
}

.hero-nav {
  display: flex;
  gap: 0;
  margin-bottom: 60px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 4px;
}

.nav-btn {
  position: relative;
  padding: 10px 24px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  letter-spacing: 1px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;

  &:hover {
    color: rgba(255, 255, 255, 0.8);
  }

  &.active {
    color: #fff;
    background: rgba(255, 255, 255, 0.08);
  }
}

.scroll-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  span {
    display: block;
    width: 6px;
    height: 6px;
    border-right: 1px solid rgba(255, 255, 255, 0.4);
    border-bottom: 1px solid rgba(255, 255, 255, 0.4);
    transform: rotate(45deg);
    animation: scrollDown 1.5s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.15s; }
    &:nth-child(3) { animation-delay: 0.3s; }
  }
}

@keyframes scrollDown {
  0%, 100% { opacity: 0.2; }
  50% { opacity: 0.8; }
}

.resume-body {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px 80px;
}

.resume-section {
  margin-bottom: 80px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 36px;
}

.section-number {
  font-size: 14px;
  font-weight: 700;
  color: rgba(77, 171, 247, 0.6);
  letter-spacing: 2px;
  font-family: 'Courier New', monospace;
}

.section-title {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 2px;
  color: #fff;
  margin: 0;
  flex-shrink: 0;
}

.section-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.15), transparent);
}

.about-content {
  padding-left: 8px;
}

.about-text {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.55);
  line-height: 2;
  letter-spacing: 0.5px;
  margin-bottom: 16px;

  .highlight {
    color: #4dabf7;
    font-weight: 600;
  }
}

.about-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 32px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 20px 16px;
  text-align: center;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(255, 255, 255, 0.15);
    transform: translateY(-3px);
    background: rgba(255, 255, 255, 0.05);
  }
}

.stat-num {
  display: block;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 1px;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1px;
}

.skills-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.skill-item {
  .skill-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  .skill-name {
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 1px;
    color: rgba(255, 255, 255, 0.75);
  }

  .skill-level {
    font-size: 13px;
    font-weight: 700;
    color: var(--skill-color);
    letter-spacing: 1px;
  }
}

.skill-bar {
  position: relative;
  height: 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  overflow: visible;
}

.skill-fill {
  height: 100%;
  border-radius: 4px;
  position: relative;
  animation: skillFill 1.5s cubic-bezier(0.16, 1, 0.3, 1) both;
  transform-origin: left;

  &::after {
    content: '';
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 16px;
    border-radius: 2px;
    background: var(--skill-color);
  }
}

@keyframes skillFill {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

.skill-glow {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: 0 0 10px currentColor, 0 0 20px currentColor;
  animation: skillGlow 2s ease-in-out infinite;
}

@keyframes skillGlow {
  0%, 100% { opacity: 0.6; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.5); }
}

.experience-timeline {
  position: relative;
  padding-left: 32px;
}

.timeline-line {
  position: absolute;
  left: 15px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
}

.exp-item {
  position: relative;
  margin-bottom: 40px;

  &:last-child {
    margin-bottom: 0;
  }
}

.exp-dot {
  position: absolute;
  left: -22px;
  top: 28px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  z-index: 2;
  box-shadow: 0 0 10px var(--exp-color), 0 0 20px var(--exp-color);
}

.exp-dot-pulse {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: inherit;
  opacity: 0.4;
  animation: dotPulse 2s ease-out infinite;
}

@keyframes dotPulse {
  0% { transform: scale(1); opacity: 0.4; }
  100% { transform: scale(2.5); opacity: 0; }
}

.exp-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 24px 28px;
  position: relative;
  transition: all 0.3s ease;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: var(--exp-color);
    border-radius: 3px 0 0 3px;
  }

  &:hover {
    border-color: rgba(255, 255, 255, 0.12);
    background: rgba(255, 255, 255, 0.05);
    transform: translateX(4px);
    box-shadow: 0 0 30px color-mix(in srgb, var(--exp-color) 10%, transparent);
  }
}

.exp-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.exp-role {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  margin-bottom: 4px;
}

.exp-company {
  font-size: 13px;
  color: var(--exp-color);
  letter-spacing: 0.5px;
}

.exp-period {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1px;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 12px;
  white-space: nowrap;
}

.exp-desc {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    position: relative;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.45);
    line-height: 1.8;
    letter-spacing: 0.5px;
    padding-left: 16px;
    margin-bottom: 6px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 10px;
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background: var(--exp-color);
      opacity: 0.6;
    }
  }
}

.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.project-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s ease;
  cursor: default;

  &:hover {
    border-color: color-mix(in srgb, var(--proj-color) 40%, transparent);
    background: rgba(255, 255, 255, 0.05);
    transform: translateY(-3px);
    box-shadow: 0 8px 30px color-mix(in srgb, var(--proj-color) 10%, transparent);
  }
}

.project-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.project-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.project-name {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  margin: 0;
}

.project-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  line-height: 1.8;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
}

.project-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.project-tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.5px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.edu-card {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 24px 28px;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(255, 255, 255, 0.12);
    background: rgba(255, 255, 255, 0.05);
  }
}

.edu-icon {
  font-size: 28px;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(77, 171, 247, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.edu-info {
  flex: 1;
}

.edu-school {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  margin-bottom: 4px;
}

.edu-degree {
  font-size: 13px;
  color: rgba(77, 171, 247, 0.7);
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.edu-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  line-height: 1.7;
}

.edu-period {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1px;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 12px;
  white-space: nowrap;
}

.resume-footer {
  text-align: center;
  padding: 60px 0 40px;

  p {
    font-size: 15px;
    color: rgba(255, 255, 255, 0.25);
    letter-spacing: 3px;
  }
}

@media (max-width: 768px) {
  .hero-name {
    font-size: 32px;
  }

  .hero-nav {
    flex-wrap: wrap;
    justify-content: center;
  }

  .nav-btn {
    padding: 8px 16px;
    font-size: 12px;
  }

  .about-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .projects-grid {
    grid-template-columns: 1fr;
  }

  .exp-card-header {
    flex-direction: column;
    gap: 8px;
  }

  .edu-card {
    flex-direction: column;
  }

  .edu-period {
    align-self: flex-start;
  }
}
</style>