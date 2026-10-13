<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import useUserStore from '@/store/user.ts'

const emit = defineEmits(['switch', 'success'])
const userStore = useUserStore()

interface RuleForm {
  account: string
  password: string
}

const isLoading = ref(false)
/** 登录成功，正在播放窗口退出动画 */
const isSuccess = ref(false)
const ruleFormRef = ref<FormInstance>()
const ruleForm = reactive<RuleForm>({
  account: '',
  password: '',
})

const rules = reactive<FormRules<RuleForm>>({
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

const handleSubmit = async (elForm: FormInstance | undefined) => {
  if (!elForm || isLoading.value) return

  const valid = await elForm.validate().catch(() => false)
  if (!valid) return

  isLoading.value = true
  const params = {
    account: ruleForm.account,
    password: ruleForm.password,
  }
  const result = await userStore.userLogin(params)
  isLoading.value = false

  if (result) {
    // 播放窗口缩放退出动画，随后由父级通知 Rust 携带凭据切换到主窗口
    isSuccess.value = true
    setTimeout(() => emit('success', result), 420)
  }
}
</script>

<template>
  <div
      :class="[
          'desktop-login',
          'animate__animated',
          isSuccess ? 'animate__zoomOut' : 'animate__fadeInUp',
      ]"
  >
    <!-- 用户头像 -->
    <div class="avatar-box">
      <img src="@/assets/images/logo.jpg" alt="avatar" />
    </div>

    <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        :rules="rules"
        class="desktop-login-form"
        @submit.prevent
    >
      <!-- 微信风格：账号/密码组合输入框 -->
      <div class="input-group">
        <el-form-item prop="account">
          <el-input
              v-model="ruleForm.account"
              :prefix-icon="User"
              placeholder="请输入手机号或邮箱"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="ruleForm.password"
              type="password"
              show-password
              :prefix-icon="Lock"
              placeholder="请输入密码"
              @keyup.enter="handleSubmit(ruleFormRef)"
          />
        </el-form-item>
      </div>

      <div class="option-row">
        <span class="option-link">自动登录</span>
        <span class="option-link">忘记密码？</span>
      </div>

      <el-button
          class="login-btn"
          type="primary"
          :loading="isLoading"
          @click="handleSubmit(ruleFormRef)"
      >
        登 录
      </el-button>

      <div class="register-row">
        <span class="register-link" @click="emit('switch')">注册新账号</span>
      </div>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.desktop-login {
  width: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;

  .avatar-box {
    width: 84px;
    height: 84px;
    border-radius: 50%;
    overflow: hidden;
    border: 1px solid #e5e5e5;
    margin-bottom: 28px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
  }

  .desktop-login-form {
    width: 100%;
  }

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  .input-group {
    width: 100%;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    overflow: hidden;
    background: #fff;

    :deep(.el-input__wrapper) {
      box-shadow: none;
      border-radius: 0;
      background: transparent;
      padding: 6px 12px;

      &.is-focus {
        box-shadow: none;
      }
    }

    // 账号与密码之间的分隔线（微信风格）
    :deep(.el-form-item + .el-form-item) {
      border-top: 1px solid #ececec;
    }
  }

  .option-row {
    width: 100%;
    display: flex;
    justify-content: space-between;
    margin: 14px 2px 20px;

    .option-link {
      font-size: 12px;
      color: #999;
      cursor: pointer;
      user-select: none;

      &:hover {
        color: #07c160;
      }
    }
  }

  .login-btn {
    width: 100%;
    height: 40px;
    font-size: 15px;
    letter-spacing: 4px;
    border: none;
    border-radius: 6px;
    background: #07c160;
    color: #fff;

    &:hover,
    &:focus {
      background: #06ad56;
    }

    &.is-loading {
      background: #07c160;
      opacity: 0.85;
    }
  }

  .register-row {
    width: 100%;
    text-align: center;
    margin-top: 18px;

    .register-link {
      font-size: 13px;
      color: #576b95;
      cursor: pointer;

      &:hover {
        opacity: 0.8;
      }
    }
  }
}
</style>
