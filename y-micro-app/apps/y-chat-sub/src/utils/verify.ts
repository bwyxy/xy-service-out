import { isCnMobile, isEmail } from '@xy/utils'

export const checkPhone = (_rule :any, value :any, callback :any) => {
    if (!value) {
        return callback(new Error('请输入手机号'))
    }
    if (!isCnMobile(value)) {
        callback(new Error('请输入正确的手机号'))
    }
    callback()
}

export const checkEmail = (_rule :any, value :any, callback :any) => {
    if (!value) {
        return callback(new Error('请输入邮箱'))
    }
    if (!isEmail(value)) {
        return callback(new Error('请输入正确的邮箱'))
    }
    callback()
}

export const checkConfirmPassword = (_rule :any, value :any, callback :any, password :string) => {
    if (!value) {
        return callback(new Error('请确认您的密码'))
    }
    if (password !== value) {
        return callback(new Error('两次密码不一致'))
    }
    callback()
}