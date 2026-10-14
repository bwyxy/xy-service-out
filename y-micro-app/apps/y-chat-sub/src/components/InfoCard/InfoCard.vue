<script setup lang="ts">
defineProps<{
  nickname: string
  head: string
}>()
</script>

<template>
  <div class="info-card">
    <el-image class="info-card-avatar" :src="head">
      <template #error>
        <img src="@/assets/images/logo.jpg" alt="head" />
      </template>
    </el-image>
    <span>{{ nickname }}</span>
  </div>
</template>

<style scoped lang="scss">
.info-card {
  width: size(42);
  display: flex;
  flex-direction: column;
  align-items: center;

  .info-card-avatar {
    width: size(42);
    height: size(42);
    border-radius: size(6);
  }
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