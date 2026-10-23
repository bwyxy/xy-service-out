import request from '@/utils/request'

/**
 * 用户提醒偏好设置
 *
 * 注意：所属用户由后端从 token 解析，前端不要传 userId。
 */

/**
 * 查提醒设置
 *
 * 后端在用户没有记录时返回默认值，所以这里永远拿得到一份完整数据。
 *
 * @returns {Promise<{code:number,msg:string,data:{
 *   taskRemindOn:number,dailyRemindOn:number,dailyRemindTime:string,
 *   focusRemindOn:number,quietStart:string,quietEnd:string
 * }}>}
 */
export function getSetting() {
	return request({ url: '/user/setting', method: 'GET' })
}

/**
 * 修改提醒设置
 *
 * 只更新传了值的字段，所以改开关时可以只传开关，不用把整套字段带上。
 *
 * @param {Object} data 入参（字段同 getSetting 的返回）
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function updateSetting(data) {
	return request({ url: '/user/setting', method: 'PUT', data })
}
