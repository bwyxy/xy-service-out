import { Spin } from 'antd'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from '@/store/authStore'
import { useDynamicRoutes } from '@/store/menuRouteStore'
import BasicLayout from '@/layouts/BasicLayout'
import Login from '@/pages/Login'
import Home from '@/pages/Home'
import NotFound from '@/pages/NotFound'
import { AuthGuard, GuestGuard } from '@/router/RouteGuard'
// import { AnimatedRoutes } from '@/router/AnimatedRoutes'

export function AppRoutes() {
  const { token } = useAuth()
  const { dynamicRoutes, loading } = useDynamicRoutes()

  if (token && loading) {
    return (
      <div
        style={{
          width: '100%',
          height: '100%',
          minHeight: 0,
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Spin size="large" description="载入路由中..." />
      </div>
    )
  }

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        minHeight: 0,
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
        <Routes>
            <Route
                path="/login"
                element={
                    <GuestGuard>
                        <Login />
                    </GuestGuard>
                }
            />
            <Route path="/404" element={<NotFound />} />
            <Route element={<AuthGuard />}>
                <Route path="/" element={<BasicLayout />}>
                    <Route index element={<Home />} />
                    {dynamicRoutes.map((r, i) => (
                        <Route
                            key={r.path ?? `route-${i}`}
                            path={r.path}
                            element={r.element}
                        />
                    ))}
                    <Route path="*" element={<Navigate to="/404" replace />} />
                </Route>
            </Route>
            <Route path="*" element={<Navigate to="/404" replace />} />
        </Routes>
    </div>
  )
}
