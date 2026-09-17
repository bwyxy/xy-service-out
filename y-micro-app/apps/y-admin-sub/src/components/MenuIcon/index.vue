<script setup lang="ts">
import { computed } from 'vue'
import * as Icons from '@element-plus/icons-vue'

interface Props {
  icon?: string
  size?: number | string
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  icon: '',
  size: 18,
  color: 'inherit'
})

const iconComponent = computed(() => {
  if (!props.icon) return null
  return (Icons as Record<string, any>)[props.icon]
})
</script>

<template>
  <el-icon
    v-if="iconComponent"
    class="menu-icon"
    :size="size"
    :color="color"
  >
    <component :is="iconComponent" />
  </el-icon>
</template>

<style scoped lang="scss">
.menu-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  margin-right: 8px;
}
</style>
