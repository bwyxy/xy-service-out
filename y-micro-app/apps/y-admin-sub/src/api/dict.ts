import request from '@/utils/request'

// ==================== 字典类型 ====================

// 查询字典类型列表
export function listDictType(query?: any) {
  return request({
    url: '/system/dict/list',
    method: 'post',
    data: query
  })
}

// 根据字典主键查询详情
export function getDictType(dictId: number | string) {
  return request({
    url: '/system/dict/' + dictId,
    method: 'get'
  })
}

// 新增字典类型
export function addDictType(data: any) {
  return request({
    url: '/system/dict',
    method: 'post',
    data: data
  })
}

// 修改字典类型
export function updateDictType(data: any) {
  return request({
    url: '/system/dict',
    method: 'put',
    data: data
  })
}

// 删除字典类型
export function delDictType(dictId: number | string) {
  return request({
    url: '/system/dict/' + dictId,
    method: 'delete'
  })
}

// ==================== 字典数据 ====================

// 查询字典数据列表
export function listDictData(query?: any) {
  return request({
    url: '/system/dict/data/list',
    method: 'post',
    data: query
  })
}

// 根据字典类型查询字典数据
export function listDictDataByType(dictType: string) {
  return request({
    url: '/system/dict/data/type/' + dictType,
    method: 'get'
  })
}

// 根据字典编码查询详情
export function getDictData(dictCode: number | string) {
  return request({
    url: '/system/dict/data/' + dictCode,
    method: 'get'
  })
}

// 新增字典数据
export function addDictData(data: any) {
  return request({
    url: '/system/dict/data',
    method: 'post',
    data: data
  })
}

// 修改字典数据
export function updateDictData(data: any) {
  return request({
    url: '/system/dict/data',
    method: 'put',
    data: data
  })
}

// 删除字典数据
export function delDictData(dictCode: number | string) {
  return request({
    url: '/system/dict/data/' + dictCode,
    method: 'delete'
  })
}

// 导出字典数据
export function exportDictData(query?: any) {
  return request({
    url: '/system/dict/data/export',
    method: 'post',
    data: query,
    responseType: 'blob'
  })
}

// 导入字典数据
export function importDictData(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/system/dict/data/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
