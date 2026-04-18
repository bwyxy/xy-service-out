import { create } from 'zustand'
import { getToken, removeToken, setToken as persistToken } from '@/utils/auth'
import { getUserInfo, type UserInfo } from '@/api/user'

export type AuthState = {
  token: string | null
  userInfo: UserInfo | null
  loading: boolean
  setToken: (t: string) => void
  setUserInfo: (userInfo: UserInfo | null) => void
  fetchUserInfo: () => Promise<UserInfo>
  logout: () => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: getToken(),
  userInfo: null,
  loading: false,
  setToken: (t) => {
    persistToken(t)
    set({ token: t })
  },
  setUserInfo: (userInfo) => set({ userInfo }),
  fetchUserInfo: async () => {
    set({ loading: true })
    try {
      const userInfo = await getUserInfo()
      set({ userInfo })
      return userInfo
    } finally {
      set({ loading: false })
    }
  },
  logout: () => {
    removeToken()
    set({ token: null, userInfo: null })
  },
  clearAuth: () => {
    removeToken()
    set({ token: null, userInfo: null })
  },
}))

if (typeof window !== 'undefined') {
  window.addEventListener('admin-token-cleared', () => {
    useAuthStore.setState({ token: null, userInfo: null })
  })
}

/** 与原先 useAuth() 用法一致 */
export function useAuth() {
  const token = useAuthStore((s) => s.token)
  const userInfo = useAuthStore((s) => s.userInfo)
  const loading = useAuthStore((s) => s.loading)
  const setToken = useAuthStore((s) => s.setToken)
  const setUserInfo = useAuthStore((s) => s.setUserInfo)
  const fetchUserInfo = useAuthStore((s) => s.fetchUserInfo)
  const logout = useAuthStore((s) => s.logout)
  return { token, userInfo, loading, setToken, setUserInfo, fetchUserInfo, logout }
}
