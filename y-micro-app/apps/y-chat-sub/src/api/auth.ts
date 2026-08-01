import request from '@/utils/request.ts'

/**
 * 登录
 * */
export const login = (data :any)=> {
    return request({
        url: '/chat/login',
        method: 'POST',
        data
    })
}

/**
 * 注册
 * */
export const register = (data :any)=> {
    return request({
        url: '/auth/register',
        method: 'POST',
        data
    })
}

/**
 * 发送邮箱验证码
 * */
export const sendEmailCode = (email :string)=> {
    return request.post(`/send/code?email=${email}`)
}

/**
 * 获取用户信息
 * */
export const getUserInfo = () => {
    return request.post('/auth/userinfo')
}