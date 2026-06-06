<script setup lang="ts">
import Logo from './logo.vue'
import MenuNav from './nav.vue'

</script>

<template>
  <div class="layout-menu">
    <logo />

    <menu-nav />
  </div>
</template>

<style scoped lang="scss">
.layout-menu {
  width: size(300);
  height: 100%;
  box-sizing: border-box;
  border-right: size(1) solid $hairline-color;
}
</style>