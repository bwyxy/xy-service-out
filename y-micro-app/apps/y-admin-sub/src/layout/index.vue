<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from './Sidebar/index.vue'
import HeaderBar from './Header/index.vue'

const route = useRoute()
const router = useRouter()

const key = computed(() => route.path)
</script>

<template>
  <div class="app-wrapper">
    <Sidebar />
    <div class="main-container">
      <HeaderBar />
      <div class="app-main">
        <router-view :key="key" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-wrapper {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-main {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #f0f2f5;
}
</style>