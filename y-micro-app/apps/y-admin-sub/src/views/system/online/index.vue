<script setup lang="ts">
import { reactive, ref } from 'vue'
import { listOnline, forceLogout } from '@/api/online'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { CircleClose } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()

const columns = reactive<ColumnType[]>([
  {
    key: 'username',
    label: '用户名',
    prop: 'username',
    render: { el: 'search' }
  },
  {
    key: 'roleName',
    label: '所属角色',
    prop: 'roleName'
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
    key: 'loginTime',
    label: '登录时间',
    prop: 'loginTime'
  }
])

const fetchOnlineList = (query: any) => {
  return listOnline(query)
}

const handleForceLogout = (row: any) => {
  ElMessageBox.confirm(`确定强退用户 "${row.username}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await forceLogout(row.tokenId)
    if (res.code === 200) {
      ElMessage.success('强退成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '强退失败')
    }
  }).catch(() => {})
}
</script>

<template>
  <base-table ref="baseTableRef" :columns="columns" :api="fetchOnlineList">
    <template #operate="{ row }">
      <el-button :icon="CircleClose" size="small" text type="danger" @click="handleForceLogout(row)">强退</el-button>
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
