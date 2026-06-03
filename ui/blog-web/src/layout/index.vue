<script setup lang="ts">
import HeaderNav from './Header/nav.vue'
import LayoutFooter from './Footer/footer.vue'

</script>

<template>
  <div class="layout-container">
    <header-nav />

    <router-view v-slot="{ Component, route }">
      <component v-if="!route.meta.starry" :is="Component" />
      <div v-else>
        <starry-sky :stars-count="1000" :distance="450" />

        <component :is="Component" />
      </div>

      <layout-footer :theme="Boolean(route.meta.starry)" />
    </router-view>
  </div>
</template>

<style scoped>
:deep(.starry-sky-bg) {
  background: #000 !important;
}
</style>