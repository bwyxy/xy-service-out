import http from '@/utils/request/index.ts'

export const getBlogConfig = () :Promise<R<any>> => {
    return http.post('/web/blog/config')
}

export const articleList = (data :any) :Promise<R<any>> => {
    return http.post('/web/article/list', data)
}

export const articleInfo = (slug :string) :Promise<R<any>> => {
    return http.get(`/web/article/info/${slug}`)
}