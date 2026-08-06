<script setup lang="ts">

</script>

<template>
  <div
      class="y-editor"
      contenteditable="true"
      role="textbox"
      aria-multiline="true"
  ></div>
</template>

<style scoped lang="scss">
.y-editor {
  width: 100%;
  height: size(240);
  border: 1px solid rgba(0, 0, 0, .08);
  border-radius: size(8);
}
</style>