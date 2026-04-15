import { Layout, Menu, theme } from 'antd'
import { Suspense, useMemo } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { buildMenuItems } from '@/layouts/buildMenuItems'
// import { useAuth } from '@/store/authStore'
import { useDynamicRoutes } from '@/store/menuRouteStore'
import { AnimatedRoutes } from '@/router/AnimatedRoutes'
import BaseUser from "@/layouts/BaseUser.tsx";
import BaseLogo from "@/layouts/BaseLogo.tsx";
import BaseTags from "@/layouts/BaseTags.tsx";
import BaseOption from "@/layouts/BaseOption.tsx";

const { Header, Sider, Content } = Layout

export default function BasicLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  // const { userInfo } = useAuth()
  const { menus } = useDynamicRoutes()
  const { token } = theme.useToken()
  const { colorBgContainer, borderRadiusLG } = token

  const menuItems = useMemo(() => buildMenuItems(menus), [menus])

  const selectedKeys = useMemo(() => {
    const p = location.pathname
    return [p === '/' ? '/' : p]
  }, [location.pathname])

  return (
    <Layout
      style={{
        width: '100%',
        height: '100%',
        minHeight: 0,
        flex: 1,
        display: 'flex',
        flexDirection: 'row',
      }}
    >
      <Sider
        breakpoint="lg"
        collapsedWidth={0}
        width={260}
        style={{ height: '100%', overflow: 'auto' }}
      >
         <BaseLogo />
          <div className="menu-main">
              <Menu
                  mode="inline"
                  selectedKeys={selectedKeys}
                  items={menuItems}
                  onClick={({ key }) => {
                      navigate(key === '/' ? '/' : key)
                  }}
              />
          </div>
          <BaseUser />
      </Sider>
      <Layout
        style={{
          flex: 1,
          minHeight: 0,
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Header
          style={{
            flexShrink: 0,
            padding: '0 24px 0 8px',
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: 16,
          }}
        >
            <BaseTags />

            <BaseOption />
        </Header>
          <AnimatedRoutes>
              <Content
                  style={{
                      flex: 1,
                      minHeight: 0,
                      margin: 16,
                      display: 'flex',
                      flexDirection: 'column',
                      overflow: 'hidden',
                      background: '#F8FAFC'
                  }}
              >
                  <div
                      style={{
                          flex: 1,
                          minHeight: 0,
                          overflow: 'auto',
                          padding: 24,
                          background: colorBgContainer,
                          borderRadius: borderRadiusLG,
                      }}
                  >
                      <Suspense fallback={<div style={{ padding: 24 }}>页面加载中…</div>}>
                          <Outlet />
                      </Suspense>
                  </div>
              </Content>
          </AnimatedRoutes>
      </Layout>
    </Layout>
  )
}
