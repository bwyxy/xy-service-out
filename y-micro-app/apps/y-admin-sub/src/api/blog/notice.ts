import request from "@/utils/request.ts";

/** 获取公告列表 */
export const getNotices = (data :any) :Promise<R<PageResult<any[]>>> => {
    return request({
        url: '/blog/notice/list',
        method: 'post',
        data
    })
}

/** 查询公告信息 */
export const getNoticeById = (id:number) :Promise<R<any>> => {
    return request({
        url: `/blog/notice/${id}`,
        method: 'get'
    })
}

/** 添加公告 */
export const addNotice = (data :any) :Promise<R<any>> => {
    return request({
        url: '/blog/notice/add',
        method: 'post',
        data
    })
}

/** 更新公告 */
export const updateNotice = (data :any) :Promise<R<any>> => {
    return request({
        url: '/blog/notice/update',
        method: 'put',
        data
    })
}

/** 删除公告 */
export const deleteNotice = (id:number) :Promise<R<any>> => {
    return request({
        url: `/blog/notice/${id}`,
        method: 'delete'
    })
}
