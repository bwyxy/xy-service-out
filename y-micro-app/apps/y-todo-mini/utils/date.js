/**
 * 日期与时间展示工具
 *
 * 后端统一返回 `yyyy-MM-dd HH:mm:ss` 字符串，小程序端（尤其 iOS）只认 `yyyy/MM/dd HH:mm:ss`，
 * 所以解析一律走 parseDateTime 做分隔符替换，不要直接 new Date(字符串)。
 */

/** 周标签，索引 0 为周一 */
const WEEK_CHARS = ['一', '二', '三', '四', '五', '六', '日']

/** 补零 */
export function pad2(value) {
	return String(value).padStart(2, '0')
}

/**
 * 解析后端时间字符串为 Date
 *
 * @param {string} value yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
 * @returns {Date}
 */
export function parseDateTime(value) {
	return new Date(String(value).replace(/-/g, '/'))
}

/**
 * 后端时间字符串 → 时间戳
 *
 * @param {string} value 后端时间字符串，空值返回 null（表示没设日期）
 * @returns {number|null}
 */
export function toTimestamp(value) {
	return value ? parseDateTime(value).getTime() : null
}

/** 当天零点，用于按自然日比较而不是按 24 小时 */
function startOfDay(date) {
	return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

/**
 * 自然日差值：今天 0、明天 1、昨天 -1
 *
 * @param {Date} date
 * @returns {number}
 */
export function dayDiff(date) {
	return Math.round((startOfDay(date) - startOfDay(new Date())) / 86400000)
}

/**
 * 相对日文案：今天 / 明天 / 昨天 / 周三
 *
 * @param {Date} date
 * @returns {string}
 */
export function dayLabel(date) {
	const diff = dayDiff(date)

	if (diff === 0) {
		return '今天'
	}

	if (diff === 1) {
		return '明天'
	}

	if (diff === -1) {
		return '昨天'
	}

	return `周${WEEK_CHARS[(date.getDay() + 6) % 7]}`
}

/**
 * 周内单字标签：一 / 二 / … / 日
 *
 * 统计页柱状图轴标签用（位置只有一格宽，放不下「周三」）；
 * 与周起始偏好无关，纯粹是这一天本身是星期几。
 *
 * @param {string} value yyyy-MM-dd
 * @returns {string}
 */
export function weekdayChar(value) {
	return WEEK_CHARS[(parseDateTime(value).getDay() + 6) % 7]
}

/**
 * 日期文案：今天，10月23日
 *
 * @param {string} value yyyy-MM-dd
 * @returns {string}
 */
export function formatDateLabel(value) {
	const date = parseDateTime(value)

	return `${dayLabel(date)}，${date.getMonth() + 1}月${date.getDate()}日`
}

/**
 * 时间点文案：HH:mm
 *
 * @param {number} dueTime 到期时间戳，空值返回空串（表示没设时间）
 * @returns {string}
 */
export function formatClock(dueTime) {
	if (!dueTime) {
		return ''
	}

	const date = new Date(dueTime)

	return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

/**
 * 历史时间文案：今天 08:15 / 昨天 17:40 / 10月20日 10:00 / 2024年10月20日 10:00
 *
 * 用于「已完成与归档」记录行：刚完成的看相对日，久远的看具体日期，跨年才补年份。
 *
 * @param {string} value 后端时间字符串 yyyy-MM-dd HH:mm:ss
 * @returns {string} 空值返回空串
 */
export function formatHistoryLabel(value) {
	if (!value) {
		return ''
	}

	const date = parseDateTime(value)
	const diff = dayDiff(date)
	const clock = formatClock(date.getTime())

	if (diff === 0 || diff === -1) {
		return `${dayLabel(date)} ${clock}`
	}

	const year = date.getFullYear() === new Date().getFullYear() ? '' : `${date.getFullYear()}年`

	return `${year}${date.getMonth() + 1}月${date.getDate()}日 ${clock}`
}

/**
 * 任务到期文案
 *
 * 未完成：今天 · 10:00 / 明天 · 10:00 / 10月25日 · 周日；没设日期为「未设置日期」。
 * 已完成：今天已完成 / 昨天已完成 / 10月19日已完成。
 *
 * @param {number} dueTime 到期时间戳
 * @param {boolean} done    是否已完成
 * @returns {string}
 */
export function formatDueLabel(dueTime, done) {
	if (!dueTime) {
		return '未设置日期'
	}

	const date = new Date(dueTime)
	const diff = dayDiff(date)

	if (done) {
		return diff === 0 || diff === -1
			? `${dayLabel(date)}已完成`
			: `${date.getMonth() + 1}月${date.getDate()}日已完成`
	}

	const time = formatClock(dueTime)

	return diff === 0 || diff === 1
		? `${dayLabel(date)} · ${time}`
		: `${date.getMonth() + 1}月${date.getDate()}日 · ${dayLabel(date)}`
}
