<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Logo from './Logo.vue'
import UserInfo from './UserInfo.vue'
import useUserStore from '@/store/modules/user'
import useAppStore from '@/store/modules/app'
import {BellFilled, Setting, SwitchButton, Fold, Expand} from "@element-plus/icons-vue";
import Breadcrumb from '@/layout/Breadcrumb/index.vue'
import ThemeSwitch from '@/components/ThemeSwitch/index.vue'
import FullScreen from '@/components/FullScreen/index.vue'
import SearchModal from '@/components/SearchModal/index.vue'
import LangSelect from '@/components/LangSelect/index.vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const { t } = useI18n()

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="header-bar">
    <div class="header-bar-left">
      <div class="mobile-menu-btn" @click="appStore.toggleSidebar">
        <el-icon :size="20">
          <Fold v-if="appStore.sidebarOpened" />
          <Expand v-else />
        </el-icon>
      </div>

      <logo class="desktop-only" />

      <div class="header-bar-breadcrumb desktop-only">
        <breadcrumb />
      </div>
    </div>

    <user-info class="desktop-only" />

    <div class="header-bar-rt">

      <div class="header-action-item desktop-only">
        <SearchModal />
      </div>

      <div class="header-action-item desktop-only">
        <FullScreen />
      </div>

      <div class="theme-switch-wrapper">
        <ThemeSwitch />
      </div>

      <div class="lang-select-wrapper">
        <LangSelect />
      </div>

      <div class="option-btn desktop-only">
        <el-icon><BellFilled /></el-icon>
        <span>{{ t('header.message') }}</span>
      </div>

      <div class="setting-btn desktop-only">
        <el-icon><Setting /></el-icon>
      </div>

      <div class="option-btn" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span class="desktop-only">{{ t('common.logout') }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.header-bar {
  height: 60px;
  background: var(--app-header-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid var(--app-border-color);
  box-sizing: border-box;
  padding: 0 16px;

  &-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
    min-width: 0;
  }

  &-breadcrumb {
    margin-left: 14px;
    display: flex;
    align-items: center;
  }

  .header-left {
    font-size: 14px;
    color: var(--app-text-color);
  }

  .header-bar-rt {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
  }
}

.mobile-menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--app-text-color);

  &:hover {
    background-color: var(--app-hover-bg);
  }
}

.setting-btn {
  @include boxBtn(13, 28, 4);
}

.option-btn {
  @include boxBtn(48, 28, 4);
}

.theme-switch-wrapper {
  display: flex;
  align-items: center;
}

.lang-select-wrapper {
  display: flex;
  align-items: center;
}

.header-action-item {
  display: flex;
  align-items: center;
}

@media (max-width: 768px) {
  .header-bar {
    padding: 0 12px;

    .desktop-only {
      display: none !important;
    }
  }

  .mobile-menu-btn {
    display: flex;
  }

  .setting-btn,
  .option-btn {
    min-width: 36px;
    height: 36px;
    padding: 0;
    margin: 0;
  }

  .theme-switch-wrapper {
    margin-right: 0;
  }

  .header-action-item {
    margin-right: 0;
  }
}
</style>