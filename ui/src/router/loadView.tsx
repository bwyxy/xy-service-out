import { lazy, type ComponentType } from 'react'

/**
 * 与若依 component 字符串对应：如 system/user/index -> views/system/user/index.tsx
 * 新增页面时在 src/views 下创建同路径即可被懒加载
 */
const viewModules = import.meta.glob<{ default: ComponentType<object> }>(
  '../views/**/*.tsx',
)

function normalizeComponentPath(component: string): string {
  return component.replace(/^\//, '').replace(/\.tsx$/i, '')
}

export function loadView(component: string): ComponentType<object> {
  const normalized = normalizeComponentPath(component)
  const key = `../views/${normalized}.tsx`
  const loader = viewModules[key]
  if (!loader) {
    const Missing = () => (
      <div style={{ padding: 24 }}>
        未找到视图模块：<code>{normalized}.tsx</code>
        <br />
        请在 <code>src/views/{normalized}.tsx</code> 创建对应文件。
      </div>
    )
    return Missing
  }
  return lazy(loader)
}
