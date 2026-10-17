/**
 * 全局配置
 */

/**
 * 接口根地址
 *
 * App 端无法使用 devServer 代理，必须使用可直连的绝对地址。
 * fg-chat 的 context-path 为 /，故本地直连地址不带 /chat-api 前缀。
 * 本地联调时把 DEV_API_URL 改为本机 / 局域网地址，例如 http://192.168.1.10:9080/
 */
// const DEV_API_URL = 'https://www.bwyxy.cn/chat-api/'
// const PROD_API_URL = 'https://www.bwyxy.cn/chat-api/'
const DEV_API_URL = 'http://localhost:9080/'
const PROD_API_URL = 'https://www.bwyxy.cn/chat-api/'

let baseApiUrl = process.env.NODE_ENV === 'production' ? PROD_API_URL : DEV_API_URL

// H5 端固定改为同源相对路径，交由开发代理 / nginx 反代转发：
// - 开发环境 vite.config.js 的 dev server 代理转发到后端，规避浏览器跨域
// - 生产环境 nginx 反代 /chat-api，前端与接口同源
// #ifdef H5
baseApiUrl = '/chat-api/'
// #endif

export const BASE_API_URL = baseApiUrl


/** 登录 token 在本地存储中的 key */
export const TOKEN_KEY = 'xy_chat_token'

/** IM 服务器地址在本地存储中的 key（取自登录返回的 imAddr.wss_addr） */
export const WSS_KEY = 'xy_wss_key'

/** 请求超时时间（毫秒） */
export const REQUEST_TIMEOUT = 15000
