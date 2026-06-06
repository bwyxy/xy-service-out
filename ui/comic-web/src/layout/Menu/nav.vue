<script setup lang="ts">
import {reactive} from "vue";
import { HomeFilled, ClockCircleFilled, FireFilled, AppstoreOutlined, TeamOutlined, MessageFilled, CustomerServiceFilled } from '@ant-design/icons-vue'

const navs = reactive([
  {
    label: '发现',
    children: [
      {
        label: '首页',
        icon: HomeFilled
      },
      {
        label: '漫画分类',
        icon: AppstoreOutlined
      },
      {
        label: '热门排行',
        icon: FireFilled
      },
      {
        label: '最近更新',
        icon: ClockCircleFilled
      },
    ]
  },
  {
    label: '社区',
    children: [
      {
        label: '漫友圈',
        icon: TeamOutlined
      },
      {
        label: '话题讨论',
        icon: MessageFilled
      },
      {
        label: '音乐',
        icon: CustomerServiceFilled
      }
    ]
  },
  {
    label: '互动',
    children: [
      {
        label: '漫友',
        icon: TeamOutlined
      },
      {
        label: '话题讨论',
        icon: MessageFilled
      },
      {
        label: '音乐',
        icon: CustomerServiceFilled
      }
    ]
  },
])
</script>

<template>
  <div class="nav">
    <div v-for="(item, index) in navs" :key="index">
      <span>{{ item.label }}</span>
      <div v-for="(row, idx) in item.children" :key="idx">
        <component :is="row.icon" />
        <span>{{ row.label }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">

</style>