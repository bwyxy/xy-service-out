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

/**
 * 登出（结束当前设备会话，token 立即失效）
 * */
export const logout = () => {
    return request.post('/auth/logout')
}

/**
 * 更新用户信息
 * */
export const updateUserInfo = (data:any):any => {
    return request({
        url: '/update/info',
        method: 'PUT',
        data
    })
}

/**
 * 修改密码
 * */
export const editPassword = (data:any):any => {
    return request({
        url: '/edit/password',
        method: 'POST',
        data
    })
}