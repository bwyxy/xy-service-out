import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { HashRouter } from 'react-router-dom'
import { AppRoutes } from '@/router/AppRoutes'
import { MenuRouteBootstrap } from '@/store/MenuRouteBootstrap'

function App() {
  return (
    <div className="app-root">
      <ConfigProvider
        locale={zhCN}
        theme={{ token:
                {
                    colorPrimary: '#52c41a',
                    borderRadius: 2,
                    colorBgContainer: '#fff'
                }
          }}
      >
        <div className="app-shell">
          <MenuRouteBootstrap>
            <HashRouter>
              <AppRoutes />
            </HashRouter>
          </MenuRouteBootstrap>
        </div>
      </ConfigProvider>
    </div>
  )
}

export default App
