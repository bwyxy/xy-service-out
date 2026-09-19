<script setup lang="ts">
import { reactive, ref } from 'vue'
import { getTagList, getTagInfo, addTag, editTag, deleteTag } from '@/api/blog/tag'
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
  name: '',
  slug: '',
  color: '',
  level: undefined,
  description: ''
})

const formRef = ref()
const rules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'name',
    label: '标签名称',
    prop: 'name',
    render: { el: 'search' }
  },
  {
    key: 'color',
    label: '颜色',
    prop: 'color'
  },
  {
    key: 'slug',
    label: 'URL别名',
    prop: 'slug'
  },
  {
    key: 'level',
    label: '等级',
    prop: 'level'
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
  form.tagId = undefined
  form.name = ''
  form.slug = ''
  form.color = ''
  form.level = undefined
  form.description = ''
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
  const res = await getTagInfo(row.tagId)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除标签 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteTag(row.tagId)
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
    const res = isEdit.value ? await editTag(data) : await addTag(data)
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
    <base-table ref="baseTableRef" :columns="columns" :api="getTagList">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加标签</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="颜色">
              <el-color-picker v-model="form.color" show-alpha />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="等级">
              <el-input-number v-model="form.level" :min="0" :max="99" placeholder="请输入等级" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="URL别名">
          <el-input v-model="form.slug" placeholder="请输入URL别名" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入标签描述" />
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