import type { RouterMenu } from '@/types/menu'
import request from '@/utils/request'

const commonApi = '/system/menu'

/**
 * 获取菜单列表
 * */
export const menuList = async (data: { menuName: string; state: string }) :Promise<any> => {
  const response :any = await request.post(`${commonApi}/list`, data)

  if (response.code && response.code === 200) {
    return response.data
  }
  return []
}

/** 若依：获取动态路由树 */
export async function getRouters(): Promise<RouterMenu[]> {
  const res = await request.get<RouterMenu>('/auth/getRouters')

  const list = res.data
  return Array.isArray(list) ? list : []
}
