<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listDictType, getDictType, addDictType, updateDictType, delDictType } from '@/api/dict'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus, Management } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const form = reactive<any>({
  dictId: undefined,
  dictName: '',
  dictType: '',
  status: '0',
  remark: ''
})

const rules = {
  dictName: [
    { required: true, message: '请输入字典名称', trigger: 'blur' }
  ],
  dictType: [
    { required: true, message: '请输入字典类型', trigger: 'blur' }
  ]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'dictName',
    label: '字典名称',
    prop: 'dictName',
    render: { el: 'search' }
  },
  {
    key: 'dictType',
    label: '字典类型',
    prop: 'dictType',
    render: { el: 'search' }
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '正常', value: '0', elTagType: 'success' },
        { label: '停用', value: '1', elTagType: 'danger' }
      ]
    }
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const resetForm = () => {
  form.dictId = undefined
  form.dictName = ''
  form.dictType = ''
  form.status = '0'
  form.remark = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增字典类型'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑字典类型'
  dialogVisible.value = true
  const res = await getDictType(row.dictId)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除字典类型 "${row.dictName}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delDictType(row.dictId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleManageData = (row: any) => {
  router.push({
    path: '/system/dict/data',
    query: { dictType: row.dictType, dictName: row.dictName }
  })
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = { ...form }
    let res: any
    if (isEdit.value) {
      res = await updateDictType(data)
    } else {
      res = await addDictType(data)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      dialogVisible.value = false
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || (isEdit.value ? '修改失败' : '新增失败'))
    }
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <base-table ref="baseTableRef" :columns="columns" :api="listDictType">
    <template #option>
      <el-button :icon="Plus" plain type="success" @click="handleAdd">新增</el-button>
    </template>
    <template #operate="{ row }">
      <el-button :icon="Management" size="small" text type="primary" @click="handleManageData(row)">数据</el-button>
      <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
      <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
    </template>
  </base-table>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="字典名称" prop="dictName">
        <el-input v-model="form.dictName" placeholder="请输入字典名称" />
      </el-form-item>
      <el-form-item label="字典类型" prop="dictType">
        <el-input v-model="form.dictType" placeholder="请输入字典类型" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio label="0">正常</el-radio>
          <el-radio label="1">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
