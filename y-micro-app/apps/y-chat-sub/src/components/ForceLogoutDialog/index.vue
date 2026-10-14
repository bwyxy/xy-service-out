<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { messageHandlerService } from '@/services/im'
import type { ForceLogoutHandler } from '@/services/im/message-handler'
import type { WukongIMMessagePayload } from '@/services/im'
import { getToken } from '@/utils/auth.ts'
import useUserStore from '@/store/user.ts'

/**
 * 强制下线弹窗
 *
 * 收到服务端 220 信令（同类型设备新登录）后立即弹出提示，
 * 同时清除本地登录态并引导用户回到登录页。
 * 全局常驻于 layout，与 CallOverlay / IncomingCallDialog 同级。
 */

/** 强制下线信令内容 */
interface ForceLogoutContent {
  uid?: string | number
  deviceType?: number
  deviceName?: string
  /** 被顶下线的 token：用于校验本机是否仍持有该会话，避免误踢新登录设备 */
  kickedToken?: string
  reason?: string
}

const DEFAULT_REASON = '您的账号已在其他同类型设备上登录，当前设备已被强制退出'

const router = useRouter()
const userStore = useUserStore()

const visible = ref(false)
const reason = ref(DEFAULT_REASON)

/** 统一触发：弹窗 + 清除本地登录态 */
function triggerForceLogout(msg?: string) {
  if (visible.value) {
    return
  }
  reason.value = msg || DEFAULT_REASON
  visible.value = true
  userStore.outLogin()
}

const handleForceLogout: ForceLogoutHandler = (_message, payload: WukongIMMessagePayload) => {
  const content = parseContent(payload.content)

  // token 校验：若服务端标记的会话 token 与本机持有的不一致，
  // 说明本机已是新登录的设备（旧会话被踢的是另一台），忽略该通知
  const kickedToken = content?.kickedToken
  if (kickedToken && kickedToken !== getToken()) {
    return
  }

  triggerForceLogout(content?.reason)
}

/** 接口返回登录态失效（HTTP 401 / 业务码 4011）时同样走强制退出流程 */
function handleSessionExpired(event: Event) {
  const detail = (event as CustomEvent<{ reason?: string }>).detail
  triggerForceLogout(detail?.reason)
}

/** 兼容 content 为 JSON 字符串或已解析对象两种形式 */
function parseContent(raw: unknown): ForceLogoutContent | null {
  if (!raw) {
    return null
  }
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw) as ForceLogoutContent
    } catch {
      return null
    }
  }
  if (typeof raw === 'object') {
    return raw as ForceLogoutContent
  }
  return null
}

/** 跳转回登录页 */
function goLogin(): void {
  visible.value = false
  router.push('/login')
}

onMounted(() => {
  messageHandlerService.onForceLogout(handleForceLogout)
  window.addEventListener('session-expired', handleSessionExpired)
})

onUnmounted(() => {
  messageHandlerService.offForceLogout(handleForceLogout)
  window.removeEventListener('session-expired', handleSessionExpired)
})
</script>

<template>
  <el-dialog
      :model-value="visible"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      width="360px"
      align-center
      class="force-logout-dialog"
      append-to-body
  >
    <div class="force-logout-body">
      <span class="force-logout-title">登录已失效</span>
      <span class="force-logout-desc">{{ reason }}</span>
      <el-button type="primary" class="force-logout-btn" @click="goLogin">重新登录</el-button>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.force-logout-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: size(12);
  padding: size(8) 0 size(4);
}

.force-logout-title {
  font-size: size(18);
  color: #333;
  font-weight: 500;
}

.force-logout-desc {
  font-size: size(14);
  color: #999;
  line-height: 1.6;
  text-align: center;
}

.force-logout-btn {
  margin-top: size(10);
  width: size(200);
}
</style>