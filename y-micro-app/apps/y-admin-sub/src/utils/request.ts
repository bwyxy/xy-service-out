// @ts-ignore
import { createAxios } from '@xy/utils'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth.ts'
import useUserStore from "@/store/modules/user.ts";

/** 登录态失效错误码 */
const SESSION_EXPIRED_CODE = 401

/** 登出请求自身也会返回 401，需要防止重复触发造成递归 */
let isClearingSession = false

/** 清理登录态并回到登录页 */
const handleSessionExpired = () => {
    if (isClearingSession) return
    isClearingSession = true

    const userStore = useUserStore()
    userStore.logout().finally(() => {
        window.location.href = '/'
    })
}

const request = createAxios({
    config: {
        baseURL: import.meta.env.VITE_BASE_API_URL,
        timeout: 10000
    },
    interceptors: {
        request: (config :any) => {
            if (getToken()) {
                config.headers['Authorization'] = 'Bearer ' + getToken()
            }

            return config
        },
        response: (response :any) => {
            const data = response.data

            if (data?.code === SESSION_EXPIRED_CODE) {
                handleSessionExpired()
            }

            return data
        },
        responseError: (error :any) => {
            // 会话已失效时后端可能返回 HTTP 401
            if (error?.response?.status === SESSION_EXPIRED_CODE) {
                handleSessionExpired()
                return Promise.reject(error)
            }

            // 网络异常 / 超时 / 5xx：此处必须继续 reject，否则调用方会拿到 undefined 误判为成功
            const status = error?.response?.status
            ElMessage.error(status ? `请求失败（${status}）` : '网络异常，请稍后重试')

            return Promise.reject(error)
        }
    }
})

export default request
