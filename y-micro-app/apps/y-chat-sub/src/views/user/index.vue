<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import {ref} from "vue";

const { userInfo } = storeToRefs(useUserStore())

const tabVal = ref<string>('user')
</script>

<template>
  <div class="user">
    <div class="user-lt">
      <div class="user-lt-content">
        <div class="user-avatar">
          <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="avatar" />
          <img v-else src="@/assets/images/logo.jpg" alt="avatar" />
        </div>
      </div>
    </div>
    <div class="user-rt">
      <el-tabs v-model="tabVal" type="card">
        <el-tab-pane label="编辑资料" name="user"></el-tab-pane>
        <el-tab-pane label="修改密码" name="password"></el-tab-pane>
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
}
</style>