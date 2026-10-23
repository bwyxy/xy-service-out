import request from '@/utils/request'

/**
 * 习惯打卡
 *
 * 注意：所属用户由后端从 token 解析，前端不要传 userId。
 * 频率 freqType 为 daily（每天）或 weekly（按周，配合 freqDays 传星期集合，1-周一 ~ 7-周日）。
 * 打卡接口是幂等的「开关」：同一习惯同一天再调一次即取消打卡。
 */

/**
 * 习惯列表
 *
 * @returns {Promise<{code:number,msg:string,data:Array<{
 *   habitId:number,name:string,icon:string,color:string,
 *   freqType:string,freqDays:number[],targetDays:number,sortNo:number,status:number,
 *   streak:number,weekDays:boolean[],createTime:string
 * }>}>} weekDays 长度 7，索引 0 为周一，表示本周实际打卡情况
 */
export function listHabits() {
	return request({ url: '/habit/list', method: 'GET' })
}

/**
 * 新建习惯
 *
 * @param {Object} data 入参
 * @param {string}   data.name       习惯名称（必传，最长 64 字）
 * @param {string}   [data.icon]     图标名（对应 y-icon 的 name）
 * @param {string}   [data.color]    主题色
 * @param {string}   [data.freqType] daily / weekly，不传按 daily
 * @param {number[]} [data.freqDays] weekly 时的星期集合，1-周一 ~ 7-周日
 * @param {number}   [data.targetDays] 目标天数，0 或不传表示不限
 * @param {number}   [data.sortNo]   排序
 * @returns {Promise<{code:number,msg:string,data:Object}>}
 */
export function createHabit(data) {
	return request({ url: '/habit/create', method: 'POST', data })
}

/**
 * 修改习惯
 *
 * @param {number} habitId 习惯 Id
 * @param {Object} data    同 createHabit 的入参
 */
export function updateHabit(habitId, data) {
	return request({ url: `/habit/${habitId}`, method: 'PUT', data })
}

/**
 * 删除习惯（历史打卡记录保留）
 *
 * @param {number} habitId 习惯 Id
 */
export function removeHabit(habitId) {
	return request({ url: `/habit/${habitId}`, method: 'DELETE' })
}

/**
 * 打卡 / 取消打卡
 *
 * @param {number} habitId  习惯 Id
 * @param {string} [logDate] 打卡日期 yyyy-MM-dd，不传表示今天
 * @returns {Promise<{code:number,msg:string,data:Object}>} data 为该习惯最新状态（含 streak、weekDays）
 */
export function checkHabit(habitId, logDate) {
	return request({ url: `/habit/${habitId}/check`, method: 'POST', data: logDate ? { logDate } : {} })
}
