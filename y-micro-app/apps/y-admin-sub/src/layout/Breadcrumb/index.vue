<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter, type RouteLocationMatched } from 'vue-router'

const route = useRoute()
const router = useRouter()

const breadcrumbs = ref<RouteLocationMatched[]>([])

const getBreadcrumbs = () => {
  const matched = route.matched.filter(
    (item) => item.meta?.title && !item.meta?.hidden
  )

  // 如果当前不是首页，则在前面补一个首页
  const first = matched[0]
  if (!first || first.path !== '/') {
    breadcrumbs.value = matched
    return
  }

  breadcrumbs.value = matched
}

const handleClick = (item: RouteLocationMatched) => {
  const { redirect, path } = item
  if (redirect) {
    router.push(redirect as string)
    return
  }
  router.push(path)
}

watch(
  () => route.path,
  () => getBreadcrumbs(),
  { immediate: true }
)
</script>

<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <el-breadcrumb-item
      v-for="(item, index) in breadcrumbs"
      :key="item.path"
    >
      <span
        v-if="index === breadcrumbs.length - 1"
        class="no-redirect"
      >
        {{ item.meta?.title }}
      </span>
      <a v-else @click.prevent="handleClick(item)">
        {{ item.meta?.title }}
      </a>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style scoped lang="scss">
.app-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 1;

  :deep(.el-breadcrumb__inner) {
    color: var(--el-text-color-regular);

    a {
      color: var(--el-text-color-regular);
      font-weight: normal;
      cursor: pointer;

      &:hover {
        color: var(--el-color-primary);
      }
    }
  }

  .no-redirect {
    color: var(--el-text-color-primary);
    cursor: text;
  }
}
</style>
