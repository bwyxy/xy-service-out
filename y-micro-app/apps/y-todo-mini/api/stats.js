import request from '@/utils/request'

/**
 * 客户端待办统计接口
 *
 * 对应后端 StatsController（/stats），与待办任务接口分文件放，
 * 统计是只读聚合，和任务增删改的调用点、失败处理都不一样。
 */

/**
 * 本周统计
 *
 * 概览三项（本周完成 / 完成率 / 连续记录）与柱状图的每日完成数一次返回，
 * 缺完成记录的日子后端已补 0，前端不用再对齐日期。
 *
 * @param {number} [weekStart] 周起始 0-周日 1-周一，跟外观设置里的周起始保持一致
 * @returns {Promise<{code:number,msg:string,data:{
 *   startDate:string,endDate:string,total:number,rate:number,streakDays:number,
 *   days:Array<{date:string,total:number}>
 * }>} startDate/endDate 为本周首尾日 rate 完成率 0~100
 */
export function getWeekStats(weekStart) {
	return request({ url: '/stats/week', method: 'GET', params: { weekStart } })
}

/**
 * 清单分布
 *
 * @returns {Promise<{code:number,msg:string,data:Array<{
 *   tagId:number,tagName:string,color:string,total:number
 * }>}>} tagId 为 null 表示未归类；color 可能为空串，由前端补默认色；按任务数倒序
 */
export function getTagDistribution() {
	return request({ url: '/stats/distribution', method: 'GET' })
}
