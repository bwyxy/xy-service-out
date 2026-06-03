<script setup lang="ts">

defineProps<{
  categoryList: any[]
}>()
const emit = defineEmits(['change'])

const categoryId = defineModel('modelValue', {
  type: Number
})

const handleSelectedCategory = (id: number) => {
  categoryId.value = id
  emit('change', id)

}
</script>

<template>
  <div class="category-tab">
    <div class="category-tab-content">
      <div
          v-for="(item, index) in categoryList"
          :key="index"
          :class="[
              'category-item',
              { 'category-active': item.categoryId === categoryId }
          ]"
          @click="handleSelectedCategory(item.categoryId)"
      >
        <span>{{ item.name }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.category-tab {
  width: 100%;
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  box-sizing: border-box;
  padding: 10px 16px;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.04);
  border-radius: 20px;
  display: flex;
  margin-bottom: 24px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    box-shadow:
      0 2px 4px rgba(0, 0, 0, 0.06),
      0 8px 24px rgba(0, 0, 0, 0.08);
  }

  &-content {
    min-height: 20px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
  }
}

.category-item {
  border-radius: 48px;
  cursor: pointer;
  padding: 6px 18px;
  display: flex;
  align-items: center;
  transition: background 0.3s ease;

  span {
    font-size: 13px;
    color: #555;
    letter-spacing: 0.5px;
    transition: color 0.3s ease;
  }

  &:hover:not(.category-active) {
    background: rgba(64, 150, 255, 0.06);

    span {
      color: #4096ff;
    }
  }
}

.category-active {
  background: linear-gradient(135deg, #4096ff, #1677ff);
  box-shadow: 0 2px 8px rgba(64, 150, 255, 0.25);

  span {
    color: #fff;
  }
}
</style>