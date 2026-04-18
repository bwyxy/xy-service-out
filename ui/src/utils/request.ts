import axios, { type AxiosResponse } from 'axios'
import { apiBaseUrl } from '@/config'
import { getToken, removeToken } from '@/utils/auth'

/** 若依常见响应包装 */
export interface RuoYiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

const service = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' },
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (res: AxiosResponse<RuoYiResponse>) => {
    const payload = res.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === 401) {
        removeToken()
        const redirect = encodeURIComponent(
          window.location.pathname + window.location.search,
        )
        window.location.replace(`/login?redirect=${redirect}`)
        return Promise.reject(new Error(payload.msg || '未授权'))
      }
      if (payload.code !== 200) {
        return Promise.reject(new Error(payload.msg || '请求失败'))
      }
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      const redirect = encodeURIComponent(
        window.location.pathname + window.location.search,
      )
      window.location.replace(`/login?redirect=${redirect}`)
    }
    return Promise.reject(error)
  },
)

export default service
