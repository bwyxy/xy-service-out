import request from "@/utils/request.ts";

/** 获取友链列表 */
export const getFriendLinks = (data :any) :Promise<R<PageResult<any[]>>> => {
    return request({
        url: '/blog/friendLink/list',
        method: 'post',
        data
    })
}

/** 查询友链信息 */
export const getFriendLinkById = (id:number) :Promise<R<any>> => {
    return request({
        url: `/blog/friendLink/${id}`,
        method: 'get'
    })
}

/** 添加友链 */
export const addFriendLink = (data :any) :Promise<R<any>> => {
    return request({
        url: '/blog/friendLink/add',
        method: 'post',
        data
    })
}

/** 更新友链 */
export const updateFriendLink = (data :any) :Promise<R<any>> => {
    return request({
        url: '/blog/friendLink/update',
        method: 'put',
        data
    })
}

/** 删除友链 */
export const deleteFriendLink = (id:number) :Promise<R<any>> => {
    return request({
        url: `/blog/friendLink/${id}`,
        method: 'delete'
    })
}
