<script setup lang="ts">
import {ref} from "vue";
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import useChatStore from "@/store/chat.ts";
import {Plus} from "@element-plus/icons-vue";
import { createGroup } from '@/api/group.ts'

const { userInfo } = storeToRefs(useUserStore())
const { isRefreshConversations } = storeToRefs(useChatStore())
const isAddGroup = ref<boolean>(false);
const friends = ref<any[]>([]);
const isLoading = ref<boolean>(false);

const handleCreateGroup = async () => {
  if (isLoading.value) return;
  isLoading.value = true;

  const members = friends.value.slice(0, 2)
  const memberNames = members.map((m) => m.nickname)
  const memberIds = friends.value.map((item) => item.friendId)
  const groupName = `${userInfo.value.nickname};${memberNames.join(';')}${friends.value.length > 2 ? '等' : '的'}群聊`
  const params = {
    uid: Number(userInfo.value.uid),
    groupName,
    memberIds: memberIds,
    remark: friends.value.map(item => item.nickname).join('、'),
  }
  const response = await createGroup(params)

  isLoading.value = false
  if (response.code === 200) {
    isRefreshConversations.value = true
  }
  clearData()
}

const clearData = () => {
  isAddGroup.value = false
  friends.value = []
}
</script>

<template>
  <div class="add-group">

    <div v-if="!$slots.default" class="add-group-plus" @click="isAddGroup = true">
      <div class="add-group-plus-icon">
        <el-icon><Plus /></el-icon>
      </div>
      <span>发起群聊</span>
    </div>
    <div v-else @click="isAddGroup = true">
      <slot></slot>
    </div>

    <el-dialog
      v-model="isAddGroup"
      width="540px"
      :show-close="false"
    >
      <div class="add-group-content">
        <div class="add-group-lt">
          <friend-list
            :isSelected="true"
            @select-friend="(list) => friends = list"
          />
        </div>
        <div class="add-group-rt">
          <div class="add-group-top">
            <div class="initiate-group">发起群聊</div>
            <span>选择联系人{{ friends.length }}</span>
          </div>

          <div class="add-group-main">
            <div
                v-for="item in friends"
                :key="item.id"
                class="friend-item"
            >
              <img v-if="item.avatar" class="user-head" :src="item.avatar" alt="avatar" />
              <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
              <span>{{ item.nickname || '' }}</span>
            </div>
          </div>

          <div class="add-group-option">
            <el-button
                size="small"
                @click="isAddGroup = false"
            >取消</el-button>

            <el-button
                size="small"
                type="primary"
                :disabled="friends.length < 2"
                v-loading="isLoading"
                @click="handleCreateGroup"
            >完成</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.add-group-plus {
  cursor: pointer;
  margin-left: size(14);
  &-icon {
    width: size(42);
    height: size(42);
    border-radius: size(6);
    box-sizing: border-box;
    border: 1px dashed #bfbfbf;
    color: #bfbfbf;
    background: #fafafa;
    font-size: size(14);
    @include flexCenter;
  }
  span {
    font-size: size(10);
    color: #bfbfbf;
    margin-top: size(4);
    @include textEllipsis(1);
  }
}

:deep(.el-dialog) {
  padding: 0;
  .el-dialog__header {
    padding: 0;
  }
}

.add-group-content {
  height: 540px;
  display: flex;
  .add-group-lt {
    width: 200px;
    background: rgba(0, 0, 0, .06);
  }
  .add-group-rt {
    flex: 1;
    display: flex;
    flex-direction: column;

    .add-group-top {
      padding: 14px 24px;
      display: flex;
      align-items: center;

      .initiate-group {
        flex: 1;
        font-size: 13px;
        font-weight: 700;
        color: #1f1f1f;
        letter-spacing: 1px;
      }

      span {
        color: #888;
        font-size: 10px;
        letter-spacing: 1px;
      }
    }

    .add-group-main {
      flex: 1;
      box-sizing: border-box;
      padding: 0 24px;
      overflow: hidden;

      .friend-item {
        display: flex;
        align-items: center;
        padding: 14px 0;
        .user-head {
          width: 32px;
          height: 32px;
          border-radius: 4px;
        }
        span {
          font-size: 13px;
          color: #1f1f1f;
          letter-spacing: 1px;
          margin-left: 6px;
          @include textEllipsis(1);
        }
      }
      &::-webkit-scrollbar {
        appearance: none;
        width: 0;
      }
    }

    .add-group-option {
      width: 100%;
      padding: 14px 0;
      display: flex;
      justify-content: center;
    }
  }
}
</style>