import request from "@/utils/request.ts";

/** 查询平台配置列表（凭证脱敏） */
export const getDispatchPlatforms = (): Promise<R<any[]>> => {
    return request({
        url: '/blog/dispatch/platform/list',
        method: 'get'
    })
}

/** 查询当前登录用户可用的启用平台 */
export const getUsableDispatchPlatforms = (): Promise<R<any[]>> => {
    return request({
        url: '/blog/dispatch/platform/usable',
        method: 'get'
    })
}

/** 查询系统支持的全部平台（适配器注册中心） */
export const getSupportedDispatchPlatforms = (): Promise<R<any[]>> => {
    return request({
        url: '/blog/dispatch/platform/supported',
        method: 'get'
    })
}

/** 查询可选角色 */
export const getDispatchRoles = (): Promise<R<any[]>> => {
    return request({
        url: '/blog/dispatch/platform/roles',
        method: 'get'
    })
}

/** 查询平台配置详情 */
export const getDispatchPlatformById = (id: number): Promise<R<any>> => {
    return request({
        url: `/blog/dispatch/platform/${id}`,
        method: 'get'
    })
}

/** 新增平台配置 */
export const addDispatchPlatform = (data: any): Promise<R<any>> => {
    return request({
        url: '/blog/dispatch/platform/add',
        method: 'post',
        data
    })
}

/** 更新平台配置 */
export const updateDispatchPlatform = (data: any): Promise<R<any>> => {
    return request({
        url: '/blog/dispatch/platform/update',
        method: 'put',
        data
    })
}

/** 删除平台配置 */
export const deleteDispatchPlatform = (id: number): Promise<R<any>> => {
    return request({
        url: `/blog/dispatch/platform/${id}`,
        method: 'delete'
    })
}

/** 平台连通性测试 */
export const testDispatchPlatform = (id: number): Promise<R<any>> => {
    return request({
        url: `/blog/dispatch/platform/test/${id}`,
        method: 'post'
    })
}

/** 提交分发（单篇/多篇、单平台/多平台） */
export const submitDispatch = (data: any): Promise<R<any>> => {
    return request({
        url: '/blog/dispatch/task/dispatch',
        method: 'post',
        data
    })
}

/** 分页查询分发任务 */
export const getDispatchTasks = (data: any): Promise<R<PageResult<any[]>>> => {
    return request({
        url: '/blog/dispatch/task/list',
        method: 'post',
        data
    })
}

/** 查询分发任务详情 */
export const getDispatchTaskById = (id: number): Promise<R<any>> => {
    return request({
        url: `/blog/dispatch/task/${id}`,
        method: 'get'
    })
}

/** 重试分发任务（单个或批量） */
export const retryDispatchTasks = (taskIds: number[]): Promise<R<any>> => {
    return request({
        url: '/blog/dispatch/task/retry',
        method: 'post',
        data: taskIds
    })
}

/** 删除分发任务（单个或批量） */
export const deleteDispatchTasks = (taskIds: number[]): Promise<R<any>> => {
    return request({
        url: '/blog/dispatch/task/remove',
        method: 'post',
        data: taskIds
    })
}

/** 分页查询分发运营数据 */
export const getDispatchStats = (data: any): Promise<R<PageResult<any[]>>> => {
    return request({
        url: '/blog/dispatch/stat/list',
        method: 'post',
        data
    })
}

/** 按平台聚合统计运营数据 */
export const getDispatchStatSummary = (platformCode?: string): Promise<R<any[]>> => {
    return request({
        url: '/blog/dispatch/stat/summary',
        method: 'get',
        params: { platformCode }
    })
}

/** 手动触发运营数据同步 */
export const syncDispatchStats = (): Promise<R<any>> => {
    return request({
        url: '/blog/dispatch/stat/sync',
        method: 'post'
    })
}
