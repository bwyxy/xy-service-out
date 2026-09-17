<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, EditPen, Delete } from '@element-plus/icons-vue'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import {
  pageModelProvider,
  getModelProvider,
  addModelProvider,
  updateModelProvider,
  delModelProvider,
  toggleModelProvider
} from '@/api/ai/model'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const form = reactive<any>({
  id: undefined,
  name: '',
  code: '',
  type: 'OPENAI_COMPATIBLE',
  endpoint: '',
  apiKey: '',
  model: '',
  priority: 0,
  enabled: 1,
  configJson: ''
})

const rules = {
  name: [
    { required: true, message: '请输入提供者名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入提供者编码', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择提供者类型', trigger: 'change' }
  ]
}

const providerTypeList = [
  { label: 'OpenAI 兼容接口', value: 'OPENAI_COMPATIBLE' },
  { label: '阿里云百炼', value: 'DASHSCOPE' },
  { label: '百度千帆', value: 'QIANFAN' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'name',
    label: '提供者名称',
    prop: 'name',
    render: { el: 'search' }
  },
  {
    key: 'code',
    label: '提供者编码',
    prop: 'code',
    render: { el: 'search' }
  },
  {
    key: 'type',
    label: '类型',
    prop: 'type'
  },
  {
    key: 'model',
    label: '默认模型',
    prop: 'model'
  },
  {
    key: 'priority',
    label: '优先级',
    prop: 'priority'
  },
  {
    key: 'enabled',
    label: '状态',
    prop: 'enabled',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '启用', value: 1, elTagType: 'success' },
        { label: '禁用', value: 0, elTagType: 'danger' }
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
  form.id = undefined
  form.name = ''
  form.code = ''
  form.type = 'OPENAI_COMPATIBLE'
  form.endpoint = ''
  form.apiKey = ''
  form.model = ''
  form.priority = 0
  form.enabled = 1
  form.configJson = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加模型提供者'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑模型提供者'
  dialogVisible.value = true
  const res = await getModelProvider(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除模型提供者 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delModelProvider(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleToggle = async (row: any) => {
  const res = await toggleModelProvider(row.id)
  if (res.code === 200) {
    ElMessage.success('状态切换成功')
    baseTableRef.value?.getTableData()
  } else {
    ElMessage.error(res.msg || '状态切换失败')
  }
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = { ...form }
    const res = isEdit.value ? await updateModelProvider(data) : await addModelProvider(data)
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
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="pageModelProvider">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加模型提供者</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button size="small" text type="warning" @click="handleToggle(row)">
          {{ row.enabled === 1 ? '禁用' : '启用' }}
        </el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="提供者名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入提供者名称" />
        </el-form-item>
        <el-form-item label="提供者编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入唯一编码，如 openai" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option
                v-for="item in providerTypeList"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Endpoint">
          <el-input v-model="form.endpoint" placeholder="请输入接口地址" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="请输入 API Key" />
        </el-form-item>
        <el-form-item label="默认模型">
          <el-input v-model="form.model" placeholder="请输入默认模型名称" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.enabled">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="扩展配置 JSON">
          <el-input v-model="form.configJson" type="textarea" :rows="3" placeholder='{"temperature": 0.7}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
</style>
