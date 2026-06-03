<script setup>
import { addBlogConfig, getBlogConfigById, getBlogConfigList, updateBlogConfig } from '@/api/blog'
import {onMounted, reactive, ref} from "vue";
import ModelDialog from "@/components/ModelDialog/ModelDialog.vue";
import { ElMessage } from "element-plus";

const params = reactive({
  pageNum: 1,
  pageSize: 10,
  id: '',
  configKey: '',
  total: 0,
  loading: false
})
const paramsBody = reactive({
  id: null,
  configKey: '',
  configValue: '',
  description: ''
})
const isShow = ref(false)
const blogConfigs = ref([])
const queryRef = ref()
const formRefs = ref()

const getBlogs = async () => {
  try {
    blogConfigs.value = await getBlogConfigList(params)
  }catch (error) {
    console.log(error)
  } finally {
    params.loading = false
  }
}

const handleOption = async (id) => {
  if (id) {
    const res = await getBlogConfigById(id)
    paramsBody.id = res.id
    paramsBody.configKey = res.configKey
    paramsBody.configValue = res.configValue
    paramsBody.description = res.description
  } else {
    paramsBody.id = null
  }
  isShow.value = true
}

const submitForm = async () => {
  const valid = await formRefs.value.validate()

  if (valid) {
    if (paramsBody.id) {
      await updateBlogConfig(paramsBody)

      ElMessage.success("编辑成功")
      isShow.value = false
      await getBlogs()
    } else {
      await addBlogConfig(paramsBody)

      ElMessage.success('添加成功')
      isShow.value = false
      await getBlogs()
    }
  }
}

const close = () => {
  paramsBody.id = null
  formRefs.value.resetFields()
}

function reset() {
  queryRef.value.resetFields()
  getBlogs()
}

onMounted(() => {
  getBlogs()
})

</script>

<template>
  <div class="app-container">
    <el-form :model="params" ref="queryRef" :inline="true">
      <el-form-item label="配置标识" prop="configKey">
        <el-input v-model="params.configKey" style="width: 200px" placeholder="配置标识" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getBlogs">搜索</el-button>
        <el-button icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleOption(null)"
        >新增</el-button>
      </el-col>
    </el-row>

    <el-table
        v-loading="params.loading"
        :data="blogConfigs"
        row-key="id"
    >
      <el-table-column prop="id" label="ID" align="center" />
      <el-table-column prop="configKey" label="配置标识" align="center" />
      <el-table-column prop="configValue" label="配置值" :show-overflow-tooltip="true" align="center" />
      <el-table-column prop="description" label="描述" :show-overflow-tooltip="true" align="center" />
      <el-table-column label="操作" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleOption(row.id)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <model-dialog
        v-model="isShow"
        :center="true"
        :title="`${paramsBody.id ? '编辑' : '添加'}配置`"
        @close="close"
    >
      <el-form :model="paramsBody" ref="formRefs" label-width="80">
        <el-form-item label="配置标识" prop="configKey">
          <el-input v-model="paramsBody.configKey" placeholder="配置标识" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="paramsBody.configValue" placeholder="配置值" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="paramsBody.description" type="textarea" placeholder="描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="isShow = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </model-dialog>
  </div>
</template>

<style scoped lang="scss">

</style>
