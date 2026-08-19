<script setup lang="ts">
import { reactive, ref } from 'vue'
import { listLogininfor, delLogininfor, cleanLogininfor, exportLogininfor } from '@/api/logininfor'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { Delete, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const exportLoading = ref(false)

const statusList = [
  { label: '成功', value: '0', elTagType: 'success' },
  { label: '失败', value: '1', elTagType: 'danger' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'userName',
    label: '用户账号',
    prop: 'userName',
    render: { el: 'search' }
  },
  {
    key: 'ipaddr',
    label: '登录IP',
    prop: 'ipaddr',
    render: { el: 'search' }
  },
  {
    key: 'loginLocation',
    label: '登录地点',
    prop: 'loginLocation'
  },
  {
    key: 'browser',
    label: '浏览器',
    prop: 'browser'
  },
  {
    key: 'os',
    label: '操作系统',
    prop: 'os'
  },
  {
    key: 'status',
    label: '登录状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: statusList
    }
  },
  {
    key: 'msg',
    label: '提示消息',
    prop: 'msg'
  },
  {
    key: 'loginTime',
    label: '登录时间',
    prop: 'loginTime'
  }
])

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除该条登录日志吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delLogininfor(row.infoId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleClean = () => {
  ElMessageBox.confirm('确定清空所有登录日志吗？', '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await cleanLogininfor()
    if (res.code === 200) {
      ElMessage.success('清空成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '清空失败')
    }
  }).catch(() => {})
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const res = await exportLogininfor(baseTableRef.value?.queryParams || {})
    const blob = new Blob([res], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `logininfor_${Date.now()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}
</script>

<template>
  <base-table ref="baseTableRef" :columns="columns" :api="listLogininfor">
    <template #option>
      <el-button :icon="Delete" plain type="danger" @click="handleClean">清空</el-button>
      <el-button :icon="Download" plain type="primary" :loading="exportLoading" @click="handleExport">导出</el-button>
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
