<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listLogininfor, delLogininfor, cleanLogininfor, exportLogininfor } from '@/api/logininfor'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { Delete, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()
const baseTableRef = ref()
const exportLoading = ref(false)

const statusList: Value[] = [
  { label: t('loginLog.success'), value: '0', elTagType: 'success' },
  { label: t('loginLog.failed'), value: '1', elTagType: 'danger' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'userName',
    label: t('loginLog.userName'),
    prop: 'userName',
    render: { el: 'search' }
  },
  {
    key: 'ipaddr',
    label: t('loginLog.loginIp'),
    prop: 'ipaddr',
    render: { el: 'search' }
  },
  {
    key: 'loginLocation',
    label: t('loginLog.loginLocation'),
    prop: 'loginLocation'
  },
  {
    key: 'browser',
    label: t('loginLog.browser'),
    prop: 'browser'
  },
  {
    key: 'os',
    label: t('loginLog.os'),
    prop: 'os'
  },
  {
    key: 'status',
    label: t('loginLog.loginStatus'),
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: statusList
    }
  },
  {
    key: 'msg',
    label: t('loginLog.msg'),
    prop: 'msg'
  },
  {
    key: 'loginTime',
    label: t('loginLog.loginTime'),
    prop: 'loginTime'
  }
])

const handleDelete = (row: any) => {
  ElMessageBox.confirm(t('loginLog.deleteConfirm'), t('common.tips'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    const res = await delLogininfor(row.infoId)
    if (res.code === 200) {
      ElMessage.success(t('dict.deleteSuccess'))
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || t('dict.deleteFailed'))
    }
  }).catch(() => {})
}

const handleClean = () => {
  ElMessageBox.confirm(t('loginLog.cleanConfirm'), t('common.tips'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    const res = await cleanLogininfor()
    if (res.code === 200) {
      ElMessage.success(t('loginLog.cleanSuccess'))
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || t('loginLog.cleanFailed'))
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
    ElMessage.success(t('dictData.exportSuccess'))
  } catch (error) {
    ElMessage.error(t('dictData.exportFailed'))
  } finally {
    exportLoading.value = false
  }
}
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="listLogininfor">
      <template #option>
        <el-button :icon="Delete" plain type="danger" @click="handleClean">{{ t('loginLog.clean') }}</el-button>
        <el-button :icon="Download" plain type="primary" :loading="exportLoading" @click="handleExport">{{ t('loginLog.export') }}</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
      </template>
    </base-table>
  </div>
</template>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
