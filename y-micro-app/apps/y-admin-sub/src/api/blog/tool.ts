import request from "@/utils/request.ts";

/** 获取工具列表 */
export const getTools = (data :any) :Promise<R<PageResult<any[]>>> => {
    return request({
        url: '/blog/tool/list',
        method: 'post',
        data
    })
}

/** 查询工具信息 */
export const getToolById = (id:number) :Promise<R<any>> => {
    return request({
        url: `/blog/tool/${id}`,
        method: 'get'
    })
}

/** 添加工具 */
export const addTool = (data :any) :Promise<R<any>> => {
    return request({
        url: '/blog/tool/add',
        method: 'post',
        data
    })
}

/** 更新工具 */
export const updateTool = (data :any) :Promise<R<any>> => {
    return request({
        url: '/blog/tool/update',
        method: 'put',
        data
    })
}

/** 删除工具 */
export const deleteTool = (id:number) :Promise<R<any>> => {
    return request({
        url: `/blog/tool/${id}`,
        method: 'delete'
    })
}
