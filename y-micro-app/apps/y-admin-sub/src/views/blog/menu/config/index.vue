<script setup lang="ts">
import {Delete, EditPen, Plus} from "@element-plus/icons-vue";
import {onMounted, reactive, ref} from "vue";
import { useRoute } from "vue-router";
import { getMenuConfigs, addMenuConfig, updateMenuConfig, getMenuConfigById } from '@/api/blog/menuConfig.ts'
import { FormInstance, FormRules } from 'element-plus'

interface RuleForm {
  id: number | null
  menuId: number
  configKey: string
  configType: ('text' | 'image')
  configValue: string
  configName: string
}

const route = useRoute();
// const menuId = ref<number>(0)
const list = ref([])
const isShowOption = ref<boolean>(false)

const ruleFormRefs = ref<FormInstance>()
const ruleForm = reactive<RuleForm>({
  id: null,
  menuId: 0,
  configKey: '',
  configType: 'text',
  configValue: '',
  configName: ''
})
const rule = reactive<FormRules<RuleForm>>({
  configKey: [
    { required: true, message: '配置标识不能为空', trigger: 'blur' }
  ],
  configName: [
    { required: true, message: '配置名称不能为空', trigger: 'blur' }
  ]
})

const menuConfigs = async (id :number) => {
  const response = await getMenuConfigs(id);

  if (response.code === 200) {
    list.value = response.data
    console.log(list.value)
  }
}

const columns = reactive<ColumnType[]>([
  {
    key: 'configKey',
    label: '配置标识',
    prop: 'configKey'
  },
  {
    key: 'configName',
    label: '配置名称',
    prop: 'configName'
  },
  {
    key: 'configValue',
    label: '配置值',
    prop: 'configValue'
  },
  {
    key: 'configType',
    label: '配置类型',
    prop: 'configType'
  },
  {
    key: 'createTime',
    label: '创建时间',
    prop: 'createTime'
  },
  {
    key: 'updateTime',
    label: '更新时间',
    prop: 'updateTime'
  }
])

const isLoading = ref<boolean>(false)

const handleEdit = async (id :number) => {
  const response = await getMenuConfigById(id)

  if (response.code === 200) {
    const { id, configKey, configName, configValue, configType } = response.data
    ruleForm.id = id
    ruleForm.configKey = configKey
    ruleForm.configValue = configValue
    ruleForm.configName = configName
    ruleForm.configType = configType
    isShowOption.value = true
  }
}
const handleSubmit = async (elForm: FormInstance | undefined) => {
  if (!elForm) return;

  const valid = await elForm.validate()

  if (!valid) return;

  isLoading.value = true
  if (ruleForm.id) {
    await updateMenuConfig(ruleForm)
  } else {
    await addMenuConfig(ruleForm)
  }

  isLoading.value = false
  await menuConfigs(ruleForm.menuId)
  handleClose()
}

const handleClose = () => {
  ruleForm.id = null
  ruleForm.configName = ''
  ruleForm.configValue = ''
  ruleForm.configType = 'text'
  ruleForm.configKey = ''
  isShowOption.value = false
}

onMounted(() => {
  if (route.query.id) {
    ruleForm.menuId = Number(route.query.id);
    menuConfigs(ruleForm.menuId)
  }
})
</script>

<template>
  <div>
    <base-table :list="list" :columns="columns">
      <template #option>
        <el-button :icon="Plus" plain type="success" @click="isShowOption = true">添加菜单配置</el-button>
      </template>
      <template #operate="{ row }">
        <el-button :icon="EditPen" text type="primary" size="small" @click="handleEdit(row.id)">编辑</el-button>
        <el-button :icon="Delete" text type="danger" size="small">删除</el-button>
      </template>
    </base-table>

    <el-dialog
        v-model="isShowOption"
        width="500px"
        :title="ruleForm.id ? '编辑配置' : '添加配置'"
        :beforeClose="handleClose"
    >
      <el-form ref="ruleFormRefs" :model="ruleForm" :rules="rule">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="ruleForm.configName" />
        </el-form-item>
        <el-form-item label="配置标识" prop="configKey">
          <el-input v-model="ruleForm.configKey" />
        </el-form-item>
        <el-form-item label="配置类型" prop="configType">
          <el-radio-group v-model="ruleForm.configType">
            <el-radio label="text">文本</el-radio>
            <el-radio label="image">图片</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-if="ruleForm.configType === 'text'" v-model="ruleForm.configValue" />
          <div v-if="ruleForm.configType === 'image'">
            <image-upload v-model="ruleForm.configValue" :multiple="true" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="isShowOption = false">取消</el-button>
        <el-button v-loading="isLoading" type="primary" @click="handleSubmit(ruleFormRefs)">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">

</style>