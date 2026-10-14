import type { WukongIMMessagePayload } from '@/services/im'

// 临时版本标记：用于确认前端是否已加载本次修复（对象 payload 兼容）。确认无误后可删除。
console.log('[WK] 消息解码器 v2 已加载（兼容对象 payload）')

/**
 * 解码悟空 IM 消息的 payload
 *
 * 悟空 IM 在不同场景下回传的 payload 格式可能不同：
 * - easyjssdk 已解码为对象（RecvMessage.payload）
 * - 后端 HTTP 接口返回 Base64 编码的 JSON
 * - SDK 推送的消息可能直接是 JSON 字符串
 *
 * 因此本函数优先识别对象；否则尝试直接 JSON.parse；失败后再尝试 Base64 解码。
 *
 * @param payload 已解码的对象、Base64 编码的 JSON 字符串，或直接的 JSON 字符串
 * @returns 解析后的消息负载对象；解析失败时回退返回 { type: 1, content: payload }
 */
export function decodePayload(payload: unknown): WukongIMMessagePayload {
  if (!payload) {
    return { type: 1, content: '' }
  }

  // 0. easyjssdk 在派发消息前已将 payload 解码为对象（RecvMessage.payload 为 any）。
  //    必须直接返回，否则会误入字符串分支兜底成 { type: 1 }，导致消息类型丢失
  //    （表现为 RTC 信令被当作普通聊天消息、通话记录 type 判定失效）。
  if (typeof payload === 'object') {
    return payload as WukongIMMessagePayload
  }

  if (typeof payload !== 'string') {
    return { type: 1, content: '' }
  }

  // 1. 尝试直接作为 JSON 字符串解析
  try {
    const parsed = JSON.parse(payload)
    if (parsed && typeof parsed === 'object') {
      return parsed as WukongIMMessagePayload
    }
  } catch {
    // 不是明文 JSON，继续尝试 Base64
  }

  // 2. 尝试 Base64 解码后再解析
  try {
    const jsonStr = atob(payload)
    return JSON.parse(jsonStr) as WukongIMMessagePayload
  } catch (error) {
    console.error('[WukongIM] payload 解码失败:', error)
    return { type: 1, content: payload }
  }
}

/**
 * 从 payload 中安全提取消息文本内容
 *
 * 兼容两种字段命名：
 * - { type: 1, content: '文本' }
 * - { type: 1, message: '文本' }
 *
 * 若 content/message 本身是对象，会尝试 JSON.stringify；失败则返回空字符串。
 */
export function extractMessageContent(payload: WukongIMMessagePayload | null | undefined): string {
  if (!payload) {
    return ''
  }

  const raw = payload.content ?? (payload as unknown as { message?: unknown }).message

  if (raw === undefined || raw === null) {
    return ''
  }

  if (typeof raw === 'string') {
    return raw
  }

  try {
    return JSON.stringify(raw)
  } catch {
    return String(raw)
  }
}

/**
 * 解码 Java 后端返回的 Base64 字符串（兼容中文字符）
 *
 * Java 默认使用 UTF-8 编码生成 Base64，atob 得到的是 Latin1 字节序列，
 * 需要手动转换为 Uint8Array 后再以 UTF-8 解码，才能正确处理中文。
 *
 * @param base64Str Java Base64 编码字符串
 * @returns UTF-8 解码后的文本
 */
export function decodeBase64FromJava(base64Str: string): string {
  const binaryString = atob(base64Str)
  const bytes = new Uint8Array(binaryString.length)

  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }

  return new TextDecoder('utf-8').decode(bytes)
}
