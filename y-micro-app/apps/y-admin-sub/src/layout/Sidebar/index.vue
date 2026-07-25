<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import usePermissionStore from '@/store/modules/permission'
import type { RouteRecordRaw } from 'vue-router'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

const menuList = computed(() => {
  return permissionStore.routes.filter(
    (r: RouteRecordRaw) => r.meta?.title && !r.meta?.hidden
  )
})

const activeMenu = computed(() => route.path)

const handleSelect = (path: string) => {
  router.push(path)
}
</script>

<template>
  <div class="sidebar-container">
    <div class="logo">忆  ·  后台管理</div>
    <el-menu
      :default-active="activeMenu"
      :collapse="false"
      background-color="#001529"
      text-color="#ffffffa6"
      active-text-color="#fff"
      @select="handleSelect"
    >
      <template v-for="menu in menuList" :key="menu.path">
        <!-- 有子菜单 -->
        <el-sub-menu
          v-if="menu.children && menu.children.length > 0"
          :index="menu.path"
        >
          <template #title>
            <span>{{ menu.meta?.title }}</span>
          </template>
          <el-menu-item
            v-for="child in menu.children.filter((c: any) => !c.hidden)"
            :key="child.path"
            :index="(menu.path + '/' + child.path).replace('//', '/')"
          >
            {{ child.meta?.title }}
          </el-menu-item>
        </el-sub-menu>
        <!-- 无子菜单 -->
        <el-menu-item v-else :index="menu.path">
          {{ menu.meta?.title }}
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<style scoped lang="scss">
.sidebar-container {
  width: 220px;
  height: 100vh;
  background: #001529;
  overflow-y: auto;

  .logo {
    height: 56px;
    line-height: 56px;
    text-align: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  :deep(.el-menu) {
    border-right: none;
  }
}
</style>