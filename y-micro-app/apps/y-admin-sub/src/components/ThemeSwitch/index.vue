<script setup lang="ts">
import { computed } from 'vue'
import { Sunny, Moon } from '@element-plus/icons-vue'
import useThemeStore from '@/store/modules/theme'

const themeStore = useThemeStore()

const isDark = computed(() => themeStore.isDark)

const handleToggle = () => {
  themeStore.toggle()
}
</script>

<template>
  <div class="theme-switch" :class="{ dark: isDark }" @click="handleToggle">
    <el-icon :size="16" class="light-icon"><Sunny /></el-icon>
    <el-icon :size="16" class="dark-icon"><Moon /></el-icon>
    <span class="switch-ball"></span>
  </div>
</template>

<style scoped lang="scss">
.theme-switch {
  position: relative;
  width: 50px;
  height: 26px;
  border-radius: 13px;
  background: #e6e8ec;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 6px;
  box-sizing: border-box;
  transition: background-color 0.3s ease;
  user-select: none;

  .el-icon {
    color: #fff;
    z-index: 1;
    transition: color 0.3s ease;
  }

  .dark-icon {
    color: var(--app-text-secondary);
  }

  .switch-ball {
    position: absolute;
    top: 3px;
    left: 3px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
    transition: transform 0.3s ease, background-color 0.3s ease;
    z-index: 2;
  }

  &.dark {
    background: #2c2c2c;

    .light-icon {
      color: var(--app-text-secondary);
    }

    .dark-icon {
      color: #fff;
    }

    .switch-ball {
      transform: translateX(24px);
      background: #f2c94c;
    }
  }
}

@media (max-width: 768px) {
  .theme-switch {
    width: 42px;
    height: 22px;
    padding: 0 4px;

    .switch-ball {
      width: 16px;
      height: 16px;
      top: 3px;
      left: 3px;
    }

    .el-icon {
      font-size: 12px;
    }

    &.dark .switch-ball {
      transform: translateX(20px);
    }
  }
}
</style>
