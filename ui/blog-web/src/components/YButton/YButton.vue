<script setup lang="ts">

</script>

<template>
  <div class="y-button">
    <slot></slot>
  </div>
</template>

<style scoped lang="scss">
.y-button {
  font-size: 15px;
  letter-spacing: 1px;
  color: #fff;
  padding: 10px 13px;
  border-radius: 48px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px) saturate(180%);
  cursor: pointer;
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  transform: translateY(-2px);
  box-shadow: 0 32px 55px -12px rgba(0, 0, 0, 0.45),
  0 4px 12px rgba(0, 0, 0, 0.15),
  inset 0 1px 1px rgba(255, 255, 255, 0.7),
  inset 0 -1px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 14px;
  &:last-child {
    margin-bottom: 0;
  }
}
</style>