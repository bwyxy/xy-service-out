import { REGEX } from "./patterns.js";

/**
 * 监测是否中国大陆手机号
 * @param {string} input 值
 * @return {boolean}
 * */
export function isCnMobile(input: string): boolean {
  return REGEX.cnMobile.test(input);
}
/**
 * 监测邮箱
 * @param {string} input 值
 * @return {boolean}
 * */
export function isEmail(input: string): boolean {
  return REGEX.email.test(input);
}
/**
 * 监测是否中国大陆身份证
 * @param {string} input 值
 * @return {boolean}
 * */
export function isCnId18(input: string): boolean {
  return REGEX.cnId18.test(input);
}
/**
 * 监测是否ipv4
 * @param {string} input 值
 * @return {boolean}
 * */
export function isIpv4(input: string): boolean {
  return REGEX.ipv4.test(input);
}
/**
 * 监测是否链接
 * @param {string} input 值
 * @return {boolean}
 * */
export function isHttpUrl(input: string): boolean {
  return REGEX.httpUrl.test(input);
}
/**
 * 6位数验证码
 * @param {string} input 值
 * @return {boolean}
 * */
export function isOtp6(input: string): boolean {
  return REGEX.otp6.test(input);
}
/**
 * 是否全为中文
 * @param {string} input 值
 * @return {boolean}
 * */
export function isChinese(input :string) :boolean {
  return  REGEX.chinese.test(input);
}
/**
 * 是否为发票抬头
 * @param {string} input 值
 * @return {boolean}
 * */
export function isLookUp(input :string) :boolean {
  return REGEX.lookUp.test(input);
}
/**
 * 是否为发票税号
 * @param {string} input 值
 * @return {boolean}
 * */
export function isDutyParagraph(input :string) :boolean {
  return REGEX.dutyParagraph.test(input);
}

