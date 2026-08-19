<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  listDictData,
  getDictData,
  addDictData,
  updateDictData,
  delDictData,
  exportDictData,
  importDictData
} from '@/api/dict'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus, Upload, Download, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const exportLoading = ref(false)
const uploadRef = ref()
const formRef = ref()

const dictType = ref<string>((route.query.dictType as string) || '')
const dictName = ref<string>((route.query.dictName as string) || '')

const form = reactive<any>({
  dictCode: undefined,
  dictSort: 0,
  dictLabel: '',
  dictValue: '',
  dictType: dictType.value,
  cssClass: '',
  listClass: '',
  isDefault: 'N',
  status: '0',
  remark: ''
})

const rules = {
  dictLabel: [
    { required: true, message: '请输入字典标签', trigger: 'blur' }
  ],
  dictValue: [
    { required: true, message: '请输入字典键值', trigger: 'blur' }
  ],
  dictType: [
    { required: true, message: '请输入字典类型', trigger: 'blur' }
  ]
}

const statusList = [
  { label: '正常', value: '0', elTagType: 'success' },
  { label: '停用', value: '1', elTagType: 'danger' }
]

const isDefaultList = [
  { label: '是', value: 'Y', elTagType: 'success' },
  { label: '否', value: 'N', elTagType: 'info' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'dictLabel',
    label: '字典标签',
    prop: 'dictLabel',
    render: { el: 'search' }
  },
  {
    key: 'dictValue',
    label: '字典键值',
    prop: 'dictValue',
    render: { el: 'search' }
  },
  {
    key: 'dictSort',
    label: '字典排序',
    prop: 'dictSort'
  },
  {
    key: 'isDefault',
    label: '是否默认',
    prop: 'isDefault',
    type: 'tag',
    render: {
      el: 'select',
      list: isDefaultList
    }
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: statusList
    }
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const fetchDictData = (query: any) => {
  return listDictData({ ...query, dictType: dictType.value })
}

const resetForm = () => {
  form.dictCode = undefined
  form.dictSort = 0
  form.dictLabel = ''
  form.dictValue = ''
  form.dictType = dictType.value
  form.cssClass = ''
  form.listClass = ''
  form.isDefault = 'N'
  form.status = '0'
  form.remark = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增字典数据'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑字典数据'
  dialogVisible.value = true
  const res = await getDictData(row.dictCode)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除字典数据 "${row.dictLabel}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delDictData(row.dictCode)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = { ...form }
    let res: any
    if (isEdit.value) {
      res = await updateDictData(data)
    } else {
      res = await addDictData(data)
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

const handleExport = async () => {
  exportLoading.value = true
  try {
    const res = await exportDictData({ dictType: dictType.value })
    const blob = new Blob([res], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `dict_data_${dictType.value}.xlsx`
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

const handleImport = async (options: any) => {
  const file = options.file
  try {
    const res = await importDictData(file)
    if (res.code === 200) {
      ElMessage.success(res.data || '导入成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '导入失败')
    }
  } catch (error) {
    ElMessage.error('导入失败')
  }
}

const goBack = () => {
  router.push('/system/dict')
}
</script>

<template>
  <div class="dict-data-container">
    <div class="dict-data-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
      <span class="dict-title">{{ dictName ? dictName + ' (' + dictType + ')' : dictType }}</span>
    </div>

    <base-table ref="baseTableRef" :columns="columns" :api="fetchDictData">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">新增</el-button>
        <el-upload
          ref="uploadRef"
          action="#"
          :http-request="handleImport"
          :show-file-list="false"
          accept=".xlsx,.xls"
          style="display: inline-block; margin: 0 12px"
        >
          <el-button :icon="Upload" plain type="warning">导入</el-button>
        </el-upload>
        <el-button :icon="Download" plain type="primary" :loading="exportLoading" @click="handleExport">导出</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>
  </div>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="字典标签" prop="dictLabel">
        <el-input v-model="form.dictLabel" placeholder="请输入字典标签" />
      </el-form-item>
      <el-form-item label="字典键值" prop="dictValue">
        <el-input v-model="form.dictValue" placeholder="请输入字典键值" />
      </el-form-item>
      <el-form-item label="字典类型" prop="dictType">
        <el-input v-model="form.dictType" placeholder="请输入字典类型" disabled />
      </el-form-item>
      <el-form-item label="字典排序" prop="dictSort">
        <el-input-number v-model="form.dictSort" :min="0" />
      </el-form-item>
      <el-form-item label="是否默认" prop="isDefault">
        <el-radio-group v-model="form.isDefault">
          <el-radio label="Y">是</el-radio>
          <el-radio label="N">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio label="0">正常</el-radio>
          <el-radio label="1">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="样式属性" prop="cssClass">
        <el-input v-model="form.cssClass" placeholder="请输入样式属性" />
      </el-form-item>
      <el-form-item label="回显样式" prop="listClass">
        <el-input v-model="form.listClass" placeholder="请输入表格回显样式" />
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
.dict-data-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.dict-data-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;

  .dict-title {
    margin-left: 16px;
    font-size: 16px;
    font-weight: 500;
  }
}
</style>
