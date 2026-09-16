<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import usePermissionStore from '@/store/modules/permission'
import type { RouteRecordRaw } from 'vue-router'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

/**
 * 获取显示的路由（仿若依常规模板）
 * 过滤掉 hidden 的路由
 */
const routes = computed(() => {
  return permissionStore.routes.filter(
    (r: RouteRecordRaw) => !r.hidden && !r.meta?.hidden
  )
})

/**
 * 解析路径（仿若依 resolvePath）
 * 处理父路径 + 子路径拼接
 */
const resolvePath = (parentPath: string, childPath: string): string => {
  if (childPath.startsWith('/')) {
    return childPath
  }
  return `${parentPath}/${childPath}`.replace(/\/+/g, '/')
}

/**
 * 获取菜单项（仿若依 getMenuItem）
 * 判断是否只有一个子菜单：如果只有一个可见子菜单且 alwaysShow 不为 true，直接显示子菜单
 */
const hasOneShowingChild = (children: RouteRecordRaw[]): boolean => {
  const showingChildren = children.filter(
    (c: RouteRecordRaw) => !c.hidden && !c.meta?.hidden
  )
  return showingChildren.length === 1
}

/** 当前激活菜单 */
const activeMenu = computed(() => route.path)

/** 点击菜单项 */
const handleSelect = (path: string) => {
  if (path === route.path) return
  router.push(path)
}
</script>

<template>
  <div class="sidebar-container">
    <el-menu
      :default-active="activeMenu"
      :collapse="false"
      mode="vertical"
      text-color="#fff"
      background-color="#597ef7"
      @select="handleSelect"
    >
      <template v-for="menu in routes" :key="menu.path">
        <!-- 有子菜单且 alwaysShow=true 或子菜单 > 1 -->
        <el-sub-menu
          v-if="
            menu.children &&
            menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden).length >= 1 &&
            (menu.meta?.alwaysShow || !hasOneShowingChild(menu.children))
          "
          :index="resolvePath(menu.path, menu.children[0]?.path || '')"
        >
          <template #title>
            <span>{{ menu.meta?.title }}</span>
          </template>
          <el-menu-item
            v-for="child in menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden)"
            :key="child.path"
            :index="resolvePath(menu.path, child.path)"
          >
            {{ child.meta?.title }}
          </el-menu-item>
        </el-sub-menu>

        <!-- 只有一个子菜单且 alwaysShow 不为 true：直接显示子菜单 -->
        <el-menu-item
          v-else-if="
            menu.children &&
            menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden).length === 1
          "
          :index="resolvePath(menu.path, menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden)[0].path)"
        >
          {{ menu.children.filter((c: any) => !c.hidden && !c.meta?.hidden)[0].meta?.title }}
        </el-menu-item>

        <!-- 无子菜单：直接显示 -->
        <el-menu-item v-else :index="menu.path">
          {{ menu.meta?.title }}
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<style scoped lang="scss">
.sidebar-container {
  width: 200px;
  height: 100%;
  background: #030852;
  box-shadow: rgba(0, 0, 0, 0.04) 4px 2px 6px 0, rgba(0, 0, 0, 0.04) -4px -2px 6px 0px;
  //border-radius: size(14);
  box-sizing: border-box;
  //margin-right: size(24);
  //padding: size(14);
  overflow-y: auto;

  :deep(.el-menu) {
    border-right: none;
    background: transparent;
    color: #fff;
  }
}
</style>