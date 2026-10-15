<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getDispatchStats, getDispatchStatSummary, syncDispatchStats } from '@/api/blog/dispatch'
import { Refresh, Search, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const summaryLoading = ref(false)
const summaryList = ref<any[]>([])
const statLoading = ref(false)
const statList = ref<any[]>([])
const syncing = ref(false)

const queryParams = reactive<any>({
  keyword: '',
  platformCode: '',
  pageNum: 1,
  pageSize: 15,
  total: 0
})

const loadSummary = async () => {
  summaryLoading.value = true
  try {
    const res = await getDispatchStatSummary()
    if (res.code === 200) {
      summaryList.value = res.data || []
    }
  } finally {
    summaryLoading.value = false
  }
}

const loadStats = async () => {
  statLoading.value = true
  try {
    const res = await getDispatchStats({ ...queryParams })
    if (res.code === 200) {
      statList.value = res.data?.list || []
      queryParams.total = res.data?.total || 0
    }
  } finally {
    statLoading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  loadStats()
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.platformCode = ''
  queryParams.pageNum = 1
  loadStats()
}

const handleSync = async () => {
  syncing.value = true
  try {
    const res = await syncDispatchStats()
    if (res.code === 200) {
      ElMessage.success(res.msg || '同步完成')
      loadSummary()
      loadStats()
    } else {
      ElMessage.error(res.msg || '同步失败')
    }
  } finally {
    syncing.value = false
  }
}

onMounted(() => {
  loadSummary()
  loadStats()
})
</script>

<template>
  <div class="dispatch-stat">
    <div class="page-toolbar">
      <el-button :icon="Refresh" plain @click="loadSummary">刷新概览</el-button>
      <el-button :icon="RefreshRight" type="primary" :loading="syncing" @click="handleSync">立即同步数据</el-button>
    </div>

    <el-table v-loading="summaryLoading" :data="summaryList" border class="summary-table">
      <el-table-column label="平台" min-width="140" align="center">
        <template #default="{ row }">
          <span class="platform-name">{{ row.platformName || row.platformCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已分发文章数" width="130" align="center" prop="articleCount" />
      <el-table-column label="浏览量" width="110" align="center" prop="viewCount" />
      <el-table-column label="点赞数" width="110" align="center" prop="likeCount" />
      <el-table-column label="评论数" width="110" align="center" prop="commentCount" />
      <el-table-column label="收藏数" width="110" align="center" prop="collectCount" />
      <el-table-column label="最近采集时间" min-width="180" align="center">
        <template #default="{ row }">
          <span>{{ row.lastStatTime || '-' }}</span>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无统计数据，分发成功并同步后展示" :image-size="80" />
      </template>
    </el-table>

    <el-divider content-position="left">分发文章明细</el-divider>

    <el-form :inline="true" class="search-form">
      <el-form-item label="文章标题">
        <el-input v-model="queryParams.keyword" placeholder="请输入文章标题" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="平台编码">
        <el-input v-model="queryParams.platformCode" placeholder="如 juejin" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button :icon="Search" type="primary" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" plain @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="statLoading" :data="statList" border>
      <el-table-column label="文章标题" min-width="200" align="center" show-overflow-tooltip prop="articleTitle" />
      <el-table-column label="平台" width="120" align="center" prop="platformCode" />
      <el-table-column label="浏览量" width="100" align="center" prop="viewCount" />
      <el-table-column label="点赞数" width="100" align="center" prop="likeCount" />
      <el-table-column label="评论数" width="100" align="center" prop="commentCount" />
      <el-table-column label="收藏数" width="100" align="center" prop="collectCount" />
      <el-table-column label="平台链接" min-width="200" align="center">
        <template #default="{ row }">
          <el-link v-if="row.resultUrl" type="primary" :href="row.resultUrl" target="_blank">
            {{ row.resultUrl }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="采集时间" width="170" align="center" prop="statTime" />
      <template #empty>
        <el-empty description="暂无数据" :image-size="80" />
      </template>
    </el-table>

    <div class="base-pagination">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        size="small"
        background
        layout="total, prev, pager, next"
        :total="queryParams.total"
        @change="loadStats"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

.summary-table {
  margin-bottom: 8px;
}

.platform-name {
  font-weight: 600;
}

.search-form {
  :deep(.el-form-item) {
    margin-bottom: 14px;
    margin-right: 14px;
  }
}

.base-pagination {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
