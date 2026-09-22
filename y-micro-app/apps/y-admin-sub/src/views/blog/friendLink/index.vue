<script setup lang="ts">
import { reactive, ref } from 'vue'
import { getFriendLinks, getFriendLinkById, addFriendLink, updateFriendLink, deleteFriendLink } from '@/api/blog/friendLink'
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
  name: '',
  url: '',
  logo: '',
  description: '',
  email: '',
  sort: 0,
  status: 1,
  remark: ''
})

const formRef = ref()
const rules = {
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入站点地址', trigger: 'blur' }]
}

const statusOptions: any[] = [
  { label: '隐藏', value: 0, elTagType: 'info' },
  { label: '显示', value: 1, elTagType: 'success' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'name',
    label: '站点名称',
    prop: 'name',
    render: { el: 'search' }
  },
  {
    key: 'url',
    label: '站点地址',
    prop: 'url'
  },
  {
    key: 'logo',
    label: '站点 Logo',
    prop: 'logo',
    type: 'image'
  },
  {
    key: 'description',
    label: '描述',
    prop: 'description'
  },
  {
    key: 'email',
    label: '站长邮箱',
    prop: 'email'
  },
  {
    key: 'sort',
    label: '排序',
    prop: 'sort'
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: { el: 'select', list: statusOptions as any }
  },
  {
    key: 'remark',
    label: '备注',
    prop: 'remark'
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
  form.url = ''
  form.logo = ''
  form.description = ''
  form.email = ''
  form.sort = 0
  form.status = 1
  form.remark = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加友链'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑友链'
  dialogVisible.value = true
  const res = await getFriendLinkById(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除友链 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteFriendLink(row.id)
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
    const res = isEdit.value ? await updateFriendLink(data) : await addFriendLink(data)
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
    <base-table ref="baseTableRef" :columns="columns" :api="getFriendLinks">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加友链</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入站点名称" />
        </el-form-item>
        <el-form-item label="站点地址" prop="url">
          <el-input v-model="form.url" placeholder="请输入站点地址" />
        </el-form-item>
        <el-form-item label="站点 Logo">
          <el-input v-model="form.logo" placeholder="请输入 Logo 地址" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入站点描述" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="站长邮箱">
              <el-input v-model="form.email" placeholder="请输入站长邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
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
