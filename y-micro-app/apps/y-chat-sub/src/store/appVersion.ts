import { defineStore } from 'pinia'
import { ref } from 'vue'
import { checkAppUpdate, type AppUpdateInfo } from '@/api/appVersion.ts'
import { detectPlatform } from '@/utils/platform.ts'

/** 应用标识，与后端 sys_app_version.app_key 一致 */
const APP_KEY = 'chat'

/** 可选更新「稍后再说」记录：保存本次会话忽略的版本码 */
const SKIP_VERSION_KEY = 'chat_skip_update_version'

/** 桌面端打开外部下载地址（Tauri 会拦截 window.open，必须走 opener 插件） */
async function openExternal(url: string): Promise<void> {
  const { openUrl } = await import('@tauri-apps/plugin-opener')
  await openUrl(url)
}

/**
 * 应用版本更新 Store
 *
 * 客户端启动静默检查一次（失败不打扰用户），也可由「检查更新」手动触发。
 * 弹窗显隐与更新内容由本 store 驱动，CheckUpdateDialog 只负责渲染。
 * */
const useAppVersionStore = defineStore('appVersion', () => {
  /** 更新弹窗显隐 */
  const visible = ref(false)
  /** 后端返回的更新内容 */
  const updateInfo = ref<AppUpdateInfo | null>(null)

  /**
   * 检查更新
   * @param silent 静默检查（启动时）：失败与「稍后再说」都不弹窗
   * @returns 命中更新时返回更新内容，否则返回 null
   * */
  async function checkUpdate(silent = true): Promise<AppUpdateInfo | null> {
    let info: AppUpdateInfo | null = null
    try {
      const res = await checkAppUpdate({
        appKey: APP_KEY,
        platform: detectPlatform(),
        versionCode: __APP_VERSION_CODE__,
      })
      if (res.code === 200 && res.data?.hasUpdate) {
        info = res.data
      }
    } catch {
      // 静默失败：网络异常或后端不可用时不打扰用户
      return null
    }
    if (!info) {
      return null
    }
    // 可选更新且本次会话已选择「稍后再说」：不再自动弹出
    if (silent && !info.forceUpdate && sessionStorage.getItem(SKIP_VERSION_KEY) === String(info.latestVersionCode)) {
      return null
    }
    updateInfo.value = info
    visible.value = true
    return info
  }

  /** 稍后再说：本次会话内不再自动提示 */
  function skipUpdate(): void {
    if (updateInfo.value?.latestVersionCode) {
      sessionStorage.setItem(SKIP_VERSION_KEY, String(updateInfo.value.latestVersionCode))
    }
    visible.value = false
  }

  /** 立即更新：网页端刷新取新资源，桌面端唤起系统浏览器下载安装包 */
  async function applyUpdate(): Promise<void> {
    const url = updateInfo.value?.downloadUrl
    if (detectPlatform() === 'web') {
      window.location.reload()
      return
    }
    if (!url) {
      return
    }
    await openExternal(url)
    visible.value = false
  }

  function close(): void {
    visible.value = false
  }

  return {
    visible,
    updateInfo,
    checkUpdate,
    skipUpdate,
    applyUpdate,
    close,
  }
})

export default useAppVersionStore
