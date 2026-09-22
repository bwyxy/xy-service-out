<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { getTools, getToolById, addTool, updateTool, deleteTool } from '@/api/blog/tool'
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
  url: '',
  icon: '',
  description: '',
  platform: '',
  priceType: 0,
  sort: 0,
  status: 1,
  isRecommend: 0
})

const formRef = ref()
const rules = {
  name: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入工具地址', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const priceTypeOptions: any[] = [
  { label: '免费', value: 0, elTagType: 'success' },
  { label: '付费', value: 1, elTagType: 'danger' },
  { label: 'Freemium', value: 2, elTagType: 'warning' }
]

const statusOptions: any[] = [
  { label: '隐藏', value: 0, elTagType: 'info' },
  { label: '显示', value: 1, elTagType: 'success' }
]

const recommendOptions: any[] = [
  { label: '否', value: 0, elTagType: 'info' },
  { label: '是', value: 1, elTagType: 'success' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'name',
    label: '工具名称',
    prop: 'name',
    render: { el: 'search' }
  },
  {
    key: 'categoryId',
    label: '所属分类',
    prop: 'categoryId',
    type: 'tag',
    render: { el: 'select', list: categoryOptions.value as any }
  },
  {
    key: 'url',
    label: '工具地址',
    prop: 'url'
  },
  {
    key: 'icon',
    label: '图标',
    prop: 'icon',
    type: 'image'
  },
  {
    key: 'platform',
    label: '平台',
    prop: 'platform'
  },
  {
    key: 'priceType',
    label: '费用',
    prop: 'priceType',
    type: 'tag',
    render: { el: 'select', list: priceTypeOptions as any }
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
    key: 'isRecommend',
    label: '推荐',
    prop: 'isRecommend',
    type: 'tag',
    render: { el: 'select', list: recommendOptions as any }
  },
  {
    key: 'clickCount',
    label: '点击数',
    prop: 'clickCount'
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const fetchCategories = async () => {
  const res = await getCategorys({ type: 'tool', pageNum: 1, pageSize: 999 })
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
  form.url = ''
  form.icon = ''
  form.description = ''
  form.platform = ''
  form.priceType = 0
  form.sort = 0
  form.status = 1
  form.isRecommend = 0
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加工具'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑工具'
  dialogVisible.value = true
  const res = await getToolById(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除工具 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteTool(row.id)
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
    const res = isEdit.value ? await updateTool(data) : await addTool(data)
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
    <base-table ref="baseTableRef" :columns="columns" :api="getTools">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加工具</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="工具名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入工具名称" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工具地址" prop="url">
          <el-input v-model="form.url" placeholder="请输入工具地址" />
        </el-form-item>
        <el-form-item label="图标">
          <image-upload v-model="form.icon" />
<!--          <el-input v-model="form.icon" placeholder="请输入图标地址" />-->
        </el-form-item>
        <el-form-item label="平台">
          <el-input v-model="form.platform" placeholder="请输入平台，如 Web / iOS / Android" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="费用类型">
              <el-select v-model="form.priceType" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in priceTypeOptions"
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
            <el-form-item label="站长推荐">
              <el-switch
                v-model="form.isRecommend"
                :active-value="1"
                :inactive-value="0"
                active-text="是"
                inactive-text="否"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入工具描述" />
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
