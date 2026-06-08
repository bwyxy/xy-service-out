<script setup>
import {onMounted, reactive, ref} from "vue";
import { configList, configInfo, addConfig, updateConfig, deleteConfig } from '@/api/config'
import ModelDialog from "@/components/ModelDialog/ModelDialog.vue";
import UploadImage from "@/components/UploadImage/UploadImage.vue";
import {ElMessageBox} from "element-plus";

const params = reactive({
  pageNum: 1,
  pageSize: 10,
  configName: '',
  configType: 'tabbar',
  total: 0,
  loading: false
})
const formParams = reactive({
  configId: null,
  configName: '',
  configValue: '',
  configValue2: '',
  configLink: '',
  configType: 'tabbar',
  color: '',
  status: 1
})
const isShow = ref(false)
const configs = ref([])

const queryRefs = ref(null)
const formRefs = ref(null)

const getConfigList = async () => {
  try {
    params.loading = true
    const res = await configList(params)

    configs.value = res.data.list
    params.total = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    params.loading = false
  }
}

const optionConfig = async (id) => {
  if (id) {
    const res = await configInfo(id)

    if (res.code === 200) {
      const info = res.data
      const values = info.configValue.split(";")
      formParams.configId = id
      formParams.configName = info.configName
      formParams.configValue = values[0] || ''
      formParams.configValue2 = values[1] || ''
      formParams.configLink = info.configLink
      formParams.status = info.status
      formParams.color = info.color || ''

      isShow.value = true
    }
  } else {
    isShow.value = true
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
    await deleteConfig(id)

    await getConfigList()
  })
}

const handleSubmit = async () => {
  const vaild = await formRefs.value.validate()

  if (!vaild) return;

  const body = resetData()

  if (formParams.configId) {
    await updateConfig(body)
  } else {
    await addConfig(body)
  }

  handleClose()
  await getConfigList()
}

const resetData = () => {
  return {
    configId: formParams.configId,
    configName: formParams.configName,
    configValue: `${formParams.configValue}${formParams.configValue2 ? ';' + formParams.configValue2 : ''}`,
    configLink: formParams.configLink,
    configType: 'tabbar',
    color: formParams.color,
    status: formParams.status
  }
}

const handleClose = () => {
  formParams.configId = null
  formParams.configName = ''
  formParams.configValue = ''
  formParams.configLink = ''
  formParams.status = 1
  formParams.configValue2 = ''
  formParams.color = ''
  isShow.value = false
}

onMounted(() => {
  getConfigList()
})
</script>

<template>
  <div class="app-container">
    <el-form :model="params" ref="queryRefs" inline>
      <el-form-item label="配置名称" prop="configName">
        <el-input v-model="params.configName" placeholder="配置名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getConfigList">搜索</el-button>
        <el-button icon="Refresh">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="optionConfig('')"
        >
          添加
        </el-button>
      </el-col>
    </el-row>

    <el-table v-loading="params.loading" :data="configs" row-key="configId">
      <el-table-column property="configId" label="ID" align="center" />
      <el-table-column property="configName" label="配置名称" align="center" />
      <el-table-column property="configValue" label="配置值" show-overflow-tooltip align="center" />
      <el-table-column property="configLink" label="配置链接" align="center" />
      <el-table-column label="状态" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center">
        <template #default="{ row }">
          <el-button type="primary" icon="Edit" link @click="optionConfig(row.configId)">编辑</el-button>
          <el-button type="danger" icon="Delete" link @click="handleDel(row.configId)">删除</el-button>
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
          @current-change="getConfigList"
      />
    </div>

    <model-dialog
        v-model="isShow"
        width="480px"
        :title="`${formParams.configId ? '编辑' : '添加'}配置`"
        @close="handleClose"
    >
      <el-form :model="formParams" ref="formRefs" label-width="70px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="formParams.configName" placeholder="配置名称" />
        </el-form-item>
        <el-form-item label="关联链接" prop="configLink">
          <el-input v-model="formParams.configLink" placeholder="关联链接" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="formParams.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="formParams.color" />
        </el-form-item>
        <el-form-item label="默认图标" prop="configValue">
          <upload-image v-model="formParams.configValue" />
        </el-form-item>
        <el-form-item label="选中图标" prop="configValue2">
          <upload-image v-model="formParams.configValue2" />
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

</style>