<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import Sidebar from './Sidebar/index.vue'
import HeaderBar from './Header/index.vue'
import AppMain from './AppMain.vue'
import TagsView from './TagsView/index.vue'
import useAppStore from '@/store/modules/app'

const appStore = useAppStore()

const checkMobile = () => {
  appStore.setMobile(window.innerWidth <= 768)
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<template>
  <div class="app-wrapper">

    <HeaderBar />

    <div class="main-container">

      <Sidebar class="desktop-sidebar" />

      <div class="app-main">
        <TagsView />
        <app-main />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-wrapper {
  display: flex;
  width: 100%;
  height: 100vh;
  background: var(--app-bg-color);
  overflow: hidden;
  flex-direction: column;
}

.main-container {
  flex: 1;
  //background: rgba(255, 255, 255, .6);
  display: flex;
  //border-radius: size(24) size(24) 0 0;
  //box-shadow: rgba(0, 0, 0, .04) 4px 2px 6px 0, rgba(0, 0, 0, .04) -4px -2px 6px 0px;
  box-sizing: border-box;
  //padding: 0 size(24) size(24);
  overflow: hidden;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.breadcrumb-wrapper {
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .desktop-sidebar {
    display: none;
  }

  .app-main {
    width: 100%;
  }
}
</style>