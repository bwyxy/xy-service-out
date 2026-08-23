<script setup lang="ts">
defineProps<{
  nickname: string
  head: string
}>()
</script>

<template>
  <div class="info-card">
    <img :src="head" alt="head" />
    <span>{{ nickname }}</span>
  </div>
</template>

<style scoped lang="scss">
.info-card {
  width: size(42);
  display: flex;
  flex-direction: column;
  align-items: center;
  img {
    width: size(42);
    height: size(42);
    border-radius: size(6);
  }
  span {
    font-size: size(10);
    color: #8c8c8c;
    margin-top: size(4);
    @include textEllipsis(1);
  }
}
</style>