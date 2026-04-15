import type { RouterMenu } from '@/types/menu'
import request, { type RuoYiResponse } from '@/utils/request'

/** 若依：获取动态路由树 */
export async function getRouters(): Promise<RouterMenu[]> {
  const res = await request.get<RuoYiResponse<RouterMenu[]>>('/getRouters')
  const body = res.data as RuoYiResponse<RouterMenu[]>
  const list = body.data
  return Array.isArray(list) ? list : []
}
