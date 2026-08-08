import { defineStore } from 'pinia'
import { login, register, getUserInfo } from '@/api/auth.ts'
import { ElMessage } from 'element-plus'
import { setToken, setCookies, removeToken } from '@/utils/auth.ts'
import useChatStore from "@/store/chat.ts";

/** 登录用户信息 */
export interface UserInfo {
  uid: string
  nickname: string
  avatar?: string
  [key: string]: unknown
}

/** 当前聊天频道 */
export interface CurrentChannel {
  channelId: string
  type: number
}

/** 用户 Store 状态 */
interface UserState {
  userInfo: UserInfo
  /** 当前正在聊天的频道信息 */
  currentChannel: CurrentChannel | null
}

const useUserStore = defineStore('user', {
  state(): UserState {
    return {
      userInfo: {} as UserInfo,
      currentChannel: null,
    }
  },

  actions: {
    /**
     * 用户登录
     * @param params 登录参数
     */
    async userLogin(params: Record<string, unknown>): Promise<boolean> {
      const wssKey = import.meta.env.VITE_WSS_KEY

      try {
        const result = await login(params)

        if (result.code === 200) {
          const { token, imAddr } = result.data

          setToken(token)
          setCookies(wssKey, imAddr.wss_addr)
          return true
        }

        return false
      } catch {
        ElMessage.error('登录失败')
        return false
      }
    },

    /**
     * 获取并设置当前用户信息
     */
    async setUserInfo(): Promise<boolean> {
      const result = await getUserInfo()

      console.log(result)
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
    async userRegister(params: Record<string, unknown>): Promise<boolean> {
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
    outLogin(): void {
      const chatStore = useChatStore()

      chatStore.resetChatData()
      removeToken()
      this.userInfo = {} as UserInfo
      this.currentChannel = null
    },
  },
})

export default useUserStore
