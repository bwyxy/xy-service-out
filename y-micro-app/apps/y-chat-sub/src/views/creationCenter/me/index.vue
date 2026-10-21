<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import {onMounted, reactive, ref} from "vue";
import WaterfallList from "@/components/WaterfallList/WaterfallList.vue";
import { getMePosts, getStat } from '@/api/forum.ts'

const { userInfo } = storeToRefs(useUserStore())

const params = reactive({
  pageNum: 1,
  pageSize: 30,
})
const currentTab = ref<number>(1)
const postTabs = reactive([
  {
    id: 1,
    key: '',
    name: '我的'
  },
  {
    id: 2,
    key: '',
    name: '关注'
  },
  {
    id: 3,
    key: '',
    name: '点赞'
  },
  {
    id: 4,
    key: '',
    name: '收藏'
  }
])
const posts = ref<ForumPost[]>([])
const isLoading = ref<boolean>(false)
const creatorStat = ref<CreatorStat>({} as CreatorStat)

const getPosts = async () => {
  if (isLoading.value) return;
  isLoading.value = true

  let response = {} as R<any>;
  if (currentTab.value === 1) {
    response = await getMePosts(params.pageNum, params.pageSize);
  }

  if (response.code === 200) {
    posts.value = response.data.list;
  }

  isLoading.value = false
}

const getCreatorStat = async () => {
  const response = await getStat()

  if (response.code === 200) {
    creatorStat.value = response.data
  }
}

const handleTabs = (id :number) => {
  currentTab.value = id
}

onMounted(() => {
  getPosts()
  getCreatorStat()
})
</script>

<template>
  <div class="me">
    <div class="me-top">
      <div class="me-avatar">
        <el-image class="user-head" :src="userInfo.avatar">
          <template #error>
            <img class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
          </template>
        </el-image>
      </div>
      <div class="me-info">
        <span class="user-name">{{ userInfo.nickname }}</span>
        <ul>
          <li>{{ creatorStat.followerCount }}粉丝</li>
          <li>{{ creatorStat.followingCount }}关注</li>
          <li>{{ creatorStat.likeCount + creatorStat.favoriteCount }}点赞与收藏</li>
          <li>{{ creatorStat.commentCount }}评论</li>
        </ul>
      </div>
    </div>

    <div class="post-tab">
      <div
          v-for="(item, index) in postTabs"
          :key="index"
          :class="['post-tab-item', {'post-tab-item_active': currentTab === item.id}]"
          @click="handleTabs(item.id)"
      >
        <span>{{ item.name }}</span>
      </div>
    </div>

    <div class="me-content">
      <el-empty v-if="posts.length === 0" description="暂无帖子" />

      <waterfall-list
          :list="posts"
          :isLoading="isLoading"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.me {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  &-content {
    flex: 1;
    overflow: hidden;
    overflow-y: scroll;
    &::-webkit-scrollbar {
      appearance: none;
      width: 0;
    }
  }
}

.me-top {
  margin: size(42) size(32) 0;
  display: flex;
}

.me-avatar {
  width: 70px;
  height: 70px;
  position: relative;
  .user-head {
    width: 70px;
    height: 70px;
    border-radius: 50px;
    z-index: 1;
    position: absolute;
    left: 0;
    top: 0;
  }
  &:after {
    content: '';
    width: 78px;
    height: 78px;
    border-radius: 50px;
    background: rgba(82, 196, 26, 0.2);
    position: absolute;
    left: -4px;
    top: -4px;
  }
}

.me-info {
  display: flex;
  flex-direction: column;
  margin-left: 24px;
  .user-name {
    flex: 1;
    font-size: 20px;
    font-weight: 700;
    color: #1f1f1f;
    letter-spacing: 2px;
    box-sizing: border-box;
    padding-top: 6px;
  }
  ul {
    display: flex;
    li {
      font-size: 12px;
      color: #8c8c8c;
      letter-spacing: 1px;
      margin-right: 24px;
      &:last-child {
        margin-right: 0;
      }
    }
  }
}

.post-tab {
  display: flex;
  margin: 24px 0 24px 24px;
  &-item {
    width: 100px;
    height: 30px;
    box-sizing: border-box;
    border: 1px solid #888888;
    border-radius: 50px;
    cursor: pointer;
    margin-right: 24px;
    @include flexCenter;
    span {
      color: #888;
      font-size: 12px;
      letter-spacing: 1px;
    }
    &_active {
      border: none !important;
      background: $theme-color !important;
      span {
        color: #fff !important;
      }
    }
    &:last-child {
      margin-right: 0;
    }
    &:hover {
      background: var(--theme-color-light-9);
      border-color: var(--theme-color-light-5);
      span {
        color: var(--theme-color);
      }
    }
  }
}
</style>