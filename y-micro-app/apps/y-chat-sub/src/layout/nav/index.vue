<script setup lang="ts">
import { ref } from 'vue'
import useChatStore from '@/store/chat'
import { ChatDotRound, User, SwitchButton } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const showLoginDialog = ref(false)
const loginAccount = ref('')
const loginPassword = ref('')
const logging = ref(false)
const loginError = ref('')

const handleLogin = async () => {
  if (!loginAccount.value.trim() || !loginPassword.value.trim()) {
    loginError.value = '请输入账号和密码'
    return
  }
  loginError.value = ''
  logging.value = true
  try {
    await chatStore.login(loginAccount.value.trim(), loginPassword.value.trim())
    showLoginDialog.value = false
    loginAccount.value = ''
    loginPassword.value = ''
  } catch (e: any) {
    loginError.value = e?.message || '登录失败，请检查账号密码'
  } finally {
    logging.value = false
  }
}

const handleLogout = () => {
  chatStore.logout()
}
</script>

<template>
  <div class="layout-nav">
    <!-- 用户头像区域 -->
    <div class="nav-avatar" @click="chatStore.connected ? handleLogout() : (showLoginDialog = true)">
      <el-icon :size="22">
        <ChatDotRound v-if="chatStore.connected" color="#52c41a" />
        <User v-else />
      </el-icon>
      <span class="avatar-text">{{ chatStore.connected ? chatStore.name : '登录' }}</span>
    </div>

    <!-- 退出按钮 -->
    <div v-if="chatStore.connected" class="nav-item" @click="handleLogout">
      <el-icon :size="20"><SwitchButton /></el-icon>
      <span class="item-text">退出</span>
    </div>
  </div>

  <!-- 登录弹窗 -->
  <el-dialog v-model="showLoginDialog" title="登录飞鸽" width="360px" :close-on-click-modal="false">
    <el-input
      v-model="loginAccount"
      placeholder="手机号 / 邮箱"
      style="margin-bottom: 12px"
      clearable
    />
    <el-input
      v-model="loginPassword"
      type="password"
      placeholder="密码"
      show-password
      @keyup.enter="handleLogin"
    />
    <p v-if="loginError" class="error-msg">{{ loginError }}</p>
    <template #footer>
      <el-button @click="showLoginDialog = false">取消</el-button>
      <el-button type="primary" :loading="logging" @click="handleLogin">登录</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.layout-nav {
  width: 84px;
  height: 100%;
  background: $nav-bg-color;
  border-radius: $border-radius 0 0 $border-radius;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 20px;
  gap: 10px;
}

.nav-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover {
    background: rgba(0, 0, 0, 0.05);
  }

  .avatar-text {
    font-size: 11px;
    color: #666;
  }
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
  color: #999;

  &:hover {
    background: rgba(0, 0, 0, 0.05);
  }

  .item-text {
    font-size: 11px;
  }
}

.error-msg {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 8px;
}
</style>