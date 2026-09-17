<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { listMenu, getMenu, addMenu, updateMenu, delMenu } from '@/api/menu'
import { ElMessage, ElMessageBox } from 'element-plus'
import IconSelect from '@/components/IconSelect/index.vue'
import { Plus, EditPen, Delete, Search, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()

// 菜单数据
const menuList = ref<any[]>([])
const loading = ref(false)
const searchMenuName = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  menuId: null as number | null,
  parentId: 0,
  menuName: '',
  menuType: 'M',
  path: '',
  component: '',
  routeName: '',
  icon: '',
  orderNum: 0,
  isFrame: '1',
  isCache: '0',
  visible: '0',
  status: '0',
  perms: '',
  query: '',
})

const rules: FormRules = {
  menuName: [{ required: true, message: t('menu.menuNameRequired'), trigger: 'blur' }],
  menuType: [{ required: true, message: t('menu.menuTypeRequired'), trigger: 'change' }],
  path: [{ required: true, message: t('menu.pathRequired'), trigger: 'blur' }],
  orderNum: [{ required: true, message: t('menu.orderNumRequired'), trigger: 'blur' }],
}

// 加载菜单列表
const loadMenuList = async () => {
  loading.value = true
  try {
    const query: any = {}
    if (searchMenuName.value.trim()) {
      query.menuName = searchMenuName.value.trim()
    }
    const res = await listMenu(query)
    menuList.value = buildTree(res.data || [])
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  loadMenuList()
}

// 重置搜索
const handleReset = () => {
  searchMenuName.value = ''
  loadMenuList()
}

// 构建树形结构
const buildTree = (list: any[], parentId = 0): any[] => {
  return list
    .filter((item) => item.parentId === parentId)
    .map((item) => ({
      ...item,
      children: buildTree(list, item.menuId),
    }))
}

// 重置表单
const resetForm = () => {
  form.menuId = null
  form.parentId = 0
  form.menuName = ''
  form.menuType = 'M'
  form.path = ''
  form.component = ''
  form.routeName = ''
  form.icon = ''
  form.orderNum = 0
  form.isFrame = '1'
  form.isCache = '0'
  form.visible = '0'
  form.status = '0'
  form.perms = ''
  form.query = ''
}

// 新增
const handleAdd = (parentId = 0) => {
  resetForm()
  form.parentId = parentId
  dialogTitle.value = t('menu.addMenu')
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: any) => {
  const res = await getMenu(row.menuId)
  const data = res.data
  Object.assign(form, {
    menuId: data.menuId,
    parentId: data.parentId,
    menuName: data.menuName,
    menuType: data.menuType,
    path: data.path,
    component: data.component || '',
    routeName: data.routeName || '',
    icon: data.icon || '',
    orderNum: data.orderNum,
    isFrame: data.isFrame,
    isCache: data.isCache,
    visible: data.visible,
    status: data.status,
    perms: data.perms || '',
    query: data.query || '',
  })
  dialogTitle.value = t('menu.editMenu')
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(t('menu.deleteConfirm'), t('common.tips'), { type: 'warning' })
  await delMenu(row.menuId)
  ElMessage.success(t('menu.deleteSuccess'))
  loadMenuList()
}

// 提交
const handleSubmit = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  if (form.menuId) {
    await updateMenu({ ...form })
    ElMessage.success(t('menu.editSuccess'))
  } else {
    await addMenu({ ...form })
    ElMessage.success(t('menu.addSuccess'))
  }
  dialogVisible.value = false
  loadMenuList()
}

const getMenuTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    M: t('menu.directory'),
    C: t('menu.menu'),
    F: t('menu.button'),
  }
  return map[type] || type
}

onMounted(() => {
  loadMenuList()
})
</script>

<template>
  <div class="menu-container">
    <div class="search-table-top">
      <el-form :inline="true">
        <el-form-item :label="t('menu.menuName')">
          <el-input v-model="searchMenuName" :placeholder="t('menu.menuNamePlaceholder')" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button :icon="Search" type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button :icon="Refresh" plain @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <div class="search-table-option">
        <el-button :icon="Plus" plain type="success" @click="handleAdd(0)">{{ t('menu.addMenu') }}</el-button>
      </div>
    </div>

    <el-table
      :data="menuList"
      v-loading="loading"
      row-key="menuId"
      class="search-table-main"
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="menuName" :label="t('menu.menuName')" min-width="160" />
      <el-table-column prop="icon" :label="t('menu.icon')" width="80" />
      <el-table-column prop="orderNum" :label="t('menu.sort')" width="70" />
      <el-table-column prop="path" :label="t('menu.path')" min-width="150" />
      <el-table-column prop="component" :label="t('menu.component')" min-width="150" />
      <el-table-column prop="perms" :label="t('menu.perms')" min-width="140" />
      <el-table-column :label="t('menu.type')" width="80">
        <template #default="{ row }">
          <el-tag :type="row.menuType === 'M' ? undefined : row.menuType === 'C' ? 'success' : 'info'" size="small">
            {{ getMenuTypeLabel(row.menuType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('menu.visible')" width="80">
        <template #default="{ row }">
          <el-tag :type="row.visible === '0' ? 'success' : 'danger'" size="small">
            {{ row.visible === '0' ? t('menu.show') : t('menu.hidden') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.status')" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
            {{ row.status === '0' ? t('menu.normal') : t('menu.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.operate')" width="240" fixed="right" align="center">
        <template #default="{ row }">
          <el-button :icon="Plus" size="small" text type="primary" @click="handleAdd(row.menuId)">{{ t('common.add') }}</el-button>
          <el-button :icon="EditPen" size="small" text type="primary" @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button :icon="Delete" size="small" text type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-null">
          <img src="@/assets/images/null.png" alt="empty" />
          <span>{{ t('common.noData') }}</span>
        </div>
      </template>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('menu.parentMenu')">
          <el-tree-select
            v-model="form.parentId"
            :data="[{ menuId: 0, menuName: t('menu.root'), children: menuList }]"
            node-key="menuId"
            :props="{ label: 'menuName', children: 'children' }"
            :placeholder="t('menu.parentPlaceholder')"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('menu.type')" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">{{ t('menu.directory') }}</el-radio>
            <el-radio value="C">{{ t('menu.menu') }}</el-radio>
            <el-radio value="F">{{ t('menu.button') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('menu.menuName')" prop="menuName">
          <el-input v-model="form.menuName" :placeholder="t('menu.menuNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menu.path')" prop="path" v-if="form.menuType !== 'F'">
          <el-input v-model="form.path" :placeholder="t('menu.pathPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menu.component')" v-if="form.menuType === 'C'">
          <el-input v-model="form.component" :placeholder="t('menu.componentPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menu.routeName')" v-if="form.menuType !== 'F'">
          <el-input v-model="form.routeName" :placeholder="t('menu.routeNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menu.perms')" v-if="form.menuType !== 'M'">
          <el-input v-model="form.perms" :placeholder="t('menu.permsPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menu.sort')" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item :label="t('menu.icon')" v-if="form.menuType !== 'F'">
          <IconSelect v-model="form.icon" />
        </el-form-item>
        <el-row v-if="form.menuType !== 'F'">
          <el-col :span="8">
            <el-form-item :label="t('menu.isFrame')">
              <el-radio-group v-model="form.isFrame">
                <el-radio value="0">{{ t('common.yes') }}</el-radio>
                <el-radio value="1">{{ t('common.no') }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('menu.isCache')">
              <el-radio-group v-model="form.isCache">
                <el-radio value="0">{{ t('menu.cache') }}</el-radio>
                <el-radio value="1">{{ t('menu.noCache') }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('menu.visible')">
              <el-radio-group v-model="form.visible">
                <el-radio value="0">{{ t('menu.show') }}</el-radio>
                <el-radio value="1">{{ t('menu.hidden') }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="t('common.status')">
          <el-radio-group v-model="form.status">
            <el-radio value="0">{{ t('menu.normal') }}</el-radio>
            <el-radio value="1">{{ t('menu.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.menu-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-table-top {
  width: 100%;
  box-sizing: border-box;

  :deep(.el-form) {
    .el-form-item {
      margin-bottom: 14px;
      margin-right: 14px;
    }
  }
}

.search-table-option {
  margin-bottom: 14px;
}

.search-table-main {
  flex: 1;
}

.empty-null {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 0;

  img {
    width: 120px;
    margin-bottom: 12px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 14px;
  }
}

@media (max-width: 768px) {
  .search-table-top {
    :deep(.el-form) {
      .el-form-item {
        width: 100%;
        margin-right: 0;

        .el-input {
          width: 100%;
        }
      }
    }
  }

  .search-table-main {
    width: 100%;
    overflow-x: auto;
  }
}
</style>