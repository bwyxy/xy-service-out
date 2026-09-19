<script setup lang="ts">
import { reactive, ref } from 'vue'
import { listTag, getTag, addTag, updateTag, delTag } from '@/api/todo/tag'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)

const form = reactive<any>({
  tagId: undefined,
  userId: undefined,
  tagName: '',
  color: ''
})

const formRef = ref()
const rules = {
  tagName: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { max: 24, message: '长度不能超过24个字符', trigger: 'blur' }
  ]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'tagName',
    label: '标签名称',
    prop: 'tagName',
    render: { el: 'search' }
  },
  {
    key: 'color',
    label: '颜色',
    prop: 'color',
  },
  {
    key: 'createBy',
    label: '创建者',
    prop: 'createBy'
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  },
  {
    key: 'updateTime',
    label: '更新时间',
    prop: 'updateTime'
  }
])

const resetForm = () => {
  form.tagId = undefined
  form.userId = undefined
  form.tagName = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加标签'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑标签'
  dialogVisible.value = true
  const res = await getTag(row.tagId)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除标签 "${row.tagName}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delTag(row.tagId)
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
    const res = isEdit.value ? await updateTag(data) : await addTag(data)
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
    <base-table ref="baseTableRef" :columns="columns" :api="listTag">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加标签</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="form.color" />
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
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
