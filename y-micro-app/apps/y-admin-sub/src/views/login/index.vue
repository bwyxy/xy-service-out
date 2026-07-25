<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
import {reactive, ref} from "vue";
import type { FormInstance, FormRules } from 'element-plus'

interface RuleFormType {
  username: string
  password: string
}

const ruleFormRefs = ref<FormInstance>()

const isLoading = ref<boolean>(false)
const ruleForm = reactive<RuleFormType>({
  username: '',
  password: '',
})

const rules = reactive<FormRules<RuleFormType>>({
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ]
})

const handleSubmit = async (formEl :FormInstance | undefined) => {
  if (!formEl) return;

  isLoading.value = true
  await formEl.validate((valid) => {
    if (valid) {

    } else {
      isLoading.value = false
    }
  })
}

console.log(import.meta.env.VITE_BASE_API_URL)
</script>

<template>
  <div class="login-container">
    <el-form :model="ruleForm" :rules="rules" ref="ruleFormRefs">
      <el-form-item>
        <div class="login-title">
          <span>您好, 欢迎登录!</span>
        </div>
      </el-form-item>
            v-model="ruleForm.username"
            style="height: 42px"
            :prefix-icon="User"
            placeholder="用户名"
          :prefix-icon="User"
          placeholder="用户名"
        />
      </el-form-item>
            v-model="ruleForm.password"
            style="height: 42px"
            :prefix-icon="Lock"
            type="password"
            show-password
            placeholder="密码"
          placeholder="密码"
      </el-form-item>
      <el-form-item>
        <el-checkbox>记住密码</el-checkbox>
          @keyup.enter="handleSubmit(ruleFormRefs)"
        />
      </el-form-item>
            v-loading="isLoading"
            @click="handleSubmit(ruleFormRefs)"
            class="login-btn"
            type="primary"
          class="login-btn"
          type="primary"
        >登录</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.login-container {
  width: 100vw;
  height: 100vh;
  background: #f0f5ff;
  display: flex;
  justify-content: center;
  box-sizing: border-box;
  padding-top: 14vh;
}

.login-title {
  font-size: 24px;
  color: #1f1f1f;
  font-weight: bold;
  letter-spacing: 2px;
  margin-bottom: 14px;
}

.login-btn {
  width: 320px;
  height: 42px;
}
</style>