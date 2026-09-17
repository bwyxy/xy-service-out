<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import usePermissionStore from '@/store/modules/permission'
import MenuIcon from '@/components/MenuIcon/index.vue'

const visible = ref(false)
const keyword = ref('')
const activeIndex = ref(0)
const inputRef = ref()
const router = useRouter()
const permissionStore = usePermissionStore()
const { t } = useI18n()

interface SearchItem {
  title: string
  path: string
  icon?: string
  breadcrumbs: string[]
}

const flattenRoutes = (routes: any[], breadcrumbs: string[] = []): SearchItem[] => {
  const result: SearchItem[] = []

  routes.forEach((route) => {
    if (route.hidden || route.meta?.hidden) return

    const current: SearchItem = {
      title: route.meta?.title || route.name,
      path: route.path,
      icon: route.meta?.icon,
      breadcrumbs: [...breadcrumbs, route.meta?.title || route.name]
    }

    if (route.meta?.title) {
      result.push(current)
    }

    if (route.children && route.children.length > 0) {
      result.push(...flattenRoutes(route.children, current.breadcrumbs))
    }
  })

  return result
}

const allItems = computed(() => flattenRoutes(permissionStore.routes))

const filteredItems = computed(() => {
  if (!keyword.value.trim()) return allItems.value
  const kw = keyword.value.toLowerCase()
  return allItems.value.filter((item) =>
    item.title.toLowerCase().includes(kw) ||
    item.path.toLowerCase().includes(kw)
  )
})

const handleOpen = () => {
  visible.value = true
  keyword.value = ''
  activeIndex.value = 0
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleClose = () => {
  visible.value = false
}

const handleSelect = (item: SearchItem) => {
  router.push(item.path)
  handleClose()
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % filteredItems.value.length
    scrollToActive()
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + filteredItems.value.length) % filteredItems.value.length
    scrollToActive()
  } else if (e.key === 'Enter') {
    e.preventDefault()
    const item = filteredItems.value[activeIndex.value]
    if (item) handleSelect(item)
  } else if (e.key === 'Escape') {
    handleClose()
  }
}

const scrollToActive = () => {
  nextTick(() => {
    const activeEl = document.querySelector('.search-result-item.active')
    activeEl?.scrollIntoView({ block: 'nearest' })
  })
}

watch(filteredItems, () => {
  activeIndex.value = 0
})

const highlight = (text: string) => {
  if (!keyword.value.trim()) return text
  const kw = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const reg = new RegExp(`(${kw})`, 'gi')
  return text.replace(reg, '<mark>$1</mark>')
}

const handleGlobalKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    handleOpen()
  }
}

watch(visible, (val) => {
  if (val) {
    document.addEventListener('keydown', handleGlobalKeydown)
  } else {
    document.removeEventListener('keydown', handleGlobalKeydown)
  }
})

defineExpose({ handleOpen })
</script>

<template>
  <div>
    <div class="search-btn" @click="handleOpen">
      <el-icon :size="18"><Search /></el-icon>
      <span>{{ t('header.searchTip') }}</span>
      <kbd>Ctrl K</kbd>
    </div>

    <el-dialog
      v-model="visible"
      width="560px"
      :show-close="false"
      :append-to-body="true"
      class="search-modal-dialog"
      @keydown="handleKeydown"
    >
      <div class="search-modal">
        <div class="search-input-wrapper">
          <el-icon :size="20"><Search /></el-icon>
          <input
            ref="inputRef"
            v-model="keyword"
            :placeholder="t('header.searchPlaceholder')"
            class="search-input"
          />
          <kbd class="esc-key">ESC</kbd>
        </div>

        <div v-if="filteredItems.length > 0" class="search-results">
          <div
            v-for="(item, index) in filteredItems"
            :key="item.path"
            class="search-result-item"
            :class="{ active: index === activeIndex }"
            @click="handleSelect(item)"
            @mouseenter="activeIndex = index"
          >
            <MenuIcon :icon="item.icon" :size="18" class="result-icon" />
            <div class="result-info">
              <div class="result-title" v-html="highlight(item.title)"></div>
              <div class="result-path">{{ item.breadcrumbs.join(' / ') }}</div>
            </div>
          </div>
        </div>

        <el-empty v-else :description="t('common.noData')" :image-size="60" />

        <div class="search-footer">
          <span><kbd>↑</kbd> <kbd>↓</kbd> {{ t('common.select') }}</span>
          <span><kbd>Enter</kbd> {{ t('common.jump') }}</span>
          <span><kbd>Esc</kbd> {{ t('common.close') }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.search-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px;
  border-radius: 4px;
  border: 1px solid var(--app-border-color);
  background: var(--app-card-bg);
  color: var(--app-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
  }

  kbd {
    padding: 1px 4px;
    border-radius: 3px;
    border: 1px solid var(--app-border-color);
    font-size: 11px;
    margin-left: 4px;
  }
}

.search-modal {
  display: flex;
  flex-direction: column;

  .search-input-wrapper {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--app-border-color);

    .search-input {
      flex: 1;
      border: none;
      outline: none;
      font-size: 16px;
      background: transparent;
      color: var(--app-text-color);

      &::placeholder {
        color: var(--app-text-secondary);
      }
    }

    .esc-key {
      padding: 2px 6px;
      border-radius: 4px;
      border: 1px solid var(--app-border-color);
      font-size: 11px;
      color: var(--app-text-secondary);
    }
  }

  .search-results {
    max-height: 320px;
    overflow-y: auto;
    padding: 8px;
  }

  .search-result-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 6px;
    cursor: pointer;
    transition: background-color 0.2s;
    color: var(--app-text-color);

    &:hover,
    &.active {
      background-color: var(--app-hover-bg);
    }

    .result-icon {
      flex-shrink: 0;
      color: var(--app-text-secondary);
    }

    .result-info {
      flex: 1;
      min-width: 0;

      .result-title {
        font-size: 14px;
        font-weight: 500;

        :deep(mark) {
          background: transparent;
          color: var(--el-color-primary);
          font-weight: bold;
        }
      }

      .result-path {
        font-size: 12px;
        color: var(--app-text-secondary);
        margin-top: 2px;
      }
    }
  }

  .search-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 16px;
    padding: 10px 16px;
    border-top: 1px solid var(--app-border-color);
    font-size: 12px;
    color: var(--app-text-secondary);

    kbd {
      padding: 1px 4px;
      border-radius: 3px;
      border: 1px solid var(--app-border-color);
      font-size: 11px;
    }
  }
}

@media (max-width: 768px) {
  :deep(.search-modal-dialog) {
    width: 90% !important;
  }

  .search-btn {
    span,
    kbd {
      display: none;
    }
  }

  .search-footer {
    gap: 8px;
    font-size: 11px;
  }
}
</style>
