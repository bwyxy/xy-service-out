<script setup lang="ts">
import { ref } from 'vue'
import useChatStore from '@/store/chat'
import { ChatDotRound, User, SwitchButton } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const showLoginDialog = ref(false)
const loginUid = ref('')
const loginName = ref('')
const logging = ref(false)

const handleLogin = async () => {
  if (!loginUid.value.trim()) return
  logging.value = true
  try {
    await chatStore.login(loginUid.value.trim(), loginName.value.trim() || loginUid.value.trim())
    localStorage.setItem('chat_uid', loginUid.value.trim())
    localStorage.setItem('chat_name', loginName.value.trim() || loginUid.value.trim())
    showLoginDialog.value = false
  } catch (e: any) {
    console.error('登录失败', e)
  } finally {
    logging.value = false
  }
}

const handleLogout = () => {
  chatStore.logout()
  localStorage.removeItem('chat_uid')
  localStorage.removeItem('chat_name')
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
  <el-dialog v-model="showLoginDialog" title="登录聊天" width="320px" :close-on-click-modal="false">
    <el-input v-model="loginUid" placeholder="输入用户ID" style="margin-bottom: 10px" />
    <el-input v-model="loginName" placeholder="昵称（可选，默认同ID）" @keyup.enter="handleLogin" />
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
</style>