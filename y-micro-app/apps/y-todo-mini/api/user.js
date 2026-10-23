import { BASE_API_URL } from '@/utils/config'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

/**
 * 个人资料
 *
 * 注意：所属用户由后端从 token 解析，前端不要传 uId。
 */

/**
 * 修改个人资料（只传要改的字段：nickname / avatar / sex）
 *
 * @param {{nickname?:string, avatar?:string, sex?:string}} data
 * @returns {Promise<{code:number,msg:string,data:null}>}
 */
export function updateProfile(data) {
	return request({ url: '/profile', method: 'PUT', data })
}

/**
 * 上传图片（头像），返回后端信封，data 为可直接访问的图片地址
 *
 * uni.uploadFile 不走 utils/request 的封装，因此这里自己拼绝对地址并带上登录态头，
 * 同时把返回内容解析成和 request 一样的信封，调用方只需判 code。
 *
 * @param {string} filePath 本地临时文件路径
 * @returns {Promise<{code:number,msg:string,data:string}>}
 */
export function uploadImage(filePath) {
	const token = getToken()

	return new Promise((resolve, reject) => {
		uni.uploadFile({
			url: `${BASE_API_URL.replace(/\/+$/, '')}/upload/image`,
			filePath,
			name: 'file',
			header: token ? { token } : {},
			success: (res) => {
				try {
					resolve(JSON.parse(res.data))
				} catch (error) {
					reject(new Error('上传返回内容解析失败'))
				}
			},
			fail: (error) => reject(error)
		})
	})
}
