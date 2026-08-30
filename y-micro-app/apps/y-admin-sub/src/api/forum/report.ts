import request from "@/utils/request.ts";

/**
 * 获取举报列表
 * */
export const getReports = (data :any) :Promise<PageResult<ForumReport[]>> => {
    return request({
        url: '/admin/forum/reports',
        method: 'post',
        data
    })
}

/**
 * 处理举报
 * */
export const handleReports = (data :any) :Promise<R<string>> => {
    return request({
        url: '/admin/forum/reports/handle',
        method: 'post',
        data
    })
}