<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { View, Check } from '@element-plus/icons-vue'
import { getReports, handleReports } from '@/api/forum/report'

const baseTableRefs = ref()
const handleVisible = ref(false)
const currentReport = ref<ForumReport>({})
const handleFormRef = ref()
const handleForm = reactive({
  reportId: undefined as number | undefined,
  result: '',
  status: 1
})
const handleRules = {
  result: [
    { required: true, message: '请输入处理结果', trigger: 'blur' }
  ]
}

const targetTypeList = [
  { label: '帖子', value: 1 },
  { label: '评论', value: 2 },
  { label: '用户', value: 3 }
]

const reportStatusList: Value[] = [
  { label: '待处理', value: 0, elTagType: 'warning' },
  { label: '已处理', value: 1, elTagType: 'success' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'reportId',
    label: '举报ID',
    prop: 'reportId',
    width: 80
  },
  {
    key: 'reporterId',
    label: '举报人ID',
    prop: 'reporterId',
    width: 100
  },
  {
    key: 'targetType',
    label: '目标类型',
    prop: 'targetType',
    width: 100,
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '帖子', value: 1, elTagType: 'info' },
        { label: '评论', value: 2, elTagType: 'primary' },
        { label: '用户', value: 3, elTagType: 'warning' }
      ]
    }
  },
  {
    key: 'targetId',
    label: '目标ID',
    prop: 'targetId',
    width: 100
  },
  {
    key: 'reason',
    label: '举报原因',
    prop: 'reason'
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    width: 100,
    type: 'tag',
    render: {
      el: 'select',
      list: reportStatusList
    }
  },
  {
    key: 'handlerId',
    label: '处理人ID',
    prop: 'handlerId',
    width: 100
  },
  {
    key: 'result',
    label: '处理结果',
    prop: 'result'
  },
  {
    key: 'createTime',
    label: '举报时间',
    prop: 'createTime',
    width: 160
  },
  {
    key: 'updateTime',
    label: '处理时间',
    prop: 'updateTime',
    width: 160
  }
])

const handleOpen = (row: ForumReport) => {
  currentReport.value = row
  handleForm.reportId = row.reportId
  handleForm.result = ''
  handleForm.status = 1
  handleVisible.value = true
}

const handleView = (row: ForumReport) => {
  currentReport.value = row
  handleVisible.value = true
  handleForm.reportId = row.reportId
  handleForm.result = row.result || ''
  handleForm.status = row.status || 0
}

const submitHandle = async () => {
  const valid = await handleFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const res = await handleReports({
    reportId: handleForm.reportId,
    result: handleForm.result,
    status: handleForm.status
  })

  if (res.code === 200) {
    ElMessage.success('处理成功')
    handleVisible.value = false
    baseTableRefs.value?.getTableData()
  } else {
    ElMessage.error(res.msg || '处理失败')
  }
}
</script>

<template>
  <base-table ref="baseTableRefs" :columns="columns" :api="getReports">
    <template #operate="{ row }">
      <el-button
        text
        type="success"
        :icon="View"
        size="small"
        @click="handleView(row)"
      >查看</el-button>
      <el-button
        v-if="row.status === 0"
        text
        type="primary"
        :icon="Check"
        size="small"
        @click="handleOpen(row)"
      >处理</el-button>
    </template>
  </base-table>

  <el-dialog
    v-model="handleVisible"
    :title="currentReport.status === 0 ? '处理举报' : '举报详情'"
    width="600px"
    append-to-body
  >
    <el-descriptions :column="2" border class="report-detail">
      <el-descriptions-item label="举报ID">{{ currentReport.reportId }}</el-descriptions-item>
      <el-descriptions-item label="举报人ID">{{ currentReport.reporterId }}</el-descriptions-item>
      <el-descriptions-item label="目标类型">
        {{ targetTypeList.find(item => item.value === currentReport.targetType)?.label || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="目标ID">{{ currentReport.targetId }}</el-descriptions-item>
      <el-descriptions-item label="举报原因" :span="2">{{ currentReport.reason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="举报时间" :span="2">{{ currentReport.createTime }}</el-descriptions-item>
    </el-descriptions>

    <el-form
      v-if="currentReport.status === 0"
      ref="handleFormRef"
      :model="handleForm"
      :rules="handleRules"
      label-width="100px"
      class="handle-form"
    >
      <el-form-item label="处理结果" prop="result">
        <el-input
          v-model="handleForm.result"
          type="textarea"
          :rows="3"
          placeholder="请输入处理结果"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleVisible = false">关闭</el-button>
      <el-button
        v-if="currentReport.status === 0"
        type="primary"
        @click="submitHandle"
      >确认处理</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.report-detail {
  margin-bottom: 20px;
}
.handle-form {
  margin-top: 16px;
}
</style>
