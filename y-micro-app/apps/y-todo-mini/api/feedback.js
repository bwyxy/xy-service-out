import request from '@/utils/request'

/**
 * 意见反馈
 *
 * 注意：提交人由后端从 token 解析，前端不要传 userId。
 */

/**
 * 提交一条意见反馈
 *
 * @param {{content:string, contact?:string, images?:string[]}} data
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function submitFeedback(data) {
	return request({ url: '/feedback', method: 'POST', data })
}
