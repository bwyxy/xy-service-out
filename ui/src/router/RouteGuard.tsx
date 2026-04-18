import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/store/authStore'
import { isWhiteListPath } from '@/router/whiteList'

/**
 * 全局路由守卫：未登录且非白名单 → 导向登录并带上 redirect
 */
export function AuthGuard() {
  const { token } = useAuth()
  const location = useLocation()

  if (!token) {
    if (isWhiteListPath(location.pathname)) {
      return <Outlet />
    }
    const redirect = encodeURIComponent(
      `${location.pathname}${location.search}`,
    )
    return (
      <Navigate
        to={`/login?redirect=${redirect}`}
        replace
        state={{ from: location }}
      />
    )
  }

  return <Outlet />
}

/** 已登录时不应再进入登录页 */
export function GuestGuard({ children }: { children: ReactNode }) {
  const { token } = useAuth()
  if (token) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
