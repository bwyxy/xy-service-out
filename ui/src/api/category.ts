import type { RuoYiResponse } from '@/utils/request'
import request from '@/utils/request'

export interface CategoryItem {
  categoryId: number
  name: string
  slug?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export interface CategoryListQuery {
  pageNum: number
  pageSize: number
  name?: string
}

export interface CategoryListResponse {
  list: CategoryItem[]
    pageNum: number
      pageSize: number
      pages: number
    total: number
}

export interface CategoryCreate {
  name: string
  slug?: string
  description?: string
}

export interface CategoryUpdate {
  categoryId: number
  name: string
  slug?: string
  description?: string
}

/** 分页获取分类列表 */
export async function getCategoryList(
  params: CategoryListQuery,
): Promise<CategoryListResponse> {
  const res :R<CategoryListResponse> = await request.post(
    '/category/list',
    params,
  )
  return res.data
}

/** 查询分类信息 */
export async function getCategoryById(
  id: number | string,
): Promise<CategoryItem> {
  const res :R<CategoryItem> = await request.get(`/category/${id}`)
  return res.data
}

/** 添加分类 */
export async function addCategory(
  data: CategoryCreate,
): Promise<CategoryItem> {
  const res :R<CategoryItem> = await request.post('/category/add', data)
  return res.data
}

/** 更新分类 */
export async function updateCategory(
  data: CategoryUpdate,
): Promise<CategoryItem> {
  const res :R<CategoryItem> = await request.put('/category/update', data)
  return res.data
}

/** 删除分类 */
export async function deleteCategory(id: number | string): Promise<void> {
  await request.delete(`/category/${id}`)
}
