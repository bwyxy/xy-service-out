<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getArticles } from '@/api/blog/article'
import { getCategorys } from '@/api/blog/category'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import DispatchDialog from '@/components/DispatchDialog/index.vue'
import { EditPen, Plus, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const baseTableRef = ref()
const categoryOptions = ref<any[]>([])

const dispatchVisible = ref(false)
const dispatchArticleIds = ref<number[]>([])

const statusOptions: { label: string; value: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }[] = [
  { label: '草稿', value: '0', type: 'info' },
  { label: '已发布', value: '1', type: 'success' },
  { label: '私密', value: '2', type: 'warning' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'title',
    label: '文章标题',
    prop: 'title',
    render: { el: 'search' }
  },
  {
    key: 'summary',
    label: '摘要',
    prop: 'summary'
  },
  {
    key: 'categoryName',
    label: '分类',
    prop: 'categoryName'
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: statusOptions.map(item => ({ label: item.label, value: item.value, elTagType: item.type }))
    }
  },
  {
    key: 'viewCount',
    label: '浏览量',
    prop: 'viewCount'
  },
  {
    key: 'likeCount',
    label: '点赞数',
    prop: 'likeCount'
  },
  {
    key: 'commentCount',
    label: '评论数',
    prop: 'commentCount'
  },
  {
    key: 'publishedTime',
    label: '发布时间',
    prop: 'publishedTime'
  }
])

const loadCategories = async () => {
  const res = await getCategorys({ pageNum: 1, pageSize: 999 })
  if (res.code === 200) {
    categoryOptions.value = res.data?.list || []
  }
}

const getCategoryName = (categoryId: number) => {
  const category = categoryOptions.value.find(item => item.categoryId === categoryId)
  return category ? category.name : categoryId
}

const handleAdd = () => {
  router.push('/blog/article/add')
}

const handleEdit = (row: any) => {
  router.push(`/blog/article/edit?id=${row.articleId}`)
}

/** 单篇分发 */
const handleDispatch = (row: any) => {
  dispatchArticleIds.value = [row.articleId]
  dispatchVisible.value = true
}

/** 批量分发 */
const handleBatchDispatch = () => {
  const selection = baseTableRef.value?.getSelection() || []
  if (selection.length === 0) {
    ElMessage.warning('请先选择需要分发的文章')
    return
  }
  dispatchArticleIds.value = selection.map((item: any) => item.articleId)
  dispatchVisible.value = true
}

const handleDispatchSuccess = () => {
  dispatchVisible.value = false
}

onMounted(() => {
  loadCategories()
})
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="getArticles" selectable>
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加文章</el-button>
        <el-button :icon="Promotion" plain type="primary" @click="handleBatchDispatch">批量分发</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Promotion" size="small" text type="success" @click="handleDispatch(row)">分发</el-button>
      </template>
      <template #categoryName="{ row }">
        <span>{{ getCategoryName(row.categoryId) }}</span>
      </template>
    </base-table>

    <dispatch-dialog
      v-model="dispatchVisible"
      :article-ids="dispatchArticleIds"
      @success="handleDispatchSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
</style>