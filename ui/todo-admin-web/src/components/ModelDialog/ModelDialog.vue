<script setup>

const isShow = defineModel('modelValue', {
  type: Boolean
})

defineProps({
  width: {
    type: [String, Number],
    default: '50%'
  },
  title: {
    type: String,
    default: ''
  },
  center: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits(['close'])

const handleClose = () => {
  isShow.value = false
  emit('close')
}
</script>

<template>
  <el-dialog
      v-model="isShow"
      :width="width"
      :title="title"
      :align-center="center"
      :before-close="handleClose"
  >
    <template #header>
      <slot name="header"></slot>
    </template>
    <slot></slot>
    <template #footer>
      <slot name="footer"></slot>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">

</style>
