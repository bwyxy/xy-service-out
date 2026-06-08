import request from "@/utils/request";

/**
 * 获取配置信息
 * */
export const configList = (data) => {
    return request({
        url: '/config/list',
        method: 'post',
        data
    })
}

/**
 * 获取配置信息
 * */
export const configInfo = (id) => {
    return request({
        url: `/config/${id}`,
        method: 'get'
    })
}

/**
 * 添加配置
 * */
export const addConfig = (data) => {
    return request({
        url: '/config/add',
        method: 'post',
        data
    })
}

/**
 * 更新配置
 * */
export const updateConfig = (data) => {
    return request({
        url: `/config/edit`,
        method: 'put',
        data
    })
}

export const deleteConfig = (id) => {
    return request({
        url: `/config/${id}`,
        method: 'delete'
    })
}