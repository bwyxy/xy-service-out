import { defineStore } from 'pinia'
import { login as loginApi, getInfo as getInfoApi, logout as logoutApi } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'

const useUserStore = defineStore('user', {
  state() {
    return {
      token: getToken() || '',
      userInfo: {} as any,
      roles: [] as string[],
      permissions: [] as any
    }
  },
  actions: {
    // 登录
    async login(loginForm: { username: string; password: string }) {
      const res = await loginApi(loginForm)

      console.log(res)
      const data = res.data
      setToken(data.token)
      this.token = data.token
      return data
    },

    // 获取用户信息
    async getInfo() {
      const res = await getInfoApi()
      const user = res.data
      this.userInfo = user
      if (user.roles && user.roles.length > 0) {
        this.roles = user.roles
      } else {
        this.roles = ['admin']
      }
      return user
    },

    // 退出登录
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // ignore
      }
      this.token = ''
      this.userInfo = {}
      this.roles = []
      removeToken()
    }
  }
})

export default useUserStore