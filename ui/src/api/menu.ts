import type { RouterMenu } from '@/types/menu'
import request, { type RuoYiResponse } from '@/utils/request'

const commonApi = '/system/menu'

/**
 * 获取菜单列表
 * */
export const menuList = (data: { menuName: string; state: string }) :Promise<never> => {
  return request.post(`${commonApi}/list`, data)
}

/** 若依：获取动态路由树 */
export async function getRouters(): Promise<RouterMenu[]> {
  const res = await request.get<RuoYiResponse<RouterMenu[]>>('/auth/getRouters')
  const body = res.data as RuoYiResponse<RouterMenu[]>
  const list = body.data
  return Array.isArray(list) ? list : []
}
