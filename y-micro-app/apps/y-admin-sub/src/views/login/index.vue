<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
import {reactive, ref} from "vue";
import { useRouter } from "vue-router";
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import useUserStore from "@/store/modules/user.ts";

interface RuleFormType {
  username: string
  password: string
}

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()
const ruleFormRefs = ref<FormInstance>()

const isLoading = ref<boolean>(false)
const ruleForm = reactive<RuleFormType>({
  username: '',
  password: '',
})

const rules = reactive<FormRules<RuleFormType>>({
  username: [
    { required: true, message: t('login.usernameRequired'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('login.passwordRequired'), trigger: 'blur' },
  ]
})

const handleSubmit = async (formEl :FormInstance | undefined) => {
  if (!formEl) return;

  isLoading.value = true
  await formEl.validate((valid) => {
    if (valid) {
      userStore.login(ruleForm).then(() => {
        isLoading.value = false
        router.push('/')
      })
    } else {
      isLoading.value = false
    }
  })
}
</script>

<template>
  <div class="login-container">
    <el-form :model="ruleForm" :rules="rules" ref="ruleFormRefs">
      <el-form-item>
        <div class="login-title">
          <span>{{ t('login.title') }}</span>
        </div>
      </el-form-item>
      <el-form-item prop="username">
        <el-input
            v-model="ruleForm.username"
            style="height: 42px"
            :placeholder="t('login.username')"
            :prefix-icon="User"
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
            v-model="ruleForm.password"
            style="height: 42px"
            :prefix-icon="Lock"
            type="password"
            show-password
            :placeholder="t('login.password')"
        />
      </el-form-item>
      <el-form-item>
        <el-checkbox>{{ t('login.remember') }}</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button
            v-loading="isLoading"
            @click="handleSubmit(ruleFormRefs)"
            class="login-btn"
            type="primary"
        >
        {{ t('login.login') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.login-container {
  width: 100vw;
  height: 100vh;
  background: var(--app-bg-color);
  display: flex;
  justify-content: center;
  box-sizing: border-box;
  padding-top: 14vh;
}

.login-title {
  font-size: 24px;
  color: var(--app-text-color);
  font-weight: bold;
  letter-spacing: 2px;
  margin-bottom: 14px;
}

.login-btn {
  width: 320px;
  height: 42px;
}
</style>