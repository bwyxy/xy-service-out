<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import { getForumBoards, addForumBoard, updateForumBoard, delForumBoard } from '@/api/forum/boards.ts'
import {Delete, Edit, Plus} from "@element-plus/icons-vue";
import {ElMessageBox, FormInstance, FormRules} from 'element-plus'

const isShowOption = ref<boolean>(false)
const ruleFormRefs = ref<FormInstance>()
const baseTableRefs = ref()
const ruleForm = reactive<ForumBoard>({
  boardId: 0,
  parentId: 0,
  name: '',
  slug: '',
  description: '',
  sortOrder: 0,
  status: 0
})
const rules = reactive<FormRules<ForumBoard>>({
  name: [
    { required: true, message: '请输入版块名称', trigger: 'blur' }
  ]
})

const isLoading = ref<boolean>(false)

const dialogTitle = computed(() => {
  if (ruleForm.parentId === ruleForm.boardId) {
    return `添加${ruleForm.parentId}子版块`
  }
  return ruleForm.boardId ? '编辑版块' : '添加版块'
})

const handleEdit = (data: ForumBoard, type: ('edit' | 'addSub')) => {
  if (type === 'addSub') {
    ruleForm.boardId = data.boardId
    ruleForm.parentId = data.boardId
  } else {
    ruleForm.name = data.name
    ruleForm.boardId = data.boardId
    ruleForm.parentId = data.parentId
    ruleForm.slug = data.slug
    ruleForm.description = data.description
    ruleForm.sortOrder = data.sortOrder
    ruleForm.status = data.status
  }
  isShowOption.value = true
}

const handleDelete = (boardId : number) => {
  ElMessageBox.confirm(`是否确认删除${boardId}版块`, '', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  }).then(async () => {
    await delForumBoard(boardId)

    baseTableRefs.value?.getTableData()
  }).catch(() => {})
}
const handleSubmit = async (elForm: FormInstance | undefined) => {
  if (!elForm) return;

  const valid = await elForm.validate()

  if (!valid) return;

  isLoading.value = true

  if (ruleForm.parentId === ruleForm.boardId) {
    ruleForm.boardId = 0
    await addForumBoard(ruleForm)
  } else if (ruleForm.boardId) {
    await updateForumBoard(ruleForm)
  } else {
    await addForumBoard(ruleForm)
  }

  isLoading.value = false
  baseTableRefs.value?.getTableData()
  isShowOption.value = false
}

const columns = reactive<ColumnType[]>([
  {
    key: 'boardId',
    label: '版块ID',
    prop: 'boardId',
  },
  {
    key: 'name',
    label: '版块名称',
    prop: 'name',
  },
  {
    key: 'slug',
    label: 'URL别名',
    prop: 'slug',
  },
  {
    key: 'icon',
    label: '图标',
    prop: 'icon',
  },
  {
    key: 'description',
    label: '描述',
    prop: 'description',
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '禁用', value: 0, elTagType: "danger" },
        { label: '正常', value: 1, elTagType: "success" }
      ]
    }
  }
])

const handleClose = () => {
  ruleForm.boardId = 0
  ruleForm.parentId = 0
  ruleForm.name = ''
  ruleForm.slug = ''
  ruleForm.description = ''
  ruleForm.status = 0
  ruleForm.sortOrder = 0
  isShowOption.value = false
}
</script>

<template>
  <base-table ref="baseTableRefs" :columns="columns" :api="getForumBoards">
    <template #option>
      <el-button
          type="success"
          :icon="Plus"
          plain
          @click="isShowOption = true"
      >添加</el-button>
    </template>
    <template #operate="{ row }">
      <el-button
          text
          type="success"
          :icon="Plus"
          size="small"
          @click="handleEdit(row, 'addSub')"
      >添加子版块</el-button>
      <el-button
          text
          type="primary"
          :icon="Edit"
          size="small"
          @click="handleEdit(row, 'edit')"
      >编辑</el-button>
      <el-button
        text
        type="danger"
        :icon="Delete"
        size="small"
        @click="handleDelete(row.boardId)"
      >删除</el-button>
    </template>
  </base-table>

  <el-dialog
      v-model="isShowOption"
      :title="dialogTitle"
      width="600px"
      :before-close="handleClose"
  >
    <el-form ref="ruleFormRefs" :model="ruleForm" :rules="rules" label-width="80px">
      <el-form-item label="版块名称" prop="name">
        <el-input v-model="ruleForm.name" />
      </el-form-item>
      <el-form-item label="URL别名" prop="slug">
        <el-input v-model="ruleForm.slug" />
      </el-form-item>
      <el-form-item label="排序" prop="sortOrder">
        <el-input-number v-model="ruleForm.sortOrder" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-switch
            v-model="ruleForm.status"
            :active-value="1"
            :inactive-value="0"
        />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input
            v-model="ruleForm.description"
            type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button plain @click="isShowOption = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit(ruleFormRefs)">确认</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">

</style>