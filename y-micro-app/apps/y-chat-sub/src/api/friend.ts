import request from '@/utils/request.ts'

/**
 * 搜索用户
 * */
export const searchUsers = (keyword: string) => {
    return request({
        url: '/friend/search',
        method: 'GET',
        params: { keyword }
    })
}

/**
 * 发送好友请求
 * */
export const sendFriendRequest = (data: { toUid: string | number; applyMsg?: string }) => {
    return request({
        url: '/friend/request/send',
        method: 'POST',
        data
    })
}

/**
 * 处理好友请求
 * */
export const handleFriendRequest = (data: { requestId: string | number; action: number; replyMsg?: string }) => {
    return request({
        url: '/friend/request/handle',
        method: 'POST',
        data
    })
}

/**
 * 收到的好友请求
 * */
export const getReceivedRequests = (status?: number) => {
    return request({
        url: '/friend/request/received',
        method: 'GET',
        params: { status }
    })
}

/**
 * 发送的好友请求
 * */
export const getSentRequests = (status?: number) => {
    return request({
        url: '/friend/request/sent',
        method: 'GET',
        params: { status }
    })
}

/**
 * 好友列表
 * */
export const getFriendList = () => {
    return request({
        url: '/friend/list',
        method: 'GET'
    })
}
