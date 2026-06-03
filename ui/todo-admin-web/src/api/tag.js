// import type { RuoYiResponse } from '@/utils/request'
import request from '@/utils/request'

/** 分页获取标签列表 */
export async function getTagList(
  params
) {
  const res = await request.post('/tag/list', params)
  return res.data
}

/** 获取标签信息 */
export async function getTagById(
  id
) {
  const res = await request.get(`/tag/${id}`)
  return res.data
}

/** 添加标签 */
export async function addTag(
  data
) {
  const res = await request.post('/tag/add', data)
  return res.data
}

/** 更新标签 */
export async function updateTag(
  data
) {
  const res = await request.put(`/tag/update`, data)
  return res.data
}

/** 删除标签 */
export async function deleteTag(
  id
) {
  await request.delete(`/tag/${id}`)
}
