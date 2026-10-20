<script setup lang="ts">
import { reactive, ref } from 'vue'
import { listChatLoginLog, delChatLoginLog } from '@/api/loginLog'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()

const columns = reactive<ColumnType[]>([
  {
    key: 'account',
    label: '登录账号',
    prop: 'account',
    width: 150,
    render: { el: 'search' }
  },
  {
    key: 'uId',
    label: 'UID',
    prop: 'uId',
    width: 110,
    render: { el: 'search' }
  },
  {
    key: 'ip',
    label: '登录IP',
    prop: 'ip',
    width: 140,
    render: { el: 'search' }
  },
  {
    key: 'loginLocation',
    label: '登录地址',
    prop: 'loginLocation',
    width: 150
  },
  {
    key: 'deviceType',
    label: '登录设备',
    prop: 'deviceType',
    width: 110,
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: 'PC端', value: 1, elTagType: 'primary' },
        { label: '平板端', value: 2, elTagType: 'warning' },
        { label: '手机端', value: 3, elTagType: 'success' }
      ]
    }
  },
  {
    key: 'browser',
    label: '浏览器',
    prop: 'browser',
    width: 120
  },
  {
    key: 'os',
    label: '操作系统',
    prop: 'os',
    width: 120
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    width: 90,
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '成功', value: '0', elTagType: 'success' },
        { label: '失败', value: '1', elTagType: 'danger' }
      ]
    }
  },
  {
    key: 'msg',
    label: '提示信息',
    prop: 'msg'
  },
  {
    key: 'loginTime',
    label: '登录时间',
    prop: 'loginTime',
    width: 170
  }
])

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除该条登录日志吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delChatLoginLog(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="listChatLoginLog">
      <template #operate="{ row }">
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>
  </div>
</template>
