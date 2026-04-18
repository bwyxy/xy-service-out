import { create } from 'zustand'
import type { RouteObject } from 'react-router-dom'
import { getRouters } from '@/api/menu'
import { transformMenuToRoutes } from '@/router/transformMenuToRoutes'
import type { RouterMenu } from '@/types/menu'
import { getToken } from '@/utils/auth'

export type MenuRouteState = {
  menus: RouterMenu[]
  dynamicRoutes: RouteObject[]
  loading: boolean
  setFromData: (data: RouterMenu[]) => void
  setLoading: (loading: boolean) => void
  reset: () => void
  reload: () => Promise<void>
}

export const useMenuRouteStore = create<MenuRouteState>((set) => ({
  menus: [],
  dynamicRoutes: [],
  loading: !!getToken(),
  setFromData: (data) =>
    set({
      menus: data,
      dynamicRoutes: transformMenuToRoutes(data),
    }),
  setLoading: (loading) => set({ loading }),
  reset: () => set({ menus: [], dynamicRoutes: [], loading: false }),
  reload: async () => {
    if (!getToken()) return
    set({ loading: true })
    try {
      const data = await getRouters()
      set({
        menus: data,
        dynamicRoutes: transformMenuToRoutes(data),
      })
    } catch {
      set({ menus: [], dynamicRoutes: [] })
    } finally {
      set({ loading: false })
    }
  },
}))

/** 与原先 useDynamicRoutes() 用法一致 */
export function useDynamicRoutes() {
  const menus = useMenuRouteStore((s) => s.menus)
  const dynamicRoutes = useMenuRouteStore((s) => s.dynamicRoutes)
  const loading = useMenuRouteStore((s) => s.loading)
  const reload = useMenuRouteStore((s) => s.reload)
  return { menus, dynamicRoutes, loading, reload }
}
