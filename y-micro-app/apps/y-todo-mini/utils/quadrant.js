/**
 * 四象限（紧急程度）定义
 *
 * 与后端 urgency_level 一一对应：1-不重要不紧急 2-紧急不重要 3-重要不紧急 4-重要且紧急。
 * 顺序按「该先做什么」排：重要且紧急最先，不重要不紧急最后；四象限页、象限任务页与新建页共用这一份，避免各处漂移。
 */

/** 四个象限：level 即后端紧急程度，key 只用于列表渲染 */
export const QUADRANTS = [
	{ level: 4, key: 'urgent-important', name: '重要且紧急', tip: '立刻去做', color: '#c2793c', bg: '#faf1e7' },
	{ level: 3, key: 'important', name: '重要不紧急', tip: '安排时间', color: '#4a6b3e', bg: '#eaf0e0' },
	{ level: 2, key: 'urgent', name: '紧急不重要', tip: '尽快处理', color: '#a98a2f', bg: '#f6f2e2' },
	{ level: 1, key: 'other', name: '不重要不紧急', tip: '有空再说', color: '#8e8e85', bg: '#f0f0ea' }
]

/** 没设紧急程度时的兜底档位，与后端 validateUrgencyLevel 的默认值保持一致 */
export const DEFAULT_URGENCY_LEVEL = 1

/**
 * 紧急程度 → 象限定义
 *
 * @param {number} level 1~4
 * @returns {Object} 象限定义；取不到时给最末一档，保证页面标题、配色都有值
 */
export function quadrantOf(level) {
	return QUADRANTS.find((item) => item.level === Number(level))
		|| QUADRANTS[QUADRANTS.length - 1]
}
