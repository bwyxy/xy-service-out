import request from '@/utils/request.ts'

export const getMenuList = () => {
    return request({
        url: "/blog/menu/list",
        method: "get"
    })
}

export const getMenuInfo = (menuId: number) => {
    return request({
        url: `/blog/menu/${menuId}`,
        method: "get"
    })
}

export const addMenu = (data :any) => {
    return request({
        url: `/blog/menu/add`,
        method: "post",
        data
    })
}

export const editMenu = (data :any) => {
    return request({
        url: 'blog/menu/edit',
        method: "put",
        data
    })
}

export const deleteMenu = (menuId :number) => {
    return request({
        url: `/blog/menu/${menuId}`,
        method: "delete"
    })
}