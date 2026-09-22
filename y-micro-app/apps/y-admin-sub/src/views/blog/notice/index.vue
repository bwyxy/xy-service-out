<script setup lang="ts">
import { reactive, ref } from 'vue'
import { getNotices, getNoticeById, addNotice, updateNotice, deleteNotice } from '@/api/blog/notice'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)

const form = reactive<any>({
  id: undefined,
  title: '',
  content: '',
  type: 1,
  status: 0,
  isTop: 0,
  sort: 0,
  linkUrl: '',
  startTime: '',
  endTime: ''
})

const formRef = ref()
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }]
}

const typeOptions: any[] = [
  { label: '普通', value: 1, elTagType: 'info' },
  { label: '重要', value: 2, elTagType: 'warning' },
  { label: '紧急', value: 3, elTagType: 'danger' }
]

const statusOptions: any[] = [
  { label: '草稿', value: 0, elTagType: 'info' },
  { label: '发布', value: 1, elTagType: 'success' },
  { label: '下线', value: 2, elTagType: 'danger' }
]

const topOptions: any[] = [
  { label: '否', value: 0, elTagType: 'info' },
  { label: '是', value: 1, elTagType: 'success' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'title',
    label: '标题',
    prop: 'title',
    render: { el: 'search' }
  },
  {
    key: 'type',
    label: '类型',
    prop: 'type',
    type: 'tag',
    render: { el: 'select', list: typeOptions as any }
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: { el: 'select', list: statusOptions as any }
  },
  {
    key: 'isTop',
    label: '置顶',
    prop: 'isTop',
    type: 'tag',
    render: { el: 'select', list: topOptions as any }
  },
  {
    key: 'sort',
    label: '排序',
    prop: 'sort'
  },
  {
    key: 'startTime',
    label: '生效时间',
    prop: 'startTime'
  },
  {
    key: 'endTime',
    label: '失效时间',
    prop: 'endTime'
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const resetForm = () => {
  form.id = undefined
  form.title = ''
  form.content = ''
  form.type = 1
  form.status = 0
  form.isTop = 0
  form.sort = 0
  form.linkUrl = ''
  form.startTime = ''
  form.endTime = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加公告'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑公告'
  dialogVisible.value = true
  const res = await getNoticeById(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除公告 "${row.title}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteNotice(row.id)
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
    const res = isEdit.value ? await updateNotice(data) : await addNotice(data)
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
    <base-table ref="baseTableRef" :columns="columns" :api="getNotices">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加公告</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入公告内容" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="公告类型">
              <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in typeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公告状态">
              <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否置顶">
              <el-switch
                v-model="form.isTop"
                :active-value="1"
                :inactive-value="0"
                active-text="是"
                inactive-text="否"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="请输入跳转链接" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生效时间">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择生效时间"
                style="width: 100%"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失效时间">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择失效时间"
                style="width: 100%"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
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
.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
