import http from '@/utils/request/index.ts'

export const tagList = (): Promise<R<any>> => {
  return http.post('/web/tag/list')
}

export const tagLevelList = () :Promise<R<any>> => {
  return http.post('/web/tag/level/list')
}

export const articleListByTag = (data: any): Promise<R<any>> => {
  return http.post('/web/article/list', data)
}