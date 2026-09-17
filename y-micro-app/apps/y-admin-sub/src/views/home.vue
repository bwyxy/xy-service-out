<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<template>
  <div class="home-wrapper">
    <div class="home-content">
      <h1>{{ t('home.welcome') }}</h1>
    </div>
  </div>
</template>

<style scoped lang="scss">
.home-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.home-content {
  text-align: center;
}
</style>
