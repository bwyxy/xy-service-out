import request from '@/utils/request'

export interface ArticleItem {
  articleId: number
  title: string
  slug?: string
  summary?: string
  content?: string
  coverImage?: string
  status?: number
  viewCount?: number
  likeCount?: number
  commentCount?: number
  authorId?: number
  categoryId?: number
  publishedTime?: string
  createTime?: string
  updateTime?: string
}

export interface ArticleListQuery {
  pageNum: number
  pageSize: number
  title?: string
  status?: number
}

export interface ArticleListResponse {
  list: ArticleItem[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface ArticleCreate {
  title: string
  content: string
  slug?: string
  summary?: string
  coverImage?: string
  status?: number
  authorId?: number
  categoryId?: number
  publishedTime?: string
}

export interface ArticleUpdate extends ArticleCreate {
  articleId: number | string
}

/** 分页获取文章列表 */
export async function getArticleList(
  params: ArticleListQuery,
): Promise<ArticleListResponse> {
  const res :R<ArticleListResponse> = await request.post(
    '/article/list',
    params,
  )
  return res.data
}

/** 查询文章信息 */
export async function getArticleById(
  id: number | string,
): Promise<ArticleItem> {
  const res :R<ArticleItem> = await request.get(
    `/article/${id}`,
  )
  return res.data
}

/** 添加文章 */
export async function addArticle(
  data: ArticleCreate,
): Promise<ArticleItem> {
  const res :R<ArticleItem> = await request.post(
    '/article/add',
    data,
  )
  return res.data
}

/** 更新文章 */
export async function updateArticle(
  data: ArticleUpdate,
): Promise<ArticleItem> {
  const res :R<ArticleItem> = await request.put(
    '/article/update',
    data,
  )
  return res.data
}
