import request from '@/utils/request'

// 分页查询 AI 模型提供者列表
export function pageModelProvider(query?: any) {
  return request({
    url: '/admin/ai/model/page',
    method: 'get',
    params: query
  })
}

// 查询 AI 模型提供者详情
export function getModelProvider(id: number | string) {
  return request({
    url: '/admin/ai/model/' + id,
    method: 'get'
  })
}

// 新增 AI 模型提供者
export function addModelProvider(data: any) {
  return request({
    url: '/admin/ai/model',
    method: 'post',
    data: data
  })
}

// 编辑 AI 模型提供者
export function updateModelProvider(data: any) {
  return request({
    url: '/admin/ai/model',
    method: 'put',
    data: data
  })
}

// 删除 AI 模型提供者
export function delModelProvider(id: number | string) {
  return request({
    url: '/admin/ai/model/' + id,
    method: 'delete'
  })
}

// 切换 AI 模型提供者启用状态
export function toggleModelProvider(id: number | string) {
  return request({
    url: '/admin/ai/model/' + id + '/toggle',
    method: 'put'
  })
}
