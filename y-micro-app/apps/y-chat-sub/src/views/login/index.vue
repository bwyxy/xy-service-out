<script setup lang="ts">
import Login from './modules/Login.vue'
import Register from './modules/Register.vue'
import DesktopLogin from './modules/DesktopLogin.vue'
import WindowControls from '@/components/WindowControls/index.vue'
import {computed, ref} from "vue";
import { isTauriLoginWindow, tauriLoginSuccess } from '@/utils/tauri.ts'

const type = ref<string>('login')
// Tauri 桌面登录小窗口：渲染微信风格紧凑布局
const isDesktop = isTauriLoginWindow()

const mainComponent = computed(() => {
  if (type.value === 'login') {
    return Login
  } else {
    return Register
  }
})

const handleSwitch = () => {
  type.value = type.value === 'login' ? 'register' : 'login'
}

// 桌面端登录成功：通知 Rust 携带凭据切换到主窗口
const handleDesktopSuccess = (creds: { token: string; wssAddr: string; wssKey: string }) => {
  tauriLoginSuccess(creds.token, creds.wssAddr, creds.wssKey).catch((e) => console.error('切换主窗口失败', e))
}
</script>

<template>
  <div :class="['login-container', isDesktop ? 'login-container--desktop' : '']">
    <template v-if="isDesktop">
      <!-- 无系统标题栏：顶部拖拽区承载窗口拖动与自定义关闭/最小化按钮 -->
      <div class="login-window-bar" data-tauri-drag-region>
        <WindowControls :maximizable="false" />
      </div>

      <DesktopLogin
          v-if="type === 'login'"
          @switch="handleSwitch"
          @success="handleDesktopSuccess"
      />
      <Register
          v-else
          @switch="handleSwitch"
      />
    </template>
    <transition
        v-else
        enter-active-class="animate__animated animate__fadeInUp"
        leave-active-class="animate__animated animate__fadeOutUp"
    >
      <component
          :is="mainComponent"
          @switch="handleSwitch"
      ></component>
    </transition>
  </div>
</template>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

// 桌面登录小窗口：微信风格，内容偏上、纯白背景
.login-container--desktop {
  position: relative;
  justify-content: flex-start;
  padding-top: 64px;
  background: #fff;
}

// 无系统标题栏时的顶部拖拽条（同时承载窗口控制按钮）
.login-window-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 6px 8px 0 0;
  box-sizing: border-box;
  z-index: 10;
}
</style>
