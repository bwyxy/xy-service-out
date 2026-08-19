import request from '@/utils/request'

// 查询操作日志列表
export function listOperLog(query?: any) {
  return request({
    url: '/system/operlog/list',
    method: 'post',
    data: query
  })
}

// 删除操作日志
export function delOperLog(operId: number | string) {
  return request({
    url: '/system/operlog/' + operId,
    method: 'delete'
  })
}

// 清空操作日志
export function cleanOperLog() {
  return request({
    url: '/system/operlog/clean',
    method: 'delete'
  })
}

// 导出操作日志
export function exportOperLog(query?: any) {
  return request({
    url: '/system/operlog/export',
    method: 'post',
    data: query,
    responseType: 'blob'
  })
}
