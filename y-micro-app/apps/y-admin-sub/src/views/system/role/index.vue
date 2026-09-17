<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listRole, getRole, addRole, updateRole, delRole } from '@/api/role'
import { listMenu } from '@/api/menu'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()

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
    { required: true, message: t('role.roleNameRequired'), trigger: 'blur' }
  ],
  roleKey: [
    { required: true, message: t('role.roleKeyRequired'), trigger: 'blur' }
  ],
  roleSort: [
    { required: true, message: t('role.roleSortRequired'), trigger: 'blur' }
  ]
}

const columns = reactive<ColumnType[]>([
  {
    key: 'roleName',
    label: t('role.roleName'),
    prop: 'roleName',
    render: { el: 'search' }
  },
  {
    key: 'roleKey',
    label: t('role.roleKey'),
    prop: 'roleKey',
    render: { el: 'search' }
  },
  {
    key: 'roleSort',
    label: t('role.roleSort'),
    prop: 'roleSort'
  },
  {
    key: 'status',
    label: t('common.status'),
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: t('menu.normal'), value: '1', elTagType: 'success' },
        { label: t('menu.disabled'), value: '0', elTagType: 'danger' }
      ]
    }
  },
  {
    key: 'createTime',
    label: t('role.createTime'),
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
  dialogTitle.value = t('role.addRole')
  dialogVisible.value = true
  loadMenus()
}

const handleEdit = async (row: any) => {
  resetForm()
  isEdit.value = true
  dialogTitle.value = t('role.editRole')
  dialogVisible.value = true
  await loadMenus()
  const res = await getRole(row.id)
  if (res.code === 200) {
    Object.assign(form, res.data)
    menuTreeRef.value?.setCheckedKeys(res.data.menuIds || [])
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(t('role.deleteConfirm', { name: row.roleName }), t('common.tips'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    const res = await delRole(row.id)
    if (res.code === 200) {
      ElMessage.success(t('role.deleteSuccess'))
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || t('role.deleteFailed'))
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
      ElMessage.success(isEdit.value ? t('role.editSuccess') : t('role.addSuccess'))
      dialogVisible.value = false
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || (isEdit.value ? t('role.updateFailed') : t('role.addFailed')))
    }
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="listRole">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="handleAdd">{{ t('common.add') }}</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
        <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('role.roleName')" prop="roleName">
          <el-input v-model="form.roleName" :placeholder="t('role.roleNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('role.roleKey')" prop="roleKey">
          <el-input v-model="form.roleKey" :placeholder="t('role.roleKeyPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('role.roleSort')" prop="roleSort">
          <el-input-number v-model="form.roleSort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('role.dataScope')" prop="dataScope">
          <el-select v-model="form.dataScope" :placeholder="t('role.dataScopePlaceholder')" style="width: 100%">
            <el-option :label="t('role.dataScopeAll')" value="1" />
            <el-option :label="t('role.dataScopeCustom')" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="1">{{ t('menu.normal') }}</el-radio>
            <el-radio label="0">{{ t('menu.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('role.menuPermission')">
          <el-tree
              ref="menuTreeRef"
              :data="menuOptions"
              show-checkbox
              node-key="id"
              :props="{ children: 'children', label: 'label' }"
              style="width: 100%; max-height: 300px; overflow-y: auto;"
          />
        </el-form-item>
        <el-form-item :label="t('role.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" :placeholder="t('role.remarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">{{ t('common.confirm') }}</el-button>
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
