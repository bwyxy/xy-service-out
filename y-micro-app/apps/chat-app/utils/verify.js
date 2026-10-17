/**
 * 表单校验工具
 *
 * 网页端依赖 @xy/utils 的 isCnMobile / isEmail，App 端无法引入该包，此处自实现。
 */

/** 校验中国大陆手机号 */
export function isCnMobile(value) {
  return /^1[3-9]\d{9}$/.test(String(value || ''))
}

/** 校验邮箱 */
export function isEmail(value) {
  return /^[\w.%+-]+@[\w-]+(\.[\w-]+)+$/.test(String(value || ''))
}
