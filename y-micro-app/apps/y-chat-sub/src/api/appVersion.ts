import request from '@/utils/request.ts'

/** 检查更新结果 */
export interface AppUpdateInfo {
  hasUpdate: boolean
  latestVersion?: string
  latestVersionCode?: number
  forceUpdate: boolean
  downloadUrl?: string
  fileSize?: number
  changelog?: string
}

/**
 * 检查应用更新（免登录接口）
 * */
export const checkAppUpdate = (params: { appKey: string; platform: string; versionCode: number }) => {
  return request({
    url: '/app/version/check',
    method: 'get',
    params,
  }) as Promise<R<AppUpdateInfo>>
}
