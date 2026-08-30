import request from '@/utils/request'

export const getBoards = (data :any) :Promise<R<ForumBoard[]>> => {
    return request({
        url: '/api/forum/boards',
        method: 'post',
        data
    })
}