<script setup lang="ts">

import {reactive, ref} from "vue";
import { ElMessage } from 'element-plus'
import type { FormRules, FormInstance } from 'element-plus'
import { sendEmailCode, editPassword } from '@/api/auth.ts'

interface RuleForm {
  oldPassword: string
  newPassword: string
  confirmNewPassword: string
  email: string
  emailCode: string
}

const ruleFormRefs = ref<FormInstance>()

const smsCode = reactive({
  text: '发送验证码',
  loading: false,
  codeCooling: false,
  remainingTime: 120,
  timer: null as any
})

const ruleForm = reactive<RuleForm>({
  oldPassword: "",
  email: "",
  emailCode: "",
  newPassword: "",
  confirmNewPassword: "",
})

const rule = reactive<FormRules<RuleForm>>({
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: "blur" },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: "blur" },
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: "blur" },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: "blur" },
  ],
  confirmNewPassword: [
    { required: true, message: '请确认新密码', trigger: "blur" },
  ]
})

/** 发送验证码 */
const handleSend = async (elForm :FormInstance | undefined) :Promise<void> => {
  if (!elForm) return;
  const valid = await elForm.validateField('email')

  if (!valid) return;

  if (smsCode.codeCooling || smsCode.loading) {
    ElMessage.warning('发送太过于频繁了')
    return
  }

  smsCode.loading = true
  const result = await sendEmailCode(ruleForm.email)

  smsCode.loading = false
  if (result.code !== 200) return;

  smsCode.codeCooling = true
  smsCode.timer = setInterval(() => {
    if (smsCode.remainingTime <= 0) {
      clearInterval(smsCode.timer)
      smsCode.timer = null
      smsCode.codeCooling = false
      smsCode.text = '重新发送'
      smsCode.remainingTime = 120
    } else {
      smsCode.remainingTime--
    }
  }, 1000)
}

/** 修改密码 */
const isLoading = ref<boolean>(false)
const handleSubmit = async (elForm: FormInstance | undefined) => {
  if (!elForm) return;

  const valid = await elForm.validate();

  if (!valid) return;

  isLoading.value = true
  const response = await editPassword(ruleForm)

  isLoading.value = false
  if (response.code === 200) {
    ElMessage.success("修改密码成功")
    elForm.resetFields()
  }
}
</script>

<template>
  <el-form ref="ruleFormRefs" :model="ruleForm" :rules="rule" label-position="right" label-width="100px">
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="ruleForm.email" />
    </el-form-item>
    <el-form-item label="邮箱验证码" prop="emailCode">
      <div class="sms-code">
        <el-input v-model="ruleForm.emailCode" />
        <el-button
            v-loading="smsCode.loading"
            type="primary"
            size="small"
            :plain="!smsCode.codeCooling"
            class="sms-code-btn"
            @click="handleSend(ruleFormRefs)"
        >
          {{ smsCode.codeCooling ? `${smsCode.remainingTime}后重新发送` : smsCode.text }}
        </el-button>
      </div>
    </el-form-item>
    <el-form-item label="旧密码" prop="oldPassword">
      <el-input
          v-model="ruleForm.oldPassword"
          type="password"
          show-password
      />
    </el-form-item>
    <el-form-item label="新密码" prop="newPassword">
      <el-input
          v-model="ruleForm.newPassword"
          type="password"
          show-password
      />
    </el-form-item>
    <el-form-item label="确认新密码" prop="confirmNewPassword">
      <el-input
          v-model="ruleForm.confirmNewPassword"
          type="password"
          show-password
      />
    </el-form-item>
    <el-form-item>
      <div class="form-btm">
        <el-button v-loading="isLoading" type="primary" @click="handleSubmit(ruleFormRefs)">确认</el-button>
      </div>
    </el-form-item>
  </el-form>
</template>

<style scoped lang="scss">
.sms-code {
  width: 100%;
  display: flex;
  align-items: center;
  .sms-input {
    flex: 1;
  }
}

.sms-code-btn {
  height: size(42);
  font-size: size(16);
  letter-spacing: 1px;
  margin-left: size(14);
}

.form-btm {
  width: 100%;
  display: flex;
  justify-content: center;
}
</style>