<script setup lang="ts">

import {Menu, ChatDotRound, UserFilled, Orange} from "@element-plus/icons-vue";
import {reactive, computed} from "vue";
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const navs = reactive([
  {
    path: '/chat',
    icon: ChatDotRound,
    name: '消息'
  },
  {
    path: '/friend',
    icon: UserFilled,
    name: '好友'
  },
  {
    path: '/forum',
    icon: Orange,
    name: '友圈'
  }
])

const routePath = computed(() => {
  return route.path
})

const toPage = (url: string) => {
  if (!url) return

  router.push(url)
}
</script>

<template>
  <div class="layout-nav">
    <div class="layout-nav-content">
      <div
          v-for="(item, index) in navs"
          :key="index"
          :class="[
              'nav-item',
              {'nav-item__active': item.path === routePath}
          ]"
          @click="toPage(item.path)"
      >
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <span class="nav-item-name">{{ item.name }}</span>
      </div>
    </div>

    <div class="layout-nav-btm">
      <div class="nav-item">
        <el-icon><Menu /></el-icon>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.layout-nav {
  width: size(84);
  height: 100%;
  background: $theme-color;
  box-sizing: border-box;
  padding: size(24) 0;
  display: flex;
  flex-direction: column;
  align-items: center;

  &-content {
    flex: 1;
    .nav-item {
      flex-direction: column;
      margin-bottom: size(24);
    }
    .nav-item-name {
      font-size: size(13) !important;
    }
  }

  .nav-item {
    width: size(54);
    height: size(54);
    color: #fff;
    font-size: size(28);
    border-radius: size(6);
    cursor: pointer;
    @include flexCenter;
    &__active {
      background: #fff !important;
      color: $theme-color;
      box-shadow: rgba(50, 50, 93, 0.15) 0 2px 5px -1px,
      rgba(0, 0, 0, 0.2) 0 1px 3px -1px;
    }
    &:hover {
      background: rgba(0, 0, 0, .1);
    }
  }

  &-btm {

  }
}
</style>