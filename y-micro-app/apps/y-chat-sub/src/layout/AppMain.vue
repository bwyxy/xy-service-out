<script setup lang="ts">

</script>

<template>
  <transition>
    <router-view v-slot="{ Component, route }">
      <component :is="Component" :key="route" />
    </router-view>
  </transition>
</template>

<style scoped lang="scss">

</style>