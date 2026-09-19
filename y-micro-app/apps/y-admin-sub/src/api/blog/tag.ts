import request from '@/utils/request'

/** 获取标签列表 */
export const getTagList = (data :any) :Promise<R<PageResult<Tag[]>>> => {
    return request({
        url: `/tag/list`,
        method: 'post',
        data
    })
}

/** 获取标签信息 */
export const getTagInfo = (id :number) :Promise<R<Tag>> => {
    return request({
        url: `/tag/${id}`,
        method: 'get'
    })
}

/** 添加标签 */
export const addTag = (data :any) :Promise<R<any>> => {
    return request({
        url: `/tag/add`,
        method: 'post',
        data
    })
}

/** 更新标签 */
export const editTag = (data :any) :Promise<R<any>> => {
    return request({
        url: `/tag/update`,
        method: 'put',
        data
    })
}

/** 删除标签 */
export const deleteTag = (id:number) :Promise<R<any>> => {
    return request({
        url: `/tag/${id}`,
        method: 'delete'
    })
}