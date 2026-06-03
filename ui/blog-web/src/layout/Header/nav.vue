<script setup lang="ts">
import {useRouter} from "vue-router";
import Logo from '@/layout/Logo/logo.vue'
import {Search} from "@element-plus/icons-vue";
import {onMounted, onUnmounted, ref, shallowReactive} from "vue";

const router = useRouter();
const navs = shallowReactive([
  {
    path: '/',
    name: '首页',
    isMenu: false,
    children: []
  },
  {
    path: '',
    name: '文章',
    isMenu: true,
    children: [
      {
        path: '/archive/index',
        name: '归档',
        isMenu: false,
      },
      {
        path: '/category/index',
        name: '分类',
        isMenu: false,
      },
      {
        path: '/tag/index',
        name: '标签',
        isMenu: false,
      }
    ]
  },
  {
    path: '/message/board',
    name: '留言',
    isMenu: false,
    children: []
  },
  {
    path: '/friendshipLinks/index',
    name: '友链',
    isMenu: false,
    children: []
  },
  {
    path: '',
    name: '关于我',
    isMenu: false,
    children: [
      {
        path: '/resume/index',
        name: '个人简历',
        isMenu: false,
      },
      {
        path: '/portfolio/index',
        name: '作品集',
        isMenu: false,
      }
    ]
  }
])
const reversePercent = ref(100)
const isFixedTop = ref(false)

const handleScroll = () => {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight
  const winHeight = window.innerHeight
  const maxScroll = docHeight - winHeight

  let progress = 0
  if (maxScroll > 0) {
    progress = (scrollTop / maxScroll) * 100
    // 限制范围 [0, 100]
    progress = Math.min(100, Math.max(0, progress))
  }
  // 反向百分比
  reversePercent.value = 100 - progress
  isFixedTop.value = reversePercent.value <= 70;
}

const toPage = (url :string) => {
  if (!url) return
  router.push({
    path: url
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <div :class="['layout-nav', { 'layout-nav_fixed': isFixedTop }]">
    <div
        :class="[
           'layout-nav-content',
           { 'fixed-content': isFixedTop }
        ]"
        :style="{ width: `${reversePercent}%`, height: `${reversePercent}%` }"
    >
      <logo />

      <div class="nav-list">
        <div v-for="(item, index) in navs" :key="index" @click="toPage(item.path)">
          <div v-if="item.children.length === 0" class="nav-item">
            <span>{{ item.name }}</span>
          </div>
          <div v-else :class="`menu-dropdown${ isFixedTop ? '_dark' : '' }`">
            <el-dropdown trigger="click" :teleported="false">
              <div class="nav-dropdown-item">
                <span>{{ item.name }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item 
                    v-for="(row, idx) in item.children" 
                    :key="idx"
                    @click="toPage(row.path)"
                  >
                    <span>{{ row.name }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
<!--            <div v-for="(row, idx) in item.children" :key="idx" class="nav-item_row">-->
<!--              <span>{{ row.name }}</span>-->
<!--            </div>-->
          </div>
        </div>
      </div>

      <div class="nav-btn">
        <el-icon><Search /></el-icon>
      </div>
      <div class="nav-text-btn" @click="toPage('/login')">
        <span>登录</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$-transition-time: .2s;

@mixin flexCenter {
  display: flex;
  align-items: center;
  justify-content: center;
}

.layout-nav {
  width: 1250px;
  height: 70px;
  z-index: 99;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: $-transition-time;
  position: fixed;
  top: 24px;
  left: calc(50% - (1250px / 2));
}

.layout-nav_fixed {
  width: 100% !important;
  top: 0 !important;
  left: 0;
}

.layout-nav-content {
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px) saturate(180%);
  //-webkit-backdrop-filter: blur(16px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 48px;
  //transition: transform 0.4s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.4s ease;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  overflow: visible;
  padding: 0 20px 0 32px;
  z-index: 1;
  //position: absolute;
  //top: 0;
}

.fixed-content {
  width: 100% !important;
  height: 100% !important;
  border-radius: 0 !important;
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, .6);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3) !important;
  padding: 0 355px 0 367px;
}

.nav-list {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.nav-item {
  height: 40px;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  transition: $-transition-time;
  letter-spacing: 2px;
  border-radius: 48px;
  padding: 0 24px;
  @include flexCenter;
  &:hover {
    //transform: scale(0.96);
    background: rgba(255, 255, 255, 0.1);
    box-shadow: 0 32px 55px -12px rgba(0, 0, 0, 0.45),
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 1px 1px rgba(255, 255, 255, 0.7),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
  }
}

.nav-btn {
  width: 40px;
  height: 40px;
  border-radius: 48px;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: $-transition-time;
  @include flexCenter;
  &:hover {
    transform: scale(0.96);
    background: rgba(255, 255, 255, 0.1);
    box-shadow: 0 32px 55px -12px rgba(0, 0, 0, 0.45),
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 1px 1px rgba(255, 255, 255, 0.7),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
  }
}

.nav-text-btn {
  height: 40px;
  color: #fff;
  border-radius: 48px;
  font-size: 15px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: $-transition-time;
  padding: 0 14px;
  @include flexCenter;
  &:hover {
    transform: scale(0.96);
    background: rgba(255, 255, 255, 0.1);
    box-shadow: 0 32px 55px -12px rgba(0, 0, 0, 0.45),
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 1px 1px rgba(255, 255, 255, 0.7),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
  }
}

.nav-dropdown-item {
  height: 70px;
  display: flex;
  cursor: pointer;
  align-items: center;
  padding: 0 24px;
  span {
    color: #fff;
    font-size: 14px;
    letter-spacing: 1px;
  }
}

.menu-dropdown {
  :deep(.el-popper) {
    background-color: rgba(255, 255, 255, 0.3) !important;
    backdrop-filter: blur(16px) brightness(1.2);
    -webkit-backdrop-filter: blur(16px) brightness(1.2);
    border: 1px solid rgba(255, 255, 255, 0.3);
    .el-popper__arrow {
      display: none !important;
    }
  }
  :deep(.el-dropdown-menu) {
    background: none !important;
    span {
      color: #fff;
      font-size: 14px;
      font-weight: 500;
    }
    .el-dropdown-menu__item {
      display: flex;
      justify-content: center;
    }
    .el-dropdown-menu__item:hover {
      background: rgba(255, 255, 255, 0.2) !important;
    }
    .el-dropdown-menu__item:focus {
      background: rgba(255, 255, 255, 0.2) !important;
    }
  }
}

.menu-dropdown_dark {
  :deep(.el-popper) {
    background-color:rgba(0, 0, 0, .6) !important;
    backdrop-filter: blur(16px) brightness(1.2);
    -webkit-backdrop-filter: blur(16px) brightness(1.2);
    border: 1px solid rgba(255, 255, 255, .3);
    .el-popper__arrow {
      display: none !important;
    }
  }
  :deep(.el-dropdown-menu) {
    background: none !important;
    span {
      color: #fff;
      font-size: 14px;
      font-weight: 500;
    }
    .el-dropdown-menu__item {
      display: flex;
      justify-content: center;
    }
    .el-dropdown-menu__item:hover {
      background: rgba(0, 0, 0, .7) !important;
    }
    .el-dropdown-menu__item:focus {
      background: rgba(0, 0, 0, .7) !important;
    }
  }
}
</style>