// import type { RuoYiResponse } from '@/utils/request'
import request from '@/utils/request'

export interface TagItem {
  tagId: number
  name: string
  color?: string
  level?: number
  slug?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export interface TagListQuery {
  pageNum: number
  pageSize: number
  name?: string
}

export interface TagListResponse {
  list: TagItem[]
  pageNum: number
    pageSize: number
    pages: number
  total: number
}

export interface TagCreate {
  name: string
  color?: string
  level?: number
  slug?: string
  description?: string
}

export interface TagUpdate {
  tagId: number
  name: string
  color?: string
  level?: number
  slug?: string
  description?: string
}

/** 分页获取标签列表 */
export async function getTagList(
  params: TagListQuery,
): Promise<TagListResponse> {
  const res :R<TagListResponse> = await request.post('/tag/list', params)
  return res.data
}

/** 获取标签信息 */
export async function getTagById(
  id: number | string,
): Promise<TagItem> {
  const res :R<TagItem> = await request.get(`/tag/${id}`)
  return res.data
}

/** 添加标签 */
export async function addTag(
  data: TagCreate,
): Promise<TagItem> {
  const res :R<TagItem> = await request.post('/tag/add', data)
  return res.data
}

/** 更新标签 */
export async function updateTag(
  data: TagUpdate,
): Promise<TagItem> {
  const res :R<TagItem> = await request.put(`/tag/update`, data)
  return res.data
}

/** 删除标签 */
export async function deleteTag(
  id: number | string,
): Promise<void> {
  await request.delete(`/tag/${id}`)
}
