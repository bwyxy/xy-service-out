<script setup lang="ts">
import {Plus, Search} from '@element-plus/icons-vue'
import {ref} from "vue";

const searchVal = ref<string>('')
</script>

<template>
  <div class="search-fun">
    <el-input
        v-model="searchVal"
        :prefix-icon="Search"
        placeholder="搜索"
        class="search-box"
    />

    <div class="add-icon">
      <el-icon><Plus /></el-icon>
    </div>
  </div>
</template>

<style scoped lang="scss">
.search-fun {
  box-sizing: border-box;
  padding: size(24);
  border-bottom: 1px solid rgba(0, 0, 0, .06);
  display: flex;
  align-items: center;
  .search-box {
    flex: 1;
    height: size(48);
    box-sizing: border-box;
    .el-input__inner {
      background: #f0f0f0;
    }
  }
  .add-icon {
    width: size(48);
    height: size(48);
    background: #f0f0f0;
    border-radius: size(6);
    margin-left: size(14);
    cursor: pointer;
    @include flexCenter;
  }
}
</style>