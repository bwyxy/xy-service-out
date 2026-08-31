<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import MePage from './me/index.vue'
import PublishPage from './publish/index.vue'
import StatPage from './stat/index.vue'
import { Histogram, UserFilled, Promotion } from '@element-plus/icons-vue'

interface TabType {
  key: ('me' | 'publish' | 'stat'),
  name: string
  icon: any
}

const currentTab = ref<('me' | 'publish' | 'stat')>('me')
const tabs = reactive<TabType[]>([
  {
    key: 'me',
    name: '我的',
    icon: UserFilled
  },
  {
    key: 'publish',
    name: '发布',
    icon: Promotion
  },
  {
    key: 'stat',
    name: '统计',
    icon: Histogram
  }
])

const mainContainer = computed(() => {
  switch (currentTab.value) {
    case 'me':
      return MePage
    case 'publish':
      return PublishPage
    case 'stat':
      return StatPage
  }
})
</script>

<template>
  <div class="creation-center">
    <div class="creation-center-lt">
      <div
          v-for="(item, index) in tabs"
          :key="index"
          :class="['tab-item', {'tab-active': item.key === currentTab}]"
          @click="currentTab = item.key"
      >
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.name }}</span>
      </div>
    </div>
    <div class="creation-center-rt">
      <component :is="mainContainer" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.creation-center {
  width: 100%;
  height: 100%;
  background: #fff;
  display: flex;
  &-lt {
    width: 280px;
    box-sizing: border-box;
    padding: 32px;
    .tab-item {
      height: 50px;
      cursor: pointer;
      @include flexCenter;
      span {
        font-size: 14px;
        letter-spacing: 1px;
        color: #1f1f1f;
        margin-left: 8px;
      }
    }
    .tab-active {
      background: $theme-color;
      border-radius: 13px;
      color: #fff;
      span {
        color: #fff;
      }
    }
  }

  &-rt {
    flex: 1;
    overflow: hidden;
  }
}
</style>