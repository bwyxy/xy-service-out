import request from '@/utils/request'

/**
 * 微信订阅消息额度
 *
 * 一次性订阅的规则是「用户授权一次 = 开发者只能下发一条」，
 * 所以前端在小程序里拿到授权后必须上报，由后端记账。
 */

/**
 * 上报订阅授权结果
 *
 * @param {Object} data 入参
 * @param {string} data.templateId 模板 Id（必须是后端配置的那个）
 * @param {number} data.count      本次新增的授权次数
 * @returns {Promise<{code:number,msg:string,data:{templateId:string,remain:number,total:number}}>}
 */
export function reportSubscribe(data) {
	return request({ url: '/notify/subscribe', method: 'POST', data })
}

/**
 * 查剩余可下发条数
 *
 * @returns {Promise<{code:number,msg:string,data:{templateId:string,remain:number,total:number}}>}
 */
export function getQuota() {
	return request({ url: '/notify/quota', method: 'GET' })
}
