<script setup lang="ts">
import { V3waterfall } from 'v3-waterfall'
import 'v3-waterfall/dist/style.css'
import {computed, ref} from "vue";

const forums = ref<ForumPost[]>([{
  postId: 0,
  authorId: 0,
  boardId: 0,
  title: '',
  summary: '',
  content: '',
  contentType: 0,
  coverImage: '',
  status: 0,
  visibility: 0,
  isTop: 0,
  isEssence: 0,
  viewCount: 0,
  likeCount: 0,
  commentCount: 0,
  favoriteCount: 0,
  shareCount: 0,
  publishedTime: '',
  createTime: '',
  updateTime: ''
},{
  postId: 0,
  authorId: 0,
  boardId: 0,
  title: '',
  summary: '',
  content: '',
  contentType: 0,
  coverImage: '',
  status: 0,
  visibility: 0,
  isTop: 0,
  isEssence: 0,
  viewCount: 0,
  likeCount: 0,
  commentCount: 0,
  favoriteCount: 0,
  shareCount: 0,
  publishedTime: '',
  createTime: '',
  updateTime: ''
}])
const forumListRefs = ref<HTMLDivElement>()

/** 设置瀑布流item 宽度 */
const getColWidth = computed(() => {
  const colWidth = forumListRefs.value?.clientWidth || window.innerWidth

  const itemCount = colWidth / 180

  return colWidth / itemCount
})
/** 设置喜欢数量显示 */
const setLikeCount = (count: number) => {
  if (count >= 9999) {
    return '9999+'
  } else {
    return count || 9999
  }
}
</script>

<template>
  <div ref="forumListRefs" class="forum-list">
    <v3waterfall
      :list="forums"
      :colWidth="getColWidth"
      :bottomGap="24"
      :virtual-time="400"
    >
      <template v-slot:default="{ item }">
        <div class="post-item">
          <el-image class="post-cover" :src="item.coverImage">
            <template #error>
              <div class="post-cover-error"></div>
            </template>
          </el-image>

          <div class="post-title">
            <span>{{ item.title || 'title' }}</span>
          </div>

          <div class="post-item-btm">
            <div class="post-info">
              <el-image class="user-avatar" :src="item.avatar">
                <template #error>
                  <div class="avatar-error"></div>
                </template>
              </el-image>
              <span>{{ item.nickname || '未知用户' }}</span>
            </div>
            <div class="post-like">
              <span>{{ setLikeCount(item.likeCount || 0) }}</span>
            </div>
          </div>
        </div>
      </template>
    </v3waterfall>
  </div>
</template>

<style scoped lang="scss">
.forum-list {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  overflow: hidden;
  overflow-y: scroll;
  &::-webkit-scrollbar {
    appearance: none;
  }
}

.post-item {
  width: 100%;
  height: 100%;
  cursor: pointer;
  .post-cover {
    width: 100%;
    box-sizing: border-box;
    border-radius: 13px;
    border: 1px solid #dbdbdb;
    &-error {
      width: 100%;
      height: 220px;
      background: #f0f0f0;
    }
  }

  .user-avatar {
    width: 22px;
    height: 22px;
    border-radius: 50px;
    .avatar-error {
      width: 100%;
      height: 100%;
      background: #f0f0f0;
    }
  }

  .post-title {
    font-size: 18px;
    color: #1f1f1f;
    letter-spacing: 1px;
    box-sizing: border-box;
    padding: 0 10px;
    margin: 14px 0;
    @include textEllipsis(2);
  }

  .post-item-btm {
    display: flex;
    align-items: center;
    box-sizing: border-box;
    padding: 0 8px;
    .post-info {
      flex: 1;
      display: flex;
      align-items: center;
      span {
        font-size: 10px;
        color: #888;
        letter-spacing: 1px;
        @include textEllipsis(1);
        margin-left: 4px;
      }
    }

    .post-like {
      display: flex;
      cursor: pointer;
      span {
        font-size: 10px;
        color: #888;
        letter-spacing: 1px;
      }
    }
  }
}
</style>