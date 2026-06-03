import request from '@/utils/request'

/** 分页获取分类列表 */
export async function getCategoryList(
  params
){
  const res = await request.post(
    '/category/list',
    params,
  )
  return res.data
}

/** 查询分类信息 */
export async function getCategoryById(
  id
) {
  const res = await request.get(`/category/${id}`)
  return res.data
}

/** 添加分类 */
export async function addCategory(
  data
) {
  const res = await request.post('/category/add', data)
  return res.data
}

/** 更新分类 */
export async function updateCategory(
  data
) {
  const res = await request.put('/category/update', data)
  return res.data
}

/** 删除分类 */
export async function deleteCategory(id) {
  await request.delete(`/category/${id}`)
}
