import { reportSubscribe } from '@/api/notify'
import { REMINDER_TEMPLATE_ID } from './config'

/**
 * 微信一次性订阅消息授权
 *
 * 一次性订阅的规则是「授权一次 = 后端只能下发一条」，所以授权与上报必须成对做：
 * 先 requestRemindSubscribe 拿本次授权条数，再 reportAcceptedCount 记账，否则后端额度为 0、提醒永远发不出。
 */

/**
 * 请求一次性订阅授权
 *
 * 返回本次拿到的授权条数：accept 记 1 条，拒绝 / 拉起失败一律记 0，绝不抛错打断调用方主流程。
 * 用户勾了「总是保持以上选择」时微信不再弹窗但同样返回 accept，照记。
 *
 * @returns {Promise<number>} 本次新增的授权条数 0 或 1
 */
export function requestRemindSubscribe() {
	return new Promise((resolve) => {
		try {
			uni.requestSubscribeMessage({
				tmplIds: [REMINDER_TEMPLATE_ID],
				success: (res) => resolve(res[REMINDER_TEMPLATE_ID] === 'accept' ? 1 : 0),
				fail: (error) => {
					console.warn('[subscribe] 订阅授权失败', error)
					resolve(0)
				}
			})
		} catch (error) {
			// H5 / 非微信端没有这个 api，直接当作没授权
			console.warn('[subscribe] 当前环境不支持订阅消息', error)
			resolve(0)
		}
	})
}

/**
 * 上报授权条数
 *
 * 失败只记日志：额度少一条比调用方主流程失败轻得多。
 *
 * @param {number} count 本次新增的授权条数
 * @returns {Promise<void>}
 */
export async function reportAcceptedCount(count) {
	try {
		await reportSubscribe({ templateId: REMINDER_TEMPLATE_ID, count })
	} catch (error) {
		console.warn('[subscribe] 上报订阅授权失败', error)
	}
}
