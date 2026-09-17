<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown } from '@element-plus/icons-vue'
import useLocaleStore from '@/store/modules/locale'

type LocaleType = 'zh-CN' | 'en-US'

const localeStore = useLocaleStore()
const { t } = useI18n()

const options = computed(() => [
  { label: t('common.langZh'), value: 'zh-CN' },
  { label: t('common.langEn'), value: 'en-US' },
])

const currentLabel = computed(() => {
  return options.value.find((item) => item.value === localeStore.locale)?.label || t('common.langZh')
})

const handleCommand = (command: LocaleType) => {
  if (command === localeStore.locale) return
  localeStore.setLocale(command)
}
</script>

<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="lang-select">
      <span class="lang-text">{{ currentLabel }}</span>
      <el-icon><ArrowDown /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in options"
          :key="item.value"
          :command="item.value"
          :class="{ active: item.value === localeStore.locale }"
        >
          {{ item.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
.lang-select {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 8px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--app-text-color);
  transition: background-color 0.2s;

  &:hover {
    background-color: var(--app-hover-bg);
  }

  .lang-text {
    font-size: 13px;
  }
}

:deep(.el-dropdown-menu__item) {
  &.active {
    color: var(--el-color-primary);
    font-weight: 500;
  }
}
</style>
