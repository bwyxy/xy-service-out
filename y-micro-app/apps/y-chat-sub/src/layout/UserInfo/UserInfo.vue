<script setup lang="ts">
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import useUserStore from '@/store/user.ts'
import { ArrowRight, Check, UserFilled} from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { themePresets, getTheme, setTheme } from '@/utils/theme.ts'

const router = useRouter();
const { userInfo } = storeToRefs(useUserStore())

/** 当前主题色，仅用于选中态展示 */
const currentTheme = ref(getTheme())

/** 切换主题色：立即生效并持久化到本地 */
const handleSelectTheme = (key: string) => {
  currentTheme.value = key
  setTheme(key)
}

const toPage = (url: string) => {
  router.push(url)
}
</script>

<template>
  <el-popover popper-class="user-popover" placement="right-end" :teleported="false" trigger="click">
    <template #reference>
      <div class="user-info">
        <div class="user-info-avatar">
          <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="avatar" />
          <img v-else src="@/assets/images/logo.jpg" alt="avatar" />
        </div>
      </div>
    </template>
    <div class="info-box">
      <div class="info-box-top">
        <div class="info-avatar">
          <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="avatar" />
          <img v-else src="@/assets/images/logo.jpg" alt="avatar" />
        </div>
        <div>
          <div class="info-name">
            <span>{{ userInfo.nickname }}</span>
            <el-icon
                :color="userInfo.sex === '1' ? '#69b1ff' : userInfo.sex === '2' ? '#f5222d' : '#bfbfbf'"
            ><UserFilled />
            </el-icon>
          </div>
          <div class="info-phone">
            <span>手机号: {{ userInfo.phone }}</span>
          </div>
        </div>
      </div>

      <div class="info-box-btm" @click="toPage('/user')">
        <span>编辑资料</span>
        <el-icon><ArrowRight /></el-icon>
      </div>

      <div class="info-box-theme">
        <span>主题色</span>
        <div class="theme-list">
          <div
              v-for="item in themePresets"
              :key="item.key"
              class="theme-dot"
              :style="{ background: item.color }"
              :title="item.name"
              @click="handleSelectTheme(item.key)"
          >
            <el-icon v-if="currentTheme === item.key" color="#fff"><Check /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<style scoped lang="scss">
.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  span {
    font-size: size(16);
    font-weight: 700;
    letter-spacing: 1px;
    margin-left: size(6);
  }
}

.user-info-avatar {
  width: size(48);
  height: size(48);
  margin: 8px 0 16px;
  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
  }
}

.info-box {
  width: 100%;
  &-top {
    display: flex;
    align-items: center;
    padding-bottom: size(16);
    border-bottom: 1px solid rgba(0, 0, 0, .06);
  }
  &-btm {
    padding: size(24) 0 size(8);
    display: flex;
    align-items: center;
    font-size: size(16);
    cursor: pointer;
    span {
      flex: 1;
      letter-spacing: 1px;
      color: #1f1f1f;
    }
  }
  &-theme {
    padding: size(16) 0 size(8);
    display: flex;
    align-items: center;
    border-top: 1px solid rgba(0, 0, 0, .06);
    > span {
      flex: 1;
      font-size: size(16);
      letter-spacing: 1px;
      color: #1f1f1f;
    }
  }
  .theme-list {
    display: flex;
    align-items: center;
    gap: size(10);
  }
  .theme-dot {
    width: size(20);
    height: size(20);
    font-size: size(12);
    border-radius: 50%;
    box-sizing: border-box;
    cursor: pointer;
    transition: transform .15s;
    @include flexCenter;
    &:hover {
      transform: scale(1.15);
    }
  }
  .info-name {
    display: flex;
    align-items: center;
    font-size: size(20);
    span {
      font-weight: 700;
      color: #1f1f1f;
      letter-spacing: 2px;
      @include textEllipsis(1);
    }
  }
  .info-phone {
    font-size: size(14);
    letter-spacing: 1px;
    font-weight: 400;
    color: #8c8c8c;
  }
  .info-avatar {
    width: size(50);
    height: size(50);
    margin-right: size(8);
    img {
      width: 100%;
      height: 100%;
      border-radius: size(8);
    }
  }
}
</style>