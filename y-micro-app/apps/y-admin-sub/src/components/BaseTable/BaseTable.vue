<script setup lang="ts">
import {reactive, ref, toRefs, watch} from "vue";
import { Search, Refresh } from '@element-plus/icons-vue'
import TableColumn from "@/components/TableColumn/TableColumn.vue";

const props = defineProps<{
  columns: Array<ColumnType>,
  api: (data :any) => Promise<any>
}>()
const { columns } = toRefs(props)

const tableData = ref<Array<any>>([])
/** 表单查询 */
const ruleForm = ref<any[]>([])
const isLoading = ref<boolean>(false)
const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 15,
  total: 0
})

/** 设置查询数据 */
const formToParams = () => {
  ruleForm.value = []
  for (let i = 0; i < columns.value.length; i++) {
    const item = columns.value[i]
    if (item.render) {
      ruleForm.value.push({
        label: item.label,
        key: item.key,
        type: item.render.el,
        value: '',
        list: item.render.list || []
      })
    }
  }
}
/** 获取数据 */
const getTableData = async () => {
  isLoading.value = true
  ruleForm.value.forEach(item => {
    queryParams[item.key] = item.value
  })
  const response = await props.api(queryParams)

  isLoading.value = false
  if (response.code === 200) {
    const { list, total } = response.data
    tableData.value = list
    queryParams.total = total
  }
}
/** 重置数据 */
const resetData = () => {
  ruleForm.value.map(item => item.value = '')
  getTableData()
}

watch(() => columns.value, () => {
  formToParams()

  getTableData()
}, {immediate: true})
</script>

<template>
  <div class="search-table">
    <div class="search-table-top">
      <el-form :inline="true">
        <el-form-item
            v-for="(item, index) in ruleForm"
            :key="index"
            :prop="item.key"
        >
          <el-input
              v-if="item.type === 'search'"
              v-model="item.value"
              :placeholder="`${item.label}`"
          />
          <el-select
              v-if="item.type === 'select'"
              v-model="item.value"
              clearable
              style="width: 160px"
              :placeholder="`${item.label}`"
          >
            <el-option
                v-for="row in item.list"
                :key="row.value"
                :label="row.label"
                :value="row.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Search" type="primary" @click="getTableData">查询</el-button>
          <el-button :icon="Refresh" plain @click="resetData">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-if="$slots.option" class="search-table-option">
        <slot name="option"></slot>
      </div>
    </div>

    <el-table
        v-loading="isLoading"
        :data="tableData"
        class="search-table-main"
    >
      <table-column
          v-for="(item, index) in columns"
          :key="index"
          :column="item"
      />
      <el-table-column label="操作" align="center">
        <template #default="{ row }">
          <slot name="operate" :row="row"></slot>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-null">
          <img src="@/assets/images/null.png" alt="empty" />
          <span>暂无数据</span>
        </div>
      </template>
    </el-table>

    <div class="base-pagination">
      <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          size="small"
          background
          layout="total, prev, pager, next"
          :total="queryParams.total"
          @change="getTableData"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.search-table {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  &-top {
    width: 100%;
    background: rgba(255, 255, 255, 0.5);
    box-shadow: rgba(0, 0, 0, 0.04) 2px 2px 6px 0, rgba(0, 0, 0, 0.04) -2px -2px 2px 0px;
    border-radius: size(14);
    box-sizing: border-box;
    margin: 0 auto size(24);
    padding: size(24) size(24) 0;
  }
  &-main {
    flex: 1;
  }
}

.search-table-option {
  margin-bottom: size(24);
}

:deep(.el-form) {

  .el-form-item {
    margin-bottom: size(24);
    margin-right: size(24);
  }
}

.base-pagination {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: size(24);
}

.empty-null {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin-top: size(42);
  img {
    width: size(120);
    height: size(120);
  }
  span {
    font-size: size(20);
    letter-spacing: 1px;
  }
}
</style>