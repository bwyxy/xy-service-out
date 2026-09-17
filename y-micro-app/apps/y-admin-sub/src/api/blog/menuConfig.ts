import request from '@/utils/request';

const api = '/blog/menu/config'

/** 获取菜单配置 */
export const getMenuConfigs = (menuId :number) => {
    return request({
        url: api + `/${menuId}`,
        method: 'GET',
    })
}

export const getMenuConfigById = (id :number) => {
    return request({
        url: api + `/info/${id}`,
        method: 'GET'
    })
}

/** 添加菜单配置 */
export const addMenuConfig = (data :any) => {
    return request({
        url: api,
        method: 'POST',
        data
    })
}

/** 更新菜单配置 */
export const updateMenuConfig = (data :any) => {
    return request({
        url: api,
        method: 'PUT',
        data
    })
}

export const deleteMenuConfig = (id:number) => {
    return request({
        url: api + `/${id}`,
        method: 'DELETE'
    })
}