<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import WindowControls from '@/components/WindowControls/index.vue'
import { isTauriLoginWindow } from '@/utils/tauri.ts'
import { agreementDocs, getAgreementDoc, type AgreementDocKey } from './content.ts'

const route = useRoute()
const router = useRouter()

/** 桌面登录小窗口：窗口无系统标题栏，页面需自带拖拽区与关闭按钮 */
const isDesktopLogin = isTauriLoginWindow()

/** 当前展示的文档（支持通过 ?type=user|privacy 直达） */
const typeParam = Array.isArray(route.query.type) ? route.query.type[0] : route.query.type
const docKey = ref<AgreementDocKey>(getAgreementDoc(typeParam).key)
const doc = computed(() => getAgreementDoc(docKey.value))

/** 滚动容器与当前高亮章节 */
const scroller = ref<HTMLElement>()
const activeId = ref('')

/** 右侧章节导航仅在宽屏展示，窄窗口（桌面登录小窗口）用顶部文档切换即可 */
const showAnchorNav = ref(false)
const syncAnchorNavVisible = () => {
  showAnchorNav.value = window.innerWidth >= 768
}

const onScroll = () => {
  const el = scroller.value
  if (!el) return
  const containerTop = el.getBoundingClientRect().top
  let current = doc.value.sections[0]?.id ?? ''
  for (const section of doc.value.sections) {
    const node = document.getElementById(`sec-${section.id}`)
    if (!node) continue
    if (node.getBoundingClientRect().top - containerTop <= 24) current = section.id
    else break
  }
  activeId.value = current
}

const switchDoc = (key: AgreementDocKey) => {
  if (docKey.value === key) return
  docKey.value = key
  scroller.value?.scrollTo({ top: 0 })
  activeId.value = doc.value.sections[0]?.id ?? ''
}

const scrollToSection = (id: string) => {
  document.getElementById(`sec-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/login')
  }
}

onMounted(() => {
  syncAnchorNavVisible()
  window.addEventListener('resize', syncAnchorNavVisible)
  activeId.value = doc.value.sections[0]?.id ?? ''
  nextTick(onScroll)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncAnchorNavVisible)
})
</script>

<template>
  <div :class="['agreement-page', isDesktopLogin ? 'agreement-page--desktop' : '']">
    <!-- 顶部栏：桌面登录小窗口依赖它拖动窗口 -->
    <div class="agreement-header" data-tauri-drag-region>
      <div class="header-back" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </div>
      <div class="header-title">{{ doc.title }}</div>
      <WindowControls v-if="isDesktopLogin" :maximizable="false" />
    </div>

    <!-- 文档切换 -->
    <div class="agreement-tabs">
      <div
          v-for="item in agreementDocs"
          :key="item.key"
          :class="['tab-item', docKey === item.key ? 'tab-item--active' : '']"
          @click="switchDoc(item.key)"
      >
        {{ item.title }}
      </div>
    </div>

    <div class="agreement-body">
      <!-- 章节导航（宽屏） -->
      <aside v-if="showAnchorNav" class="agreement-nav">
        <div
            v-for="section in doc.sections"
            :key="section.id"
            :class="['nav-item', activeId === section.id ? 'nav-item--active' : '']"
            @click="scrollToSection(section.id)"
        >
          {{ section.title }}
        </div>
      </aside>

      <!-- 正文 -->
      <div ref="scroller" class="agreement-content" @scroll="onScroll">
        <div class="content-inner">
          <p class="content-meta">更新时间：{{ doc.updatedAt }}</p>
          <section
              v-for="section in doc.sections"
              :id="`sec-${section.id}`"
              :key="section.id"
              class="content-section"
          >
            <h3 class="section-title">{{ section.title }}</h3>
            <p v-for="(text, index) in section.paragraphs" :key="index" class="section-text">
              {{ text }}
            </p>
          </section>
          <p class="content-tail">—— 本文档结束 ——</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
/**
 * 说明：本页不使用 size()。size() 基于 --vw（=100vw/1920，窄屏回退到 100vw/768）缩放，
 * 桌面登录小窗口仅 400px 宽时会被压到 0.5 倍左右，协议正文将难以阅读。
 * 协议属于阅读型长文，统一用固定 px 保证各窗口下字号一致。
 */
.agreement-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  box-sizing: border-box;
  overflow: hidden;
}

.agreement-header {
  flex-shrink: 0;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;

  .header-back {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    color: #576b95;
    cursor: pointer;
    user-select: none;

    &:hover {
      opacity: 0.8;
    }
  }

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}

.agreement-tabs {
  flex-shrink: 0;
  display: flex;
  gap: 24px;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;

  .tab-item {
    position: relative;
    padding: 12px 0;
    font-size: 14px;
    color: #888;
    cursor: pointer;
    user-select: none;

    &:hover {
      color: #07c160;
    }

    &--active {
      color: #07c160;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        left: 0;
        right: 0;
        bottom: -1px;
        height: 2px;
        border-radius: 1px;
        background: #07c160;
      }
    }
  }
}

.agreement-body {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: center;
}

.agreement-nav {
  flex-shrink: 0;
  width: 180px;
  padding: 20px 8px 20px 16px;
  overflow-y: auto;
  border-right: 1px solid #f5f5f5;
  box-sizing: border-box;

  .nav-item {
    padding: 6px 8px;
    font-size: 13px;
    color: #666;
    line-height: 1.4;
    border-radius: 4px;
    cursor: pointer;

    &:hover {
      color: #07c160;
      background: #f7f7f7;
    }

    &--active {
      color: #07c160;
      font-weight: 600;
      background: #f2fbf6;
    }
  }
}

.agreement-content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 20px 16px 40px;
  box-sizing: border-box;
}

.content-inner {
  max-width: 760px;
  margin: 0 auto;
}

.content-meta {
  font-size: 12px;
  color: #999;
  margin-bottom: 16px;
}

.content-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #222;
  margin-bottom: 10px;
  scroll-margin-top: 12px;
}

.section-text {
  font-size: 14px;
  color: #555;
  line-height: 1.8;
  margin-bottom: 8px;
  text-align: justify;
  word-break: break-word;
}

.content-tail {
  text-align: center;
  font-size: 12px;
  color: #bbb;
  padding: 16px 0;
}

// 桌面登录小窗口：顶栏收窄（对齐 login/index.vue 的 40px 拖拽条），正文两侧留白收窄
.agreement-page--desktop {
  .agreement-header {
    height: 40px;
    padding: 0 10px;
  }

  .agreement-tabs {
    padding: 0 10px;
    gap: 18px;
  }

  .agreement-content {
    padding: 14px 12px 28px;
  }
}
</style>