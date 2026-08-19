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
