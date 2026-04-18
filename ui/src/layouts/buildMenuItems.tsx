import type { MenuProps } from 'antd'
import type { RouterMenu } from '@/types/menu'
import { joinMenuPath } from '@/utils/menuPath'

const LAYOUT = 'Layout'
const PARENT_VIEW = 'ParentView'
const INNER_LINK = 'InnerLink'

/** 侧边栏菜单（略过 hidden、外链、InnerLink） */
export function buildMenuItems(
  menus: RouterMenu[] | undefined,
  parentPath = '',
): MenuProps['items'] {
  if (!menus?.length) return []
  const items: NonNullable<MenuProps['items']> = []
  for (const m of menus) {
    if (m.hidden) continue
    if (m.path?.startsWith('http')) continue
    if (m.meta?.link?.startsWith('http')) continue

    const fullPath = joinMenuPath(parentPath, m.path ?? '')
    const comp = m.component
    const sub = buildMenuItems(m.children, fullPath)

    if (comp === INNER_LINK) continue

    /**
     * 第一层 path 为 `/`（根节点包装）时：不展示为带子菜单的分组，
     * 将子项平铺到当前层级，避免多一层折叠菜单。
     */
    const isFirstLayerRootPath =
      parentPath === '' && (m.path === '/' || fullPath === '/')
    if (
      isFirstLayerRootPath &&
      (comp === LAYOUT || comp === PARENT_VIEW) &&
      sub?.length
    ) {
      items.push(...sub)
      continue
    }

    if (comp === LAYOUT || comp === PARENT_VIEW) {
      if (sub?.length) {
        items.push({
          key: fullPath || m.name || 'node',
          label: m.meta?.title ?? m.name ?? fullPath,
          children: sub,
        })
      }
      continue
    }

    if (sub?.length) {
      items.push({
        key: fullPath || m.name || 'sub',
        label: m.meta?.title ?? m.name ?? fullPath,
        children: sub,
      })
    } else if (fullPath) {
      items.push({
        key: fullPath,
        label: m.meta?.title ?? m.name ?? fullPath,
      })
    }
  }
  return items
}
