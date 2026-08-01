<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
import {reactive, ref} from "vue";
import type { FormInstance, FormRules } from 'element-plus'
import useUserStore from "@/store/user.ts";
import { useRouter } from "vue-router";

const router = useRouter();
const emit = defineEmits(['switch'])
const userStore = useUserStore()

interface RuleForm {
  account: string
  password: string
  remPassword: boolean
}

const isLoading = ref(false);
const ruleFormRefs = ref<FormInstance>()
const ruleForm = reactive<RuleForm>({
  account: '',
  password: '',
  remPassword: false,
})

const rules = reactive<FormRules<RuleForm>>({
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
})

const handleSubmit = async (elForm: FormInstance | undefined) => {
  if (!elForm) return;

  const valid = await elForm.validate()

  if (!valid) return;

  isLoading.value = true
  const params = {
    account: ruleForm.account,
    password: ruleForm.password,
  }
  const isSuccess = await userStore.userLogin(params)

  isLoading.value = false
  if (isSuccess) {
    router.push({
      path: '/'
    })
  }
}
</script>

<template>
  <el-form
      ref="ruleFormRefs"
      :model="ruleForm"
      :rules="rules"
      :class="[
          'login-panel',
      ]"
  >
    <el-form-item>
      <div class="logo-box">
        <img src="@/assets/images/logo.jpg" alt="logo" />
      </div>
    </el-form-item>
    <el-form-item prop="account">
      <el-input
          v-model="ruleForm.account"
          :prefix-icon="User"
          class="login-input"
          placeholder="请输入手机号或者邮箱"
      />
    </el-form-item>
    <el-form-item prop="password">
      <el-input
          v-model="ruleForm.password"
          type="password"
          show-password
          :prefix-icon="Lock"
          class="login-input"
          placeholder="请输入密码"
      />
    </el-form-item>
    <el-form-item>
      <div class="option-box">
        <el-checkbox v-model="ruleForm.remPassword">记住密码</el-checkbox>
        <div class="option-box-rt">
          <span>忘记密码</span>
        </div>
      </div>
    </el-form-item>
    <el-form-item>
      <el-button
          v-loading="isLoading"
          class="submit-btn"
          type="primary"
          @click="handleSubmit(ruleFormRefs)"
      >登录</el-button>
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