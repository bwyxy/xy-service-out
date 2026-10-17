import { uploadFile } from '@/utils/request'

/**
 * 上传图片到七牛云（后端转发）
 *
 * @param filePath uni.chooseImage 返回的本地临时文件路径
 * @returns { code, data: 图片访问 URL, msg }
 */
export function uploadImage(filePath) {
  return uploadFile({
    url: '/upload/image',
    filePath,
    name: 'file',
  })
}

/**
 * 更新用户头像
 *
 * @param filePath 本地临时文件路径
 */
export function uploadAvatar(filePath) {
  return uploadFile({
    url: '/upload/avatar',
    filePath,
    name: 'file',
  })
}
