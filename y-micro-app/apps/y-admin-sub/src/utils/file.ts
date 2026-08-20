/**
 * 文件相关工具函数
 * */

/**
 * 判断文件是否为图片
 * */
export function isImage(file: File): boolean {
  return file.type.startsWith('image/')
}

/**
 * 校验文件大小
 * @param file 文件对象
 * @param maxSizeMB 最大限制（MB）
 * */
export function checkFileSize(file: File, maxSizeMB: number): boolean {
  return file.size <= maxSizeMB * 1024 * 1024
}

/**
 * 格式化文件大小
 * */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 校验图片文件是否合法
 * @returns 校验通过返回 null，否则返回错误信息
 * */
export function validateImage(file: File, maxSizeMB: number): string | null {
  if (!isImage(file)) {
    return '请上传图片文件'
  }
  if (!checkFileSize(file, maxSizeMB)) {
    return `图片大小不能超过 ${maxSizeMB}MB，当前大小为 ${formatFileSize(file.size)}`
  }
  return null
}
