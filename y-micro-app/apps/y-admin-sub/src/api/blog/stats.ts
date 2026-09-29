import request from "@/utils/request.ts";

/** 访问概览 */
export const getStatsOverview = () :Promise<R<any>> => {
    return request({
        url: '/blog/stats/overview',
        method: 'get'
    })
}

/** 每日访问汇总 */
export const getStatsDaily = (params :any) :Promise<R<any[]>> => {
    return request({
        url: '/blog/stats/daily',
        method: 'get',
        params
    })
}

/** 热门页面排行 */
export const getStatsTopPages = (params :any) :Promise<R<any[]>> => {
    return request({
        url: '/blog/stats/top/pages',
        method: 'get',
        params
    })
}

/** 访问来源排行 */
export const getStatsTopSources = (params :any) :Promise<R<any[]>> => {
    return request({
        url: '/blog/stats/top/sources',
        method: 'get',
        params
    })
}

/** 设备分布 */
export const getStatsDevices = (params :any) :Promise<R<any[]>> => {
    return request({
        url: '/blog/stats/devices',
        method: 'get',
        params
    })
}
