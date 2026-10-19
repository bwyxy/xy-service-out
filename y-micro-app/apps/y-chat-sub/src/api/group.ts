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

/**
 * 编辑群资料（名称/头像/全员禁言/历史消息可见）
 * */
export const editGroup = (data :any) => {
    return request({
        url: '/group/edit',
        method: 'put',
        data
    })
}

/**
 * 修改当前用户在群内的昵称
 * */
export const updateGroupNickname = (data :any) => {
    return request({
        url: '/group/member/nickname',
        method: 'put',
        data
    })
}

/**
 * 群成员禁言/解禁
 * muteMinutes 为空或小于等于 0 表示解除禁言
 * */
export const muteMember = (data :any) => {
    return request({
        url: '/group/member/mute',
        method: 'put',
        data
    })
}

/**
 * 发布群公告
 * */
export const publishAnnouncement = (data :any) => {
    return request({
        url: '/group/announcement',
        method: 'post',
        data
    })
}

/**
 * 查询群公告列表
 * */
export const findAnnouncements = (groupId :string | number) => {
    return request({
        url: `/group/${groupId}/announcement`,
        method: 'get'
    })
}

/**
 * 删除群公告
 * */
export const deleteAnnouncement = (groupId :string | number, id :number) => {
    return request({
        url: `/group/${groupId}/announcement/${id}`,
        method: 'delete'
    })
}

/**
 * 解散群聊（仅群主可操作）
 * */
export const dissolveGroup = (groupId :string | number) => {
    return request({
        url: `/group/${groupId}`,
        method: 'delete'
    })
}