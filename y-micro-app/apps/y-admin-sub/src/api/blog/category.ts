import request from "@/utils/request.ts";

/** 获取分类列表 */
export const getCategorys = (data :any) :Promise<R<PageResult<Category[]>>> => {
    return request({
        url: '/category/list',
        method: 'post',
        data
    })
}

/** 查询分类信息 */
export const getCategoryById = (id:number) :Promise<R<Category>> => {
    return request({
        url: `/category/${id}`,
        method: 'get'
    })
}

/** 添加分类 */
export const addCategory = (data :any) :Promise<R<any>> => {
    return request({
        url: '/category/add',
        method: 'post',
        data
    })
}

/** 更新分类 */
export const updateCategory = (data :any) :Promise<R<any>> => {
    return request({
        url: '/category/update',
        method: 'put',
        data
    })
}

/** 删除分类 */
export const deleteCategory = (id:number) :Promise<R<any>> => {
    return request({
        url: `/category/${id}`,
        method: 'delete'
    })
}