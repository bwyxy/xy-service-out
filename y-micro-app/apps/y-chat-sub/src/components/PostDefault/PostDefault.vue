<script setup lang="ts">
import {Plus, Edit} from '@element-plus/icons-vue'
import {ref, watch} from "vue";
import { formatRelativeTime } from '@/utils/utils.ts'
import { getPostDetail, setPostLike } from '@/api/forum.ts'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  post?: ForumPost
}>()

const isShow = defineModel('modelValue', {
  type: Boolean
})

const defaultCover = new URL('@/assets/images/logo.jpg', import.meta.url).toString()
const postInfo = ref<ForumPost>({} as ForumPost)
const likeLoading = ref(false)

const loadDetail = async () => {
  if (!props.post?.postId) return
  const response = await getPostDetail(props.post.postId)
  if (response.code === 200) {
    postInfo.value = response.data
  } else {
    ElMessage.error(response.msg || '获取帖子详情失败')
  }
}

const handleClose = () => {
  isShow.value = false
}

const handleLike = async () => {
  if (likeLoading.value || !postInfo.value.postId) return
  likeLoading.value = true
  try {
    const response = await setPostLike(postInfo.value.postId)
    if (response.code === 200) {
      postInfo.value.liked = response.data
      let count = postInfo.value.likeCount || 0
      if (response.data) {
        count++
      } else if (count > 0) {
        count--
      }
      postInfo.value.likeCount = count
    } else {
      ElMessage.error(response.msg || '操作失败')
    }
  } finally {
    likeLoading.value = false
  }
}

watch(() => isShow.value, (val) => {
  if (val) {
    postInfo.value = props.post || ({} as ForumPost)
    loadDetail()
  }
})
</script>

<template>
  <div class="postDefault">
    <el-dialog
        v-model="isShow"
        width="1000px"
        :center="true"
        :append-to-body="false"
        :show-close="false"
        :align-center="true"
        modal-class="post-model"
        :before-close="handleClose"
    >
      <div class="post-default">
        <div class="post-default-cover">
          <el-image class="cover-image" :src="postInfo.coverImage || defaultCover">
            <template #error>
              <el-image class="cover-image" :src="defaultCover" />
            </template>
          </el-image>
        </div>
        <div class="post-default-rt">
          <div class="post-default-header">
            <div class="user-info">
              <el-image class="user-head" :src="postInfo.avatar">
                <template #error>
                  <el-image class="user-head" :src="defaultCover"></el-image>
                </template>
              </el-image>
              <span>{{ postInfo.nickname || '未知' }}</span>
            </div>
            <el-button
              :icon="Plus"
              type="primary"
              round
            >关注</el-button>
          </div>

          <div class="post-default-content">
            <div class="post-title">
              <span>{{ postInfo.title || '标题' }}</span>
            </div>
            <div class="post-content">
              <span>{{ postInfo.content || '内容' }}</span>
            </div>

            <div class="post-time-ip">
              <span>{{ formatRelativeTime(postInfo.publishedTime || '') }}</span>
              <span>{{ postInfo.ipAddr || 'ip' }}</span>
            </div>

            <div class="comment-count">
              <span>共{{ postInfo.commentCount || 0 }}条评论</span>
            </div>
          </div>

          <div class="post-default-footer">
            <div class="not-comment-box">
              <div class="not-comment-input">
                <el-icon><Edit /></el-icon>
                <span>🚂想说点什么</span>
              </div>
              <div class="post-option" @click="handleLike">
                <div class="post-option-icon">
                  <img v-if="!postInfo.liked" src="@/assets/images/like.png" alt="icon" />
                  <img v-else src="@/assets/images/like_avtive.png" alt="icon" />
                </div>
                <span>{{ postInfo.likeCount || 0 }}</span>
              </div>
              <div class="post-option">
                <div class="post-option-icon">
                  <img src="@/assets/images/favorite.png" alt="icon" />
                </div>
                <span>{{ postInfo.favoriteCount || 0 }}</span>
              </div>
              <div class="post-option">
                <div class="post-option-icon">
                  <img src="@/assets/images/comment.png" alt="icon" />
                </div>
                <span>{{ postInfo.commentCount || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.post-default {
  width: 100%;
  height: 550px;
  display: flex;

  &-cover {
    width: 55%;
    height: 100%;
    .cover-image {
      width: 100%;
      height: 100%;
    }
  }

  &-rt {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  &-header {
    height: 60px;
    box-sizing: border-box;
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    padding: 0 24px;
    display: flex;
    align-items: center;
    .user-info {
      flex: 1;
      display: flex;
      align-items: center;
      .user-head {
        width: 35px;
        height: 35px;
        border-radius: 50px;
      }
      span {
        font-size: 16px;
        color: #1f1f1f;
        letter-spacing: 1px;
        margin-left: 4px;
      }
    }
  }

  &-content {
    flex: 1;
    box-sizing: border-box;
    padding: 8px 24px;
    overflow: hidden;
    overflow-y: auto;

    &::-webkit-scrollbar {
      appearance: none;
      width: 0;
    }
  }

  &-footer {
    min-height: 60px;
    box-sizing: border-box;
    border-top: 1px solid rgba(0, 0, 0, .06);
    padding: 0 24px;
    .not-comment-box {
      height: 100%;
      display: flex;
      align-items: center;
    }
  }
}

.not-comment-input {
  flex: 1;
  height: 40px;
  background: rgba(0, 0, 0, .04);
  border-radius: 50px;
  border: 1px solid rgba(0, 0, 0, .06);
  box-sizing: border-box;
  padding: 0 13px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  span {
    font-size: 13px;
    color: #888;
    letter-spacing: 1px;
    margin-left: 4px;
  }
}

.post-time-ip {
  display: flex;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(0, 0, 0, .06);
  span {
    font-size: 12px;
    color: #8c8c8c;
    letter-spacing: 1px;
    margin-right: 6px;
    &:last-child {
      margin-right: 0;
    }
  }
}

.comment-count {
  font-size: 14px;
  color: #1f1f1f;
  letter-spacing: 1px;
  margin: 13px 0;
}

.post-option {
  cursor: pointer;
  display: flex;
  align-items: center;
  margin-right: 12px;
  &-icon {
    width: 20px;
    height: 20px;
    margin-right: 6px;
    img {
      width: 100%;
      height: 100%;
    }
  }
  span {
    font-size: 16px;
    letter-spacing: 1px;
    color: #888;
  }
  &:last-child {
    margin-right: 0;
  }
}

:deep(.el-dialog) {
  padding: 0 !important;
  border-radius: 16px;
  overflow: hidden;
  .el-dialog__header {
    padding: 0;
  }
}

:deep(.el-overlay) {
  background: rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}
</style>
