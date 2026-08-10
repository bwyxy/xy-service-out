<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from '@/store/user.ts'
import {ArrowDown, ArrowRight, UserFilled} from "@element-plus/icons-vue";
import { useRouter } from "vue-router";

const router = useRouter();
const { userInfo } = storeToRefs(useUserStore())

const toPage = (url: string) => {
  router.push(url)
}
</script>

<template>
  <el-popover popper-class="user-popover" :teleported="false" trigger="click">
    <template #reference>
      <div class="user-info">
        <div class="user-info-avatar">
          <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="avatar" />
          <img v-else src="@/assets/images/logo.jpg" alt="avatar" />
        </div>
        <span>{{ userInfo.nickname || '未登录' }}</span>
        <el-icon><ArrowDown /></el-icon>
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
    </div>
  </el-popover>
</template>

<style scoped lang="scss">
.user-info {
  display: flex;
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
  width: size(28);
  height: size(28);
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