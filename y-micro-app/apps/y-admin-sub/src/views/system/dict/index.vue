<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { listDictType, getDictType, addDictType, updateDictType, delDictType } from '@/api/dict'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus, Management } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()
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
    { required: true, message: t('dict.dictNamePlaceholder'), trigger: 'blur' }
  ],
  dictType: [
    { required: true, message: t('dict.dictTypePlaceholder'), trigger: 'blur' }
  ]
}

const statusList: Value[] = [
  { label: t('menu.normal'), value: '0', elTagType: 'success' },
  { label: t('menu.disabled'), value: '1', elTagType: 'danger' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'dictName',
    label: t('dict.dictName'),
    prop: 'dictName',
    render: { el: 'search' }
  },
  {
    key: 'dictType',
    label: t('dict.dictType'),
    prop: 'dictType',
    render: { el: 'search' }
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
  dialogTitle.value = t('dict.addDictType')
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = t('dict.editDictType')
  dialogVisible.value = true
  const res = await getDictType(row.dictId)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(t('dict.deleteConfirm', { name: row.dictName }), t('common.tips'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    const res = await delDictType(row.dictId)
    if (res.code === 200) {
      ElMessage.success(t('dict.deleteSuccess'))
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || t('dict.deleteFailed'))
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
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="listDictType">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">{{ t('common.add') }}</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="Management" size="small" text type="primary" @click="handleManageData(row)">{{ t('dict.manageData') }}</el-button>
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('dict.dictName')" prop="dictName">
          <el-input v-model="form.dictName" :placeholder="t('dict.dictNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dict.dictType')" prop="dictType">
          <el-input v-model="form.dictType" :placeholder="t('dict.dictTypePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">{{ t('menu.normal') }}</el-radio>
            <el-radio label="1">{{ t('menu.disabled') }}</el-radio>
          </el-radio-group>
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
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
