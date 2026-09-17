<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import usePermissionStore from '@/store/modules/permission'
import useAppStore from '@/store/modules/app'
import MenuIcon from '@/components/MenuIcon/index.vue'
import type { RouteRecordRaw } from 'vue-router'

type MenuRoute = RouteRecordRaw & {
  hidden?: boolean
  meta?: Record<string, any>
}

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const appStore = useAppStore()

const routes = computed(() => {
  return (permissionStore.routes as MenuRoute[]).filter(
    (r) => !r.hidden && !r.meta?.hidden
  )
})

const activeMenu = computed(() => route.path)

const resolvePath = (parentPath: string, childPath: string): string => {
  if (childPath.startsWith('/')) {
    return childPath
  }
  return `${parentPath}/${childPath}`.replace(/\/+/g, '/')
}

const hasOneShowingChild = (children: MenuRoute[]): boolean => {
  const showingChildren = children.filter(
    (c) => !c.hidden && !c.meta?.hidden
  )
  return showingChildren.length === 1
}

const handleSelect = (path: string) => {
  if (path === route.path) {
    if (appStore.isMobile) appStore.closeSidebar()
    return
  }
  router.push(path)
  if (appStore.isMobile) appStore.closeSidebar()
}
</script>

<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="false"
    mode="vertical"
    @select="handleSelect"
  >
    <template v-for="menu in routes" :key="menu.path">
      <el-sub-menu
        v-if="
          menu.children &&
          menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden).length >= 1 &&
          (menu.meta?.alwaysShow || !hasOneShowingChild(menu.children))
        "
        :index="resolvePath(menu.path, menu.children[0]?.path || '')"
      >
        <template #title>
          <MenuIcon :icon="menu.meta?.icon" />
          <span>{{ menu.meta?.title }}</span>
        </template>
        <el-menu-item
          v-for="child in menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden)"
          :key="child.path"
          :index="resolvePath(menu.path, child.path)"
        >
          <MenuIcon :icon="child.meta?.icon" />
          <span>{{ child.meta?.title }}</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item
        v-else-if="
          menu.children &&
          menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden).length === 1
        "
        :index="resolvePath(menu.path, menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden)[0].path)"
      >
        <MenuIcon :icon="menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden)[0].meta?.icon" />
        <span>{{ menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden)[0].meta?.title }}</span>
      </el-menu-item>

      <el-menu-item v-else :index="menu.path">
        <MenuIcon :icon="menu.meta?.icon" />
        <span>{{ menu.meta?.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>
