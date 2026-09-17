<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import { getSystemUser, addSystemUser, updateSystemUser } from '@/api/wk/system.ts'
import ImageUpload from "@/components/ImageUpload/ImageUpload.vue";
import { Plus, EditPen, Delete } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

const dialogVisible = ref<boolean>(false)
const ruleForm = reactive({
  id: undefined,
  avatar: '',
  nickname: '',
  userKey: '',
  wkImKey: ''
})
const ruleFormRefs = ref<FormInstance>()
const baseTableRef = ref()
const columns = reactive<ColumnType[]>([
  {
    key: 'avatar',
    label: '头像',
    type: 'image',
    prop: 'avatar',
  },
  {
    key: 'nickname',
    label: '账号名称',
    prop: 'nickname',
  },
  {
    key: 'userKey',
    label: '账号标识',
    prop: 'userKey',
  },
  {
    key: 'wkImKey',
    label: 'IM系统标识',
    prop: 'wkImKey',
  }
])

const dialogTitle = computed(() => {
  return ruleForm.id ? '编辑账号' : '添加账号'
})

const isLoading = ref<boolean>(false)

const handleEdit = (row :any) => {
  ruleForm.id = row.id
  ruleForm.avatar = row.avatar
  ruleForm.nickname = row.nickname
  ruleForm.userKey = row.userKey
  dialogVisible.value = true
}
const submit = async (elForm: FormInstance | undefined) => {
  if (!elForm) return;

  const valid = await elForm.validate()

  console.log('valid', valid, ruleForm)
  if (!valid) return

  isLoading.value = true
  if (ruleForm.id) {
    await updateSystemUser(ruleForm)
  } else {
    await addSystemUser(ruleForm)
  }
  isLoading.value = false
  baseTableRef.value?.getTableData()
  handleClose()
}

const handleClose = () => {
  dialogVisible.value = false
  ruleFormRefs.value?.resetFields()
}
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="getSystemUser">
      <template #option>
        <el-button @click="dialogVisible = true" :icon="Plus" plain type="success">添加</el-button>
      </template>
      <template #operate="{ row }">
        <el-button
            text
            type="warning"
            size="small"
            :icon="EditPen"
            @click="handleEdit(row)"
        >编辑</el-button>
        <el-button
            text
            type="danger"
            size="small"
            :icon="Delete"
        >删除</el-button>
      </template>
    </base-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="ruleFormRefs" :model="ruleForm" label-width="80px">
        <el-form-item label="头像" prop="avatar">
          <image-upload v-model="ruleForm.avatar" />
        </el-form-item>
        <el-form-item label="系统名称" prop="nickname">
          <el-input v-model="ruleForm.nickname" />
        </el-form-item>
        <el-form-item label="系统标识" prop="userKey">
          <el-input v-model="ruleForm.userKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button plain>取消</el-button>
        <el-button v-loading="isLoading" type="primary" @click="submit(ruleFormRefs)">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">

</style>