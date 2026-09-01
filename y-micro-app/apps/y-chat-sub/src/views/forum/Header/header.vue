<script setup lang="ts">
import {onMounted, reactive, ref} from "vue";
import { useRouter } from "vue-router";
import { Search, UserFilled } from '@element-plus/icons-vue'
import { getBoards } from '@/api/forum.ts'
import useForumStore from "@/store/forum.ts";

const router = useRouter()
const tabs = reactive<ForumBoard[]>([])
const tabActive = ref<string>('recommend')
const forumStore = useForumStore()

const getBoardList = async () => {
  const response = await getBoards({})

  if (response.code === 200) {
    response.data.map((item) => {
      item.key = item.slug
    })
    setCurrentBoardId(response.data)
    tabs.push(...response.data)
  }
}
/** 设置当前版块Id */
const setCurrentBoardId = (list : ForumBoard[]) => {
  const boards = list.find(item => item.key === tabActive.value)

  if (boards) {
    forumStore.currentBoardId = boards.boardId || 0
  } else if (list.length > 0) {
    forumStore.currentBoardId = list[0].boardId
  }
}
const toPage = (url : string) => {
  router.push({
    path: url
  })
}

onMounted(() => getBoardList())
</script>

<template>
  <div class="forum-header">
    <div class="search-box">
      <el-input :prefix-icon="Search" placeholder="查询帖子, 热点" />
    </div>

    <ul class="forum-header-content">
      <li
          v-for="(item, index) in tabs"
          :key="index"
          :class="{'tab-active': tabActive === item.key}"
      >
        <span>{{ item.name }}</span>
        <img src="@/assets/images/tab_icon.png" alt="tabIcon" />
      </li>
    </ul>

    <div class="forum-header-rt">
      <div class="rt-item" @click="toPage('/creationCenter')">
        <el-icon><UserFilled /></el-icon>
        <span>创作中心</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.forum-header {
  height: 70px;
  box-sizing: border-box;
  padding: 0 42px;
  display: flex;
  align-items: center;
  position: relative;
  .search-box {
    width: 320px;
    height: 40px;
    //background: rgba(0, 0, 0, .04);
    border-radius: 10px;
    position: absolute;
    left: 42px;
    :deep(.el-input) {
      height: 100%;
      .el-input__wrapper {
        background: rgba(0, 0, 0, .04);
        border-radius: 10px;
        box-shadow: none;
      }
      .el-input__inner {
        height: 100%;
      }
    }
  }

  &-content {
    flex: 1;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    li {
      width: 80px;
      height: 100%;
      cursor: pointer;
      display: flex;
      align-items: center;
      position: relative;
      span {
        z-index: 1;
        font-size: 16px;
        letter-spacing: 1px;
        text-align: center;
        position: absolute;
      }
      img {
        width: 32px;
        height: 36px;
        transform: rotate(45deg);
        display: none;
        position: absolute;
        right: 26px;
        bottom: 10px;
      }
    }
    .tab-active {
      span {
        font-weight: 600;
      }
      img {
        display: block;
      }
    }
  }

  &-rt {
    position: absolute;
    right: 42px;
    .rt-item {
      font-size: 14px;
      color: #1f1f1f;
      letter-spacing: 1px;
      cursor: pointer;
      display: flex;
      align-items: center;
      span {
        margin-left: 6px;
      }
    }
  }
}
</style>