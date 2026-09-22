<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { getPhotos, getPhotoById, addPhoto, updatePhoto, deletePhoto } from '@/api/blog/photo'
import { getCategorys } from '@/api/blog/category'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const categoryOptions = ref<any[]>([])

const form = reactive<any>({
  id: undefined,
  categoryId: undefined,
  name: '',
  photoUrl: '',
  description: '',
  status: 1,
  sort: 0,
  publishedTime: ''
})

const formRef = ref()
const rules = {
  name: [{ required: true, message: '请输入照片名称', trigger: 'blur' }],
  photoUrl: [{ required: true, message: '请输入照片链接', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择相册分类', trigger: 'change' }]
}

const statusOptions: any[] = [
  { label: '私密', value: 0, elTagType: 'info' },
  { label: '公开', value: 1, elTagType: 'success' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'name',
    label: '照片名称',
    prop: 'name',
    render: { el: 'search' }
  },
  {
    key: 'categoryId',
    label: '所属相册',
    prop: 'categoryId',
    type: 'tag',
    render: { el: 'select', list: categoryOptions.value as any }
  },
  {
    key: 'photoUrl',
    label: '照片',
    prop: 'photoUrl',
    type: 'image'
  },
  {
    key: 'description',
    label: '描述',
    prop: 'description'
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: { el: 'select', list: statusOptions as any }
  },
  {
    key: 'sort',
    label: '排序',
    prop: 'sort'
  },
  {
    key: 'publishedTime',
    label: '拍摄时间',
    prop: 'publishedTime'
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const fetchCategories = async () => {
  const res = await getCategorys({ type: 'album', pageNum: 1, pageSize: 999 })
  if (res.code === 200) {
    const list = res.data.list || []
    categoryOptions.value = list.map((item: any) => ({
      label: item.name,
      value: item.categoryId
    }))
    const categoryColumn = columns.find(item => item.key === 'categoryId')
    if (categoryColumn && categoryColumn.render) {
      categoryColumn.render.list = categoryOptions.value as any
    }
  }
}

const resetForm = () => {
  form.id = undefined
  form.categoryId = undefined
  form.name = ''
  form.photoUrl = ''
  form.description = ''
  form.status = 1
  form.sort = 0
  form.publishedTime = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加照片'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑照片'
  dialogVisible.value = true
  const res = await getPhotoById(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除照片 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deletePhoto(row.id)
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
    const res = isEdit.value ? await updatePhoto(data) : await addPhoto(data)
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

onMounted(() => {
  fetchCategories()
})
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="getPhotos">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加照片</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="照片名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入照片名称" />
        </el-form-item>
        <el-form-item label="所属相册" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择相册分类" style="width: 100%">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="照片链接" prop="photoUrl">
          <image-upload v-model="form.photoUrl" />
<!--          <el-input v-model="form.photoUrl" placeholder="请输入照片链接" />-->
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态">
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
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="拍摄时间">
          <el-date-picker
            v-model="form.publishedTime"
            type="datetime"
            placeholder="选择拍摄时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入照片描述" />
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
