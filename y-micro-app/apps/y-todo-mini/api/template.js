import request from '@/utils/request'

/**
 * 待办清单模板
 *
 * 对应后端 TemplateController（/template）。
 * 模板是全局数据，不需要登录也能拉列表；套用会真正建清单和任务，需要登录。
 */

/**
 * 模板列表
 *
 * @returns {Promise<{code:number,msg:string,data:Array<{
 *   templateId:number,code:string,name:string,description:string,
 *   category:string,icon:string,itemCount:number
 * }>}>} itemCount 为模板预置的任务条数
 */
export function listTemplates() {
	return request({ url: '/template/list', method: 'GET' })
}

/**
 * 套用模板：新建一个清单 + 按模板预置任务
 *
 * @param {string} code 模板编码
 * @param {Object} [data] 入参
 * @param {string} [data.batchId] 幂等批次号，同一次点击传同一个值，弱网重试不会建出第二个清单
 * @returns {Promise<{code:number,msg:string,data:{
 *   tagId:number,tagName:string,taskCount:number
 * }>}>} tagName 是后端定下来的清单名（重名时加了序号），提示要用它而不是模板名
 */
export function applyTemplate(code, data) {
	return request({ url: `/template/${code}/apply`, method: 'POST', data })
}
