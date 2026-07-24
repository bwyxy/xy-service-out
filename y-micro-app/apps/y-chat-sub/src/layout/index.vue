<script setup lang="ts">
import LayoutNav from '@/layout/nav/index.vue'
</script>

<template>
  <div class="app-container">
    <layout-nav />

    <router-view />
  </div>
</template>

<style lang="scss" scoped>
.app-container {
  width: 100vw;
  height: 100vh;
  box-sizing: border-box;
  border: 8px solid $theme-color;
  border-radius: $border-radius-lg;
  display: flex;
}
</style>
