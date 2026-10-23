import request from '@/utils/request'

/**
 * 待办清单
 *
 * 注意：所属用户由后端从 token 解析，前端不要传 userId。
 * 清单重名由后端拦截，同名会返回失败提示。
 */

/**
 * 清单列表
 *
 * @returns {Promise<{code:number,msg:string,data:Array<{
 *   tagId:number,tagName:string,icon:string,color:string,
 *   sortNo:number,isDefault:number,pendingCount:number,createTime:string
 * }>}>}
 */
export function listTags() {
	return request({ url: '/tag/list', method: 'GET' })
}

/**
 * 新建清单
 *
 * @param {Object} data 入参
 * @param {string} data.tagName 清单名称（必传，最长 24 字）
 * @param {string} [data.icon]  图标名（对应 y-icon 的 name）
 * @param {string} [data.color] 主题色
 * @param {number} [data.sortNo] 排序
 * @returns {Promise<{code:number,msg:string,data:Object}>}
 */
export function createTag(data) {
	return request({ url: '/tag/create', method: 'POST', data })
}

/**
 * 修改清单
 *
 * @param {number} tagId 清单 Id
 * @param {Object} data  同 createTag 的入参
 */
export function updateTag(tagId, data) {
	return request({ url: `/tag/${tagId}`, method: 'PUT', data })
}

/**
 * 删除清单（清单下的任务会变为未归类，不会被一起删除）
 *
 * @param {number} tagId 清单 Id
 */
export function removeTag(tagId) {
	return request({ url: `/tag/${tagId}`, method: 'DELETE' })
}
