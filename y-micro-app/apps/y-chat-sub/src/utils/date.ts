/** 聊天消息时间格式化 */

const DAY_MS = 24 * 60 * 60 * 1000

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

export interface MessageTimeOptions {
  /**
   * 非今天的日期使用数字格式
   *
   * 聊天列表用「6月3日」，搜索结果用「06-03」。
   */
  numericDate?: boolean
}

/**
 * 消息时间展示
 *
 * 今天只显示 HH:mm，昨天带「昨天」，更早显示日期 + 时间。
 */
export function formatMessageTime(timestamp: number, options: MessageTimeOptions = {}): string {
  if (!timestamp) return ''

  const date = new Date(timestamp)
  const now = new Date()
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`

  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const startOfThatDay = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()

  if (startOfThatDay === startOfToday) {
    return time
  }

  if (startOfToday - startOfThatDay === DAY_MS) {
    return `昨天 ${time}`
  }

  const month = date.getMonth() + 1
  const day = date.getDate()

  if (date.getFullYear() === now.getFullYear()) {
    return options.numericDate
      ? `${pad(month)}-${pad(day)} ${time}`
      : `${month}月${day}日 ${time}`
  }

  return options.numericDate
    ? `${date.getFullYear()}-${pad(month)}-${pad(day)} ${time}`
    : `${date.getFullYear()}年${month}月${day}日 ${time}`
}
