<script setup lang="ts">
import ForumHeader from './Header/header.vue'

</script>

<template>
  <div class="froum-box">
    <forum-header />
  </div>
</template>

<style scoped lang="scss">
.froum-box {
  width: 100%;
  height: 100%;
  background: #fff;
  overflow: auto;
  overflow-y: scroll;
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}
</style>