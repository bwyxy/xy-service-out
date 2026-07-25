import { createAxios } from '@xy/utils'
import { getToken } from '@/utils/auth.ts'

const request = createAxios({
    config: {
        baseUrl: import.meta.env.VITE_BASE_API_URL,
        timeout: 10000
    },
    interceptors: {
        request: (config :any) => {
            config.headers['Authorization'] = 'Bearer ' + getToken()

            return config
        },
        response: (response :any) => {
            const data = response.data

            if (data.code === 401) {

            }

            return data
        }
    }
})

export default request