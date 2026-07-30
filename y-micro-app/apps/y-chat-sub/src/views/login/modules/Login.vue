<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
import {reactive} from "vue";

const emit = defineEmits(['switch'])

const ruleForm = reactive({
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  emailCode: '',
  sex: ''
})
</script>

<template>
  <el-form
      :class="[
          'login-panel',
      ]"
  >
    <el-form-item>
      <div class="logo-box">
        <img src="@/assets/images/logo.jpg" alt="logo" />
      </div>
    </el-form-item>
    <el-form-item>
      <el-input
          :prefix-icon="User"
          class="login-input"
          placeholder="请输入手机号或者邮箱"
      />
    </el-form-item>
    <el-form-item>
      <el-input
          type="password"
          show-password
          :prefix-icon="Lock"
          class="login-input"
          placeholder="请输入密码"
      />
    </el-form-item>
    <el-form-item>
      <div class="option-box">
        <el-checkbox>记住密码</el-checkbox>
        <div class="option-box-rt">
          <span>忘记密码</span>
        </div>
      </div>
    </el-form-item>
    <el-form-item>
      <el-button class="submit-btn" type="primary">登录</el-button>
    </el-form-item>
    <el-form-item>
      <span class="register-text" @click="emit('switch')">没有账号, 注册用户>>></span>
    </el-form-item>
  </el-form>
</template>

<style scoped lang="scss">
:deep(.el-form-item) {
  width: 100%;
  margin-bottom: size(36);
  .el-form-item__content {
    justify-content: center;
  }
}
</style>