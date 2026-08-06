import request from '@/utils/request.ts'

/**
 * 根据用户 uId 查询用户信息
 * */
export const getUserInfo = (uId: string) => {
    return request({
        url: `/user/${uId}`,
        method: 'GET'
    })
}