import request from '@/utils/request'

export const getBoards = (data :any) :Promise<R<ForumBoard[]>> => {
    return request({
        url: '/api/forum/boards',
        method: 'post',
        data
    })
}

export const getPosts = (data :any) :Promise<R<PageResult<ForumPost[]>>> => {
    return request({
        url: '/forum/posts',
        method: 'post',
        data
    })
}

export const setPostLike = (postId :number) :Promise<R<boolean>> => {
    return request({
        url: `/forum/posts/${postId}/like`,
        method: 'post'
    })
}

export const hasPostLike = (postId :number) :Promise<R<boolean>> => {
    return request({
        url: `/forum/posts/${postId}/like`,
        method: 'get'
    })
}