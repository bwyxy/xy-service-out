import request from "@/utils/request.ts";

/** 获取照片列表 */
export const getPhotos = (data :any) :Promise<R<PageResult<any[]>>> => {
    return request({
        url: '/blog/photo/list',
        method: 'post',
        data
    })
}

/** 查询照片信息 */
export const getPhotoById = (id:number) :Promise<R<any>> => {
    return request({
        url: `/blog/photo/${id}`,
        method: 'get'
    })
}

/** 添加照片 */
export const addPhoto = (data :any) :Promise<R<any>> => {
    return request({
        url: '/blog/photo/add',
        method: 'post',
        data
    })
}

/** 更新照片 */
export const updatePhoto = (data :any) :Promise<R<any>> => {
    return request({
        url: '/blog/photo/edit',
        method: 'put',
        data
    })
}

/** 删除照片 */
export const deletePhoto = (id:number) :Promise<R<any>> => {
    return request({
        url: `/blog/photo/${id}`,
        method: 'delete'
    })
}
