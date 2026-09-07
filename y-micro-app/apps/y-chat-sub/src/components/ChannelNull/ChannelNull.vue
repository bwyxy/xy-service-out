<script setup lang="ts">

</script>

<template>
  <div class="channel-null">
    <img src="@/assets/images/logo.jpg" alt="null" />
    <span>飞鸽即时通讯</span>
  </div>
</template>

<style scoped lang="scss">
.channel-null {
  width: 100%;
  height: 100%;
  flex-direction: column;
  background: #f0f0f0;
  @include flexCenter;
  img {
    width: size(140);
    height: size(140);
    border-radius: size(13);
  }
  span {
    font-size: size(24);
    color: #8c8c8c;
    letter-spacing: size(2);
    margin-top: size(18);
  }
}
</style>