<script setup lang="ts">

import {Plus} from "@element-plus/icons-vue";
</script>

<template>
  <div class="list-top">
    <div class="list-top-search"></div>

    <el-dropdown :teleported="false" trigger="click">
      <div>
        <el-icon><Plus /></el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item>Action 1</el-dropdown-item>
          <el-dropdown-item>Action 2</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style lang="scss" scoped>
.list-top {
  padding-bottom: $padding;
  border-bottom: 1px solid $border-color-1;
}
</style>
