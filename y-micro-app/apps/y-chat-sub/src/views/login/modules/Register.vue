<script setup lang="ts">

import {Iphone, Message, Lock} from "@element-plus/icons-vue";

const emit = defineEmits(['switch'])
</script>

<template>
  <el-form
      :class="[
          'login-panel'
      ]"
  >
    <el-form-item>
      <div class="register-title">
        <span>注册</span>
      </div>
    </el-form-item>
    <el-form-item>
      <el-input
          :prefix-icon="Iphone"
          class="login-input"
          placeholder="请输入手机号"
      />
    </el-form-item>
    <el-form-item>
      <el-input
          :prefix-icon="Message"
          class="login-input"
          placeholder="请输入邮箱"
      >
      </el-input>
    </el-form-item>
    <el-form-item>
      <div class="sms-code">
        <el-input
            class="login-input sms-input"
            placeholder="请输入邮箱验证码"
        >
        </el-input>
        <el-button
            type="primary"
            plain
            class="sms-code-btn"
        >发送验证码</el-button>
      </div>

    </el-form-item>
    <el-form-item>
      <el-input
          :prefix-icon="Lock"
          class="login-input"
          placeholder="请输入密码"
      />
    </el-form-item>
    <el-form-item>
      <el-input
          :prefix-icon="Lock"
          class="login-input"
          placeholder="请确认密码"
      />
    </el-form-item>
    <el-form-item>
      <el-button class="submit-btn" type="primary">登录</el-button>
    </el-form-item>
    <el-form-item>
      <span class="register-text" @click="emit('switch')">前往登录 >>></span>
    </el-form-item>
  </el-form>
</template>

<style scoped lang="scss">
.register-title {
  font-size: size(42);
  letter-spacing: 2px;
  margin-top: size(24);
}

.sms-code {
  width: 100%;
  display: flex;
  align-items: center;
  .sms-input {
    flex: 1;
  }
}

.sms-code-btn {
  height: size(52);
  font-size: size(16);
  letter-spacing: 1px;
  margin-left: size(14);
}

:deep(.el-form-item) {
  width: 100%;
  margin-bottom: size(36);
  .el-form-item__content {
    justify-content: center;
  }
}
</style>