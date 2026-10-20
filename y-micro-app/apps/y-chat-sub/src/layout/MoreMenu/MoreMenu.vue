<script setup lang="ts">

import {Menu} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import {logout} from "@/api/auth.ts";
import {isTauri} from "@/utils/tauri.ts";
import useUserStore from "@/store/user.ts";
import useNotificationStore from "@/store/notification.ts";
import useAppVersionStore from "@/store/appVersion.ts";
import {useRouter} from "vue-router";

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const appVersionStore = useAppVersionStore()
const router = useRouter()

/** 手动检查更新：无新版本时给出明确反馈 */
const handleCheckUpdate = async () => {
  const info = await appVersionStore.checkUpdate(false)
  if (!info) {
    ElMessage.success('当前已是最新版本')
  }
}

const handleLogout = () => {
  // 先通知服务端结束会话（token 立即失效），再清理本地状态；
  // 服务端标记失败不阻断本地退出流程
  logout().catch(() => {})
  userStore.outLogin()
  notificationStore.reset()
  // 网页端需主动跳转登录页；桌面端由 Rust 隐藏主窗口并弹出登录小窗口
  if (!isTauri()) {
    router.push('/login')
  }
}
</script>

<template>
  <el-popover
      popper-class="more-menu-popover"
      placement="right-end"
      :teleported="false"
      trigger="click"
      width="60px"
  >
    <template #reference>
      <div class="layout-nav-btm">
        <div class="nav-item">
          <el-icon><Menu /></el-icon>
        </div>
      </div>
    </template>
    <div class="more-menu-box">
      <div class="more-menu-btm" @click="handleCheckUpdate">
        <span>检查更新</span>
      </div>
      <div class="more-menu-btm" @click="handleLogout">
        <span>退出登录</span>
      </div>
    </div>
  </el-popover>
</template>

<style scoped lang="scss">
.layout-nav-btm {
  color: #8c8c8c;
  font-size: size(28);
  cursor: pointer;
}

.more-menu-box {
  .more-menu-btm {
    width: 100%;
    height: 40px;
    font-size: 13px;
    letter-spacing: 1px;
    color: #1f1f1f;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    border-top: 1px solid #f0f0f0;
    box-sizing: border-box;
    &:hover {
      background: $body-bg-color;
    }
  }
}
</style>