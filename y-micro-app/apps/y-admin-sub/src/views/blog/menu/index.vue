<script setup lang="ts">
import { getMenuList, addMenu, editMenu, getMenuInfo } from '@/api/blog/menu.ts'
import {reactive, ref} from "vue";
import { useRouter } from "vue-router";
import {Plus, Edit, Delete, Setting} from '@element-plus/icons-vue'
import {ElMessage, FormInstance} from 'element-plus'

const router = useRouter()
const columns = reactive<ColumnType[]>([
  {
    key: 'expand',
    label: '折叠/展开',
    prop: 'expand'
  },
  {
    key: 'name',
    label: '菜单名称',
    prop: 'name'
  },
  {
    key:'icon',
    label: '图标',
    prop: 'icon',
    type: 'image'
  },
  {
    key: 'path',
    label: '路由地址',
    prop: 'path'
  },
  {
    key: 'routeName',
    label: '路由名称',
    prop: 'routeName'
  },
])

const isShow = ref<boolean>(false)
const ruleFormRefs = ref<FormInstance>()
const ruleForm = reactive({
  id: null,
  name: '',
  parentId: 0,
  orderNum: 0,
  path: '',
  routeName: '',
  isFrame: 1,
  status: 1,
  icon: ''
})
const baseTableRefs = ref()

const handleEdit = async (id: number) => {
  const response = await getMenuInfo(id)

  if (response.code === 200) {
    const { id, name, parentId, orderNum, path, routeName, isFrame, status, icon } = response.data

    ruleForm.id = id
    ruleForm.name = name
    ruleForm.parentId = parentId
    ruleForm.orderNum = orderNum
    ruleForm.path = path
    ruleForm.routeName = routeName
    ruleForm.isFrame = isFrame
    ruleForm.status = status
    ruleForm.icon = icon
    isShow.value = true
  }
}

const handAddSubMenu = async (id: number) => {
  ruleForm.id = null
  ruleForm.parentId = id
  isShow.value = true
}

const isLoading = ref<boolean>(false)
const handleSubmit = async (elForm :FormInstance | undefined) => {
  if (!elForm) return;

  const valid = await elForm.validate()

  if (!valid) return;

  isLoading.value = true
  if (ruleForm.id) {
    await editMenu(ruleForm)
  } else {
    await addMenu(ruleForm)
  }
  ElMessage.success("操作成功")
  isShow.value = false
  isLoading.value = false
  handleClose()
  if(baseTableRefs.value) {
    await baseTableRefs.value.getTableData()
  }
}
/** 配置菜单 */
const configMenu = (id :number) => {
  router.push({
    path: '/blog/menu/config',
    query: { id },
  })
}

const handleClose = () => {
  isShow.value = false
  ruleForm.id = null
  ruleForm.name = ''
  ruleForm.parentId = 0
  ruleForm.orderNum = 0
  ruleForm.path = ''
  ruleForm.routeName = ''
  ruleForm.isFrame = 1
  ruleForm.status = 1
  ruleForm.icon = ''
  ruleFormRefs.value && ruleFormRefs.value.resetFields()
}
</script>

<template>
  <div>
    <base-table ref="baseTableRefs" :columns="columns" :api="getMenuList">
      <template #option>
        <el-button
            :icon="Plus"
            type="success"
            plain
            @click="isShow = true"
        >添加</el-button>
      </template>
      <template #operate="{ row }">
        <el-button size="small" text type="primary" :icon="Edit" @click="handleEdit(row.id)"></el-button>
        <el-button size="small" text type="danger" :icon="Delete"></el-button>
        <el-button size="small" text type="warning" :icon="Plus" @click="handAddSubMenu(row.id)">子菜单</el-button>
        <el-button size="small" text type="primary" :icon="Setting" @click="configMenu(row.id)">菜单配置</el-button>
      </template>
    </base-table>

    <el-dialog v-model="isShow" :before-close="handleClose">
    <el-form ref="ruleFormRefs" :model="ruleForm">
      <el-form-item label="菜单名称" prop="name">
        <el-input v-model="ruleForm.name" />
      </el-form-item>
      <el-form-item label="菜单图标" prop="icon">
        <image-upload v-model="ruleForm.icon" />
      </el-form-item>
      <el-form-item label="上级菜单" prop="parentId">
        <el-select v-model="ruleForm.parentId" disabled></el-select>
      </el-form-item>
      <el-form-item label="路由地址" prop="path">
        <el-input v-model="ruleForm.path" />
      </el-form-item>
      <el-form-item label="路由名称" prop="routeName">
        <el-input v-model="ruleForm.routeName" />
      </el-form-item>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="是否外链" prop="isFrame">
            <el-radio-group v-model="ruleForm.isFrame">
              <el-radio :label="1">否</el-radio>
              <el-radio :label="0">是</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否停用" prop="status">
            <el-radio-group v-model="ruleForm.status">
              <el-radio :label="1">否</el-radio>
              <el-radio :label="0">是</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="isShow = false">取消</el-button>
      <el-button v-loading="isLoading" type="primary" @click="handleSubmit(ruleFormRefs)">确认</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<style scoped lang="scss">

</style>