import request from '@/utils/request'

// ========== 知识库 ==========

// 分页查询知识库列表
export function pageKnowledgeBase(query?: any) {
  return request({
    url: '/admin/ai/knowledge/page',
    method: 'get',
    params: query
  })
}

// 查询知识库详情
export function getKnowledgeBase(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/' + id,
    method: 'get'
  })
}

// 新增知识库
export function addKnowledgeBase(data: any) {
  return request({
    url: '/admin/ai/knowledge',
    method: 'post',
    data: data
  })
}

// 编辑知识库
export function updateKnowledgeBase(data: any) {
  return request({
    url: '/admin/ai/knowledge',
    method: 'put',
    data: data
  })
}

// 删除知识库
export function delKnowledgeBase(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/' + id,
    method: 'delete'
  })
}

// 切换知识库启用状态
export function toggleKnowledgeBase(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/' + id + '/toggle',
    method: 'put'
  })
}

// ========== 文档 ==========

// 分页查询知识库文档
export function pageKnowledgeDocument(kbId: number | string, query?: any) {
  return request({
    url: '/admin/ai/knowledge/' + kbId + '/document/page',
    method: 'get',
    params: query
  })
}

// 查询文档详情
export function getKnowledgeDocument(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/document/' + id,
    method: 'get'
  })
}

// 新增文档
export function addKnowledgeDocument(data: any) {
  return request({
    url: '/admin/ai/knowledge/document',
    method: 'post',
    data: data
  })
}

// 编辑文档
export function updateKnowledgeDocument(data: any) {
  return request({
    url: '/admin/ai/knowledge/document',
    method: 'put',
    data: data
  })
}

// 删除文档
export function delKnowledgeDocument(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/document/' + id,
    method: 'delete'
  })
}

// 重新索引文档
export function reindexKnowledgeDocument(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/document/' + id + '/reindex',
    method: 'post'
  })
}

// ========== 版本 ==========

// 查询知识库版本列表
export function listKnowledgeBaseVersion(kbId: number | string) {
  return request({
    url: '/admin/ai/knowledge/' + kbId + '/version/list',
    method: 'get'
  })
}

// 新增/编辑版本
export function saveKnowledgeBaseVersion(data: any) {
  return request({
    url: '/admin/ai/knowledge/version',
    method: 'post',
    data: data
  })
}

// 发布版本
export function publishKnowledgeBaseVersion(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/version/' + id + '/publish',
    method: 'put'
  })
}

// 归档版本
export function archiveKnowledgeBaseVersion(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/version/' + id + '/archive',
    method: 'put'
  })
}

// 删除版本
export function delKnowledgeBaseVersion(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/version/' + id,
    method: 'delete'
  })
}

// ========== 权限 ==========

// 查询知识库权限列表
export function listKnowledgeBasePermission(kbId: number | string) {
  return request({
    url: '/admin/ai/knowledge/' + kbId + '/permission/list',
    method: 'get'
  })
}

// 授权
export function grantKnowledgeBasePermission(data: any) {
  return request({
    url: '/admin/ai/knowledge/permission',
    method: 'post',
    data: data
  })
}

// 撤销权限
export function revokeKnowledgeBasePermission(id: number | string) {
  return request({
    url: '/admin/ai/knowledge/permission/' + id,
    method: 'delete'
  })
}
