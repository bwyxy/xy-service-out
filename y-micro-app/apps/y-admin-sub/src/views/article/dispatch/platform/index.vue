<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getDispatchPlatforms,
  getSupportedDispatchPlatforms,
  getDispatchRoles,
  getDispatchPlatformById,
  addDispatchPlatform,
  updateDispatchPlatform,
  deleteDispatchPlatform,
  testDispatchPlatform
} from '@/api/blog/dispatch'
import { Connection, Delete, EditPen, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const platformList = ref<any[]>([])
const supportedPlatforms = ref<any[]>([])
const roleOptions = ref<any[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const form = reactive<any>({
  id: undefined,
  platformCode: '',
  platformName: '',
  enabled: 1,
  credentials: {},
  credentialConfigured: false,
  configText: '',
  titleMaxLength: 100,
  tagMaxCount: 5,
  summaryMaxLength: 200,
  contentMaxLength: 100000,
  rateLimitPerMinute: 30,
  requestTimeoutSeconds: 15,
  maxRetry: 3,
  sort: 0,
  roleIds: [],
  remark: ''
})

const rules = {
  platformCode: [{ required: true, message: '请选择接入平台', trigger: 'change' }],
  platformName: [{ required: true, message: '请输入平台名称', trigger: 'blur' }]
}

/** 当前所选平台在适配器中要求的凭证字段 */
const requiredCredentialKeys = computed<string[]>(() => {
  const target = supportedPlatforms.value.find(item => item.platformCode === form.platformCode)
  return target?.requiredCredentialKeys || []
})

/** 新增时仅允许选择尚未接入的平台 */
const selectablePlatforms = computed(() => {
  const usedCodes = platformList.value.map(item => item.platformCode)
  return supportedPlatforms.value.filter(item => !usedCodes.includes(item.platformCode))
})

const roleNameOf = (roleId: number) => {
  const role = roleOptions.value.find(item => item.id === roleId)
  return role ? role.roleName : roleId
}

const loadPlatforms = async () => {
  loading.value = true
  try {
    const res = await getDispatchPlatforms()
    if (res.code === 200) {
      platformList.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

const loadSupportedPlatforms = async () => {
  const res = await getSupportedDispatchPlatforms()
  if (res.code === 200) {
    supportedPlatforms.value = res.data || []
  }
}

const loadRoles = async () => {
  const res = await getDispatchRoles()
  if (res.code === 200) {
    roleOptions.value = res.data || []
  }
}

const resetForm = () => {
  form.id = undefined
  form.platformCode = ''
  form.platformName = ''
  form.enabled = 1
  form.credentials = {}
  form.credentialConfigured = false
  form.configText = ''
  form.titleMaxLength = 100
  form.tagMaxCount = 5
  form.summaryMaxLength = 200
  form.contentMaxLength = 100000
  form.rateLimitPerMinute = 30
  form.requestTimeoutSeconds = 15
  form.maxRetry = 3
  form.sort = 0
  form.roleIds = []
  form.remark = ''
  formRef.value?.clearValidate()
}

const handlePlatformChange = (code: string) => {
  const target = supportedPlatforms.value.find(item => item.platformCode === code)
  if (target && !form.platformName) {
    form.platformName = target.platformName
  }
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增分发平台'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = `编辑平台 - ${row.platformName}`
  dialogVisible.value = true
  const res = await getDispatchPlatformById(row.id)
  if (res.code === 200) {
    const data = res.data || {}
    Object.assign(form, {
      id: data.id,
      platformCode: data.platformCode,
      platformName: data.platformName,
      enabled: data.enabled,
      configText: data.config ? JSON.stringify(data.config, null, 2) : '',
      titleMaxLength: data.titleMaxLength,
      tagMaxCount: data.tagMaxCount,
      summaryMaxLength: data.summaryMaxLength,
      contentMaxLength: data.contentMaxLength,
      rateLimitPerMinute: data.rateLimitPerMinute,
      requestTimeoutSeconds: data.requestTimeoutSeconds,
      maxRetry: data.maxRetry,
      sort: data.sort,
      roleIds: data.roleIds || [],
      remark: data.remark || ''
    })
    form.credentials = {}
    form.credentialConfigured = data.credentialConfigured === true
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除平台配置 "${row.platformName}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteDispatchPlatform(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadPlatforms()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleTest = async (row: any) => {
  const loadingInstance = ElMessage.info(`正在测试 ${row.platformName} 连通性...`)
  const res = await testDispatchPlatform(row.id)
  loadingInstance.close()
  if (res.code === 200) {
    const result = res.data || {}
    if (result.success) {
      ElMessage.success(result.message || '连通性测试通过')
    } else {
      ElMessage.warning(result.message || '连通性测试失败')
    }
    loadPlatforms()
  } else {
    ElMessage.error(res.msg || '连通性测试失败')
  }
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  let config: Record<string, any> = {}
  const configText = (form.configText || '').trim()
  if (configText) {
    try {
      config = JSON.parse(configText)
    } catch (e) {
      ElMessage.error('发布默认规则不是合法的 JSON 格式')
      return
    }
  }

  const credentials: Record<string, string> = {}
  Object.keys(form.credentials || {}).forEach(key => {
    const value = form.credentials[key]
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      credentials[key] = value
    }
  })

  submitLoading.value = true
  try {
    const data: any = {
      id: form.id,
      platformCode: form.platformCode,
      platformName: form.platformName,
      enabled: form.enabled,
      credentials,
      config,
      titleMaxLength: form.titleMaxLength,
      tagMaxCount: form.tagMaxCount,
      summaryMaxLength: form.summaryMaxLength,
      contentMaxLength: form.contentMaxLength,
      rateLimitPerMinute: form.rateLimitPerMinute,
      requestTimeoutSeconds: form.requestTimeoutSeconds,
      maxRetry: form.maxRetry,
      sort: form.sort,
      roleIds: form.roleIds,
      remark: form.remark
    }
    const res = isEdit.value ? await updateDispatchPlatform(data) : await addDispatchPlatform(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      dialogVisible.value = false
      loadPlatforms()
    } else {
      ElMessage.error(res.msg || (isEdit.value ? '修改失败' : '新增失败'))
    }
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadPlatforms()
  loadSupportedPlatforms()
  loadRoles()
})
</script>

<template>
  <div class="dispatch-platform">
    <div class="page-toolbar">
      <el-button :icon="Refresh" plain @click="loadPlatforms">刷新</el-button>
      <el-button :icon="Plus" type="primary" @click="handleAdd">新增平台配置</el-button>
    </div>

    <el-table v-loading="loading" :data="platformList" border>
      <el-table-column label="平台" min-width="160" align="center">
        <template #default="{ row }">
          <div class="platform-cell">
            <span class="platform-name">{{ row.platformName }}</span>
            <el-tag size="small" effect="plain">{{ row.platformCode }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
            {{ row.enabled === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="凭证" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.credentialConfigured ? 'success' : 'warning'" effect="plain">
            {{ row.credentialConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="适配器" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.adapterSupported ? 'success' : 'danger'" effect="plain">
            {{ row.adapterSupported ? '可用' : '缺失' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="限制（标题/标签/摘要）" width="200" align="center">
        <template #default="{ row }">
          <span>{{ row.titleMaxLength }} / {{ row.tagMaxCount }} / {{ row.summaryMaxLength }}</span>
        </template>
      </el-table-column>
      <el-table-column label="限流" width="110" align="center">
        <template #default="{ row }">
          <span>{{ row.rateLimitPerMinute }} 次/分</span>
        </template>
      </el-table-column>
      <el-table-column label="最大重试" width="90" align="center" prop="maxRetry" />
      <el-table-column label="适用角色" min-width="160" align="center">
        <template #default="{ row }">
          <span v-if="!row.roleIds || row.roleIds.length === 0">不限角色</span>
          <template v-else>
            <el-tag
              v-for="roleId in row.roleIds"
              :key="roleId"
              size="small"
              effect="plain"
              class="role-tag"
            >
              {{ roleNameOf(roleId) }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="最近测试" min-width="180" align="center">
        <template #default="{ row }">
          <div v-if="row.lastTestTime" class="test-cell">
            <span>{{ row.lastTestTime }}</span>
            <el-tooltip :content="row.lastTestResult || ''" placement="top">
              <span class="test-result">{{ row.lastTestResult }}</span>
            </el-tooltip>
          </div>
          <span v-else class="text-muted">未测试</span>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="220" align="center">
        <template #default="{ row }">
          <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button :icon="Connection" size="small" text type="warning" @click="handleTest(row)">测试</el-button>
          <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无平台配置，请先新增" />
      </template>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="接入平台" prop="platformCode">
              <el-select
                v-model="form.platformCode"
                :disabled="isEdit"
                placeholder="请选择平台"
                style="width: 100%"
                @change="handlePlatformChange"
              >
                <el-option
                  v-for="item in isEdit ? supportedPlatforms : selectablePlatforms"
                  :key="item.platformCode"
                  :label="item.platformName"
                  :value="item.platformCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台名称" prop="platformName">
              <el-input v-model="form.platformName" placeholder="请输入平台名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>

        <el-divider content-position="left">账号凭证（加密存储）</el-divider>
        <el-alert
          v-if="requiredCredentialKeys.length === 0"
          title="该平台适配器未声明凭证字段"
          type="info"
          :closable="false"
          show-icon
        />
        <el-alert
          v-if="isEdit && !form.credentialConfigured && requiredCredentialKeys.length > 0"
          title="该平台尚未保存过凭证，请填写下方凭证，否则分发会因缺少凭证失败"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-form-item
          v-for="key in requiredCredentialKeys"
          :key="key"
          :label="key"
        >
          <el-input
            v-model="form.credentials[key]"
            type="password"
            show-password
            :placeholder="isEdit && form.credentialConfigured ? '留空表示保持原有凭证不变' : `请输入 ${key}`"
          />
        </el-form-item>

        <el-divider content-position="left">发布默认规则（JSON）</el-divider>
        <el-form-item label="规则配置">
          <el-input
            v-model="form.configText"
            type="textarea"
            :rows="4"
            placeholder='例如：{"category_id":"6809637773935378440","is_original":true}'
          />
          <div class="form-tip">
            用于平台适配器的额外参数（如分类、原创标识、发布地址模板等），留空表示使用适配器默认值
          </div>
        </el-form-item>

        <el-divider content-position="left">内容适配限制</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="标题长度">
              <el-input-number v-model="form.titleMaxLength" :min="1" :max="500" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标签数量">
              <el-input-number v-model="form.tagMaxCount" :min="0" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="摘要长度">
              <el-input-number v-model="form.summaryMaxLength" :min="1" :max="2000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="正文长度">
          <el-input-number v-model="form.contentMaxLength" :min="100" :max="1000000" :step="1000" style="width: 100%" />
        </el-form-item>

        <el-divider content-position="left">请求与重试</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="限流(次/分)">
              <el-input-number v-model="form.rateLimitPerMinute" :min="1" :max="600" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时(秒)">
              <el-input-number v-model="form.requestTimeoutSeconds" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大重试">
              <el-input-number v-model="form.maxRetry" :min="0" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">权限与排序</el-divider>
        <el-form-item label="可用角色">
          <el-select v-model="form.roleIds" multiple clearable placeholder="不选表示所有角色均可用" style="width: 100%">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
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
.page-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

.platform-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.platform-name {
  font-weight: 600;
}

.role-tag {
  margin: 2px;
}

.test-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.4;
}

.test-result {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #909399;
  font-size: 12px;
}

.text-muted {
  color: #909399;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
