<script setup lang="ts">
import {Plus, Search} from '@element-plus/icons-vue'
import {ref} from "vue";
import SearchUserDialog from './SearchUserDialog.vue'
import FriendRequestDialog from './FriendRequestDialog.vue'

const searchVal = ref<string>('')
const searchDialogVisible = ref(true)
const requestDialogVisible = ref(false)

const openSearchDialog = () => {
  searchDialogVisible.value = true
}
</script>

<template>
  <div class="search-fun">
    <el-input
        v-model="searchVal"
        :prefix-icon="Search"
        placeholder="搜索"
        class="search-box"
    />

    <div class="action-icons">
<!--      <div class="icon-btn" title="好友请求" @click="openRequestDialog">-->
<!--        <el-icon><UserFilled /></el-icon>-->
<!--      </div>-->
      <div class="icon-btn" title="添加好友" @click="openSearchDialog">
        <el-icon><Plus /></el-icon>
      </div>
    </div>

    <search-user-dialog v-model:visible="searchDialogVisible" />
    <friend-request-dialog v-model:visible="requestDialogVisible" />
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
  .action-icons {
    display: flex;
    align-items: center;
    margin-left: size(14);
    gap: size(12);
  }
  .icon-btn {
    width: size(48);
    height: size(48);
    background: #f0f0f0;
    border-radius: size(6);
    cursor: pointer;
    @include flexCenter;
    &:hover {
      background: #e6e6e6;
    }
  }
}
</style>