import request from '@/utils/request'

export type UploadProgressCallback = (progressEvent: ProgressEvent) => void

/**
 * 上传单张图片
 * @param file 图片文件
 * @param onProgress 上传进度回调
 * */
export function uploadImage(file: File, onProgress?: UploadProgressCallback) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
  }) as Promise<any>
}

/**
 * 上传通用文件（安装包等）
 * @param file 文件
 * @param onProgress 上传进度回调
 * */
export function uploadFile(file: File, onProgress?: UploadProgressCallback) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/file',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
  }) as Promise<any>
}
