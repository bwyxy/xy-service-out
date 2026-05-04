import request from '@/utils/request'

export interface BlogConfigQuery {
  configKey?: string
  id?: number | string
}

export interface BlogConfigItem {
  id: number
  configKey: string
  configValue?: string
  description?: string
}

export interface BlogConfigCreate {
  configKey: string
  configValue?: string
  description?: string
}

export interface BlogConfigUpdate {
  id: number | string
  configKey: string
  configValue?: string
  description?: string
}

/** 博客配置列表查询 */
export async function getBlogConfigList(
  params: BlogConfigQuery,
): Promise<BlogConfigItem[]> {
  const res :R<BlogConfigItem[]> = await request.post('/blog/list', params)

  console.log('获取博客配置列表响应:', res)
  return res.data
}

/** 获取博客配置信息 */
export async function getBlogConfigById(
  id: number | string,
): Promise<BlogConfigItem> {
  const res :R<BlogConfigItem> = await request.get(
    `/blog/${id}`,
  )

  return res.data
}

/** 新增博客配置 */
export async function addBlogConfig(
  data: BlogConfigCreate,
): Promise<BlogConfigItem> {
  const res :R<BlogConfigItem> = await request.post(
    '/blog/add',
    data,
  )
  return res.data
}

/** 更新博客配置 */
export async function updateBlogConfig(
  data: BlogConfigUpdate,
): Promise<BlogConfigItem> {
  const res :R<BlogConfigItem> = await request.put(
    '/blog/update',
    data,
  )
  return res.data
}
