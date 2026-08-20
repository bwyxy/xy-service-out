import request from '@/utils/request'

// 分页查询 AI API 密钥列表
export function pageApiKey(query?: any) {
  return request({
    url: '/ai/api-key/page',
    method: 'post',
    data: query
  })
}

// 生成 AI API 密钥对
export function generateApiKey(data: any) {
  return request({
    url: '/ai/api-key/generate',
    method: 'post',
    data: data
  })
}

// 启用 AI API 密钥
export function enableApiKey(id: number | string) {
  return request({
    url: '/ai/api-key/' + id + '/enable',
    method: 'put'
  })
}

// 禁用 AI API 密钥
export function disableApiKey(id: number | string) {
  return request({
    url: '/ai/api-key/' + id + '/disable',
    method: 'put'
  })
}

// 删除 AI API 密钥
export function delApiKey(id: number | string) {
  return request({
    url: '/ai/api-key/' + id,
    method: 'delete'
  })
}
