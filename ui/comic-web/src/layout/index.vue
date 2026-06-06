<script setup lang="ts">
import LayoutMenu from '@/layout/Menu/index.vue'
</script>

<template>
  <div class="app-container">
    <layout-menu />
    <div class="main-container">
      <router-view />
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-container {
  width: 100vw;
  height: 100vh;
  background: $theme-container-light;
  overflow: hidden;
  display: flex;
}

.main-container {
  flex: 1;
  overflow: hidden;
  overflow-y: scroll;
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}
</style>