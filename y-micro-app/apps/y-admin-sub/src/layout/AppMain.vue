<script setup lang="ts">
import { computed, markRaw, type Component } from 'vue'
import { useRoute } from 'vue-router'
import useTagsViewStore from '@/store/modules/tagsView'

const route = useRoute()
const tagsViewStore = useTagsViewStore()

const cachedViews = computed(() => tagsViewStore.cachedViews)

/**
 * keep-alive 的 include 按「组件 name」匹配，而页面组件文件名大多为 index.vue，
 * 推断出的组件名不唯一，导致缓存完全失效。这里按路由名给组件补一个稳定的 name，
 * 使 cachedViews（存路由名）能够正确命中对应页面。
 */
const namedComponents = new Map<string, Component>()

const withRouteName = (component: unknown, name: unknown) => {
  if (!component || typeof component !== 'object' || typeof name !== 'string') {
    return component
  }

  const cached = namedComponents.get(name)
  if (cached) {
    return cached
  }

  const named = markRaw({ ...(component as Component), name })
  namedComponents.set(name, named)

  return named
}
</script>

<template>
  <div class="app-main-wrapper">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="withRouteName(Component, route.name)" :key="route.path" />
        </keep-alive>
      </transition>
    </router-view>
  </div>
</template>

<style scoped lang="scss">
.app-main-wrapper {
  flex: 1;
  box-sizing: border-box;
  padding: 24px;
  overflow-y: auto;
  background: var(--app-bg-color);
}

@media (max-width: 768px) {
  .app-main-wrapper {
    padding: 12px;
    // 适配底部安全区（全面屏手势条），避免底部内容被遮挡
    padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  }
}

/* 路由切换动画：淡入 + 左右滑入 */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
