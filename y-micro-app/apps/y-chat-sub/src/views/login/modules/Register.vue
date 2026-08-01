<script setup lang="ts">
import {reactive, ref} from "vue";
import {Iphone, Message, Lock} from "@element-plus/icons-vue";
import {ElMessage, FormInstance, FormRules} from 'element-plus'
import { checkPhone, checkEmail, checkConfirmPassword } from '@/utils/verify.ts'
import { sendEmailCode } from '@/api/auth.ts'
import useUserStore from '@/store/user.ts'

const userStore = useUserStore()
const emit = defineEmits(['switch'])

interface RuleForm {
  phone: string
  email: string
  password: string
  confirmPassword: string
  emailCode: string
  sex: string
}

const smsCode = reactive({
  text: '发送验证码',
  loading: false,
  codeCooling: false,
  remainingTime: 120,
  timer: null as any
})

const isLoading = ref(false)
const ruleFormRefs = ref<FormInstance>()
const ruleForm = reactive<RuleForm>({
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  emailCode: '',
  sex: '0'
})

const rules = reactive<FormRules<RuleForm>>({
  phone: [
    { validator: checkPhone, trigger: 'blur' },
  ],
  email: [
    { validator: checkEmail, trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ],
  confirmPassword: [
    {
      validator: (rule :any, value :any, callback :any) =>
          checkConfirmPassword(rule, value, callback,ruleForm.password),
      trigger: 'blur'
    },
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
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

/** 提交 */
const handleSubmit = async (elForm: FormInstance | undefined) => {
  if(!elForm) return;

  const valid = await elForm.validate();

  if (!valid) return;

  isLoading.value = true
  const isSuccess = await userStore.userRegister(ruleForm);

  isLoading.value = false
  if (isSuccess) {
    ElMessage.success('注册成功')
    elForm.resetFields();
    emit('switch')
  }
}
</script>

<template>
  <el-form
      ref="ruleFormRefs"
      :model="ruleForm"
      :rules="rules"
      :class="[
          'login-panel'
      ]"
  >
    <el-form-item>
      <div class="register-title">
        <span>注册</span>
      </div>
    </el-form-item>
    <el-form-item prop="phone">
      <el-input
          v-model="ruleForm.phone"
          :prefix-icon="Iphone"
          class="login-input"
          placeholder="请输入手机号"
      />
    </el-form-item>
    <el-form-item prop="email">
      <el-input
          v-model="ruleForm.email"
          :prefix-icon="Message"
          class="login-input"
          placeholder="请输入邮箱"
      >
      </el-input>
    </el-form-item>
    <el-form-item prop="emailCode">
      <div class="sms-code">
        <el-input
            v-model="ruleForm.emailCode"
            class="login-input sms-input"
            placeholder="请输入邮箱验证码"
        >
        </el-input>
        <el-button
            v-loading="smsCode.loading"
            type="primary"
            :plain="!smsCode.codeCooling"
            class="sms-code-btn"
            @click="handleSend(ruleFormRefs)"
        >
          {{ smsCode.codeCooling ? `${smsCode.remainingTime}后重新发送` : smsCode.text }}
        </el-button>
      </div>

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
    <el-form-item prop="confirmPassword">
      <el-input
          v-model="ruleForm.confirmPassword"
          type="password"
          show-password
          :prefix-icon="Lock"
          class="login-input"
          placeholder="请确认密码"
      />
    </el-form-item>
    <el-form-item>
      <el-button
          v-loading="isLoading"
          class="submit-btn"
          type="primary"
          @click="handleSubmit(ruleFormRefs)"
      >提交</el-button>
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