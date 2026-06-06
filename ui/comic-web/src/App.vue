<script setup lang="ts">
import { ConfigProvider } from 'ant-design-vue'
</script>

<template>
  <config-provider :theme="{ token: { colorPrimary: '#FF6B35' } }">
    <router-view />
  </config-provider>
</template>

<style scoped>

</style>
