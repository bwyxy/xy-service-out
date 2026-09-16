<script setup lang="ts">
import {Plus} from "@element-plus/icons-vue";
import {onMounted, reactive, ref} from "vue";
import { useRoute } from "vue-router";
import { getMenuConfigs } from '@/api/blog/menuConfig.ts'

const route = useRoute();
const menuId = ref<number>(0)
const list = ref([])

const menuConfigs = async (id :number) => {
  const response = await getMenuConfigs(id);

  if (response.code === 200) {
    list.value = response.data
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

onMounted(() => {
  if (route.query.id) {
    menuId.value = Number(route.query.id);
    menuConfigs(menuId.value)
  }
})
</script>

<template>
  <base-table :list="list" :columns="columns">
    <template #option>
      <el-button :icon="Plus" plain type="success">添加菜单配置</el-button>
    </template>
  </base-table>
<!--  <div class="app-container">-->
<!--    <el-button :icon="Plus" plain type="success">添加菜单配置</el-button>-->
<!--  </div>-->
</template>

<style scoped lang="scss">

</style>