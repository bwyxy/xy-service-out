import request from '@/utils/request'

// 查询应用版本列表
export function listAppVersion(query?: any) {
  return request({
    url: '/system/app-version/list',
    method: 'post',
    data: query
  })
}

// 查询应用版本详情
export function getAppVersion(id: number | string) {
  return request({
    url: '/system/app-version/' + id,
    method: 'get'
  })
}

// 新增应用版本
export function addAppVersion(data: any) {
  return request({
    url: '/system/app-version',
    method: 'post',
    data
  })
}

// 修改应用版本
export function updateAppVersion(data: any) {
  return request({
    url: '/system/app-version',
    method: 'put',
    data
  })
}

// 删除应用版本
export function delAppVersion(id: number | string) {
  return request({
    url: '/system/app-version/' + id,
    method: 'delete'
  })
}

// 上传并绑定安装包
export function uploadAppPackage(id: number | string, file: File, onProgress?: (e: ProgressEvent) => void) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `/system/app-version/${id}/package`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
  }) as Promise<any>
}
