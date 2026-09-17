<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import * as Icons from '@element-plus/icons-vue'
import { Search, ArrowDown, CircleClose } from '@element-plus/icons-vue'

interface Props {
  modelValue?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

// 全部图标
const allIconNames = Object.keys(Icons)
  .filter((name) => name !== 'default')
  .sort()

const searchKeyword = ref('')
const popoverVisible = ref(false)
const triggerRef = ref<HTMLDivElement>()

const filteredIcons = computed(() => {
  if (!searchKeyword.value.trim()) return allIconNames
  const kw = searchKeyword.value.toLowerCase()
  return allIconNames.filter((name) => name.toLowerCase().includes(kw))
})

const selectedIcon = computed(() => props.modelValue || '')

const getIconComponent = (name: string) => {
  return (Icons as Record<string, any>)[name]
}

const handleSelect = (name: string) => {
  emit('update:modelValue', name)
  popoverVisible.value = false
  searchKeyword.value = ''
}

const handleClear = (e: Event) => {
  e.stopPropagation()
  emit('update:modelValue', '')
}

const handleShow = () => {
  nextTick(() => {
    const input = triggerRef.value?.querySelector('.icon-search-input input') as HTMLInputElement
    input?.focus()
  })
}
</script>

<template>
  <div class="icon-select">
    <el-popover
      v-model:visible="popoverVisible"
      placement="bottom-start"
      :width="360"
      trigger="click"
      popper-class="icon-select-popper"
      @show="handleShow"
    >
      <template #reference>
        <div ref="triggerRef" class="icon-select-trigger" :class="{ empty: !selectedIcon }">
          <template v-if="selectedIcon">
            <el-icon :size="18">
              <component :is="getIconComponent(selectedIcon)" />
            </el-icon>
            <span class="selected-name">{{ selectedIcon }}</span>
            <el-icon class="clear-icon" :size="14" @click.stop="handleClear">
              <CircleClose />
            </el-icon>
          </template>
          <template v-else>
            <span class="placeholder">请选择图标</span>
          </template>
          <el-icon class="arrow-icon" :size="14"><ArrowDown /></el-icon>
        </div>
      </template>

      <div class="icon-dropdown">
        <div class="icon-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索图标"
            clearable
            :prefix-icon="Search"
            class="icon-search-input"
          />
        </div>
        <el-scrollbar max-height="280px" class="icon-scroll">
          <div class="icon-grid">
            <div
              v-for="name in filteredIcons"
              :key="name"
              class="icon-grid-item"
              :class="{ active: selectedIcon === name }"
              :title="name"
              @click="handleSelect(name)"
            >
              <el-icon :size="22">
                <component :is="getIconComponent(name)" />
              </el-icon>
              <span class="icon-name">{{ name }}</span>
            </div>
          </div>
          <el-empty v-if="filteredIcons.length === 0" description="未找到图标" :image-size="60" />
        </el-scrollbar>
      </div>
    </el-popover>
  </div>
</template>

<style scoped lang="scss">
.icon-select {
  width: 100%;
}

.icon-select-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background-color: var(--el-bg-color);
  cursor: pointer;
  transition: border-color 0.2s;
  box-sizing: border-box;

  &:hover {
    border-color: var(--el-border-color-hover);
  }

  &.empty {
    color: var(--el-text-color-placeholder);
  }

  .selected-name {
    flex: 1;
    font-size: 13px;
    color: var(--el-text-color-regular);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .placeholder {
    flex: 1;
    font-size: 13px;
  }

  .arrow-icon {
    color: var(--el-text-color-secondary);
    transition: transform 0.2s;
  }

  .clear-icon {
    color: var(--el-text-color-secondary);

    &:hover {
      color: var(--el-color-danger);
    }
  }
}

.icon-dropdown {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .icon-search {
    padding: 0 4px;
  }

  .icon-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 8px;
    padding: 4px;
  }

  .icon-grid-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 8px 4px;
    border: 1px solid transparent;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: var(--el-color-primary-light-7);
      background-color: var(--el-fill-color-light);
    }

    &.active {
      border-color: var(--el-color-primary);
      background-color: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
    }

    .icon-name {
      width: 100%;
      font-size: 10px;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>

<style>
.icon-select-popper {
  padding: 12px !important;
}
</style>
