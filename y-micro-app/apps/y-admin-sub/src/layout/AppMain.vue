<script setup lang="ts">

</script>

<template>
  <transition>
    <router-view v-slot="{ Component }">
      <component :is="Component" />
    </router-view>
  </transition>
</template>

<style scoped lang="scss">

</style>