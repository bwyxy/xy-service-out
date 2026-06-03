<script setup>
import {onMounted, reactive, ref} from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '@/api/article'

const router = useRouter()
const statusType = reactive([
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '私密' },
])
const params = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  status: '',
  loading: false,
  total: 0
})
const queryRef = ref(null)
const list = ref([])

const getList = async () => {
  params.loading = true
  try {
    const res = await getArticleList(params)

    list.value = res.list
    params.total = res.total
  } catch (e) {
    console.error(e)
  } finally {
    params.loading = false
  }
}

const optionArticle = (id) => {
  if (id) {
    router.push({
      path: '/article/edit',
      query: {
        id: id
      }
    })
  } else {
    router.push({
      path: '/article/add'
    })
  }
}

const reset = () => {
  queryRef.value.resetFields()
}

const statusText = (status) => {
  const obj = statusType.find((item) => item.value == status) || {}

  return obj.label || ''
}

onMounted(() => {
  getList()
})
</script>

<template>
  <div class="app-container">
    <el-form :model="params" ref="queryRef" inline>
      <el-form-item label="标题" prop="title">
        <el-input v-model="params.title" placeholder="标题"></el-input>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="params.status" placeholder="状态">
          <el-option
            v-for="item in statusType"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search">搜索</el-button>
        <el-button icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="optionArticle('')"
        >新增</el-button>
      </el-col>
    </el-row>

    <el-table
        v-loading="params.loading"
        :data="list"
    >
      <el-table-column label="ID" property="articleId" align="center" />
      <el-table-column label="标题" property="title" align="center" show-overflow-tooltip />
      <el-table-column label="封面" align="center">
        <template #default="{ row }">
          <el-image style="width: 60px;height: 40px" :src="row.coverImage" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center">
        <template #default="{ row }">
          <span>{{ statusText(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" property="publishedTime" align="center" />
      <el-table-column label="操作" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="optionArticle(row.articleId)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="scss">

</style>
