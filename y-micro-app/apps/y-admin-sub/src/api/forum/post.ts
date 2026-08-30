import request from '@/utils/request'

/**
 * 获取帖子列表
 * */
export const getPosts = (data :any) :Promise<PageResult<ForumPost[]>> => {
    return request({
        url: '/admin/forum/posts',
        method: 'POST',
        data
    })
}

/**
 * 审核帖子
 * */
export const auditPost = (postId :number, status :number) :Promise<R<string>> => {
    return request({
        url: `/admin/forum/posts/${postId}/audit/${status}`,
        method: 'POST'
    })
}

/**
 * 置顶帖子
 * */
export const topPost = (postId :number) :Promise<R<string>> => {
    return request({
        url: `/admin/forum/posts/${postId}/top`,
        method: 'POST'
    })
}

/**
 * 加精帖子
 * */
export const essencePost = (postId :number) :Promise<R<string>> => {
    return request({
        url: `/admin/forum/posts/${postId}/essence`,
        method: 'POST'
    })
}