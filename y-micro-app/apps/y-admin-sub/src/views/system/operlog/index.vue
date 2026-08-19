<script setup lang="ts">
import { reactive, ref } from 'vue'
import { listOperLog, delOperLog, cleanOperLog } from '@/api/operlog'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { Delete, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()

const businessTypeList = [
  { label: '新增', value: 1, elTagType: 'success' },
  { label: '修改', value: 2, elTagType: 'warning' },
  { label: '删除', value: 3, elTagType: 'danger' },
  { label: '其它', value: 0, elTagType: 'info' }
]

const statusList = [
  { label: '正常', value: 0, elTagType: 'success' },
  { label: '异常', value: 1, elTagType: 'danger' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'title',
    label: '操作模块',
    prop: 'title',
    render: { el: 'search' }
  },
  {
    key: 'businessType',
    label: '操作类型',
    prop: 'businessType',
    type: 'tag',
    render: {
      el: 'select',
      list: businessTypeList
    }
  },
  {
    key: 'requestMethod',
    label: '请求方式',
    prop: 'requestMethod'
  },
  {
    key: 'operName',
    label: '操作人员',
    prop: 'operName',
    render: { el: 'search' }
  },
  {
    key: 'operIp',
    label: '主机地址',
    prop: 'operIp'
  },
  {
    key: 'status',
    label: '操作状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: statusList
    }
  },
  {
    key: 'costTime',
    label: '消耗时间(ms)',
    prop: 'costTime'
  },
  {
    key: 'operTime',
    label: '操作时间',
    prop: 'operTime'
  }
])

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定删除该条日志吗？', '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delOperLog(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleClean = () => {
  ElMessageBox.confirm('确定清空所有操作日志吗？', '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await cleanOperLog()
    if (res.code === 200) {
      ElMessage.success('清空成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '清空失败')
    }
  }).catch(() => {})
}
</script>

<template>
  <base-table ref="baseTableRef" :columns="columns" :api="listOperLog">
    <template #option>
      <el-button :icon="Delete" plain type="danger" @click="handleClean">清空</el-button>
      <el-button :icon="RefreshRight" plain type="primary" @click="baseTableRef?.getTableData()">刷新</el-button>
    </template>
    <template #operate="{ row }">
      <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
    </template>
  </base-table>
</template>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
