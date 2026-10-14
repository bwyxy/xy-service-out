import { createAxios } from '@xy/utils'
import { getToken } from '@/utils/auth.ts'
import { ElMessage } from 'element-plus'

/**
 * 登录态失效错误码
 * - 401：未登录 / token 已过期
 * - 4011：会话被同类型设备顶下线或已登出（后端 ResultCode.SESSION_EXPIRED）
 */
const SESSION_EXPIRED_CODES = [401, 4011]

/**
 * 派发全局登录态失效事件
 *
 * 由 ForceLogoutDialog 统一消费：弹窗提示 + 清除本地登录态 + 跳转登录页，
 * 避免在请求层直接依赖 store / router 造成循环引用。
 */
function notifySessionExpired(msg?: string) {
    window.dispatchEvent(new CustomEvent('session-expired', { detail: { reason: msg } }))
}

const request = createAxios({
    config: {
        baseURL: import.meta.env.VITE_BASE_API_URL,
        timeout: 15000,
    },
    interceptors: {
        request: (config :any) => {

            config.headers["token"] = getToken() || ''

            return config
        },
        response: (response :any) => {

            const data = response.data

            if (SESSION_EXPIRED_CODES.includes(data.code)) {
                notifySessionExpired(data.msg)
                return data
            }

            if (data.code !== 200) {
                ElMessage.error(data.msg)
            }

            return data
        },
        responseError: (error :any) => {

            // 会话已失效时后端返回 HTTP 401 + 业务码 4011
            if (error?.response?.status === 401) {
                notifySessionExpired(error.response.data?.msg)
            }

            return Promise.reject(error)
        }
    }
})

export default request