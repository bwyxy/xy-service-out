import { isTauri, isMacOS } from '@/utils/tauri.ts'

/** 客户端运行平台标识，与后端 sys_app_version.platform 取值一致 */
export type AppPlatform = 'web' | 'windows' | 'macos'

/**
 * 检测当前运行平台
 *
 * 非 Tauri 环境一律视为 web（浏览器网页端），
 * Tauri 环境再按系统区分 windows / macos。
 * */
export function detectPlatform(): AppPlatform {
  if (!isTauri()) {
    return 'web'
  }
  return isMacOS() ? 'macos' : 'windows'
}
