import request from '@/utils/request'

/**
 * 获取论坛版块
 * */
export const getForumBoards = (data :any) :Promise<PageResult<ForumBoard>> => {
    return request({
        url: '/admin/forum/boards/list',
        method: 'post',
        data: data
    })
}

/**
 * 添加论坛版块
 * */
export const addForumBoard = (data :ForumBoard) :Promise<R<string>> => {
    return request({
        url: '/admin/forum/boards',
        method: 'post',
        data: data
    })
}

/**
 * 更新论坛版块
 * */
export const updateForumBoard = (data :ForumBoard) :Promise<R<string>> => {
    return request({
        url: '/admin/forum/boards',
        method: 'put',
        data: data
    })
}

/**
 * 删除论坛版块
 * */
export const delForumBoard = (id:number) :Promise<R<string>> => {
    return request({
        url: `/admin/forum/boards/${id}`,
        method: 'delete'
    })
}