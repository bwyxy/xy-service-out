import request from '@/utils/request'

/**
 * 上传图片到七牛云（后端转发）
 *
 * @param file 图片文件
 * @returns 图片访问 URL
 */
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  }) as Promise<{ code: number; data: string; msg: string }>
}

export const uploadAvatar = (formData: FormData) => {
  return request({
    url: '/upload/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    }
  })
}
