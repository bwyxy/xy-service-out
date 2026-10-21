<script setup lang="ts">

import { ChatDotRound, UserFilled, Bell, ChatLineSquare, Reading, Memo } from "@element-plus/icons-vue";
import {reactive, computed, onMounted, onUnmounted, ref} from "vue";
import { useRoute, useRouter } from 'vue-router'
import UserInfo from '@/layout/UserInfo/UserInfo.vue'
import MoreMenu from "@/layout/MoreMenu/MoreMenu.vue";
import {storeToRefs} from "pinia";
import useNotificationStore from "@/store/notification.ts";
import NotificationCenter from "@/layout/Header/NotificationCenter.vue";
import {isHttpUrl} from "@xy/utils";

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
    path: '/group',
    icon: ChatLineSquare,
    name: '群聊'
  },
  {
    type: 'message',
    path: '',
    icon: Bell,
    name: '通知'
  },
  // {
  //   path: '/forum',
  //   icon: Orange,
  //   name: '友圈'
  // }
  {
    path: 'https://xiaee.cn',
    icon: Reading,
    name: '我的博客'
  },
  {
    path: 'https://gitee.com/bwyxy/xy-service-out',
    icon: Memo,
    name: 'Gitee'
  }
])

const notificationStore = useNotificationStore()
const { unreadCount } = storeToRefs(notificationStore)

const notificationVisible = ref(false)

let timer: ReturnType<typeof setInterval> | null = null
const routePath = computed(() => {
  return route.path
})

const onNotificationRefresh = () => {
  notificationStore.fetchUnreadCount()
}

onMounted(() => {
  notificationStore.fetchUnreadCount()
  // 每 60 秒同步一次后端未读数
  timer = setInterval(() => {
    notificationStore.fetchUnreadCount()
  }, 60000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})

const toPage = (url: string) => {
  if (!url) {
    notificationVisible.value = true
    return
  }

  if (isHttpUrl(url)) {
    window.open(url, '_blank')
    return;
  }

  router.push(url)
}
</script>

<template>
  <div class="layout-nav">
    <user-info />

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
        <div v-if="item.type && item.type === 'message' && unreadCount" class="nav-item-unrend">
          <span>{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </div>
      </div>
    </div>

    <MoreMenu />

    <notification-center v-model:visible="notificationVisible" @refresh="onNotificationRefresh" />
  </div>
</template>

<style lang="scss" scoped>
.layout-nav {
  width: size(84);
  height: 100%;
  background: #f6ffed;
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
    color: #8c8c8c;
    font-size: size(28);
    border-radius: size(6);
    cursor: pointer;
    @include flexCenter;
    position: relative;
    &__active {
      background: #fff !important;
      color: $theme-color;
      box-shadow: rgba(50, 50, 93, 0.15) 0 2px 5px -1px,
      rgba(0, 0, 0, 0.2) 0 1px 3px -1px;
    }
    .nav-item-unrend {
      min-width: size(12);
      min-height: size(12);
      background: #f00e00;
      border-radius: 50px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: size(2);
      position: absolute;
      right: 0;
      top: 0;
      span {
        font-size: size(10);
        color: #fff;
        line-height: 0;
      }
    }
    &:hover {
      background: rgba(0, 0, 0, .1);
    }
  }

  &-btm {

  }
}
</style>