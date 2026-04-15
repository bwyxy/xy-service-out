import type { RouteObject } from 'react-router-dom'
import { loadView } from '@/router/loadView'
import type { RouterMenu } from '@/types/menu'
import { joinMenuPath } from '@/utils/menuPath'

const LAYOUT = 'Layout'
const PARENT_VIEW = 'ParentView'
const INNER_LINK = 'InnerLink'

/** 挂在 path 为 `/` 的 Layout 下时，使用相对 path（不带前导 /） */
function toNestedPath(fullPath: string): string {
  if (!fullPath || fullPath === '/') return ''
  return fullPath.replace(/^\//, '')
}

function walk(menus: RouterMenu[], parentPath: string): RouteObject[] {
  const routes: RouteObject[] = []
  for (const item of menus) {
    if (item.path?.startsWith('http')) continue
    if (item.meta?.link?.startsWith('http')) continue

    const currentPath = joinMenuPath(parentPath, item.path ?? '')
    const comp = item.component

    if (comp === LAYOUT || comp === PARENT_VIEW) {
      routes.push(...walk(item.children ?? [], currentPath))
      continue
    }

    if (comp === INNER_LINK) continue

    if (item.children?.length) {
      routes.push(...walk(item.children, currentPath))
    }

    if (
      comp &&
      comp !== LAYOUT &&
      comp !== PARENT_VIEW &&
      comp !== INNER_LINK
    ) {
      const path = toNestedPath(currentPath)
      if (path === '' && currentPath !== '/') {
        continue
      }
      const Cmp = loadView(comp)
      routes.push({
        path,
        element: <Cmp />,
      })
    }
  }
  return routes
}

/** 将菜单转为挂在根 Layout 下的子路由 */
export function transformMenuToRoutes(menus: RouterMenu[]): RouteObject[] {
  return walk(menus, '')
}
