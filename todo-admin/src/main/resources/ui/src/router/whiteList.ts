/** 无需登录即可访问的路由（若依白名单概念） */
export const whiteList = new Set<string>(['/login', '/register', '/404'])

export function isWhiteListPath(pathname: string): boolean {
  return whiteList.has(pathname)
}
