<script setup lang="ts">
import { getFriendList } from '@/api/friend.ts'
import { getUserInfo } from '@/api/user.ts'
import {onMounted, ref} from "vue";
import {ChatRound, Phone, VideoCamera} from "@element-plus/icons-vue";

const friends = ref<any[]>([])
const friendInfo = ref<any>(null)

/** 获取好友列表 */
const fetchFriends = async () => {
  const response = await getFriendList()

  if (response.code === 200) {
    friends.value = response.data
  }
}

/** 获取联系人详细信息 */
const getInfo = async (uId: string) => {
  const response = await getUserInfo(Number(uId))

  if (response.code === 200) {
    friendInfo.value = response.data
  }
}

onMounted(() => {
  fetchFriends()
})
</script>

<template>
  <div class="friend">
    <div class="friend-list">
      <div class="friend-tiler">
        <span>联系人列表</span>
      </div>
      <div
          v-for="item in friends"
          :key="item.id"
          class="friend-item"
          @click="getInfo(item.friendId)"
      >
        <img v-if="item.avatar" class="user-head" :src="item.avatar" alt="avatar" />
        <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
        <span>{{ item.nickname || '' }}</span>
      </div>
    </div>

    <div class="friend-main">
      <div v-if="friendInfo" class="friend-main-content">
        <div class="user-box">
          <img v-if="friendInfo.avatar" class="user-head" :src="friendInfo.avatar" alt="avatar" />
          <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
          <div class="user-box-info">
            <span>{{ friendInfo.nickname }}</span>
            <span>{{ friendInfo.sex === '1' ? '男' : friendInfo.sex === '2' ? '女' : '未知' }}</span>
          </div>
        </div>

        <div class="user-chat">
          <div class="user-chat-item">
            <el-icon><ChatRound /></el-icon>
            <span>发消息</span>
          </div>
          <div class="user-chat-item">
            <el-icon><Phone /></el-icon>
            <span>语言聊天</span>
          </div>
          <div class="user-chat-item">
            <el-icon><VideoCamera /></el-icon>
            <span>视频聊天</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.friend {
  width: 100%;
  height: 100%;
  display: flex;
}

.friend-list {
  width: size(320);
  height: 100%;
  background-color: #fafafa;
  border-right: 1px solid rgba(0, 0, 0, .06);
  box-sizing: border-box;
  padding: size(24);
  display: flex;
  flex-direction: column;
  overflow-y: scroll;
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }

  .friend-tiler {
    font-size: size(20);
    font-weight: 700;
    color: #1f1f1f;
    letter-spacing: 1px;
  }

  .friend-item {
    display: flex;
    align-items: center;
    padding: size(24) 0;
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    cursor: pointer;

    span {
      font-size: size(20);
      font-weight: 500;
      letter-spacing: 1px;
      @include textEllipsis(1);
    }
  }
}

.user-head {
  width: size(60);
  height: size(60);
  border-radius: size(8);
  margin-right: size(13);
}

.friend-main {
  flex: 1;
  @include flexCenter;
  &-content {
    width: size(480);
  }
  .user-box {
    display: flex;
    align-items: center;
    padding-bottom: size(24);
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    .user-box-info {
      margin-left: size(8);
      display: flex;
      flex-direction: column;
      span:nth-child(1) {
        font-size: size(22);
        color: #1f1f1f;
        font-weight: 700;
        letter-spacing: 1px;
        @include textEllipsis(1);
      }
      span:nth-child(2) {
        font-size: size(16);
        color: #888;
        margin-top: size(8);
      }
    }
  }
}

.user-chat {
  display: flex;
  margin-top: size(13);
  &-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: size(24) 0;
    .el-icon {
      font-size: size(36);
      color: $theme-color;
    }
    span {
      font-size: size(16);
      color: #888;
      letter-spacing: 1px;
      margin-top: size(8);
    }
    &:hover {
      cursor: pointer;
      background: rgba(0, 0, 0, .02);
    }
  }
}
</style>