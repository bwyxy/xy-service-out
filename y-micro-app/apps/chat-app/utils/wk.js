/**
 * 悟空 IM 消息 payload 解码
 *
 * 悟空 IM 在不同场景下回传的 payload 格式可能不同：
 * - easyjssdk 已解码为对象（RecvMessage.payload）
 * - 后端 HTTP 接口返回 Base64 编码的 JSON
 * - SDK 推送的消息可能直接是 JSON 字符串
 */

/**
 * 解码 payload
 *
 * 优先识别对象；否则尝试直接 JSON.parse；失败后再尝试 Base64 解码。
 *
 * @param payload 已解码的对象、Base64 编码的 JSON 字符串，或直接的 JSON 字符串
 * @returns 解析后的消息负载对象；解析失败时回退返回 { type: 1, content: payload }
 */
export function decodePayload(payload) {
  if (!payload) {
    return { type: 1, content: '' }
  }

  // easyjssdk 在派发消息前已将 payload 解码为对象，必须直接返回，
  // 否则会误入字符串分支兜底成 { type: 1 }，导致消息类型丢失。
  if (typeof payload === 'object') {
    return payload
  }

  if (typeof payload !== 'string') {
    return { type: 1, content: '' }
  }

  try {
    const parsed = JSON.parse(payload)
    if (parsed && typeof parsed === 'object') {
      return parsed
    }
  } catch {
    // 不是明文 JSON，继续尝试 Base64
  }

  try {
    return JSON.parse(atob(payload))
  } catch (error) {
    console.error('[WukongIM] payload 解码失败:', error)
    return { type: 1, content: payload }
  }
}

/**
 * 从 payload 中安全提取消息文本内容
 *
 * 兼容 { content } 与 { message } 两种字段命名；若本身是对象则序列化。
 */
export function extractMessageContent(payload) {
  if (!payload) {
    return ''
  }

  const raw = payload.content ?? payload.message

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
 * 解码 Java 后端返回的 Base64 字符串（兼容中文）
 *
 * Java 默认使用 UTF-8 生成 Base64，atob 得到的是 Latin1 字节序列，
 * 需要转换为 Uint8Array 后再以 UTF-8 解码。
 */
export function decodeBase64FromJava(base64Str) {
  const binaryString = atob(base64Str)
  const bytes = new Uint8Array(binaryString.length)

  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }

  return new TextDecoder('utf-8').decode(bytes)
}
