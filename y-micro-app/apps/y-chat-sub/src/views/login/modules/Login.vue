<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
import {reactive, ref} from "vue";
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import useUserStore from "@/store/user.ts";
import { useRouter } from "vue-router";

const router = useRouter();
const emit = defineEmits(['switch'])
const userStore = useUserStore()

interface RuleForm {
  account: string
  password: string
  remPassword: boolean
  agreed: boolean
}

const isLoading = ref(false);
const ruleFormRefs = ref<FormInstance>()
const ruleForm = reactive<RuleForm>({
  account: '',
  password: '',
  remPassword: false,
  agreed: false,
})

const rules = reactive<FormRules<RuleForm>>({
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
})

/** 打开用户协议 / 隐私政策独立页 */
const openAgreement = (type: 'user' | 'privacy') => {
  router.push({ path: '/agreement', query: { type } })
}

const handleSubmit = async (elForm: FormInstance | undefined) => {
  if (!elForm) return;

  if (!ruleForm.agreed) {
    ElMessage.warning('请先阅读并同意《用户协议》和《隐私政策》')
    return
  }

  const valid = await elForm.validate()

  if (!valid) return;

  isLoading.value = true
  const params = {
    account: ruleForm.account,
    password: ruleForm.password,
  }
  const result = await userStore.userLogin(params)

  isLoading.value = false
  if (result) {
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
      <div class="login-title">
        <div class="logo-box">
          <img src="@/assets/images/logo.jpg" alt="logo" />
        </div>
        <span>飞鸽即时通讯</span>
<!--        <span>古驿飞鸽远，今屏传讯捷。千山一线牵，万里语声接。</span>-->
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
      <div class="agreement-box">
        <el-checkbox v-model="ruleForm.agreed" class="agreement-checkbox">我已阅读并同意</el-checkbox>
        <span class="agreement-link" @click="openAgreement('user')">《用户协议》</span>
        <span class="agreement-text">和</span>
        <span class="agreement-link" @click="openAgreement('privacy')">《隐私政策》</span>
      </div>
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

.agreement-box {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  font-size: size(12);
  line-height: 1.6;

  :deep(.el-checkbox) {
    height: auto;
    margin-right: 0;
  }

  :deep(.el-checkbox__label) {
    font-size: size(12);
    color: #909399;
  }

  .agreement-text {
    margin: 0 size(2);
    color: #909399;
  }

  .agreement-link {
    color: $theme-color;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>