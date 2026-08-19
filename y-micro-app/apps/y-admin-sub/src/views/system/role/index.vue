<script setup lang="ts">
import { reactive, ref } from 'vue'
import { listRole, getRole, addRole, updateRole, delRole } from '@/api/role'
import { listMenu } from '@/api/menu'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const menuOptions = ref<any[]>([])
const menuTreeRef = ref()
const submitLoading = ref(false)

const form = reactive<any>({
  id: undefined,
  roleName: '',
  roleKey: '',
  roleSort: 0,
  dataScope: '2',
  status: '0',
  remark: '',
  menuIds: []
})

const formRef = ref()
const rules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  roleKey: [
    { required: true, message: '请输入权限字符', trigger: 'blur' }
  ],
  roleSort: [
    { required: true, message: '请输入显示顺序', trigger: 'blur' }
  ]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'roleName',
    label: '角色名称',
    prop: 'roleName',
    render: { el: 'search' }
  },
  {
    key: 'roleKey',
    label: '权限字符',
    prop: 'roleKey',
    render: { el: 'search' }
  },
  {
    key: 'roleSort',
    label: '显示顺序',
    prop: 'roleSort'
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '正常', value: '1', elTagType: "success" },
        { label: '停用', value: '0', elTagType: "danger" }
      ]
    }
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  }
])

const loadMenus = async () => {
  const res = await listMenu({})
  if (res.code === 200) {
    menuOptions.value = buildTree(res.data || [])
  }
}

const buildTree = (menus: any[]) => {
  const map: Record<string, any> = {}
  const roots: any[] = []
  menus.forEach((item: any) => {
    map[item.menuId] = { ...item, id: item.menuId, label: item.menuName, children: [] }
  })
  menus.forEach((item: any) => {
    const node = map[item.menuId]
    if (item.parentId === 0 || !map[item.parentId]) {
      roots.push(node)
    } else {
      map[item.parentId]?.children.push(node)
    }
  })
  return roots
}

const resetForm = () => {
  form.id = undefined
  form.roleName = ''
  form.roleKey = ''
  form.roleSort = 0
  form.dataScope = '2'
  form.status = '0'
  form.remark = ''
  form.menuIds = []
  menuTreeRef.value?.setCheckedKeys([])
  formRef.value?.resetFields()
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加角色'
  dialogVisible.value = true
  loadMenus()
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
  await loadMenus()
  const res = await getRole(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
    menuTreeRef.value?.setCheckedKeys(res.data.menuIds || [])
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除角色 "${row.roleName}" 吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await delRole(row.id)
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
    const checkedKeys = menuTreeRef.value?.getCheckedKeys() || []
    const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
    form.menuIds = [...checkedKeys, ...halfCheckedKeys]
    const data = { ...form }
    const res = isEdit.value ? await updateRole(data) : await addRole(data)
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
  <base-table ref="baseTableRef" :columns="columns" :api="listRole">
    <template #option>
      <el-button :icon="Plus" plain type="success" @click="handleAdd">添加</el-button>
    </template>
    <template #operate="{ row }">
      <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
      <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
    </template>
  </base-table>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="form.roleName" placeholder="请输入角色名称" />
      </el-form-item>
      <el-form-item label="权限字符" prop="roleKey">
        <el-input v-model="form.roleKey" placeholder="请输入权限字符" />
      </el-form-item>
      <el-form-item label="显示顺序" prop="roleSort">
        <el-input-number v-model="form.roleSort" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item label="数据范围" prop="dataScope">
        <el-select v-model="form.dataScope" placeholder="请选择数据范围" style="width: 100%">
          <el-option label="全部数据" value="1" />
          <el-option label="自定义数据" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio label="1">正常</el-radio>
          <el-radio label="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单权限">
        <el-tree
            ref="menuTreeRef"
            :data="menuOptions"
            show-checkbox
            node-key="id"
            :props="{ children: 'children', label: 'label' }"
            style="width: 100%; max-height: 300px; overflow-y: auto;"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
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
