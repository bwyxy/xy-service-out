import { defineStore } from 'pinia'
import { login, register, getUserInfo } from '@/api/auth'
import { setToken, setCookies, removeToken } from '@/utils/auth'
import { getDeviceInfo } from '@/utils/device'
import { WSS_KEY } from '@/utils/config'
import useChatStore from './chat'

/**
 * 用户 Store
 *
 * 迁移自网页端 store/user.ts，去除 Tauri / Element Plus 依赖：
 * - 桌面端分支改为 App 端通用逻辑
 * - ElMessage 提示由 utils/request.js 统一 toast 兜底
 */
const useUserStore = defineStore('user', {
  state() {
    return {
      /** 登录用户信息，含 uid / nickname / avatar */
      userInfo: {},
      /** 当前正在聊天的频道信息 { channelId, type } */
      currentChannel: null,
    }
  },

  actions: {
    /**
     * 用户登录
     *
     * 多端登录管控：额外上报设备类型与设备唯一标识。
     *
     * @param params 登录参数
     * @returns 登录成功时返回凭据信息，失败返回 null
     */
    async userLogin(params) {
      try {
        const result = await login({ ...params, ...getDeviceInfo() })

        if (result.code === 200) {
          const { token, imAddr } = result.data

          setToken(token)
          setCookies(WSS_KEY, imAddr.wss_addr)
          return { token, wssAddr: imAddr.wss_addr, wssKey: WSS_KEY }
        }

        return null
      } catch {
        return null
      }
    },

    /**
     * 获取并设置当前用户信息
     */
    async setUserInfo() {
      const result = await getUserInfo()

      if (result.code === 200) {
        this.userInfo = result.data
        return true
      }

      throw new Error('获取登录信息失败')
    },

    /**
     * 用户注册
     * @param params 注册参数
     */
    async userRegister(params) {
      try {
        const result = await register(params)
        return result.code === 200
      } catch {
        return false
      }
    },

    /**
     * 退出登录
     */
    outLogin() {
      const chatStore = useChatStore()

      chatStore.resetChatData()
      removeToken()
      this.userInfo = {}
      this.currentChannel = null
    },
  },
})

export default useUserStore
