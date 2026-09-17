<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import useUserStore from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
</script>

<template>
  <div class="home-container">
    <el-card>
      <template #header>
        <span>{{ t('home.welcomeBack') }}</span>
      </template>
      <p>{{ t('home.hello', { name: userStore.userInfo?.nickname || userStore.userInfo?.username || t('home.admin') }) }}</p>
      <p>{{ t('home.systemWelcome') }}</p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.home-container {
  .el-card {
    max-width: 600px;
  }
}
</style>