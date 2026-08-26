<script setup lang="ts">
import {Plus, Search} from '@element-plus/icons-vue'
import {ref} from "vue";
import SearchUserDialog from './SearchUserDialog.vue'
import FriendRequestDialog from './FriendRequestDialog.vue'

const searchVal = ref<string>('')
const searchDialogVisible = ref(false)
const requestDialogVisible = ref(false)
const searchBoxRefs = ref<HTMLElement>()

const openSearchDialog = () => {
  searchDialogVisible.value = true
}
</script>

<template>
  <div class="search-fun">
    <div class="search-fun-content">
      <el-popover :append-to="searchBoxRefs" :teleported="false" trigger="click">
        <template #reference>
          <el-input
              v-model="searchVal"
              :prefix-icon="Search"
              placeholder="搜索"
              class="search-box"
          />
        </template>
        <div>
          <div class="search-btn">
            <el-icon><Search /></el-icon>
            <span>查找{{ searchVal }}</span>
          </div>
          <div class="search-label">
            <span>最近在搜索</span>
          </div>
        </div>
      </el-popover>
    </div>

    <div class="action-icons">
      <el-dropdown>
        <div class="icon-btn" @click="openSearchDialog">
          <el-icon><Plus /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>添加好友</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <search-user-dialog v-model:visible="searchDialogVisible" />
    <friend-request-dialog v-model:visible="requestDialogVisible" />
  </div>
</template>

<style scoped lang="scss">
.search-fun {
  height: size(70);
  box-sizing: border-box;
  padding: 0 size(14);
  border-bottom: 1px solid rgba(0, 0, 0, .06);
  display: flex;
  align-items: center;
  &-content {
    flex: 1;
    .el-popper {
      width: 100% !important;
    }
  }
  .search-box {
    flex: 1;
    height: size(36);
    background: #e6e6e6;
    box-sizing: border-box;
    .el-input__inner {
      background: #e6e6e6;
    }
  }
  .action-icons {
    display: flex;
    align-items: center;
    margin-left: size(14);
    gap: size(12);
  }
  .icon-btn {
    width: size(34);
    height: size(34);
    border-radius: 50px;
    font-size: size(16);
    color: #bfbfbf;
    cursor: pointer;
    border: 1px solid #dbdbdb;
    @include flexCenter;
    &:hover {
      background: rgba(0, 0, 0, .02);
    }
  }
}

.search-fun-content {
  position: relative;
  :deep(.el-popper) {
    width: 100% !important;
  }
  .search-btn {
    font-size: size(12);
    color: #8c8c8c;
    letter-spacing: 1px;
    display: flex;
    align-items: center;
    box-sizing: border-box;
    cursor: pointer;
    padding: size(8) size(14) size(8) size(2);
    span {
      margin-left: size(4);
    }
    &:hover {
      background: rgba(0, 0, 0, .06);
    }
  }
  .search-label {
    padding: size(8) 0;
    border-bottom: 1px solid #f0f0f0;
    span {
      font-size: size(12);
      color: #8c8c8c;
      letter-spacing: 1px;
    }
  }
}
</style>