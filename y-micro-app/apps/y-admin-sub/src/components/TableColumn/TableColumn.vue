<script setup lang="ts">

defineProps<{
  column: ColumnType,
}>()
</script>

<template>
  <el-table-column
      :label="column.label"
      :prop="column.prop"
      :width="column.width || ''"
      align="center"
      :show-overflow-tooltip="true"
  >
    <template v-if="column.type && column.type === 'image'" #default="{ row }">
      <el-image
          style="max-width: 120px;max-height: 80px"
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
    <template #default v-if="column.children">
      <table-column v-for="col in column.children" :key="col.key" :column="col" />
    </template>
  </el-table-column>
</template>

<style scoped lang="scss">

</style>