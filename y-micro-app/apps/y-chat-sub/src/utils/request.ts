import { createAxios } from '@xy/utils'
import { getToken } from '@/utils/auth.ts'
import { ElMessage } from 'element-plus'

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

            if (data.code !== 200) {
                ElMessage.error(data.msg)
            }

            if (data.code === 401) {
                ElMessage.error('登录过期，请重新登录')
                window.location.href = '/'
            }

            return data
        }
    }
})

export default request