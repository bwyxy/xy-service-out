<script setup lang="ts">
import { ReadFilled } from '@ant-design/icons-vue'

</script>

<template>
  <div class="logo">
    <div class="logo-icon">
      <ReadFilled />
    </div>
    <div class="logo-title">
      <span>漫次元</span>
      <span>发现精彩漫画</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.logo {
  height: size(110);
  border-bottom: size(1) solid $hairline-color;
  @include flexCenter;
  &-icon {
    width: size(60);
    height: size(60);
    background: $theme-color;
    border-radius: 50%;
    font-size: size(34);
    color: #fff;
    padding-top: size(4);
    @include flexCenter;
  }
  &-title {
    display: flex;
    flex-direction: column;
    margin-left: size(12);
    margin-top: size(8);
    span:nth-child(1) {
      font-size: size(28);
      font-weight: bold;
      letter-spacing: size(6);
    }
    span:nth-child(2) {
      font-size: size(14);
      color: $muted-soft-color;
      letter-spacing: size(2);
      margin-top: size(4);
    }
  }
}
</style>