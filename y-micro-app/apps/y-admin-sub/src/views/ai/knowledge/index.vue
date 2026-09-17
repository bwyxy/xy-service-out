<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, EditPen, Delete, Setting, Document, Collection, Key } from '@element-plus/icons-vue'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import {
  pageKnowledgeBase,
  getKnowledgeBase,
  addKnowledgeBase,
  updateKnowledgeBase,
  delKnowledgeBase,
  toggleKnowledgeBase,
  pageKnowledgeDocument,
  addKnowledgeDocument,
  updateKnowledgeDocument,
  delKnowledgeDocument,
  reindexKnowledgeDocument,
  listKnowledgeBaseVersion,
  saveKnowledgeBaseVersion,
  publishKnowledgeBaseVersion,
  archiveKnowledgeBaseVersion,
  delKnowledgeBaseVersion,
  listKnowledgeBasePermission,
  grantKnowledgeBasePermission,
  revokeKnowledgeBasePermission
} from '@/api/ai/knowledge'
import { listRole } from '@/api/role'
import { listUser } from '@/api/user'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const manageVisible = ref(false)
const activeTab = ref('document')
const currentKb = reactive<any>({})

const documentTableRef = ref()
const documentDialogVisible = ref(false)
const documentIsEdit = ref(false)
const documentFormRef = ref()
const documentSubmitLoading = ref(false)
const documentForm = reactive<any>({
  id: undefined,
  kbId: undefined,
  title: '',
  sourceType: 'TEXT',
  sourceUrl: '',
  content: ''
})

const versionDialogVisible = ref(false)
const versionFormRef = ref()
const versionSubmitLoading = ref(false)
const versionForm = reactive<any>({
  id: undefined,
  kbId: undefined,
  version: '',
  description: ''
})

const permissionFormRef = ref()
const permissionSubmitLoading = ref(false)
const permissionForm = reactive<any>({
  kbId: undefined,
  targetType: 'ROLE',
  targetId: undefined,
  permission: 'READ'
})

const roleOptions = ref<any[]>([])
const userOptions = ref<any[]>([])

const form = reactive<any>({
  id: undefined,
  name: '',
  description: '',
  embeddingModel: '',
  providerCode: ''
})

const rules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' }
  ]
}

const documentRules = {
  title: [
    { required: true, message: '请输入文档标题', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入文档内容', trigger: 'blur' }
  ]
}

const versionRules = {
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' }
  ]
}

const permissionRules = {
  targetType: [
    { required: true, message: '请选择授权对象类型', trigger: 'change' }
  ],
  targetId: [
    { required: true, message: '请选择授权对象', trigger: 'change' }
  ],
  permission: [
    { required: true, message: '请选择权限类型', trigger: 'change' }
  ]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'name',
    label: '知识库名称',
    prop: 'name',
    render: { el: 'search' }
  },
  {
    key: 'embeddingModel',
    label: 'Embedding 模型',
    prop: 'embeddingModel'
  },
  {
    key: 'providerCode',
    label: '模型提供者',
    prop: 'providerCode'
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '启用', value: 0, elTagType: 'success' },
        { label: '禁用', value: 1, elTagType: 'danger' }
      ]
    }
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const documentColumns = reactive<ColumnType[]>([
  {
    key: 'title',
    label: '文档标题',
    prop: 'title'
  },
  {
    key: 'sourceType',
    label: '来源类型',
    prop: 'sourceType'
  },
  {
    key: 'chunkCount',
    label: '片段数',
    prop: 'chunkCount'
  },
  {
    key: 'status',
    label: '处理状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '处理中', value: 0, elTagType: 'info' },
        { label: '已完成', value: 1, elTagType: 'success' },
        { label: '失败', value: 2, elTagType: 'danger' }
      ]
    }
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const versionColumns = reactive<ColumnType[]>([
  {
    key: 'version',
    label: '版本号',
    prop: 'version'
  },
  {
    key: 'description',
    label: '版本描述',
    prop: 'description'
  },
  {
    key: 'docCount',
    label: '文档数',
    prop: 'docCount'
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '草稿', value: 0, elTagType: 'info' },
        { label: '已发布', value: 1, elTagType: 'success' },
        { label: '已归档', value: 2, elTagType: 'warning' }
      ]
    }
  },
  {
    key: 'publishedTime',
    label: '发布时间',
    prop: 'publishedTime'
  }
])

const permissionColumns = reactive<ColumnType[]>([
  {
    key: 'targetType',
    label: '授权对象类型',
    prop: 'targetType'
  },
  {
    key: 'targetId',
    label: '对象 ID',
    prop: 'targetId'
  },
  {
    key: 'permission',
    label: '权限',
    prop: 'permission'
  },
  {
    key: 'createTime',
    label: '授权时间',
    prop: 'createTime'
  }
])

const resetForm = () => {
  form.id = undefined
  form.name = ''
  form.description = ''
  form.embeddingModel = ''
  form.providerCode = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加知识库'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑知识库'
  dialogVisible.value = true
  const res = await getKnowledgeBase(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除知识库 "${row.name}" 吗？相关文档与向量将一并删除。`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delKnowledgeBase(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleToggle = async (row: any) => {
  const res = await toggleKnowledgeBase(row.id)
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
    const res = isEdit.value ? await updateKnowledgeBase(data) : await addKnowledgeBase(data)
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

// ===== 知识库管理 =====

const handleManage = (row: any) => {
  Object.assign(currentKb, row)
  manageVisible.value = true
  activeTab.value = 'document'
  loadRoles()
  loadUsers()
}

// ===== 文档管理 =====

const resetDocumentForm = () => {
  documentForm.id = undefined
  documentForm.kbId = currentKb.id
  documentForm.title = ''
  documentForm.sourceType = 'TEXT'
  documentForm.sourceUrl = ''
  documentForm.content = ''
  documentFormRef.value?.resetFields()
}

const handleAddDocument = () => {
  resetDocumentForm()
  documentIsEdit.value = false
  documentDialogVisible.value = true
}

const handleEditDocument = (row: any) => {
  resetDocumentForm()
  documentIsEdit.value = true
  Object.assign(documentForm, row)
  documentDialogVisible.value = true
}

const handleDeleteDocument = (row: any) => {
  ElMessageBox.confirm(`确定删除文档 "${row.title}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delKnowledgeDocument(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      documentTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleReindexDocument = async (row: any) => {
  const res = await reindexKnowledgeDocument(row.id)
  if (res.code === 200) {
    ElMessage.success('重新索引已触发')
    documentTableRef.value?.getTableData()
  } else {
    ElMessage.error(res.msg || '重新索引失败')
  }
}

const submitDocumentForm = async () => {
  const valid = await documentFormRef.value?.validate().catch(() => false)
  if (!valid) return
  documentSubmitLoading.value = true
  try {
    const data = { ...documentForm, kbId: currentKb.id }
    const res = documentIsEdit.value ? await updateKnowledgeDocument(data) : await addKnowledgeDocument(data)
    if (res.code === 200) {
      ElMessage.success(documentIsEdit.value ? '修改成功' : '新增成功')
      documentDialogVisible.value = false
      documentTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || (documentIsEdit.value ? '修改失败' : '新增失败'))
    }
  } finally {
    documentSubmitLoading.value = false
  }
}

// ===== 版本管理 =====

const resetVersionForm = () => {
  versionForm.id = undefined
  versionForm.kbId = currentKb.id
  versionForm.version = ''
  versionForm.description = ''
  versionFormRef.value?.resetFields()
}

const handleAddVersion = () => {
  resetVersionForm()
  versionDialogVisible.value = true
}

const handlePublishVersion = async (row: any) => {
  const res = await publishKnowledgeBaseVersion(row.id)
  if (res.code === 200) {
    ElMessage.success('发布成功')
    versionTableRef.value?.getTableData()
  } else {
    ElMessage.error(res.msg || '发布失败')
  }
}

const handleArchiveVersion = async (row: any) => {
  const res = await archiveKnowledgeBaseVersion(row.id)
  if (res.code === 200) {
    ElMessage.success('归档成功')
    versionTableRef.value?.getTableData()
  } else {
    ElMessage.error(res.msg || '归档失败')
  }
}

const handleDeleteVersion = (row: any) => {
  ElMessageBox.confirm(`确定删除版本 "${row.version}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delKnowledgeBaseVersion(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      versionTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

const submitVersionForm = async () => {
  const valid = await versionFormRef.value?.validate().catch(() => false)
  if (!valid) return
  versionSubmitLoading.value = true
  try {
    const data = { ...versionForm, kbId: currentKb.id }
    const res = await saveKnowledgeBaseVersion(data)
    if (res.code === 200) {
      ElMessage.success(versionForm.id ? '修改成功' : '新增成功')
      versionDialogVisible.value = false
      versionTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || (versionForm.id ? '修改失败' : '新增失败'))
    }
  } finally {
    versionSubmitLoading.value = false
  }
}

// ===== 权限管理 =====

const permissionTableRef = ref()
const versionTableRef = ref()

const loadRoles = async () => {
  const res = await listRole({ pageSize: 999 })
  if (res.code === 200) {
    roleOptions.value = (res.data?.list || []).map((item: any) => ({
      label: item.roleName,
      value: item.roleId
    }))
  }
}

const loadUsers = async () => {
  const res = await listUser({ pageSize: 999 })
  if (res.code === 200) {
    userOptions.value = (res.data?.list || []).map((item: any) => ({
      label: item.userName,
      value: item.userId
    }))
  }
}

const resetPermissionForm = () => {
  permissionForm.kbId = currentKb.id
  permissionForm.targetType = 'ROLE'
  permissionForm.targetId = undefined
  permissionForm.permission = 'READ'
  permissionFormRef.value?.resetFields()
}

const handleDeletePermission = (row: any) => {
  ElMessageBox.confirm('确定撤销该授权吗？', '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await revokeKnowledgeBasePermission(row.id)
    if (res.code === 200) {
      ElMessage.success('撤销成功')
      permissionTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '撤销失败')
    }
  }).catch(() => {})
}

const submitPermissionForm = async () => {
  const valid = await permissionFormRef.value?.validate().catch(() => false)
  if (!valid) return
  permissionSubmitLoading.value = true
  try {
    const data = { ...permissionForm, kbId: currentKb.id }
    const res = await grantKnowledgeBasePermission(data)
    if (res.code === 200) {
      ElMessage.success('授权成功')
      resetPermissionForm()
      permissionTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || '授权失败')
    }
  } finally {
    permissionSubmitLoading.value = false
  }
}

const targetOptions = (type: string) => {
  return type === 'ROLE' ? roleOptions.value : userOptions.value
}
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="pageKnowledgeBase">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加知识库</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Setting" size="small" text type="primary" @click="handleManage(row)">管理</el-button>
        <el-button size="small" text type="warning" @click="handleToggle(row)">
          {{ row.status === 0 ? '禁用' : '启用' }}
        </el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <!-- 知识库表单 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="Embedding 模型">
          <el-input v-model="form.embeddingModel" placeholder="如 text-embedding-v3" />
        </el-form-item>
        <el-form-item label="模型提供者">
          <el-input v-model="form.providerCode" placeholder="如 dashscope-embedding" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 知识库管理抽屉 -->
    <el-dialog v-model="manageVisible" :title="`管理知识库：${currentKb.name}`" width="900px" append-to-body>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="文档内容" name="document">
          <base-table ref="documentTableRef" :columns="documentColumns" :api="(query: any) => pageKnowledgeDocument(currentKb.id, query)">
            <template #option>
              <el-button :icon="Document" plain type="success" @click="handleAddDocument">添加文档</el-button>
            </template>
            <template #operate="{ row }">
              <el-button :icon="EditPen" size="small" text type="primary" @click="handleEditDocument(row)">编辑</el-button>
              <el-button size="small" text type="warning" @click="handleReindexDocument(row)">重新索引</el-button>
              <el-button :icon="Delete" size="small" text type="danger" @click="handleDeleteDocument(row)">删除</el-button>
            </template>
          </base-table>
        </el-tab-pane>

        <el-tab-pane label="版本控制" name="version">
          <base-table ref="versionTableRef" :columns="versionColumns" :api="() => listKnowledgeBaseVersion(currentKb.id)">
            <template #option>
              <el-button :icon="Collection" plain type="success" @click="handleAddVersion">添加版本</el-button>
            </template>
            <template #operate="{ row }">
              <el-button v-if="row.status === 0" size="small" text type="success" @click="handlePublishVersion(row)">发布</el-button>
              <el-button v-if="row.status === 1" size="small" text type="warning" @click="handleArchiveVersion(row)">归档</el-button>
              <el-button :icon="Delete" size="small" text type="danger" @click="handleDeleteVersion(row)">删除</el-button>
            </template>
          </base-table>
        </el-tab-pane>

        <el-tab-pane label="权限设置" name="permission">
          <el-form ref="permissionFormRef" :model="permissionForm" :rules="permissionRules" inline>
            <el-form-item label="授权对象类型" prop="targetType">
              <el-select v-model="permissionForm.targetType" placeholder="请选择" style="width: 120px">
                <el-option label="角色" value="ROLE" />
                <el-option label="用户" value="USER" />
              </el-select>
            </el-form-item>
            <el-form-item label="授权对象" prop="targetId">
              <el-select v-model="permissionForm.targetId" placeholder="请选择" style="width: 160px">
                <el-option
                    v-for="item in targetOptions(permissionForm.targetType)"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="权限" prop="permission">
              <el-select v-model="permissionForm.permission" placeholder="请选择" style="width: 120px">
                <el-option label="只读" value="READ" />
                <el-option label="管理" value="MANAGE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button :icon="Key" type="primary" :loading="permissionSubmitLoading" @click="submitPermissionForm">授权</el-button>
            </el-form-item>
          </el-form>
          <base-table ref="permissionTableRef" :columns="permissionColumns" :api="() => listKnowledgeBasePermission(currentKb.id)">
            <template #operate="{ row }">
              <el-button :icon="Delete" size="small" text type="danger" @click="handleDeletePermission(row)">撤销</el-button>
            </template>
          </base-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 文档表单 -->
    <el-dialog v-model="documentDialogVisible" :title="documentIsEdit ? '编辑文档' : '添加文档'" width="680px" append-to-body>
      <el-form ref="documentFormRef" :model="documentForm" :rules="documentRules" label-width="100px">
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="documentForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-input v-model="documentForm.sourceType" placeholder="如 TEXT / URL / PDF" />
        </el-form-item>
        <el-form-item label="来源地址">
          <el-input v-model="documentForm.sourceUrl" placeholder="请输入来源地址" />
        </el-form-item>
        <el-form-item label="文档内容" prop="content">
          <el-input v-model="documentForm.content" type="textarea" :rows="8" placeholder="请输入文档内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="documentDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="documentSubmitLoading" @click="submitDocumentForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 版本表单 -->
    <el-dialog v-model="versionDialogVisible" title="添加版本" width="500px" append-to-body>
      <el-form ref="versionFormRef" :model="versionForm" :rules="versionRules" label-width="90px">
        <el-form-item label="版本号" prop="version">
          <el-input v-model="versionForm.version" placeholder="如 v1.0.0" />
        </el-form-item>
        <el-form-item label="版本描述">
          <el-input v-model="versionForm.description" type="textarea" :rows="3" placeholder="请输入版本描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="versionDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="versionSubmitLoading" @click="submitVersionForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
</style>
