<script setup lang="ts">
import { reactive } from 'vue'
import { getForumBoards } from '@/api/forum/boards.ts'

const columns = reactive<ColumnType[]>([
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
</script>

<template>
  <base-table :columns="columns" :api="getForumBoards"></base-table>
</template>

<style scoped lang="scss">

</style>