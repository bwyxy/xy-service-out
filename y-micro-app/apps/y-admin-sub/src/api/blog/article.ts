import request from "@/utils/request.ts";

/** 查询文章列表 */
export const getArticles = (data :any) :Promise<R<PageResult<Article[]>>> => {
    return request({
        url: '/article/list',
        method: 'post',
        data
    })
}

/** 查询文章信息 */
export const getArticleInfo = (articleId :number) :Promise<R<Article>> => {
    return request({
        url: `/article/${articleId}`,
        method: 'get'
    })
}

/** 添加文章 */
export const addArticle = (data :any) :Promise<R<any>> => {
    return request({
        url: '/article/add',
        method: 'post',
        data
    })
}

/** 更新文章 */
export const updateArticle = (data :any) :Promise<R<any>> => {
    return request({
        url: '/article/update',
        method: 'put',
        data
    })
}