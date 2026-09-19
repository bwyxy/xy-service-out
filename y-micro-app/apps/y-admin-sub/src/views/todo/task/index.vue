<script setup lang="ts">
import { reactive, ref } from 'vue'
import { listTask, getTask, addTask, updateTask, delTask } from '@/api/todo/task'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)

const form = reactive<any>({
  taskId: undefined,
  userId: undefined,
  parentId: undefined,
  parentTitle: '',
  taskTitle: '',
  description: '',
  completed: 0,
  urgencyLevel: 1,
  startTime: '',
  dueDate: ''
})

const formRef = ref()
const rules = {
  taskTitle: [
    { required: true, message: '请输入任务标题', trigger: 'blur' }
  ],
  urgencyLevel: [
    { required: true, message: '请选择紧急程度', trigger: 'change' }
  ]
}

const urgencyOptions: { label: string; value: number; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }[] = [
  { label: '不重要不紧急', value: 1, type: 'info' },
  { label: '紧急不重要', value: 2, type: 'warning' },
  { label: '重要不紧急', value: 3, type: 'primary' },
  { label: '重要且紧急', value: 4, type: 'danger' }
]

const completedOptions: { label: string; value: number; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }[] = [
  { label: '未完成', value: 0, type: 'info' },
  { label: '已完成', value: 1, type: 'success' }
]

const columns = reactive<ColumnType[]>([
  {
    key: 'taskTitle',
    label: '任务标题',
    prop: 'taskTitle',
    render: { el: 'search' }
  },
  {
    key: 'description',
    label: '描述',
    prop: 'description'
  },
  {
    key: 'urgencyLevel',
    label: '紧急程度',
    prop: 'urgencyLevel',
    type: 'tag',
    render: {
      el: 'select',
      list: urgencyOptions.map(item => ({ label: item.label, value: item.value, elTagType: item.type }))
    }
  },
  {
    key: 'completed',
    label: '完成状态',
    prop: 'completed',
    type: 'tag',
    render: {
      el: 'select',
      list: completedOptions.map(item => ({ label: item.label, value: item.value, elTagType: item.type }))
    }
  },
  {
    key: 'startTime',
    label: '开始时间',
    prop: 'startTime'
  },
  {
    key: 'dueDate',
    label: '截止时间',
    prop: 'dueDate'
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const resetForm = () => {
  form.taskId = undefined
  form.userId = undefined
  form.parentId = undefined
  form.parentTitle = ''
  form.taskTitle = ''
  form.description = ''
  form.completed = 0
  form.urgencyLevel = 1
  form.startTime = ''
  form.dueDate = ''
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加任务'
  dialogVisible.value = true
}

const handleAddSubTask = (row: any) => {
  resetForm()
  isEdit.value = false
  form.parentId = row.taskId
  form.parentTitle = row.taskTitle
  dialogTitle.value = '添加子任务'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑任务'
  dialogVisible.value = true
  const res = await getTask(row.taskId)
  if (res.code === 200) {
    Object.assign(form, res.data)
    if (form.parentId) {
      const parentRes = await getTask(form.parentId)
      if (parentRes.code === 200) {
        form.parentTitle = parentRes.data?.taskTitle || ''
      }
    }
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除任务 "${row.taskTitle}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delTask(row.taskId)
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
    const res = isEdit.value ? await updateTask(data) : await addTask(data)
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
    <base-table ref="baseTableRef" :columns="columns" :api="listTask">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加任务</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Plus" size="small" text type="success" @click="handleAddSubTask(row)">子任务</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="form.parentId" label="父任务">
          <el-input v-model="form.parentTitle" disabled />
        </el-form-item>
        <el-form-item label="任务标题" prop="taskTitle">
          <el-input v-model="form.taskTitle" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="紧急程度" prop="urgencyLevel">
          <el-select v-model="form.urgencyLevel" placeholder="请选择紧急程度" style="width: 100%">
            <el-option v-for="item in urgencyOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="完成状态" prop="completed">
          <el-radio-group v-model="form.completed">
            <el-radio :label="0">未完成</el-radio>
            <el-radio :label="1">已完成</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="请选择开始时间" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="截止时间" prop="dueDate">
          <el-date-picker v-model="form.dueDate" type="datetime" placeholder="请选择截止时间" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
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
