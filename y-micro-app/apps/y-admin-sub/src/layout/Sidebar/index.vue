<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'
import useAppStore from '@/store/modules/app'
import SidebarMenu from './SidebarMenu.vue'

const { t } = useI18n()

// const appStore = useAppStore()
const { isMobile, sidebarOpened } = storeToRefs(useAppStore())
</script>

<template>
  <div class="sidebar-container">
    <!-- 桌面端菜单 -->
    <SidebarMenu class="desktop-menu" />

    <!-- 移动端抽屉 -->
    <el-drawer
      v-if="isMobile"
      v-model="sidebarOpened"
      direction="ltr"
      :size="220"
      :with-header="false"
      :append-to-body="true"
      :modal-append-to-body="true"
      class="mobile-sidebar-drawer"
    >
      <div class="mobile-sidebar-content">
        <div class="mobile-sidebar-logo">
          <span>{{ t('login.title') }}</span>
        </div>
        <SidebarMenu />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.sidebar-container {
  width: 200px;
  height: 100%;
  flex-shrink: 0;
  background: var(--app-sidebar-bg);
  box-shadow: rgba(0, 0, 0, 0.04) 4px 2px 6px 0, rgba(0, 0, 0, 0.04) -4px -2px 6px 0px;
  box-sizing: border-box;
  overflow-y: auto;

  :deep(.el-menu) {
    border-right: none;
    background: transparent !important;
    color: var(--app-sidebar-text);

    .el-sub-menu__title,
    .el-menu-item {
      display: flex;
      align-items: center;
      background: transparent !important;

      .menu-icon {
        margin-right: 10px;
        flex-shrink: 0;
      }
    }

    .el-menu-item {
      &.is-active {
        color: var(--app-sidebar-text);
        background-color: var(--el-color-primary);
      }

      &:hover {
        background-color: rgba(255, 255, 255, 0.1);
      }

      &.is-disabled {
        opacity: 0.5;
      }
    }

    .el-sub-menu__title:hover {
      background-color: rgba(255, 255, 255, 0.1);
    }

    .el-sub-menu.is-active .el-sub-menu__title {
      color: var(--app-sidebar-text);
    }

    html.dark & {
      color: var(--app-sidebar-text);

      .el-sub-menu__title,
      .el-menu-item {
        color: var(--app-sidebar-text);
        background: transparent !important;
      }

      .el-menu-item:hover,
      .el-sub-menu__title:hover {
        background-color: var(--app-hover-bg);
      }

      .el-menu-item.is-active {
        background-color: var(--el-color-primary);
      }

      .el-sub-menu.is-active .el-sub-menu__title {
        color: var(--app-sidebar-text);
      }
    }
  }
}

.mobile-sidebar-content {
  height: 100%;
  background: var(--app-sidebar-bg);
  overflow-y: auto;
}

.mobile-sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);

  span {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 1px;
    background: linear-gradient(90deg, #003eb3, #69b1ff);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }

  html.dark & span {
    text-shadow: 0 0 12px rgba(105, 177, 255, 0.6), 0 0 24px rgba(105, 177, 255, 0.3);
  }
}

@media (max-width: 768px) {
  .sidebar-container {
    width: auto;
    background: transparent;
    box-shadow: none;
  }

  .desktop-menu {
    display: none;
  }
}
</style>
