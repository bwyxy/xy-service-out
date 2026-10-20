<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  listAppVersion,
  getAppVersion,
  addAppVersion,
  updateAppVersion,
  delAppVersion
} from '@/api/appVersion'
import { uploadFile } from '@/api/upload'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { Plus, EditPen, Delete, UploadFilled, Top, Bottom, Link } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const uploadLoading = ref(false)
const formRef = ref()

/** 应用选项，后续接入新应用时在此追加 */
const appList: Value[] = [
  { label: '飞鸽聊天', value: 'chat', elTagType: 'primary' }
]

/** 平台选项 */
const platformList: Value[] = [
  { label: '网页端', value: 'web', elTagType: 'primary' },
  { label: 'Windows', value: 'windows', elTagType: 'success' },
  { label: 'macOS', value: 'macos', elTagType: 'info' }
]

const forceList: Value[] = [
  { label: '可选更新', value: '0', elTagType: 'info' },
  { label: '强制更新', value: '1', elTagType: 'danger' }
]

const statusList: Value[] = [
  { label: '已上架', value: '0', elTagType: 'success' },
  { label: '已下架', value: '1', elTagType: 'danger' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'appKey',
    label: '应用',
    prop: 'appKey',
    width: 130,
    type: 'tag',
    render: { el: 'select', list: appList }
  },
  {
    key: 'platform',
    label: '平台',
    prop: 'platform',
    width: 110,
    type: 'tag',
    render: { el: 'select', list: platformList }
  },
  {
    key: 'versionName',
    label: '版本号',
    prop: 'versionName',
    width: 130,
    render: { el: 'search' }
  },
  {
    key: 'versionCode',
    label: '版本码',
    prop: 'versionCode',
    width: 100
  },
  {
    key: 'fileSizeText',
    label: '安装包',
    prop: 'fileSizeText',
    width: 200
  },
  {
    key: 'forceUpdate',
    label: '更新策略',
    prop: 'forceUpdate',
    width: 110,
    type: 'tag',
    render: { el: 'select', list: forceList }
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    width: 100,
    type: 'tag',
    render: { el: 'select', list: statusList }
  },
  {
    key: 'createTime',
    label: '发布时间',
    prop: 'createTime',
    width: 170
  }
])

const form = reactive<any>({
  id: undefined,
  appKey: 'chat',
  platform: 'windows',
  versionName: '',
  versionCode: undefined,
  fileName: '',
  fileSize: undefined,
  downloadUrl: '',
  changelog: '',
  forceUpdate: '0',
  status: '0'
})

const rules = {
  appKey: [{ required: true, message: '请选择应用', trigger: 'change' }],
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
  versionName: [{ required: true, message: '请输入版本号，如 1.2.0', trigger: 'blur' }],
  versionCode: [{ required: true, message: '请输入版本码，如 10200', trigger: 'blur' }]
}

/** 字节转可读大小 */
const formatSize = (size?: number) => {
  if (!size || size <= 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

const resetForm = () => {
  form.id = undefined
  form.appKey = 'chat'
  form.platform = 'windows'
  form.versionName = ''
  form.versionCode = undefined
  form.fileName = ''
  form.fileSize = undefined
  form.downloadUrl = ''
  form.changelog = ''
  form.forceUpdate = '0'
  form.status = '0'
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '发布新版本'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑版本'
  dialogVisible.value = true
  const res = await getAppVersion(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

/** 安装包上传，成功后回填下载地址与文件信息 */
const handleUpload = async (options: any) => {
  const file: File = options.file
  uploadLoading.value = true
  try {
    const res = await uploadFile(file)
    if (res.code === 200) {
      form.downloadUrl = res.data
      form.fileName = file.name
      form.fileSize = file.size
      ElMessage.success('安装包上传成功')
      options.onSuccess?.(res)
    } else {
      ElMessage.error(res.msg || '安装包上传失败')
      options.onError?.(new Error(res.msg || '上传失败'))
    }
  } catch (e) {
    options.onError?.(e as Error)
  } finally {
    uploadLoading.value = false
  }
}

const handleToggleStatus = (row: any) => {
  const offline = row.status === '0'
  const action = offline ? '下架' : '上架'
  ElMessageBox.confirm(`确定${action}版本 ${row.versionName}（${row.platform}）吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await updateAppVersion({ id: row.id, appKey: row.appKey, platform: row.platform, versionName: row.versionName, versionCode: row.versionCode, status: offline ? '1' : '0' })
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || `${action}失败`)
    }
  }).catch(() => {})
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除版本 ${row.versionName}（${row.platform}）吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delAppVersion(row.id)
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
    const res: any = isEdit.value ? await updateAppVersion(data) : await addAppVersion(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '发布成功')
      dialogVisible.value = false
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || (isEdit.value ? '修改失败' : '发布失败'))
    }
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="listAppVersion">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">发布新版本</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button
            :icon="row.status === '0' ? Bottom : Top"
            size="small"
            text
            :type="row.status === '0' ? 'warning' : 'success'"
            @click="handleToggleStatus(row)"
        >
          {{ row.status === '0' ? '下架' : '上架' }}
        </el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="应用" prop="appKey">
          <el-select v-model="form.appKey" placeholder="请选择应用" style="width: 100%">
            <el-option v-for="item in appList" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-select v-model="form.platform" placeholder="请选择平台" style="width: 100%">
            <el-option v-for="item in platformList" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="versionName">
          <el-input v-model="form.versionName" placeholder="展示给用户的版本，如 1.2.0" />
        </el-form-item>
        <el-form-item label="版本码" prop="versionCode">
          <el-input v-model.number="form.versionCode" placeholder="用于比对大小，需严格递增，如 10200" />
        </el-form-item>
        <el-form-item label="安装包">
          <el-upload
              :show-file-list="false"
              :http-request="handleUpload"
              :disabled="uploadLoading"
          >
            <el-button :icon="UploadFilled" :loading="uploadLoading">选择安装包并上传</el-button>
          </el-upload>
          <div v-if="form.downloadUrl" class="upload-tip">
            <el-icon><Link /></el-icon>
            <span>{{ form.fileName }}（{{ formatSize(form.fileSize) }}）</span>
            <el-link type="primary" :href="form.downloadUrl" target="_blank">下载地址</el-link>
          </div>
          <div v-else class="upload-tip upload-tip--empty">网页端可不上传安装包，客户端更新必须上传</div>
        </el-form-item>
        <el-form-item label="更新策略" prop="forceUpdate">
          <el-radio-group v-model="form.forceUpdate">
            <el-radio label="0">可选更新</el-radio>
            <el-radio label="1">强制更新</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">上架</el-radio>
            <el-radio label="1">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="更新说明" prop="changelog">
          <el-input v-model="form.changelog" type="textarea" :rows="4" placeholder="每行一条更新内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.upload-tip {
  display: flex;
  align-items: center;
  gap: size(8);
  margin-top: size(8);
  font-size: size(13);
  color: var(--el-text-color-regular);

  &--empty {
    color: var(--el-text-color-secondary);
  }
}
</style>
