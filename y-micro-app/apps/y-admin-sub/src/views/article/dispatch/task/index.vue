<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getDispatchTasks, getDispatchPlatforms, retryDispatchTasks, deleteDispatchTasks } from '@/api/blog/dispatch'
import { Delete, Refresh, RefreshRight, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const taskList = ref<any[]>([])
const selectedRows = ref<any[]>([])
const platformOptions = ref<any[]>([])

const queryParams = reactive<any>({
  articleTitle: '',
  platformCode: '',
  status: '',
  pageNum: 1,
  pageSize: 15,
  total: 0
})

const statusOptions: Value[] = [
  { label: '待分发', value: 0, elTagType: 'info' },
  { label: '分发中', value: 1, elTagType: 'primary' },
  { label: '分发成功', value: 2, elTagType: 'success' },
  { label: '分发失败', value: 3, elTagType: 'danger' }
]

const detailVisible = ref(false)
const detail = ref<any>({})

const statusTagType = (status: number) => {
  const target = statusOptions.find(item => Number(item.value) === Number(status))
  return target ? target.elTagType : 'info'
}

const statusLabel = (status: number) => {
  const target = statusOptions.find(item => Number(item.value) === Number(status))
  return target ? target.label : status
}

const formatDuration = (ms?: number) => {
  if (ms === undefined || ms === null) return '-'
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(2)} s`
}

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await getDispatchTasks({ ...queryParams })
    if (res.code === 200) {
      taskList.value = res.data?.list || []
      queryParams.total = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

const loadPlatforms = async () => {
  const res = await getDispatchPlatforms()
  if (res.code === 200) {
    platformOptions.value = res.data || []
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  loadTasks()
}

const handleReset = () => {
  queryParams.articleTitle = ''
  queryParams.platformCode = ''
  queryParams.status = ''
  queryParams.pageNum = 1
  loadTasks()
}

const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
}

const handleRetry = (row: any) => {
  retryTaskIds([row.id])
}

const handleBatchRetry = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择需要重试的任务')
    return
  }
  retryTaskIds(selectedRows.value.map(item => item.id))
}

const retryTaskIds = (taskIds: number[]) => {
  ElMessageBox.confirm(`确定重试选中的 ${taskIds.length} 个分发任务吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await retryDispatchTasks(taskIds)
    if (res.code === 200) {
      ElMessage.success(res.msg || '已提交重试')
      loadTasks()
    } else {
      ElMessage.error(res.msg || '重试失败')
    }
  }).catch(() => {})
}

const handleDelete = (row: any) => {
  deleteTaskIds([row.id])
}

const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择需要删除的任务')
    return
  }
  deleteTaskIds(selectedRows.value.map(item => item.id))
}

const deleteTaskIds = (taskIds: number[]) => {
  ElMessageBox.confirm(`确定删除选中的 ${taskIds.length} 个分发任务吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteDispatchTasks(taskIds)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTasks()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleDetail = (row: any) => {
  detail.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadTasks()
  loadPlatforms()
})
</script>

<template>
  <div class="dispatch-task">
    <el-form :inline="true" class="search-form">
      <el-form-item label="文章标题">
        <el-input v-model="queryParams.articleTitle" placeholder="请输入文章标题" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="目标平台">
        <el-select v-model="queryParams.platformCode" clearable placeholder="全部平台" style="width: 160px">
          <el-option
            v-for="item in platformOptions"
            :key="item.platformCode"
            :label="item.platformName"
            :value="item.platformCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :icon="Search" type="primary" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" plain @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="page-toolbar">
      <el-button :icon="RefreshRight" plain type="warning" @click="handleBatchRetry">批量重试</el-button>
      <el-button :icon="Delete" plain type="danger" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="taskList"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="文章标题" min-width="200" align="center" show-overflow-tooltip prop="articleTitle" />
      <el-table-column label="目标平台" width="130" align="center" prop="platformName" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试次数" width="100" align="center">
        <template #default="{ row }">
          <span>{{ row.retryCount }} / {{ row.maxRetry }}</span>
        </template>
      </el-table-column>
      <el-table-column label="执行耗时" width="110" align="center">
        <template #default="{ row }">
          <span>{{ formatDuration(row.durationMs) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="失败原因" min-width="200" align="center" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.status === 3" class="error-text">{{ row.errorDesc || row.errorMsg }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="平台链接" min-width="180" align="center">
        <template #default="{ row }">
          <el-link
            v-if="row.resultUrl"
            type="primary"
            :href="row.resultUrl"
            target="_blank"
          >
            {{ row.resultUrl }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170" align="center" prop="createTime" />
      <el-table-column fixed="right" label="操作" width="200" align="center">
        <template #default="{ row }">
          <el-button :icon="View" size="small" text type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button :icon="RefreshRight" size="small" text type="warning" @click="handleRetry(row)">重试</el-button>
          <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无分发任务" />
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
        @change="loadTasks"
      />
    </div>

    <el-dialog v-model="detailVisible" title="分发任务详情" width="640px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="批次号">{{ detail.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="文章标题">{{ detail.articleTitle }}</el-descriptions-item>
        <el-descriptions-item label="目标平台">{{ detail.platformName }}（{{ detail.platformCode }}）</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="statusTagType(detail.status)">{{ detail.statusDesc || statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ detail.retryCount }} / {{ detail.maxRetry }}</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ formatDuration(detail.durationMs) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.finishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="平台文章Id">{{ detail.platformArticleId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="平台链接">
          <el-link v-if="detail.resultUrl" type="primary" :href="detail.resultUrl" target="_blank">
            {{ detail.resultUrl }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="错误类型">
          <span v-if="detail.errorType">{{ detail.errorType }}{{ detail.errorDesc ? `（${detail.errorDesc}）` : '' }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="失败原因">
          <span v-if="detail.errorMsg" class="error-text">{{ detail.errorMsg }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="处理建议">
          <el-tag v-if="detail.needManualFix" type="danger" effect="plain">需人工处理（如重新配置凭证或修改内容）</el-tag>
          <el-tag v-else-if="detail.retryable" type="warning" effect="plain">可自动重试</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.search-form {
  margin-bottom: 4px;

  :deep(.el-form-item) {
    margin-bottom: 14px;
    margin-right: 14px;
  }
}

.page-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

.error-text {
  color: #f56c6c;
}

.base-pagination {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
