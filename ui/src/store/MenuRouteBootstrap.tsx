import { useEffect, useLayoutEffect } from 'react'
import { getRouters } from '@/api/menu'
import { useAuthStore } from '@/store/authStore'
import { useMenuRouteStore } from '@/store/menuRouteStore'

/**
 * 在 token 变化时拉取 /getRouters，写入 menuRouteStore（替代原 DynamicRoutesProvider）
 */
export function MenuRouteBootstrap({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token)
  const setFromData = useMenuRouteStore((s) => s.setFromData)
  const setLoading = useMenuRouteStore((s) => s.setLoading)
  const reset = useMenuRouteStore((s) => s.reset)

  useLayoutEffect(() => {
    if (token) setLoading(true)
    else setLoading(false)
  }, [token, setLoading])

  useEffect(() => {
    if (!token) {
      reset()
      return
    }
    let cancelled = false
    ;(async () => {
      try {
        const data = await getRouters()
        if (cancelled) return
        setFromData(data)
      } catch {
        if (!cancelled) reset()
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [token, reset, setFromData, setLoading])

  return <>{children}</>
}
