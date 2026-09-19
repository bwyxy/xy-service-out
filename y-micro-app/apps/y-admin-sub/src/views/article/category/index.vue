<script setup lang="ts">
import { reactive, ref } from 'vue'
import { getCategorys, getCategoryById, addCategory, updateCategory, deleteCategory } from '@/api/blog/category'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)

const form = reactive<any>({
  categoryId: undefined,
  name: '',
  slug: '',
  description: ''
})

const formRef = ref()
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'name',
    label: '分类名称',
    prop: 'name',
    render: { el: 'search' }
  },
  {
    key: 'slug',
    label: 'URL别名',
    prop: 'slug'
  },
  {
    key: 'description',
    label: '描述',
    prop: 'description'
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const resetForm = () => {
  form.categoryId = undefined
  form.name = ''
  form.slug = ''
  form.description = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加分类'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑分类'
  dialogVisible.value = true
  const res = await getCategoryById(row.categoryId)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除分类 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteCategory(row.categoryId)
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
    const res = isEdit.value ? await updateCategory(data) : await addCategory(data)
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
    <base-table ref="baseTableRef" :columns="columns" :api="getCategorys">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加分类</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="URL别名">
          <el-input v-model="form.slug" placeholder="请输入URL别名" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入分类描述" />
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
.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>