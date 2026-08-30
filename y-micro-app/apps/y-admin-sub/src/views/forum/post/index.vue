<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Top, View, Warning, Star } from '@element-plus/icons-vue'
import { getPosts, auditPost, topPost, essencePost } from '@/api/forum/post'

const baseTableRefs = ref()
const detailVisible = ref(false)
const currentPost = ref<ForumPost>({})

const postStatusList: Value[] = [
  { label: '草稿', value: 0, elTagType: 'info' },
  { label: '审核中', value: 1, elTagType: 'warning' },
  { label: '已发布', value: 2, elTagType: 'success' },
  { label: '拒绝', value: 3, elTagType: 'danger' },
  { label: '下架', value: 4, elTagType: 'danger' }
]

const visibilityList = [
  { label: '公开', value: 1 },
  { label: '好友可见', value: 2 },
  { label: '私密', value: 3 }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'postId',
    label: '帖子ID',
    prop: 'postId',
    width: 80
  },
  {
    key: 'title',
    label: '标题',
    prop: 'title',
    render: { el: 'search' }
  },
  {
    key: 'authorId',
    label: '作者ID',
    prop: 'authorId',
    width: 90
  },
  {
    key: 'boardId',
    label: '版块ID',
    prop: 'boardId',
    width: 90
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    width: 100,
    type: 'tag',
    render: {
      el: 'select',
      list: postStatusList
    }
  },
  {
    key: 'isTop',
    label: '置顶',
    prop: 'isTop',
    width: 80,
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '普通', value: 0, elTagType: 'info' },
        { label: '置顶', value: 1, elTagType: 'success' }
      ]
    }
  },
  {
    key: 'isEssence',
    label: '精华',
    prop: 'isEssence',
    width: 80,
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '普通', value: 0, elTagType: 'info' },
        { label: '精华', value: 1, elTagType: 'warning' }
      ]
    }
  },
  {
    key: 'viewCount',
    label: '浏览',
    prop: 'viewCount',
    width: 80
  },
  {
    key: 'likeCount',
    label: '点赞',
    prop: 'likeCount',
    width: 80
  },
  {
    key: 'commentCount',
    label: '评论',
    prop: 'commentCount',
    width: 80
  },
  {
    key: 'publishedTime',
    label: '发布时间',
    prop: 'publishedTime',
    width: 160
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime',
    width: 160
  }
])

const handleAudit = (row: ForumPost, status: number) => {
  const statusText = status === 2 ? '通过' : '拒绝'
  ElMessageBox.confirm(`是否确认${statusText}该帖子？`, '提示', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  }).then(async () => {
    const res = await auditPost(row.postId as number, status)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      baseTableRefs.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  }).catch(() => {})
}

const handleTop = (row: ForumPost) => {
  const topText = row.isTop === 1 ? '取消置顶' : '置顶'
  ElMessageBox.confirm(`是否确认${topText}该帖子？`, '提示', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  }).then(async () => {
    const res = await topPost(row.postId as number)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      baseTableRefs.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  }).catch(() => {})
}

const handleEssence = (row: ForumPost) => {
  const essenceText = row.isEssence === 1 ? '取消加精' : '加精'
  ElMessageBox.confirm(`是否确认${essenceText}该帖子？`, '提示', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  }).then(async () => {
    const res = await essencePost(row.postId as number)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      baseTableRefs.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  }).catch(() => {})
}

const handleView = (row: ForumPost) => {
  currentPost.value = row
  detailVisible.value = true
}
</script>

<template>
  <base-table ref="baseTableRefs" :columns="columns" :api="getPosts">
    <template #operate="{ row }">
      <el-button
        text
        type="success"
        :icon="View"
        size="small"
        @click="handleView(row)"
      >详情</el-button>
      <el-button
        v-if="row.status === 1"
        text
        type="primary"
        size="small"
        @click="handleAudit(row, 2)"
      >通过</el-button>
      <el-button
        v-if="row.status === 1"
        text
        type="danger"
        :icon="Warning"
        size="small"
        @click="handleAudit(row, 3)"
      >拒绝</el-button>
      <el-button
        text
        :type="row.isTop === 1 ? 'info' : 'warning'"
        :icon="Top"
        size="small"
        @click="handleTop(row)"
      >{{ row.isTop === 1 ? '取消置顶' : '置顶' }}</el-button>
      <el-button
        text
        :type="row.isEssence === 1 ? 'info' : 'warning'"
        :icon="Star"
        size="small"
        @click="handleEssence(row)"
      >{{ row.isEssence === 1 ? '取消加精' : '加精' }}</el-button>
    </template>
  </base-table>

  <el-dialog v-model="detailVisible" title="帖子详情" width="700px" append-to-body>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="帖子ID">{{ currentPost.postId }}</el-descriptions-item>
      <el-descriptions-item label="作者ID">{{ currentPost.authorId }}</el-descriptions-item>
      <el-descriptions-item label="版块ID">{{ currentPost.boardId }}</el-descriptions-item>
      <el-descriptions-item label="可见性">
        {{ visibilityList.find(item => item.value === currentPost.visibility)?.label || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="浏览数">{{ currentPost.viewCount }}</el-descriptions-item>
      <el-descriptions-item label="点赞数">{{ currentPost.likeCount }}</el-descriptions-item>
      <el-descriptions-item label="评论数">{{ currentPost.commentCount }}</el-descriptions-item>
      <el-descriptions-item label="收藏数">{{ currentPost.favoriteCount }}</el-descriptions-item>
      <el-descriptions-item label="标题" :span="2">{{ currentPost.title }}</el-descriptions-item>
      <el-descriptions-item label="摘要" :span="2">{{ currentPost.summary || '-' }}</el-descriptions-item>
      <el-descriptions-item label="内容" :span="2">
        <div class="post-content">{{ currentPost.content }}</div>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<style scoped lang="scss">
.post-content {
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}
</style>
