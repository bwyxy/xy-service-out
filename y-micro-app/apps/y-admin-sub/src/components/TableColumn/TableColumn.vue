<script setup lang="ts">

defineProps<{
  column: ColumnType,
  columns?: ColumnType[]
  width?: number | string,
  isSub?: boolean
}>()
</script>

<template>
  <el-table-column
      :label="column.label"
      :prop="column.prop"
      :width="column.width || width || ''"
      align="center"
      :show-overflow-tooltip="true"
  >
    <template v-if="column.type && column.type === 'image'" #default="{ row }">
      <el-image
          class="image-box"
          :src="row[column.prop]"
      >
        <template #error>
          <div class="error-image"></div>
        </template>
      </el-image>
    </template>
    <template v-if="column.type === 'tag'" #default="{ row }">
      <div v-for="(tag, tagIndex) in column.render ? column.render.list : []" :key="tagIndex">
        <el-tag
            v-if="String(tag.value) === String(row[column.key])"
            :type="tag.elTagType"
        >
          {{ tag.label }}
        </el-tag>
      </div>
    </template>
  </el-table-column>
</template>

<style scoped lang="scss">
.image-box {
  width: 40px;
  height: 40px;
  box-shadow: rgba(0, 0, 0, 0.1) 0 4px 6px -1px, rgba(0, 0, 0, 0.06) 0px 2px 4px -1px;
  border-radius: 8px;
}
</style>