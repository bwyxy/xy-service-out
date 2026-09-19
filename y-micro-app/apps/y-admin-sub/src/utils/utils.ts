/**
 * 获取相对今天偏移 N 天的日期
 * @param offsetDays 正数表示未来，负数表示过去
 * @returns 新的 Date 对象（时间归零到当天 00:00:00）
 */
export function getDateOffset(offsetDays: number): Date {
    const base = new Date()
    base.setHours(0, 0, 0, 0) // 归零，避免时区/夏令时影响

    const result = new Date(base.getTime())
    result.setDate(result.getDate() + offsetDays)

    return result
}

/**
 * 将 Date 格式化为 YYYY-MM-DD（本地时区，避免 toISOString 的 UTC 偏移问题）
 */
export function formatDate(date: Date): string {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
}