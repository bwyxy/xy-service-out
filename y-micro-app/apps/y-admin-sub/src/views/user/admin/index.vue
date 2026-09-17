<script setup lang="ts">
import { reactive, ref } from 'vue'
import { listUser, getUser, addUser, updateUser, delUser } from '@/api/user'
import { listRole } from '@/api/role'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const roleOptions = ref<any[]>([])
const submitLoading = ref(false)

const form = reactive<any>({
  id: undefined,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  sex: '0',
  status: '0',
  roleIds: []
})

const formRef = ref()
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: !isEdit.value, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '长度在 5 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'username',
    label: '用户名',
    prop: 'username',
    render: { el: 'search' }
  },
  {
    key: 'nickname',
    label: '昵称',
    prop: 'nickname',
    render: { el: 'search' }
  },
  {
    key: 'email',
    label: '邮箱',
    prop: 'email',
    render: { el: 'search' }
  },
  {
    key: 'phone',
    label: '手机号',
    prop: 'phone',
    render: { el: 'search' }
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '正常', value: '0', elTagType: "success" },
        { label: '停用', value: '1', elTagType: "danger" }
      ]
    }
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime',
  }
])

const loadRoles = async () => {
  const res = await listRole({ pageNum: 1, pageSize: 999 })
  if (res.code === 200) {
    roleOptions.value = res.data.list || []
  }
}

const resetForm = () => {
  form.id = undefined
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.sex = '0'
  form.status = '0'
  form.roleIds = []
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加用户'
  dialogVisible.value = true
  loadRoles()
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  dialogVisible.value = true
  await loadRoles()
  const res = await getUser(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
    form.password = ''
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delUser(row.id)
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
    if (isEdit.value) {
      if (!data.password) delete data.password
      const res = await updateUser(data)
      if (res.code === 200) {
        ElMessage.success('修改成功')
      } else {
        ElMessage.error(res.msg || '修改失败')
        return
      }
    } else {
      const res = await addUser(data)
      if (res.code === 200) {
        ElMessage.success('新增成功')
      } else {
        ElMessage.error(res.msg || '新增失败')
        return
      }
    }
    dialogVisible.value = false
    baseTableRef.value?.getTableData()
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="listUser">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">添加</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码，编辑时留空不修改" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio label="0">未知</el-radio>
            <el-radio label="1">男</el-radio>
            <el-radio label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
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
