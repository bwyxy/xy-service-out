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

    <HeaderBar />

    <div class="main-container"></div>
<!--    <Sidebar />-->
<!--    <div class="main-container">-->
<!--      <HeaderBar />-->
<!--      <div class="app-main">-->
<!--        <router-view :key="key" />-->
<!--      </div>-->
<!--    </div>-->
  </div>
</template>

<style scoped lang="scss">
.app-wrapper {
  display: flex;
  width: 100%;
  height: 100vh;
  background: linear-gradient(170deg, #bae0ff, #fafafa, #fff);
  overflow: hidden;
  flex-direction: column;
}

.main-container {
  flex: 1;
  background: rgba(255, 255, 255, .6);
  display: flex;
  flex-direction: column;
  border-radius: size(24) size(24) 0 0;
  box-shadow: rgba(0, 0, 0, .04) 4px 2px 6px 0, rgba(0, 0, 0, .04) -4px -2px 6px 0px;
  overflow: hidden;
}

.app-main {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #f0f2f5;
}
</style>