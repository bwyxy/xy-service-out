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