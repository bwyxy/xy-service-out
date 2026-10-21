<script setup lang="ts">
import { ref, watch, nextTick, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Close, ArrowDown } from '@element-plus/icons-vue'
import useTagsViewStore from '@/store/modules/tagsView'
import type { TagView } from '@/store/modules/tagsView'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()
const { t } = useI18n()

const selectedTag = ref<TagView>()
const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ left: 0, top: 0 })

const visitedViews = computed(() => tagsViewStore.visitedViews)

const isActive = (tag: TagView) => tag.path === route.path

const addTags = () => {
  const { name } = route
  if (name) {
    tagsViewStore.addView(route)
  }
  return false
}

const moveToCurrentTag = () => {
  nextTick(() => {
    const tags = document.querySelectorAll('.tags-view-item')
    tags.forEach((tag) => {
      if ((tag as HTMLElement).dataset.path === route.path) {
        tag.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' })
      }
    })
  })
}

const toLastView = (visitedViews: TagView[]) => {
  const latestView = visitedViews.slice(-1)[0]
  if (latestView) {
    router.push(latestView.fullPath)
  } else {
    if (route.path === '/dashboard' || route.path === '/') {
      router.push('/dashboard')
    } else {
      router.push('/')
    }
  }
}

const handleTagClick = (tag: TagView) => {
  router.push(tag.fullPath)
}

const handleClose = (tag: TagView) => {
  tagsViewStore.delVisitedView(tag)
  if (isActive(tag)) {
    toLastView(visitedViews.value)
  }
}

const handleCloseAll = () => {
  tagsViewStore.delAllViews()
  router.push('/')
}

const openContextMenu = (tag: TagView, e: MouseEvent) => {
  selectedTag.value = tag
  contextMenuPosition.value = { left: e.clientX, top: e.clientY }
  contextMenuVisible.value = true
}

const closeContextMenu = () => {
  contextMenuVisible.value = false
}

const closeSelectedTag = () => {
  if (selectedTag.value) {
    handleClose(selectedTag.value)
  }
}

const closeOthersTags = () => {
  if (selectedTag.value) {
    tagsViewStore.delOthersViews(selectedTag.value)
    router.push(selectedTag.value.fullPath)
  }
}

const closeAllTags = () => {
  handleCloseAll()
}

watch(
  () => route.path,
  () => {
    addTags()
    moveToCurrentTag()
  },
  { immediate: true }
)

watch(contextMenuVisible, (visible) => {
  if (visible) {
    document.body.addEventListener('click', closeContextMenu)
  } else {
    document.body.removeEventListener('click', closeContextMenu)
  }
})

onUnmounted(() => {
  document.body.removeEventListener('click', closeContextMenu)
})
</script>

<template>
  <div class="tags-view-container">
    <div class="tags-view-wrapper">
      <div
        v-for="tag in visitedViews"
        :key="tag.path"
        :data-path="tag.path"
        class="tags-view-item"
        :class="{ active: isActive(tag) }"
        @click.middle="handleClose(tag)"
        @contextmenu.prevent="openContextMenu(tag, $event)"
      >
        <span class="tag-title" @click.stop="handleTagClick(tag)">
          {{ tag.title }}
        </span>
        <el-icon
          v-if="!tag.meta?.affix"
          class="close-icon"
          @click.stop="handleClose(tag)"
        >
          <Close />
        </el-icon>
      </div>
    </div>

    <div class="tags-view-actions">
      <el-dropdown trigger="click" @command="handleCloseAll">
        <span class="action-btn">
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="all">{{ t('tagsView.closeAll') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <ul
      v-show="contextMenuVisible"
      class="contextmenu"
      :style="{
        left: contextMenuPosition.left + 'px',
        top: contextMenuPosition.top + 'px'
      }"
    >
      <li @click="closeSelectedTag">{{ t('tagsView.close') }}</li>
      <li @click="closeOthersTags">{{ t('tagsView.closeOthers') }}</li>
      <li @click="closeAllTags">{{ t('tagsView.closeAll') }}</li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.tags-view-container {
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  box-sizing: border-box;
}

.tags-view-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  height: 100%;
  padding: 0 12px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  height: 28px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  background-color: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-7);
  }

  &.active {
    color: var(--el-color-primary);
    background-color: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary);
  }

  .tag-title {
    margin-right: 6px;
  }

  .close-icon {
    width: 14px;
    height: 14px;
    border-radius: 50%;
    transition: all 0.2s;

    &:hover {
      color: #fff;
      background-color: var(--el-color-danger);
    }
  }
}

.tags-view-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 100%;
  border-left: 1px solid var(--el-border-color-light);
  flex-shrink: 0;

  .action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    cursor: pointer;
    color: var(--el-text-color-regular);

    &:hover {
      color: var(--el-color-primary);
    }
  }
}

.contextmenu {
  position: fixed;
  z-index: 3000;
  margin: 0;
  padding: 6px 0;
  list-style: none;
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  box-shadow: var(--el-box-shadow-light);

  li {
    padding: 8px 16px;
    font-size: 13px;
    color: var(--el-text-color-regular);
    cursor: pointer;

    &:hover {
      color: var(--el-color-primary);
      background-color: var(--el-fill-color-light);
    }
  }
}

@media (max-width: 768px) {
  .tags-view-container {
    height: 36px;
  }

  .tags-view-wrapper {
    padding: 0 8px;
  }

  .tags-view-item {
    height: 26px;
    padding: 0 8px;
    font-size: 12px;
  }

  .tags-view-actions {
    width: 32px;
  }
}
</style>
