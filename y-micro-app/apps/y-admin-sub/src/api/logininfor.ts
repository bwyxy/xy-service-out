import request from '@/utils/request'

// 查询登录日志列表
export function listLogininfor(query?: any) {
  return request({
    url: '/system/logininfor/list',
    method: 'post',
    data: query
  })
}

// 删除登录日志
export function delLogininfor(infoId: number | string) {
  return request({
    url: '/system/logininfor/' + infoId,
    method: 'delete'
  })
}

// 清空登录日志
export function cleanLogininfor() {
  return request({
    url: '/system/logininfor/clean',
    method: 'delete'
  })
}

// 导出登录日志
export function exportLogininfor(query?: any) {
  return request({
    url: '/system/logininfor/export',
    method: 'post',
    data: query,
    responseType: 'blob'
  })
}
