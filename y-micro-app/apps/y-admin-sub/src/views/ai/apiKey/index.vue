<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CopyDocument, Delete } from '@element-plus/icons-vue'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { pageApiKey, generateApiKey, enableApiKey, disableApiKey, delApiKey } from '@/api/ai/apiKey'

const baseTableRef = ref()
const dialogVisible = ref(false)
const generateLoading = ref(false)
const resultDialogVisible = ref(false)
const formRef = ref()

const form = reactive<any>({
  appName: '',
  tokenExpireSeconds: 3600
})

const generatedResult = reactive<any>(({
  apiKey: '',
  apiSecret: ''
}))

const rules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' }
  ],
  tokenExpireSeconds: [
    { required: true, message: '请输入 token 有效期', trigger: 'blur' },
    { type: 'number', min: 60, message: '有效期至少 60 秒', trigger: 'blur' }
  ]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'appName',
    label: '应用名称',
    prop: 'appName',
    render: { el: 'search' }
  },
  {
    key: 'apiKey',
    label: 'API Key',
    prop: 'apiKey',
    render: { el: 'search' }
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
    key: 'tokenExpireSeconds',
    label: 'Token 有效期(秒)',
    prop: 'tokenExpireSeconds'
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const resetForm = () => {
  form.appName = ''
  form.tokenExpireSeconds = 3600
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  generateLoading.value = true
  try {
    const res: any = await generateApiKey({ ...form })
    if (res.code === 200) {
      ElMessage.success('生成成功')
      dialogVisible.value = false
      Object.assign(generatedResult, res.data)
      resultDialogVisible.value = true
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  } finally {
    generateLoading.value = false
  }
}

const copyText = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('复制成功')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const handleEnable = async (row: any) => {
  const res: any = await enableApiKey(row.id)
  if (res.code === 200) {
    ElMessage.success('启用成功')
    baseTableRef.value?.getTableData()
  } else {
    ElMessage.error(res.msg || '启用失败')
  }
}

const handleDisable = async (row: any) => {
  const res: any = await disableApiKey(row.id)
  if (res.code === 200) {
    ElMessage.success('禁用成功')
    baseTableRef.value?.getTableData()
  } else {
    ElMessage.error(res.msg || '禁用失败')
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除应用 "${row.appName}" 的 API Key 吗？删除后无法恢复。`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res: any = await delApiKey(row.id)
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
    <base-table ref="baseTableRef" :columns="columns" :api="pageApiKey">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">生成密钥</el-button>
      </template>
      <template #operate="{ row }">
        <el-button v-if="row.enabled === 0" size="small" text type="primary" @click="handleEnable(row)">启用</el-button>
        <el-button v-if="row.enabled === 1" size="small" text type="warning" @click="handleDisable(row)">禁用</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <!-- 生成密钥表单 -->
    <el-dialog v-model="dialogVisible" title="生成 AI API 密钥" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="form.appName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="Token 有效期(秒)" prop="tokenExpireSeconds">
          <el-input-number v-model="form.tokenExpireSeconds" :min="60" :step="60" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="generateLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 生成结果展示 -->
    <el-dialog v-model="resultDialogVisible" title="密钥生成成功" width="620px" append-to-body>
      <el-alert title="API Secret 仅展示一次，请妥善保存，关闭后将无法再次查看。" type="warning" :closable="false" show-icon />
      <div class="key-result">
        <div class="key-item">
          <div class="key-label">API Key</div>
          <div class="key-value">
            <el-input v-model="generatedResult.apiKey" readonly>
              <template #append>
                <el-button :icon="CopyDocument" @click="copyText(generatedResult.apiKey)">复制</el-button>
              </template>
            </el-input>
          </div>
        </div>
        <div class="key-item">
          <div class="key-label">API Secret</div>
          <div class="key-value">
            <el-input v-model="generatedResult.apiSecret" type="password" show-password readonly>
              <template #append>
                <el-button :icon="CopyDocument" @click="copyText(generatedResult.apiSecret)">复制</el-button>
              </template>
            </el-input>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="resultDialogVisible = false">我已保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.key-result {
  margin-top: 16px;
}
.key-item {
  margin-bottom: 16px;
}
.key-label {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--app-text-secondary);
}
.key-value {
  width: 100%;
}
</style>
