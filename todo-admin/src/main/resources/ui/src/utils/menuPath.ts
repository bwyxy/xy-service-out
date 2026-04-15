/** 拼接若依菜单 path（父 + 子相对 path） */
export function joinMenuPath(parent: string, child: string): string {
  if (!child) return parent
  if (child.startsWith('/')) return child
  const p = parent.replace(/\/$/, '')
  return p ? `${p}/${child}` : `/${child}`
}
