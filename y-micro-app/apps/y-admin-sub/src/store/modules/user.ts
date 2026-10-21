import { defineStore } from 'pinia'
import { login as loginApi, getInfo as getInfoApi, logout as logoutApi } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import usePermissionStore from '@/store/modules/permission'
import useTagsViewStore from '@/store/modules/tagsView'
import router from '@/router'

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
      this.permissions = user.permissions || []
      return user
    },

    // 退出登录
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // ignore
      }

      // 清理动态路由，避免切换账号后仍能访问上一个账号的菜单
      const permissionStore = usePermissionStore()
      permissionStore.addRoutes.forEach((route: any) => {
        if (route.name && router.hasRoute(route.name)) {
          router.removeRoute(route.name)
        }
      })
      if (router.hasRoute('NotFound')) {
        router.removeRoute('NotFound')
      }
      permissionStore.$reset()
      useTagsViewStore().$reset()

      this.token = ''
      this.userInfo = {}
      this.roles = []
      this.permissions = []
      removeToken()
    }
  }
})

export default useUserStore