/**
 * 图标表
 *
 * 统一 24×24 视窗的细线描边风格，与设计稿线稿图标一致。
 * value 为 SVG 内容片段，其中 `@c` 会被替换为实际颜色（用于需要实心的小圆点/色块）。
 */
export const ICON_PATHS = {
	/* 通用 */
	'arrow-right': '<line x1="4" y1="12" x2="19" y2="12"/><polyline points="13,6 19,12 13,18"/>',
	'chevron-right': '<polyline points="9,5 16,12 9,19"/>',
	'chevron-left': '<polyline points="15,5 8,12 15,19"/>',
	check: '<polyline points="4,12.5 9.5,18 20,6.5"/>',
	plus: '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>',

	/* 工具页 */
	calendar:
		'<rect x="3.5" y="5.5" width="17" height="15" rx="3"/><line x1="3.5" y1="10.5" x2="20.5" y2="10.5"/><line x1="8" y1="3" x2="8" y2="7"/><line x1="16" y1="3" x2="16" y2="7"/>',
	grid: '<rect x="4" y="4" width="7" height="7" rx="2"/><rect x="13" y="4" width="7" height="7" rx="2"/><rect x="4" y="13" width="7" height="7" rx="2"/><rect x="13" y="13" width="7" height="7" rx="2"/>',
	chart:
		'<line x1="6" y1="20" x2="6" y2="11"/><line x1="12" y1="20" x2="12" y2="5"/><line x1="18" y1="20" x2="18" y2="15"/>',
	'file-text':
		'<rect x="5" y="3" width="14" height="18" rx="3"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="13" y2="16"/>',

	/* 清单分类 */
	briefcase:
		'<rect x="3" y="7" width="18" height="13" rx="3"/><path d="M8.5 7V5.5a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2V7"/><line x1="3" y1="12" x2="21" y2="12"/>',
	coffee:
		'<path d="M4 8h13v6a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5Z"/><path d="M17 10h1.5a2.5 2.5 0 0 1 0 5H17"/><line x1="3" y1="22" x2="19" y2="22"/>',
	book: '<path d="M12 6.5C10.5 4.8 8 4 4 4v13.5c4 0 6.5 1 8 2.5 1.5-1.5 4-2.5 8-2.5V4c-4 0-6.5.8-8 2.5Z"/><line x1="12" y1="6.5" x2="12" y2="20"/>',
	sprout:
		'<path d="M12 21v-8"/><path d="M12 13c0-4-2.8-6.5-7-6.5 0 4 3 6.5 7 6.5Z"/><path d="M12 13c0-3 2.4-4.7 5.5-4.7 0 3-2.2 4.7-5.5 4.7Z"/>',

	/* 我的 */
	bell: '<path d="M6 17v-5.5a6 6 0 0 1 12 0V17"/><line x1="4" y1="17" x2="20" y2="17"/><line x1="10" y1="20" x2="14" y2="20"/>',
	contrast:
		'<circle cx="12" cy="12" r="8.5"/><path d="M12 3.5a8.5 8.5 0 0 1 0 17Z" fill="@c" stroke="none"/>',
	refresh:
		'<polyline points="22,4 22,10 16,10"/><polyline points="2,20 2,14 8,14"/><path d="M4.5 9a8.5 8.5 0 0 1 13.6-3.1L22 10"/><path d="M19.5 15a8.5 8.5 0 0 1-13.6 3.1L2 14"/>',
	archive:
		'<rect x="3" y="3.5" width="18" height="4.5" rx="1.5"/><path d="M4.5 8v11a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V8"/><line x1="10" y1="12.5" x2="14" y2="12.5"/>',
	help: '<circle cx="12" cy="12" r="8.5"/><path d="M9.6 9.6a2.5 2.5 0 1 1 3.2 2.4c-.8.3-.9.9-.9 1.6v.4"/><circle cx="12" cy="17" r="1" fill="@c" stroke="none"/>',
	info: '<circle cx="12" cy="12" r="8.5"/><line x1="12" y1="11" x2="12" y2="16.5"/><circle cx="12" cy="7.8" r="1" fill="@c" stroke="none"/>',

	/* 新建任务 */
	folder:
		'<path d="M3 7a2.5 2.5 0 0 1 2.5-2.5h3.2l2 2.5h7.8A2.5 2.5 0 0 1 21 9.5v7.5a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 17Z"/>',
	clock: '<circle cx="12" cy="12" r="8.5"/><polyline points="12,7 12,12.5 16,15"/>',
	repeat:
		'<polyline points="16.5,2.5 20.5,6.5 16.5,10.5"/><path d="M3.5 12.5v-2a4 4 0 0 1 4-4h13"/><polyline points="7.5,21.5 3.5,17.5 7.5,13.5"/><path d="M20.5 11.5v2a4 4 0 0 1-4 4h-13"/>',
	flag: '<line x1="5" y1="21" x2="5" y2="3.5"/><path d="M5 4.5h12l-1.8 4 1.8 4H5"/>',
	dash: '<line x1="6" y1="12" x2="18" y2="12"/>',
	align: '<line x1="4" y1="6.5" x2="20" y2="6.5"/><line x1="4" y1="12" x2="14" y2="12"/><line x1="4" y1="17.5" x2="20" y2="17.5"/>',
	pencil: '<path d="M4 20h4l11-11-4-4L4 16Z"/><line x1="14.5" y1="5.5" x2="18.5" y2="9.5"/>',

	/* 搜索 */
	search: '<circle cx="11" cy="11" r="7"/><line x1="16.2" y1="16.2" x2="21" y2="21"/>',
	'close-circle':
		'<circle cx="12" cy="12" r="8.5"/><line x1="9.5" y1="9.5" x2="14.5" y2="14.5"/><line x1="14.5" y1="9.5" x2="9.5" y2="14.5"/>',
	bulb: '<path d="M9 15.5a5.5 5.5 0 1 1 6 0v2H9Z"/><line x1="10" y1="20.5" x2="14" y2="20.5"/>',

	/* 番茄专注 */
	play: '<path d="M8 5.5 18.5 12 8 18.5Z"/>',
	pause: '<line x1="9.5" y1="5" x2="9.5" y2="19"/><line x1="14.5" y1="5" x2="14.5" y2="19"/>',
	stop: '<rect x="6" y="6" width="12" height="12" rx="3"/>',
	rotate: '<polyline points="20,6 20,11 15,11"/><path d="M19.4 15a8 8 0 1 1-1.8-8.6L20 11"/>',

	/* 习惯打卡 */
	flame:
		'<path d="M12 21c3.6 0 6-2.3 6-5.5 0-3.7-3.2-5.7-4.2-9.5-2 1.6-2.6 3.6-2.6 5.1-1 0-1.8-.9-2-2C7.4 10.6 6 12.6 6 15.5 6 18.7 8.4 21 12 21Z"/>',
	star: '<path d="M12 3.5l2.7 5.6 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1L3.2 10l6.1-.9Z"/>',
	list: '<line x1="9" y1="7" x2="20" y2="7"/><line x1="9" y1="12" x2="20" y2="12"/><line x1="9" y1="17" x2="20" y2="17"/><circle cx="5" cy="7" r="1.3" fill="@c" stroke="none"/><circle cx="5" cy="12" r="1.3" fill="@c" stroke="none"/><circle cx="5" cy="17" r="1.3" fill="@c" stroke="none"/>',

	/* 外观与同步 */
	moon: '<path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z"/>',
	sun: '<circle cx="12" cy="12" r="4.5"/><line x1="12" y1="2.5" x2="12" y2="5"/><line x1="12" y1="19" x2="12" y2="21.5"/><line x1="2.5" y1="12" x2="5" y2="12"/><line x1="19" y1="12" x2="21.5" y2="12"/><line x1="5.6" y1="5.6" x2="7.4" y2="7.4"/><line x1="16.6" y1="16.6" x2="18.4" y2="18.4"/><line x1="18.4" y1="5.6" x2="16.6" y2="7.4"/><line x1="7.4" y1="16.6" x2="5.6" y2="18.4"/>',
	cloud: '<path d="M7.5 19h9.5a4 4 0 0 0 .4-8 5.5 5.5 0 0 0-10.6 1.3A3.6 3.6 0 0 0 7.5 19Z"/>',

	/* 个人资料与反馈 */
	user: '<circle cx="12" cy="8.5" r="4"/><path d="M4.5 20.5a7.5 7.5 0 0 1 15 0"/>',
	camera:
		'<path d="M4 8.5h3l1.5-2h7l1.5 2h3a1.5 1.5 0 0 1 1.5 1.5v7.5A1.5 1.5 0 0 1 20 19H4a1.5 1.5 0 0 1-1.5-1.5V10A1.5 1.5 0 0 1 4 8.5Z"/><circle cx="12" cy="13.5" r="3.5"/>',
	message:
		'<path d="M20.5 12.5c0 4-3.8 7-8.5 7-1 0-2-.1-2.9-.4L4.5 21l1.3-3.4A6.9 6.9 0 0 1 3.5 12.5c0-4 3.8-7 8.5-7s8.5 3 8.5 7Z"/>',
	trash:
		'<polyline points="4,7 20,7"/><path d="M9.5 7V4.5h5V7"/><path d="M6.5 7l1 13h9l1-13"/><line x1="10" y1="11" x2="10" y2="16.5"/><line x1="14" y1="11" x2="14" y2="16.5"/>'
}

/** 描边宽度（基于 24×24 视窗） */
const STROKE_WIDTH = 1.7

/**
 * 生成可直接用于 background-image 的 SVG data uri
 *
 * @param {string} name 图标名
 * @param {string} color 描边颜色
 * @returns {string} 编码后的 data uri，未匹配到图标时返回空串
 */
export function buildIcon(name, color) {
	const body = ICON_PATHS[name]

	if (!body) {
		return ''
	}

	const svg =
		`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"` +
		` stroke="${color}" stroke-width="${STROKE_WIDTH}" stroke-linecap="round"` +
		` stroke-linejoin="round">${body.replace(/@c/g, color)}</svg>`

	return `data:image/svg+xml,${encodeURIComponent(svg)}`
}
