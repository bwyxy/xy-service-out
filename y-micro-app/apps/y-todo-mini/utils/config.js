/**
 * 全局配置
 */

/**
 * 接口根地址
 *
 * 后端为 customer-api/xy-app（端口 9100，无 context-path）。
 * 小程序端无法使用 devServer 代理，必须使用可直连的绝对地址；
 * 本地联调时把 DEV_API_URL 改为本机 / 局域网地址，例如 http://192.168.1.10:9100/
 */
const DEV_API_URL = 'http://localhost:9100/'
/** 线上地址需与 Nginx 反代域名保持一致 */
const PROD_API_URL = 'https://api.bwyxy.cn/'

export const BASE_API_URL = process.env.NODE_ENV === 'production' ? PROD_API_URL : DEV_API_URL

/** 登录 token 在本地存储中的 key */
export const TOKEN_KEY = 'xy_todo_token'

/** 请求超时时间（毫秒） */
export const REQUEST_TIMEOUT = 15000

/**
 * 任务提醒订阅消息模板 Id
 *
 * 必须与后端 application.yml 的 wechat.subscribe.reminder-template-id 保持一致，
 * 否则上报授权时后端会因为模板不匹配直接拒绝。
 */
export const REMINDER_TEMPLATE_ID = 'BxqEHs6-5c-giwcoSe_S74k8FHRNo-tygxD0lelwrhw'
