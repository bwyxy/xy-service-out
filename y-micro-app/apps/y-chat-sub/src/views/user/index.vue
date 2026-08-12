<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import {ref} from "vue";
import EditInfo from "@/views/user/EditInfo.vue";
import EditPassword from "@/views/user/EditPassword.vue";

const { userInfo } = storeToRefs(useUserStore())

const tabVal = ref<string>('user')
</script>

<template>
  <div class="user">
    <div class="user-lt">
      <div class="user-lt-content">
        <upload-avatar>
          <div class="user-avatar">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="avatar" />
            <img v-else src="@/assets/images/logo.jpg" alt="avatar" />
          </div>
        </upload-avatar>
        <div class="user-item">
          <span>昵称</span>
          <span>{{ userInfo.nickname }}</span>
        </div>
        <div class="user-item">
          <span>性别</span>
          <span>{{ userInfo.sex === '1' ? '男' : userInfo.sex === '2' ? '女' : '未知' }}</span>
        </div>
        <div class="user-item">
          <span>手机号</span>
          <span>{{ userInfo.phone }}</span>
        </div>
        <div class="user-item">
          <span>注册邮箱</span>
          <span>{{ userInfo.email }}</span>
        </div>

      </div>
    </div>
    <div class="user-rt">
      <el-tabs v-model="tabVal" type="card">
        <el-tab-pane label="编辑资料" name="user">
          <edit-info />
        </el-tab-pane>
        <el-tab-pane label="修改密码" name="password">
          <edit-password />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped lang="scss">
.user {
  width: 100%;
  display: flex;
  box-sizing: border-box;
  padding: size(32);
  &-lt {
    margin-right: size(32);
  }
  &-rt {
    flex: 1;
    background: #fff;
    border-radius: size(13);
    box-shadow: rgba(0, 0, 0, 0.1) 0 2px 4px;
    box-sizing: border-box;
    padding: size(32);
  }
  .user-lt-content {
    width: size(420);
    min-height: size(240);
    background: #fff;
    border-radius: size(13);
    box-shadow: rgba(0, 0, 0, 0.1) 0 2px 4px;
    box-sizing: border-box;
    padding: size(32);
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .user-avatar {
    width: size(80);
    height: size(80);
    margin: size(16) 0 size(48);
    border-radius: 50px;
    overflow: hidden;
    position: relative;
    img {
      width: 100%;
      height: 100%;
      border-radius: 50px;
    }
    &:after {
      content: '上传头像';
      width: 100%;
      height: size(30);
      background: rgba(0, 0, 0, .2);
      color: #fff;
      font-size: size(12);
      @include flexCenter;
      position: absolute;
      left: 0;
      bottom: 0;
    }
  }

  .user-item {
    width: 100%;
    display: flex;
    align-items: center;
    font-size: size(18);
    letter-spacing: 1px;
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    box-sizing: border-box;
    padding: size(24) 0;
    span:nth-child(1) {
      flex: 1;
      color: #1f1f1f;
    }
    span:nth-child(2) {
      color: #8c8c8c;
    }
    &:first-child {
      padding-top: 0;
    }
  }
}
</style>