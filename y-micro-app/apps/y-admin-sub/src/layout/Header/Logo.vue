<script setup lang="ts">

</script>

<template>
  <div class="logo">
    <span>忆管家</span>
  </div>
</template>

<style scoped lang="scss">
.logo {
  width: 200px;
  display: flex;
  justify-content: center;
  span {
    font-size: 20px;
    font-weight: 700;
    letter-spacing: 2px;
    background: linear-gradient(90deg, #003eb3, #69b1ff);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }

  html.dark & span {
    text-shadow: 0 0 12px rgba(105, 177, 255, 0.6), 0 0 24px rgba(105, 177, 255, 0.3);
  }
}
</style>