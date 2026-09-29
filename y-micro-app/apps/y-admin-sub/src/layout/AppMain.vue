<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import useTagsViewStore from '@/store/modules/tagsView'

const route = useRoute()
const tagsViewStore = useTagsViewStore()

const cachedViews = computed(() => tagsViewStore.cachedViews)
</script>

<template>
  <div class="app-main-wrapper">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="route.path" />
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