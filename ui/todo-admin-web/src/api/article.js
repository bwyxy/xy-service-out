import request from '@/utils/request'


/** 分页获取文章列表 */
export async function getArticleList(
  params
) {
  const res = await request.post(
    '/article/list',
    params,
  )
  return res.data
}

/** 查询文章信息 */
export async function getArticleById(
  id
) {
  const res = await request.get(
    `/article/${id}`,
  )
  return res.data
}

/** 添加文章 */
export async function addArticle(
  data,
) {
  const res = await request.post(
    '/article/add',
    data,
  )
  return res.data
}

/** 更新文章 */
export async function updateArticle(
  data
) {
  const res = await request.put(
    '/article/update',
    data,
  )
  return res.data
}
