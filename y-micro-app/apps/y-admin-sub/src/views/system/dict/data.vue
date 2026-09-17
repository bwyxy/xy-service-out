<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
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
    { required: true, message: t('dictData.dictLabelPlaceholder'), trigger: 'blur' }
  ],
  dictValue: [
    { required: true, message: t('dictData.dictValuePlaceholder'), trigger: 'blur' }
  ],
  dictType: [
    { required: true, message: t('dictData.dictTypePlaceholder'), trigger: 'blur' }
  ]
}

const statusList: Value[] = [
  { label: t('menu.normal'), value: '0', elTagType: 'success' },
  { label: t('menu.disabled'), value: '1', elTagType: 'danger' }
]

const isDefaultList: Value[] = [
  { label: t('common.yes'), value: 'Y', elTagType: 'success' },
  { label: t('common.no'), value: 'N', elTagType: 'info' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'dictLabel',
    label: t('dictData.dictLabel'),
    prop: 'dictLabel',
    render: { el: 'search' }
  },
  {
    key: 'dictValue',
    label: t('dictData.dictValue'),
    prop: 'dictValue',
    render: { el: 'search' }
  },
  {
    key: 'dictSort',
    label: t('dictData.dictSort'),
    prop: 'dictSort'
  },
  {
    key: 'isDefault',
    label: t('dictData.isDefault'),
    prop: 'isDefault',
    type: 'tag',
    render: {
      el: 'select',
      list: isDefaultList
    }
  },
  {
    key: 'status',
    label: t('common.status'),
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: statusList
    }
  },
  {
    key: 'createTime',
    label: t('role.createTime'),
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
  dialogTitle.value = t('dictData.addDictData')
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = t('dictData.editDictData')
  dialogVisible.value = true
  const res = await getDictData(row.dictCode)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(t('dictData.deleteConfirm', { name: row.dictLabel }), t('common.tips'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    const res = await delDictData(row.dictCode)
    if (res.code === 200) {
      ElMessage.success(t('dict.deleteSuccess'))
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || t('dict.deleteFailed'))
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
      ElMessage.success(isEdit.value ? t('dict.editSuccess') : t('dict.addSuccess'))
      dialogVisible.value = false
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || (isEdit.value ? t('dict.editFailed') : t('dict.addFailed')))
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
    ElMessage.success(t('dictData.exportSuccess'))
  } catch (error) {
    ElMessage.error(t('dictData.exportFailed'))
  } finally {
    exportLoading.value = false
  }
}

const handleImport = async (options: any) => {
  const file = options.file
  try {
    const res = await importDictData(file)
    if (res.code === 200) {
      ElMessage.success(res.data || t('dictData.importSuccess'))
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || t('dictData.importFailed'))
    }
  } catch (error) {
    ElMessage.error(t('dictData.importFailed'))
  }
}

const goBack = () => {
  router.push('/system/dict')
}
</script>

<template>
  <div>
    <div class="dict-data-container">
      <div class="dict-data-header">
        <el-button :icon="ArrowLeft" @click="goBack">{{ t('dictData.return') }}</el-button>
        <span class="dict-title">{{ dictName ? dictName + ' (' + dictType + ')' : dictType }}</span>
      </div>

      <base-table ref="baseTableRef" :columns="columns" :api="fetchDictData">
        <template #option>
          <el-button :icon="Plus" plain type="success" @click="handleAdd">{{ t('common.add') }}</el-button>
          <el-upload
              ref="uploadRef"
              action="#"
              :http-request="handleImport"
              :show-file-list="false"
              accept=".xlsx,.xls"
              style="display: inline-block; margin: 0 12px"
          >
            <el-button :icon="Upload" plain type="warning">{{ t('dictData.import') }}</el-button>
          </el-upload>
          <el-button :icon="Download" plain type="primary" :loading="exportLoading" @click="handleExport">{{ t('dictData.export') }}</el-button>
        </template>
        <template #operate="{ row }">
          <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
        </template>
      </base-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('dictData.dictLabel')" prop="dictLabel">
          <el-input v-model="form.dictLabel" :placeholder="t('dictData.dictLabelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dictData.dictValue')" prop="dictValue">
          <el-input v-model="form.dictValue" :placeholder="t('dictData.dictValuePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dict.dictType')" prop="dictType">
          <el-input v-model="form.dictType" :placeholder="t('dictData.dictTypePlaceholder')" disabled />
        </el-form-item>
        <el-form-item :label="t('dictData.dictSort')" prop="dictSort">
          <el-input-number v-model="form.dictSort" :min="0" />
        </el-form-item>
        <el-form-item :label="t('dictData.isDefault')" prop="isDefault">
          <el-radio-group v-model="form.isDefault">
            <el-radio label="Y">{{ t('common.yes') }}</el-radio>
            <el-radio label="N">{{ t('common.no') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('common.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">{{ t('menu.normal') }}</el-radio>
            <el-radio label="1">{{ t('menu.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('dictData.cssClass')" prop="cssClass">
          <el-input v-model="form.cssClass" :placeholder="t('dictData.cssClassPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dictData.listClass')" prop="listClass">
          <el-input v-model="form.listClass" :placeholder="t('dictData.listClassPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dict.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="t('dict.remarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">{{ t('common.confirm') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
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
