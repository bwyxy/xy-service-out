import request from '@/utils/request'

/**
 * 待办任务
 *
 * 注意：所属用户由后端从 token 解析，前端不要传 userId。
 * clientId 由页面生成，弱网重试或重复提交时后端按 (user_id, client_id) 幂等去重。
 */

/**
 * 创建任务
 *
 * @param {Object} data 入参
 * @param {string} data.clientId     客户端幂等 Id（必传，重试沿用同一个）
 * @param {string} data.title        任务标题
 * @param {string} [data.description] 备注
 * @param {number} [data.tagId]      所属清单 Id
 * @param {number} [data.priority]   优先级 0-无 1-普通 2-重要
 * @param {number} [data.urgencyLevel] 紧急程度 1~4（四象限）
 * @param {string} [data.dueDate]    截止时间 yyyy-MM-dd HH:mm:ss
 * @param {string} [data.repeatRule] 重复规则
 * @param {number} [data.remindBefore] 提前提醒分钟数，0-不提醒
 * @returns {Promise<{code:number,msg:string,data:{taskId:number,remindAt:string,seq:number}}>}
 */
export function createTask(data) {
	return request({ url: '/task/create', method: 'POST', data })
}

/**
 * 标记任务完成 / 取消完成
 *
 * 传的是目标状态而不是「切换」：弱网重试、连点两次都会按同一个目标状态写入，
 * 后端遇到状态已一致会直接返回成功，不会把状态又翻回去。
 *
 * @param {Object} data 入参
 * @param {number} data.taskId    任务 Id
 * @param {number} data.completed 目标状态 0-未完成 1-已完成
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function completeTask(data) {
	return request({ url: '/task/complete', method: 'POST', data })
}

/**
 * 归档 / 取消归档
 *
 * 与完成接口同一套语义：传目标状态而不是「切换」，只有已完成的任务能归档，
 * 取消归档后它会回到「已完成」分组。
 *
 * @param {Object} data 入参
 * @param {number} data.taskId   任务 Id
 * @param {number} data.archived 目标状态 0-取消归档 1-归档
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function archiveTask(data) {
	return request({ url: '/task/archive', method: 'POST', data })
}

/**
 * 清空已完成任务（后端逻辑删除未归档的已完成任务，已归档的不受影响）
 *
 * @returns {Promise<{code:number,msg:string,data:number}>} data 为清空的条数
 */
export function clearDoneTasks() {
	return request({ url: '/task/clear-done', method: 'POST' })
}

/**
 * 我的页统计
 *
 * 三个数字都由后端算：分页列表拿不到「累计完成」和「连续记录」，
 * 前端也不该为了三个数字各发一次请求。
 *
 * @returns {Promise<{code:number,msg:string,data:{
 *   doneTotal:number, streakDays:number, tagCount:number
 * }>} doneTotal-累计完成（含已归档、已清空的） streakDays-连续记录天数 tagCount-未归档清单数
 */
export function getTaskStats() {
	return request({ url: '/task/stats', method: 'GET' })
}

/**
 * 更新任务
 *
 * 全量覆盖语义：没传的字段按清空处理，所以编辑时要和创建一样把整套字段带上。
 *
 * @param {number} taskId 任务 Id
 * @param {Object} data   入参与 createTask 相同（title 必传，其余可空）
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function updateTask(taskId, data) {
	return request({ url: `/task/${taskId}`, method: 'PUT', data })
}

/**
 * 删除任务（后端逻辑删除，任务不再出现在任何列表里）
 *
 * @param {number} taskId 任务 Id
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function removeTask(taskId) {
	return request({ url: `/task/${taskId}`, method: 'DELETE' })
}

/**
 * 任务详情
 *
 * 详情页用：比列表项多了清单名、创建时间与下次提醒时间。
 * 任务不存在或不属于当前用户时后端直接返回失败。
 *
 * @param {number} taskId 任务 Id
 * @returns {Promise<{code:number,msg:string,data:{
 *   taskId:number,title:string,description:string,tagId:number,tagName:string,
 *   priority:number,urgencyLevel:number,completed:number,completionTime:string,
 *   archived:number,archiveTime:string,dueDate:string,repeatRule:string,
 *   remindBefore:number,remindAt:string,createTime:string
 * }>}>
 */
export function getTaskDetail(taskId) {
	return request({ url: `/task/${taskId}`, method: 'GET' })
}

/**
 * 任务分页列表
 *
 * 排序由后端负责（未完成在前、快到期在前、没设日期的最后），前端不要再本地排序。
 *
 * @param {Object} [data] 入参
 * @param {number} [data.tagId] 清单 Id，不传表示不限清单
 * @param {string} [data.status] 状态筛选 all-全部（默认） pending-待完成 done-已完成
 * @param {string} [data.keyword] 搜索关键词，按任务标题与备注模糊匹配；不传表示不搜索
 * @param {string} [data.date] 到期日 yyyy-MM-dd，取当天任务；不传表示不限日期
 * @param {number} [data.urgencyLevel] 紧急程度 1~4（四象限），不传表示不限象限
 * @param {number} [data.archived] 归档筛选 0-否（默认，主线列表） 1-是（已完成与归档页的「已归档」分组）
 * @param {number} [data.pageNum] 页码，从 1 开始
 * @param {number} [data.pageSize] 每页条数，默认 20、上限 100
 * @returns {Promise<{code:number,msg:string,data:{
 *   list:Array<{taskId:number,title:string,description:string,tagId:number,
 *     priority:number,urgencyLevel:number,completed:number,completionTime:string,
 *     archived:number,archiveTime:string,
 *     dueDate:string,repeatRule:string,remindBefore:number,seq:number}>,
 *   total:number,pageNum:number,pageSize:number,hasMore:boolean,
 *   allCount:number,pendingCount:number,doneCount:number
 * }>} 分页后前端算不出三个筛选的条数，计数以后端返回的 allCount/pendingCount/doneCount 为准
 */
export function listTasks(data) {
	return request({ url: '/task/list', method: 'POST', data })
}

/**
 * 日历视图 · 按月统计
 *
 * 只返回有任务的日子（没有任务的日子不返回），日历点阵与「本月有 N 天」都基于它；
 * 选中日的任务另外走 listTasks 的 date 参数。
 *
 * @param {Object} data 入参
 * @param {string} data.month 月份 yyyy-MM
 * @returns {Promise<{code:number,msg:string,data:{
 *   year:number,month:number,busyDays:number,
 *   days:Array<{date:string,total:number}>
 * }>}>} busyDays-本月有安排的天数 days-每天的任务数，date 为 yyyy-MM-dd
 */
export function getMonthTasks(data) {
	return request({ url: '/task/month', method: 'POST', data })
}

/**
 * 四象限 · 按紧急程度分组
 *
 * 每个象限给「待完成件数 + 前几条预览」（只算未完成、未归档的）；
 * 某一格看全部走 listTasks 的 urgencyLevel 过滤。
 *
 * @returns {Promise<{code:number,msg:string,data:{groups:Array<{
 *   urgencyLevel:number,total:number,tasks:Array<Object>
 * }>}}>} groups 顺序固定：重要且紧急 → 重要不紧急 → 紧急不重要 → 不重要不紧急
 */
export function getQuadrantTasks() {
	return request({ url: '/task/quadrant', method: 'POST' })
}
