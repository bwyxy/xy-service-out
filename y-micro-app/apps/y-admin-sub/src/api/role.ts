import request from '@/utils/request'

// 查询角色列表
export function listRole(query?: any) {
  return request({
    url: '/system/role/list',
    method: 'post',
    data: query
  })
}

// 根据角色ID查询详情
export function getRole(roleId: number | string) {
  return request({
    url: '/system/role/' + roleId,
    method: 'get'
  })
}

// 新增角色
export function addRole(data: any) {
  return request({
    url: '/system/role',
    method: 'post',
    data: data
  })
}

// 修改角色
export function updateRole(data: any) {
  return request({
    url: '/system/role',
    method: 'put',
    data: data
  })
}

// 删除角色
export function delRole(roleId: number | string) {
  return request({
    url: '/system/role/' + roleId,
    method: 'delete'
  })
}
