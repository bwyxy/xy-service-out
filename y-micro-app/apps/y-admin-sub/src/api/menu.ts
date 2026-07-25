import request from '@/utils/request'

// 查询菜单列表
export function listMenu(query?: any) {
  return request({
    url: '/system/menu/list',
    method: 'post',
    data: query
  })
}

// 查询菜单详情
export function getMenu(menuId: number) {
  return request({
    url: '/system/menu/' + menuId,
    method: 'get'
  })
}

// 新增菜单
export function addMenu(data: any) {
  return request({
    url: '/system/menu',
    method: 'post',
    data
  })
}

// 修改菜单
export function updateMenu(data: any) {
  return request({
    url: '/system/menu',
    method: 'put',
    data
  })
}

// 删除菜单
export function delMenu(menuId: number) {
  return request({
    url: '/system/menu/' + menuId,
    method: 'delete'
  })
}