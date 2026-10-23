import request from '@/utils/request'

/**
 * 番茄专注
 *
 * 一轮专注分两次上报：开始时报 start（写一条进行中记录），结束时报 finish（写实际专注秒数与结束状态）。
 * clientId 由前端生成，两次传同一个，后端按 (user_id, client_id) 幂等去重，弱网重试不会重复计数。
 */

/**
 * 今日专注统计
 *
 * @returns {Promise<{code:number,msg:string,data:{
 *   todayCount:number,todayMinutes:number,yesterdayCount:number
 * }>}>} todayCount-今日完成的番茄数（只算完整跑完的专注轮） todayMinutes-今日累计专注分钟 yesterdayCount-昨日番茄数
 */
export function getFocusToday() {
	return request({ url: '/focus/today', method: 'GET' })
}

/**
 * 开始一轮专注
 *
 * @param {Object} data 入参
 * @param {string} data.clientId          客户端幂等 Id（必传，结束时沿用同一个）
 * @param {string} [data.mode]            focus-专注 short-短休息 long-长休息，不传按 focus
 * @param {number} [data.plannedSeconds]  计划秒数，不传按模式默认（专注 1500 / 短休息 300 / 长休息 900）
 * @param {number} [data.taskId]          关联任务 Id，不传表示这轮不挂任务
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function startFocusSession(data) {
	return request({ url: '/focus/session/start', method: 'POST', data })
}

/**
 * 结束一轮专注
 *
 * @param {Object} data 入参
 * @param {string} data.clientId            与 start 同一轮的 clientId（必传）
 * @param {string} data.status              finished-完整跑完一轮 aborted-中途放弃
 * @param {number} data.actualSeconds       实际专注秒数（不含暂停时间）
 * @param {string} [data.mode]              模式，仅在后端没找到 start 记录、需要补一条时用到
 * @param {number} [data.plannedSeconds]    计划秒数，同上
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function finishFocusSession(data) {
	return request({ url: '/focus/session/finish', method: 'POST', data })
}
