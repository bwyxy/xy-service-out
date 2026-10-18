import request from '@/utils/request';

/**
 * 创建群聊
 * */
export const createGroup = (data :any) => {
    return request({
        url: '/group/create',
        method: 'post',
        data
    })
}

/**
 * 查询群聊信息
 * */
export const findGroupInfo = (id :string) => {
    return request({
        url: `/group/${id}`,
        method: 'get'
    })
}

/**
 * 查询群成员
 * */
export const findGroupMembers = (id :string) => {
    return request({
        url: `/group/${id}/members`,
        method: 'get'
    })
}