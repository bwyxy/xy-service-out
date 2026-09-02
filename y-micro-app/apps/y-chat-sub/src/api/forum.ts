import request from '@/utils/request'

export const createPost = (data :any) :Promise<R<number>> => {
    return request({
        url: '/forum/posts/create',
        method: 'post',
        data
    })
}

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

/**
 * 获取个人帖子
 * */
export const getMePosts = (page :number, size :number) :Promise<R<PageResult<ForumPost[]>>> => {
    return request({
        url: '/forum/posts/me',
        method: 'get',
        params: {
            page,
            size
        }
    })
}

export const getStat = () :Promise<R<CreatorStat>> => {
    return request({
        url: '/forum/creator/stat',
        method: 'get'
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

export const getPostDetail = (postId :number) :Promise<R<ForumPost>> => {
    return request({
        url: `/forum/posts/${postId}`,
        method: 'get'
    })
}