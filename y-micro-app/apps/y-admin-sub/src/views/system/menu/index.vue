<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listMenu, getMenu, addMenu, updateMenu, delMenu } from '@/api/menu'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

// 菜单数据
const menuList = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
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
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  path: [{ required: true, message: '请输入路由地址', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入排序号', trigger: 'blur' }],
}

// 加载菜单列表
const loadMenuList = async () => {
  loading.value = true
  try {
    const res = await listMenu()
    menuList.value = buildTree(res.data || [])
  } finally {
    loading.value = false
  }
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
  dialogTitle.value = '新增菜单'
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
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该菜单？', '提示', { type: 'warning' })
  await delMenu(row.menuId)
  ElMessage.success('删除成功')
  loadMenuList()
}

// 提交
const handleSubmit = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  if (form.menuId) {
    await updateMenu({ ...form })
    ElMessage.success('修改成功')
  } else {
    await addMenu({ ...form })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadMenuList()
}

// 菜单类型文本
const menuTypeMap: Record<string, string> = {
  M: '目录',
  C: '菜单',
  F: '按钮',
}

onMounted(() => {
  loadMenuList()
})
</script>

<template>
  <div class="menu-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <el-button type="primary" size="small" @click="handleAdd(0)">新增菜单</el-button>
        </div>
      </template>

      <el-table
        :data="menuList"
        v-loading="loading"
        row-key="menuId"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="160" />
        <el-table-column prop="icon" label="图标" width="80" />
        <el-table-column prop="orderNum" label="排序" width="70" />
        <el-table-column prop="path" label="路由地址" min-width="150" />
        <el-table-column prop="component" label="组件路径" min-width="150" />
        <el-table-column prop="perms" label="权限标识" min-width="140" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.menuType === 'M' ? undefined : row.menuType === 'C' ? 'success' : 'info'" size="small">
              {{ menuTypeMap[row.menuType] || row.menuType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleAdd(row.menuId)">新增</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="[{ menuId: 0, menuName: '根目录', children: menuList }]"
            node-key="menuId"
            :props="{ label: 'menuName', children: 'children' }"
            placeholder="选择上级菜单"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="路由地址" prop="path" v-if="form.menuType !== 'F'">
          <el-input v-model="form.path" placeholder="请输入路由地址" />
        </el-form-item>
        <el-form-item label="组件路径" v-if="form.menuType === 'C'">
          <el-input v-model="form.component" placeholder="如: system/menu/index" />
        </el-form-item>
        <el-form-item label="路由名称" v-if="form.menuType !== 'F'">
          <el-input v-model="form.routeName" placeholder="请输入路由名称" />
        </el-form-item>
        <el-form-item label="权限标识" v-if="form.menuType !== 'M'">
          <el-input v-model="form.perms" placeholder="如: system:menu:list" />
        </el-form-item>
        <el-form-item label="显示排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="图标" v-if="form.menuType !== 'F'">
          <el-input v-model="form.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-row v-if="form.menuType !== 'F'">
          <el-col :span="8">
            <el-form-item label="是否外链">
              <el-radio-group v-model="form.isFrame">
                <el-radio value="0">是</el-radio>
                <el-radio value="1">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否缓存">
              <el-radio-group v-model="form.isCache">
                <el-radio value="0">缓存</el-radio>
                <el-radio value="1">不缓存</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="显示状态">
              <el-radio-group v-model="form.visible">
                <el-radio value="0">显示</el-radio>
                <el-radio value="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="菜单状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
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
</style>