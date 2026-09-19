import request from '@/utils/request'

// 查询待办任务列表
export function listTask(query?: any) {
  return request({
    url: '/todo/task/list',
    method: 'post',
    data: query
  })
}

// 根据任务ID查询详情
export function getTask(taskId: number | string) {
  return request({
    url: '/todo/task/' + taskId,
    method: 'get'
  })
}

// 新增待办任务
export function addTask(data: any) {
  return request({
    url: '/todo/task',
    method: 'post',
    data: data
  })
}

// 修改待办任务
export function updateTask(data: any) {
  return request({
    url: '/todo/task',
    method: 'put',
    data: data
  })
}

// 删除待办任务
export function delTask(taskId: number | string) {
  return request({
    url: '/todo/task/' + taskId,
    method: 'delete'
  })
}

// 查询日程表任务
export function listScheduleTasks(params: { start: string; end: string }) {
  return request({
    url: '/todo/task/schedule',
    method: 'get',
    params
  })
}

// 查询

// 查询任务统计
export function getTaskStatistics() {
  return request({
    url: '/todo/task/statistics',
    method: 'get'
  })
}
