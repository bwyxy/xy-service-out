<script setup>

import {onMounted, reactive, ref} from "vue";
import { getTagList, getTagById, addTag, updateTag, deleteTag } from '@/api/tag'
import ModelDialog from "@/components/ModelDialog/ModelDialog.vue";
import { ElMessageBox } from 'element-plus'

const params = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  loading: false,
  total: 0
})

const isShow = ref(false)
const list = ref([])
const formParams = reactive({
  tagId: null,
  name: '',
  color: '',
  level: 0,
  slug: '',
  description: '',
})
const queryRef = ref(null)
const formRef = ref(null)

const getList = async () => {
  params.loading = true
  try {
    const data = await getTagList(params)

    list.value = data.list
    params.total = data.total
  } catch (err) {
    console.error(err)
  } finally {
    params.loading = false
  }
}

function reset() {
  queryRef.value.resetFields()
}

const openOptionTag = (id) => {
  if (id) {
    formParams.tagId = id
    setFormValue(id)
  }
  isShow.value = true
}

const setFormValue = async (id) => {
  try {
    const res = await getTagById(id)

    formParams.tagId = id
    formParams.name = res.name || ''
    formParams.color = res.color || ''
    formParams.level = res.level || 0
    formParams.slug = res.slug || ''
    formParams.description = res.description || ''
  } catch (err) {
    console.error(err)
  }
}

const handleDel = (id) => {
  ElMessageBox.confirm(
      `是否确认删除${id}?`,
      '是否删除',
      {
        type: 'warning'
      }
  ).then(async () => {
    await deleteTag(id)

    await getList()
  }).catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate()

  if (!valid) return;

  if (formParams.tagId) {
    await updateTag(formParams)
  } else {
    await addTag(formParams)
  }

  handleClose()
  await getList()
}

const handleClose = () => {
  formParams.tagId = null
  formRef.value.resetFields()
  isShow.value = false
}

onMounted(() => {
  getList()
})
</script>

<template>
  <div class="app-container">
    <el-form :model="params" ref="queryRef" inline>
      <el-form-item label="标签名称" prop="configKey">
        <el-input v-model="params.name" style="width: 200px" placeholder="标签名称" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="openOptionTag('')"
        >新增</el-button>
      </el-col>
    </el-row>

    <el-table
        v-loading="params.loading"
        :data="list"
        row-key="tagId"
    >
      <el-table-column label="ID" property="tagId" align="center" />
      <el-table-column label="标签名称" property="name" align="center" />
      <el-table-column label="颜色" align="center">
        <template #default="{ row }">
          <div class="tag-item" :style="{ '--color': row.color }">
            <span>{{ row.color }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="水平" property="level" align="center" />
      <el-table-column label="URL 别名" property="slug" align="center" />
      <el-table-column label="描述" property="description" align="center" />
      <el-table-column label="操作" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="openOptionTag(row.tagId)">编辑</el-button>
          <el-button link type="danger" icon="Delect" @click="handleDel(row.tagId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="total-pagination">
      <el-pagination
          v-model:current-page="params.pageNum"
          :page-size="params.pageSize"
          background
          layout="total, prev, pager, next"
          :total="params.total"
          @current-change="getList"
      />
    </div>

    <model-dialog
        v-model="isShow"
        width="540px"
        :title="`${formParams.tagId ? '编辑' : '添加'}标签`"
        @close="handleClose"
    >
      <el-form :model="formParams" ref="formRef" label-width="70px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="formParams.name" placeholder="标签名称" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="formParams.color" />
        </el-form-item>
        <el-form-item label="水平" prop="level">
          <el-input v-model="formParams.level" placeholder="水平" />
        </el-form-item>
        <el-form-item label="URL别名" prop="slug">
          <el-input v-model="formParams.slug" placeholder="URL" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formParams.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </template>
    </model-dialog>
  </div>
</template>

<style scoped lang="scss">
.tag-item {
  font-size: 12px;
  color: var(--color);
  letter-spacing: 1px;
  position: relative;
  span {
    padding: 6px;
    position: relative;
    &:after {
      content: '';
      width: 100%;
      height: 100%;
      background: var(--color);
      border-radius: 2px;
      opacity: .3;
      position: absolute;
      left: 0;
      top: 0;
    }
  }
}
</style>
