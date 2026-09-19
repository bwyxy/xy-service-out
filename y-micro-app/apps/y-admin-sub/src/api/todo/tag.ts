import request from '@/utils/request'

// 查询任务标签列表
export function listTag(query?: any) {
  return request({
    url: '/todo/tag/list',
    method: 'post',
    data: query
  })
}

// 根据标签ID查询详情
export function getTag(tagId: number | string) {
  return request({
    url: '/todo/tag/' + tagId,
    method: 'get'
  })
}

// 新增任务标签
export function addTag(data: any) {
  return request({
    url: '/todo/tag',
    method: 'post',
    data: data
  })
}

// 修改任务标签
export function updateTag(data: any) {
  return request({
    url: '/todo/tag',
    method: 'put',
    data: data
  })
}

// 删除任务标签
export function delTag(tagId: number | string) {
  return request({
    url: '/todo/tag/' + tagId,
    method: 'delete'
  })
}
