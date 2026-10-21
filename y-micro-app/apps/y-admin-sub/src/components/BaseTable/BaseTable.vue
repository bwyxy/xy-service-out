<script setup lang="ts">
import {computed, reactive, ref, toRefs, watch} from "vue";
import { useI18n } from 'vue-i18n'
import { Search, Refresh } from '@element-plus/icons-vue'
import TableColumn from "@/components/TableColumn/TableColumn.vue";

const { t } = useI18n()

const props = defineProps<{
  columns?: Array<ColumnType>,
  api?: (data :any) => Promise<any>,
  list?: Array<any>,
  selectable?: boolean,
}>()
const { columns, list } = toRefs(props)

const tableData = ref<Array<any>>([])
/** 多选选中的行 */
const selection = ref<Array<any>>([])
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
  if (!columns.value) return []
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

/** 表格展示列（排除展开列） */
const visibleColumns = computed(() => {
  if (!columns.value) {
    return []
  }
  return columns.value.filter(item => item.key !== 'expand')
})

const isChildren = computed(() => {
  if (!columns.value) {
    return false
  }
  return !!columns.value.find(item => item.key === 'expand')
})
/** 获取数据 */
const getTableData = async () => {
  if (!props.api) return;

  isLoading.value = true

  // 仅提交分页与查询条件，避免把 total 等本地字段一起发给后端
  const params: any = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize
  }
  ruleForm.value.forEach(item => {
    params[item.key] = item.value
  })

  try {
    const response = await props.api(params)

    if (response?.code !== 200) {
      return
    }

    if (!response.data.list) {
      tableData.value = response.data
      queryParams.total = 1
      return
    }
    const { list, total } = response.data
    tableData.value = list
    queryParams.total = total
  } finally {
    // 接口异常时也必须复位 loading，否则表格会一直转圈
    isLoading.value = false
  }
}
/** 重置数据 */
const resetData = () => {
  ruleForm.value.map(item => item.value = '')
  queryParams.pageNum = 1
  getTableData()
}

/** 多选变化 */
const handleSelectionChange = (rows: Array<any>) => {
  selection.value = rows
}

/** 获取选中行 */
const getSelection = () => {
  return selection.value
}

defineExpose({ getTableData, getSelection })

watch(() => columns.value, () => {
  formToParams()

  getTableData()

  if (list.value) {
    tableData.value = list.value
  }
}, {immediate: true})

watch(() => list.value, () => {
  if (list.value) {
    tableData.value = list.value
  }
}, {deep: true})
</script>

<template>
  <div class="search-table">
    <div class="search-table-top">
      <el-form v-if="ruleForm.length > 0" :inline="true">
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
          <el-button :icon="Search" type="primary" @click="getTableData">{{ t('common.search') }}</el-button>
          <el-button :icon="Refresh" plain @click="resetData">{{ t('common.reset') }}</el-button>
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
        @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="selectable" type="selection" width="50" align="center" />
      <el-table-column v-if="isChildren" type="expand">
        <template #default="{row}">
          <el-table :data="row.children">
            <table-column
                v-for="(item, index) in visibleColumns"
                :key="index"
                :column="item"
                :columns="visibleColumns"
            />
            <el-table-column fixed="right" min-width="180" :label="t('common.operate')" align="center">
              <template #default="{ row }">
                <slot name="operate" :row="row"></slot>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty-null">
                <img src="@/assets/images/null.png" alt="empty" />
                <span>{{ t('common.noData') }}</span>
              </div>
            </template>
          </el-table>
        </template>
      </el-table-column>
      <table-column
          v-for="(item, index) in visibleColumns"
          :key="index"
          :column="item"
          :columns="visibleColumns"
      />
      <el-table-column fixed="right" min-width="180" :label="t('common.operate')" align="center">
        <template #default="{ row }">
          <slot name="operate" :row="row"></slot>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-null">
          <img src="@/assets/images/null.png" alt="empty" />
          <span>{{ t('common.noData') }}</span>
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
    box-sizing: border-box;
  }
  &-main {
    flex: 1;
  }
}

.search-table-option {
  margin-bottom: size(24);
}

:deep(.el-table__expanded-cell) {
  background: none;
  .search-table-top {
    display: none;
  }
  .base-pagination {
    display: none;
  }
}

:deep(.el-table-fixed-column--right) {
  background: var(--app-sidebar-bg) !important;
  border-left: 1px solid var(--app-bg-color);
}

:deep(th.el-table__cell.el-table-fixed-column--right) {
  background: var(--el-fill-color-light) !important;
}

:deep(.el-form) {

  .el-form-item {
    margin-bottom: 14px;
    margin-right: 14px;
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

@media (max-width: 768px) {
  :deep(.el-form) {
    .el-form-item {
      width: 100%;
      margin-right: 0;

      .el-input,
      .el-select {
        width: 100% !important;
      }
    }
  }

  .base-pagination {
    justify-content: center;
  }
}
</style>