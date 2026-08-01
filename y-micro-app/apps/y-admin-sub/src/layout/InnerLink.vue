<script setup lang="ts">
// InnerLink — 内链组件，用于在 iframe 中打开外部链接
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const iframeSrc = ref('')

watch(
  () => route.meta?.link,
  (link) => {
    if (link) {
      iframeSrc.value = link as string
    }
  },
  { immediate: true }
)
</script>

<template>
  <div v-if="iframeSrc" class="inner-link-container">
    <iframe :src="iframeSrc" frameborder="0" class="inner-link-iframe" />
  </div>
</template>

<style scoped>
.inner-link-container {
  width: 100%;
  height: 100%;
}
.inner-link-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>