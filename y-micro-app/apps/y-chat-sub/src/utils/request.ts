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
 * 后端统一响应体
 */
export interface ApiResult<T = unknown> {
    code: number
    msg?: string
    data: T
}

/**
 * 接口业务错误
 *
 * 错误提示已由请求层拦截器统一弹出，调用方只需处理后续流程，不要再重复提示。
 */
export class ApiBusinessError extends Error {}

/**
 * 解包后端统一响应体，业务失败时抛出 ApiBusinessError
 *
 * 供需要直接拿到 data 的调用方复用，避免各处手写 code 判断。
 */
export function unwrapData<T>(res: unknown): T {
    const result = res as ApiResult<T>

    if (!result || result.code !== 200) {
        throw new ApiBusinessError(result?.msg || '请求失败')
    }

    return result.data
}

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