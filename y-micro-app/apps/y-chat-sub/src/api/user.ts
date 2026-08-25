import request from '@/utils/request.ts'

/**
 * 根据用户 uId 查询用户信息
 * */
export const getUserInfo = (uId: number) => {
    return request({
        url: `/user/${uId}`,
        method: 'GET'
    })
}

/**
 * 根据系统标识 查询系统用户信息
 * */
export const getSystemUserInfo = (systemKey : string) => {
    return request({
        url: `/user/system/${systemKey}`,
        method: 'GET'
    })
}